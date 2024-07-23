package run.halo.gradle.openapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Volume;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import run.halo.gradle.Assert;
import run.halo.gradle.Constant;
import run.halo.gradle.FileUtils;
import run.halo.gradle.HaloExtension;
import run.halo.gradle.HaloPluginExtension;
import run.halo.gradle.docker.DockerExistingImage;
import run.halo.gradle.docker.FrameConsumerResultCallback;
import run.halo.gradle.docker.OutputFrame;
import run.halo.gradle.docker.ToStringConsumer;

@Getter
@Slf4j
public class OpenApiDocsGeneratorTask extends DockerExistingImage {
    public static final String CONTAINER_NAME = "halo-openapi-docs-generator";
    public static final String TASK_NAME = "generateOpenApiDocs";

    @Internal
    final HaloPluginExtension pluginExtension =
        getProject().getExtensions().getByType(HaloPluginExtension.class);

    @Input
    @Optional
    final Property<String> platform = getProject().getObjects().property(String.class);

    @Input
    @Optional
    final MapProperty<String, String> requestHeaders = getProject().getObjects()
        .mapProperty(String.class, String.class);

    @Input
    final MapProperty<String, String> groupedApiMappings = getProject().getObjects()
        .mapProperty(String.class, String.class);

    @Input
    @Optional
    final Property<String> trustStore = getProject().getObjects().property(String.class);

    @Input
    @Optional
    final Property<char[]> trustStorePassword = getProject().getObjects().property(char[].class);

    @Input
    final Property<String> apiDocsVersion = getProject().getObjects().property(String.class);

    @Internal
    final Property<Integer> waitTimeInSeconds = getProject().getObjects().property(Integer.class);

    @Internal
    final Property<String> apiDocsUrl = getProject().getObjects().property(String.class);

    @Internal
    final Property<Integer> port = getProject().getObjects().property(Integer.class);

    @Internal
    final DirectoryProperty outputDir = getProject().getObjects().directoryProperty();

    @Internal
    final Property<String> containerId = getProject().getObjects().property(String.class);

    @Internal
    final MapProperty<String, GroupedOpenApiExtension> groupingRules = getProject().getObjects()
        .mapProperty(String.class, GroupedOpenApiExtension.class);

    public OpenApiDocsGeneratorTask() {
        var openApi = pluginExtension.getOpenApi();

        this.groupingRules.convention(openApi.getGroupingRules().getAsMap());
        groupedApiMappings.convention(openApi.getGroupedApiMappings());
        requestHeaders.convention(openApi.getRequestHeaders());
        waitTimeInSeconds.convention(openApi.getWaitTimeInSeconds());
        apiDocsUrl.convention(openApi.getApiDocsUrl());
        port.convention(openApi.getApiDocsPort());
        outputDir.convention(openApi.getOutputDir());
        apiDocsVersion.convention(openApi.getApiDocsVersion());
    }

    @Override
    public void runRemoteCommand() throws Exception {
        if (!groupedApiMappings.isPresent() || groupedApiMappings.get().isEmpty()) {
            throw new IllegalArgumentException("No 'groupedApiMappings' found, please configure it "
                + "in the 'haloPlugin.openApi' in the build.gradle file first.");
        }
        // cleanup output directory
        var outputDirFile = this.outputDir.getAsFile().get();
        var outputFilePath = outputDirFile.toPath();
        if (Files.exists(outputFilePath)) {
            FileUtils.deleteRecursively(outputFilePath);
        }
        try (var dockerClient = getDockerClient()) {
            prepareApiDocsServer(dockerClient);

            var apiDocGenerator = ApiDocGenerator.builder()
                .outputDir(outputDirFile)
                .requestHeaders(requestHeaders.get())
                .trustStore(trustStore)
                .trustStorePassword(trustStorePassword)
                .waitTimeInSeconds(waitTimeInSeconds.get())
                .build();
            groupedApiMappings.get().forEach((k, v) -> {
                var url = joinUrl(apiDocsUrl.get(), k);
                ReadinessCheck.builder()
                    .dockerClient(dockerClient)
                    .containerId(this.containerId.get())
                    .endpoint(url)
                    .requestHeaders(requestHeaders.get())
                    .build()
                    .awaitReadiness();
                apiDocGenerator.generateApiDocs(url, v);
            });

            removeContainer(dockerClient);
        }
    }

    private String joinUrl(String baseUrl, String path) {
        return StringUtils.removeEnd(baseUrl, "/")
            + "/"
            + StringUtils.removeStart(path, "/");
    }

    private void prepareApiDocsServer(DockerClient dockerClient) {
        String imageId = getImageId().get();
        try {
            removeContainer(dockerClient);

            var containerCommand = dockerClient.createContainerCmd(imageId);
            setContainerCommandConfig(containerCommand);
            var container = containerCommand.exec();

            this.containerId.convention(container.getId());

            log.info("Created container with ID [{}]", CONTAINER_NAME);
            Action<? super Object> nextHandler = getNextHandler();
            if (nextHandler != null) {
                nextHandler.execute(container);
            }

            dockerClient.startContainerCmd(container.getId()).exec();

            final FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
            ToStringConsumer toStringConsumer = new ToStringConsumer();
            callback.addConsumer(OutputFrame.OutputType.STDOUT, toStringConsumer);
            callback.addConsumer(OutputFrame.OutputType.STDERR, toStringConsumer);

            dockerClient.attachContainerCmd(container.getId())
                .withStdErr(true)
                .withStdOut(true)
                .withFollowStream(true)
                .withLogs(true)
                .exec(callback);

            ReadinessCheck.builder()
                .dockerClient(dockerClient)
                .containerId(container.getId())
                .endpoint(joinUrl(apiDocsUrl.get(), "/actuator/health"))
                .requestHeaders(requestHeaders.get())
                .waitTimeInSeconds(180)
                .build()
                .awaitReadiness();
        } catch (Exception e) {
            throw new GradleException("Failed to start Halo application", e);
        }
    }

    void removeContainer(DockerClient dockerClient) {
        try {
            dockerClient.removeContainerCmd(CONTAINER_NAME)
                .withForce(true).exec();
        } catch (NotFoundException e) {
            // ignore this exception
        }
    }

    @Builder
    record ApiDocGenerator(File outputDir,
                           Map<String, String> requestHeaders,
                           Property<String> trustStore,
                           Property<char[]> trustStorePassword,
                           Integer waitTimeInSeconds) {
        private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

        public void generateApiDocs(String url, String fileName) {
            boolean isYaml = url.toLowerCase(Locale.getDefault()).matches(".+[./]yaml(/.+)*");
            try {
                SSLContext sslContext = getCustomSslContext();
                log.info("Generating OpenApi Docs..");
                HttpURLConnection connection = getHttpURLConnection(url, sslContext);

                String response =
                    new String(connection.getInputStream().readAllBytes(),
                        StandardCharsets.UTF_8);
                String apiDocs = isYaml ? response : prettifyJson(response);

                if (!outputDir.exists()) {
                    Files.createDirectories(outputDir.toPath());
                }

                File outputFile = new File(outputDir, fileName);
                try (FileWriter writer = new FileWriter(outputFile)) {
                    writer.write(apiDocs);
                }
            } catch (Exception e) {
                log.error("Unable to connect to {} waited for {} seconds", url,
                    waitTimeInSeconds, e);
                throw new RuntimeException(
                    "Unable to connect to " + url + " waited for " + waitTimeInSeconds
                        + " seconds", e);
            }
        }

        private HttpURLConnection getHttpURLConnection(String url, SSLContext sslContext)
            throws IOException {
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            connection.connect();
            return connection;
        }

        public SSLContext getCustomSslContext() throws Exception {
            if (trustStore.isPresent()) {
                log.debug("Reading truststore: {}", trustStore.get());
                try (FileInputStream truststoreFile = new FileInputStream(trustStore.get())) {
                    TrustManagerFactory trustManagerFactory =
                        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    KeyStore truststore = KeyStore.getInstance(KeyStore.getDefaultType());
                    truststore.load(truststoreFile, trustStorePassword.get());
                    trustManagerFactory.init(truststore);
                    SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                    KeyManager[] keyManagers = new KeyManager[0];
                    sslContext.init(keyManagers, trustManagerFactory.getTrustManagers(),
                        new SecureRandom());
                    return sslContext;
                }
            }
            return SSLContext.getDefault();
        }

        private String prettifyJson(String response) {
            try {
                var jsonObject = OBJECT_MAPPER.readTree(response);
                ObjectWriter writer = OBJECT_MAPPER.writerWithDefaultPrettyPrinter();
                return writer.writeValueAsString(jsonObject);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(
                    "Failed to parse the API docs response string. Please ensure that the "
                        + "response is in the correct format. response="
                        + response, e);
            }
        }
    }

    @Builder
    record ReadinessCheck(DockerClient dockerClient, String containerId, String endpoint,
                          Map<String, String> requestHeaders,
                          Integer waitTimeInSeconds) {
        private static final int MAX_HTTP_STATUS_CODE = 299;
        private static final int INITIAL_DELAY = 0;
        private static final int PERIOD = 1;
        private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
        private static final int TIMEOUT = 60;

        public ReadinessCheck {
            Assert.notNull(endpoint, "Endpoint must not be null");
            Assert.notNull(dockerClient, "DockerClient must not be null");
            Assert.notNull(containerId, "ContainerId must not be null");
            if (requestHeaders == null) {
                requestHeaders = Collections.emptyMap();
            }
            if (waitTimeInSeconds == null) {
                waitTimeInSeconds = TIMEOUT;
            }
        }

        private boolean containerIsRunning() {
            var containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
            return Boolean.TRUE.equals(containerInfo.getState().getRunning());
        }

        public void awaitReadiness() {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

            try {
                CompletableFuture<Boolean> readinessFuture = new CompletableFuture<>();
                ScheduledFuture<?> scheduledFuture = scheduler.scheduleAtFixedRate(() -> {
                    try {
                        if (!containerIsRunning()) {
                            readinessFuture.completeExceptionally(
                                new GradleException("Container is stopped unexpectedly"));
                        }
                        if (isReadiness()) {
                            readinessFuture.complete(true);
                        }
                    } catch (Exception e) {
                        readinessFuture.completeExceptionally(e);
                    }
                }, INITIAL_DELAY, PERIOD, TIME_UNIT);

                readinessFuture.whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Filed to start Halo application", throwable);
                    }
                    scheduledFuture.cancel(true);
                });
                readinessFuture.get(TIMEOUT, TIME_UNIT);
            } catch (TimeoutException e) {
                log.error("Timeout to wait for container readiness");
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                scheduler.shutdown();
            }
        }

        private boolean isReadiness() throws Exception {
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            requestHeaders.forEach(connection::setRequestProperty);
            try {
                int responseCode = connection.getResponseCode();
                log.trace("apiDocsUrl = {} status code = {}", url, responseCode);
                return responseCode < MAX_HTTP_STATUS_CODE;
            } catch (SocketException e) {
                return false;
            } finally {
                connection.disconnect();
            }
        }
    }

    private void setContainerCommandConfig(CreateContainerCmd containerCommand) {
        var haloExtension = getProject().getExtensions().getByType(HaloExtension.class);
        containerCommand.withName(CONTAINER_NAME);

        if (platform.getOrNull() != null) {
            containerCommand.withPlatform(platform.get());
        }
        int containerPort = port.get();
        containerCommand.withCmd("--rm");

        String pluginName = pluginExtension.getPluginName();

        List<String> envs = new ArrayList<>();
        envs.add("SERVER_PORT=" + containerPort);
        envs.add("HALO_EXTERNAL_URL=" + haloExtension.getExternalUrl());
        envs.add("HALO_SECURITY_INITIALIZER_SUPERADMINPASSWORD="
            + haloExtension.getSuperAdminPassword());
        envs.add("SPRINGDOC_API_DOCS_ENABLED=true");
        envs.add("SPRINGDOC_SWAGGER_UI_ENABLED=true");
        envs.add("SPRINGDOC_API_DOCS_VERSION=" + apiDocsVersion.get());
        envs.add("SPRINGDOC_SHOW_ACTUATOR=true");
        // Add grouped openapi config envs
        envs.addAll(generateSpringDocGroupEnvs());

        envs.add("HALO_SECURITY_INITIALIZER_SUPERADMINUSERNAME="
            + haloExtension.getSuperAdminUsername());

        envs.add("HALO_PLUGIN_RUNTIMEMODE=development");
        envs.add("HALO_PLUGIN_FIXEDPLUGINPATH=" + buildPluginDestPath(pluginName));
        containerCommand.withEnv(envs);

        containerCommand.withImage(getImageId().get());
        containerCommand.withLabels(Map.of(Constant.DEFAULT_CONTAINER_LABEL, CONTAINER_NAME));

        List<ExposedPort> exposedPorts = new ArrayList<>(2);
        exposedPorts.add(ExposedPort.parse(String.valueOf(containerPort)));
        containerCommand.withExposedPorts(exposedPorts);

        List<PortBinding> portBindings = new ArrayList<>(2);
        portBindings.add(PortBinding.parse("%s:%s".formatted(containerPort, containerPort)));
        var hostConfig = new HostConfig();
        hostConfig.withPortBindings(portBindings);

        File projectDir = getProject().getLayout().getBuildDirectory().getAsFile().get();

        List<Bind> binds = new ArrayList<>();
        binds.add(new Bind(projectDir.toString(),
            new Volume(buildPluginDestPath(pluginName) + "build")));
        hostConfig.withBinds(binds);

        containerCommand.withHostConfig(hostConfig);
    }

    List<String> generateSpringDocGroupEnvs() {
        if (!groupingRules.isPresent()) {
            return List.of();
        }
        var groupMap = groupingRules.get();
        var envs = new ArrayList<String>();
        int i = 0;
        for (var entry : groupMap.entrySet()) {
            var group = entry.getKey();
            var config = entry.getValue();
            var pathsToMatch = config.getPathsToMatch().get();
            var pathsToExclude = config.getPathsToExclude().get();
            var displayName = config.getDisplayName().getOrElse(group);

            envs.add("SPRINGDOC_GROUPCONFIGS_" + i + "_GROUP=" + group);
            envs.add("SPRINGDOC_GROUPCONFIGS_" + i + "_DISPLAYNAME=" + displayName);
            if (!pathsToMatch.isEmpty()) {
                envs.add("SPRINGDOC_GROUPCONFIGS_" + i + "_PATHSTOMATCH="
                    + String.join(",", pathsToMatch));
            }
            if (!pathsToExclude.isEmpty()) {
                envs.add("SPRINGDOC_GROUPCONFIGS_" + i + "_PATHSTOEXCLUDE="
                    + String.join(",", pathsToExclude));
            }
            i++;
        }
        return envs;
    }

    String buildPluginDestPath(String pluginName) {
        return "/data/plugins/" + pluginName + "/";
    }
}
