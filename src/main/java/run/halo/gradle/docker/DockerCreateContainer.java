package run.halo.gradle.docker;

import static run.halo.gradle.utils.HaloServerConfigure.buildPluginConfigYamlPath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Volume;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.Transformer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import run.halo.gradle.extension.HaloExtension;
import run.halo.gradle.extension.HaloPluginExtension;
import run.halo.gradle.model.Constant;
import run.halo.gradle.utils.HaloServerConfigure;
import run.halo.gradle.utils.YamlUtils;

@Slf4j
public class DockerCreateContainer extends DockerExistingImage {
    private final HaloPluginExtension pluginExtension =
        getProject().getExtensions().getByType(HaloPluginExtension.class);

    private final HaloExtension haloExtension =
        getProject().getExtensions().getByType(HaloExtension.class);

    @Input
    @Optional
    @Getter
    final Property<String> containerName = getProject().getObjects().property(String.class);

    @Optional
    @Getter
    @InputDirectory
    final DirectoryProperty pluginWorkplaceDir = getProject().getObjects().directoryProperty();

    @InputFile
    @Optional
    @Getter
    final RegularFileProperty additionalApplicationConfig =
        getProject().getObjects().fileProperty();

    /**
     * Output file containing the container ID of the container created.
     * Defaults to "$buildDir/.docker/$taskpath-containerId.txt".
     * If path contains ':' it will be replaced by '_'.
     */
    @Getter
    @OutputFile
    final RegularFileProperty containerIdFile = getProject().getObjects().fileProperty();

    /**
     * The ID of the container created. The value of this property requires the task action to be
     * executed.
     */
    @Getter
    @Internal
    final Property<String> containerId = getProject().getObjects().property(String.class);

    /**
     * The target platform in the format {@code os[/arch[/variant]]}, for example {@code linux
     * /s390x} or {@code darwin}.
     */
    @Input
    @Getter
    @Optional
    final Property<String> platform = getProject().getObjects().property(String.class);

    public DockerCreateContainer() {
        containerId.convention(containerIdFile.map(new RegularFileToStringTransformer()));

        additionalApplicationConfig.convention(getProject().provider(() -> {
            var file = pluginWorkplaceDir.file("config/application.yaml").get();
            return file.getAsFile().exists() ? file : null;
        }));

        String safeTaskPath = getPath().replaceFirst("^:", "").replaceAll(":", "_");
        containerIdFile.convention(getProject().getLayout().getBuildDirectory()
            .file(".docker/" + safeTaskPath + "-containerId.txt"));

        getOutputs().upToDateWhen(getUpToDateWhenSpec());
    }

    private Spec<Task> getUpToDateWhenSpec() {
        return element -> {
            File file = getContainerIdFile().get().getAsFile();
            if (file.exists()) {
                try {
                    String fileContainerId;
                    try {
                        fileContainerId = Files.readString(file.toPath());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    try (
                        InspectContainerCmd inspectContainerCmd = getDockerClient()
                            .inspectContainerCmd(fileContainerId)) {
                        inspectContainerCmd.exec();
                    }
                    return true;
                } catch (DockerException ignored) {
                }
            }
            return false;
        };
    }

    public static class RegularFileToStringTransformer
        implements Transformer<String, RegularFile>, Serializable {
        @Override
        @Nonnull
        public String transform(RegularFile it) {
            File file = it.getAsFile();
            if (file.exists()) {
                try {
                    return Files.readString(file.toPath());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return StringUtils.EMPTY;
        }
    }

    @Override
    public void runRemoteCommand() {
        String imageId = getImageId().get();

        try (CreateContainerCmd containerCommand = getDockerClient().createContainerCmd(imageId)) {
            setContainerCommandConfig(containerCommand);
            CreateContainerResponse container = containerCommand.exec();

            final String localContainerName =
                containerName.getOrNull() == null ? container.getId() : containerName.get();
            log.info("Created container with ID [{}]", localContainerName);
            Files.writeString(containerIdFile.get().getAsFile().toPath(), container.getId());
            Action<? super Object> nextHandler = getNextHandler();
            if (nextHandler != null) {
                nextHandler.execute(container);
            }
        } catch (Exception e) {
            throw new GradleException("Failed to create container", e);
        }
    }

    private void setContainerCommandConfig(CreateContainerCmd containerCommand) {
        if (containerName.getOrNull() != null) {
            containerCommand.withName(containerName.get());
        }

        if (platform.getOrNull() != null) {
            containerCommand.withPlatform(platform.get());
        }
        containerCommand.withCmd("--rm");

        String pluginName = pluginExtension.getPluginName();
        Integer port = haloExtension.getPort();

        // Set environment variables and port bindings
        List<String> envs = new ArrayList<>();
        var applicationJson = HaloServerConfigure.builder()
            .port(port)
            .externalUrl(haloExtension.getExternalUrl())
            .workDir(haloWorkDir())
            .fixedPluginPath(HaloServerConfigure.buildPluginDestPath(pluginName))
            .build()
            .mergeWithUserConfigAsJson(readExternalConfig());
        envs.add("SPRING_APPLICATION_JSON=" + applicationJson);

        boolean isDebugMode = isDebugMode();
        int debugPort = getDebugPort();
        if (isDebugMode) {
            String suspend = haloExtension.getSuspend() ? "y" : "n";
            envs.add(
                ("JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=%s,"
                    + "address=*:%s")
                    .formatted(suspend, debugPort)
            );
        }
        containerCommand.withEnv(envs);
        containerCommand.withImage(getImageId().get());
        containerCommand.withLabels(Map.of(Constant.DEFAULT_CONTAINER_LABEL, "halo-gradle-plugin"));

        List<ExposedPort> exposedPorts = new ArrayList<>(2);
        exposedPorts.add(ExposedPort.parse(String.valueOf(port)));
        if (isDebugMode) {
            exposedPorts.add(ExposedPort.tcp(debugPort));
        }
        containerCommand.withExposedPorts(exposedPorts);

        List<PortBinding> portBindings = new ArrayList<>(2);
        portBindings.add(PortBinding.parse("%s:%s".formatted(port, port)));
        if (isDebugMode) {
            portBindings.add(PortBinding.parse(debugPort + ":" + debugPort));
        }
        HostConfig hostConfig = new HostConfig();
        hostConfig.withPortBindings(portBindings);

        File projectDir = getProject().getLayout().getBuildDirectory().getAsFile().get();

        List<Bind> binds = new ArrayList<>();
        binds.add(new Bind(projectDir.toString(),
            new Volume(HaloServerConfigure.buildPluginDestPath(pluginName) + "build")));
        if (pluginWorkplaceDir.isPresent()) {
            var sourceDir = pluginWorkplaceDir.getAsFile().get().getAbsolutePath();
            binds.add(new Bind(sourceDir, new Volume(haloWorkDir())));
        }

        var pluginConfigYaml = pluginExtension.getConfigurationPropertiesFile()
            .getAsFile().getOrNull();
        if (pluginConfigYaml != null && Files.exists(pluginConfigYaml.toPath())) {
            binds.add(new Bind(pluginConfigYaml.getAbsolutePath(),
                new Volume(
                    buildPluginConfigYamlPath(haloExtension.getServerWorkDir(), pluginName))));
        }

        hostConfig.withBinds(binds);

        containerCommand.withHostConfig(hostConfig);
    }

    @Nonnull
    JsonNode readExternalConfig() {
        if (!additionalApplicationConfig.isPresent()) {
            return JsonNodeFactory.instance.missingNode();
        }
        System.out.println(additionalApplicationConfig.getAsFile().get().toPath());
        var additionalFile = additionalApplicationConfig.getAsFile().get();
        if (Files.exists(additionalFile.toPath())) {
            return YamlUtils.read(additionalFile, JsonNode.class);
        }
        return JsonNodeFactory.instance.missingNode();
    }

    String haloWorkDir() {
        return haloExtension.getServerWorkDir();
    }

    @Internal
    boolean isDebugMode() {
        return jvmDebugPort() != null || haloExtension.getDebug();
    }

    @Internal
    int getDebugPort() {
        Integer jvmDebugPort = jvmDebugPort();
        if (jvmDebugPort != null) {
            return jvmDebugPort;
        }
        return haloExtension.getDebugPort();
    }

    @Nullable
    private Integer jvmDebugPort() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        List<String> inputArguments = runtimeMXBean.getInputArguments();
        return inputArguments.stream()
            .filter(argument -> argument.startsWith("-agentlib:jdwp="))
            .findFirst()
            .map(this::parsePort)
            .orElse(null);
    }

    Integer parsePort(String debugOption) {
        Pattern pattern = Pattern.compile("address=.*:(\\d{1,5})$");
        Matcher matcher = pattern.matcher(debugOption);
        if (matcher.find()) {
            String port = matcher.group(1);
            return Integer.parseInt(port);
        }
        return null;
    }
}
