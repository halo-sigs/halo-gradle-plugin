package run.halo.gradle.openapi;

import static run.halo.gradle.utils.HaloServerConfigure.buildPluginConfigYamlPath;
import static run.halo.gradle.utils.HaloServerConfigure.buildPluginDestPath;

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
import java.net.URI;
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
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.jetbrains.annotations.NotNull;
import run.halo.gradle.docker.FrameConsumerResultCallback;
import run.halo.gradle.docker.OutputFrame;
import run.halo.gradle.docker.ToStringConsumer;
import run.halo.gradle.extension.HaloExtension;
import run.halo.gradle.extension.HaloPluginExtension;
import run.halo.gradle.model.Constant;
import run.halo.gradle.steps.HaloSiteOption;
import run.halo.gradle.steps.PluginClient;
import run.halo.gradle.steps.SetupHaloStep;
import run.halo.gradle.utils.Assert;
import run.halo.gradle.utils.FileUtils;
import run.halo.gradle.utils.HaloServerConfigure;

@Getter
@Slf4j
public class OpenApiDocsGeneratorTask extends AbstractOpenApiDocsTask {
    public static final String CONTAINER_NAME = "halo-openapi-docs-generator";
    public static final String TASK_NAME = "generateOpenApiDocs";

    @Input
    @Optional
    final Property<String> platform = getProject().getObjects().property(String.class);

    @Input
    @Optional
    final MapProperty<String, String> requestHeaders = getProject().getObjects()
        .mapProperty(String.class, String.class);

    @Input
    @Optional
    final Property<String> trustStore = getProject().getObjects().property(String.class);

    @Input
    @Optional
    final Property<char[]> trustStorePassword = getProject().getObjects().property(char[].class);

    @Internal
    final Property<Integer> waitTimeInSeconds = getProject().getObjects().property(Integer.class);

    @Internal
    final Property<Integer> port = getProject().getObjects().property(Integer.class);

    @Internal
    final Property<String> containerId = getProject().getObjects().property(String.class);

    public OpenApiDocsGeneratorTask() {
        var openApi = getPluginExtension().getOpenApi();
        requestHeaders.convention(openApi.getRequestHeaders());
        waitTimeInSeconds.convention(openApi.getWaitTimeInSeconds());
        port.convention(openApi.getApiDocsPort());
    }

    @Override
    public void runRemoteCommand() throws Exception {
        if (!groupedApiMappings.isPresent() || groupedApiMappings.get().isEmpty()) {
            throw new IllegalArgumentException("No 'groupedApiMappings' found, please configure it "
                + "in the 'haloPlugin.openApi' in the build.gradle file first.");
        }
        // cleanup output directory
        var outputDirFile = outputDir.getAsFile().get();
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

            System.out.println("Start generating API documentation...");
            groupedApiMappings.get().forEach((k, v) -> {
                var url = joinUrl(getApiDocsUrl().get(), k);
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

            var siteOption = createHaloSiteOption();
            waitForSetup(siteOption);
            waitForPluginReady(siteOption);

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

                System.out.println("Generating OpenApi Docs for url: " + url);
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

    private void waitForSetup(HaloSiteOption siteOption) {
        new SetupHaloStep(siteOption).execute();
    }

    private void waitForPluginReady(HaloSiteOption siteOption) {
        var pluginName = getPluginExtension().getPluginName();
        var client = new PluginClient(pluginName, siteOption);
        client.checkPluginState();
    }

    private HaloSiteOption createHaloSiteOption() {
        var haloExt = getProject().getExtensions().getByType(HaloExtension.class);
        var baseUri = URI.create(getApiDocsUrl().get());
        return new HaloSiteOption(haloExt.getSuperAdminUsername(),
            haloExt.getSuperAdminPassword(), baseUri);
    }

    private void setContainerCommandConfig(CreateContainerCmd containerCommand) {
        var haloExtension = getProject().getExtensions().getByType(HaloExtension.class);
        containerCommand.withName(CONTAINER_NAME);

        if (platform.getOrNull() != null) {
            containerCommand.withPlatform(platform.get());
        }
        int containerPort = port.get();
        containerCommand.withCmd("--rm");

        String pluginName = getPluginExtension().getPluginName();

        List<String> envs = new ArrayList<>();
        var applicationJson = HaloServerConfigure.builder()
            .port(containerPort)
            .externalUrl(haloExtension.getExternalUrl())
            .fixedPluginPath(buildPluginDestPath(pluginName))
            .otherConfig(generateSpringDocApplicationConfig())
            .build()
            .toApplicationJsonString();
        envs.add("SPRING_APPLICATION_JSON=" + applicationJson);
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

        var pluginConfigYaml = getPluginExtension().getConfigurationPropertiesFile()
            .getAsFile().getOrNull();
        if (pluginConfigYaml != null && Files.exists(pluginConfigYaml.toPath())) {
            binds.add(new Bind(pluginConfigYaml.getAbsolutePath(),
                new Volume(
                    buildPluginConfigYamlPath(haloExtension.getServerWorkDir(), pluginName))));
        }

        hostConfig.withBinds(binds);

        containerCommand.withHostConfig(hostConfig);
    }
}
