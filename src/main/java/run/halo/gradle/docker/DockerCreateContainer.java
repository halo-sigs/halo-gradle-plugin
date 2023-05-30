package run.halo.gradle.docker;

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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.Transformer;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import run.halo.gradle.Constant;
import run.halo.gradle.HaloExtension;
import run.halo.gradle.HaloPluginExtension;
import run.halo.gradle.utils.PathUtils;

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

    @Input
    @Getter
    @Optional
    final Property<String> workingDir = getProject().getObjects().property(String.class);

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
    public void runRemoteCommand() throws Exception {
        String imageId = getImageId().get();

        CreateContainerCmd containerCommand = getDockerClient().createContainerCmd(imageId);
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
    }

    private void setContainerCommandConfig(CreateContainerCmd containerCommand) {
        if (containerName.getOrNull() != null) {
            containerCommand.withName(containerName.get());
        }

        if (workingDir.getOrNull() != null) {
            containerCommand.withWorkingDir(workingDir.get());
        }

        if (platform.getOrNull() != null) {
            containerCommand.withPlatform(platform.get());
        }
        containerCommand.withCmd("--rm");

        HaloExtension.HaloSecurity security = haloExtension.getSecurity();
        String pluginName = pluginExtension.getPluginName();

        // Set environment variables and port bindings
        Integer debugPort = debugPort();
        List<String> envs = new ArrayList<>();
        envs.add("HALO_EXTERNAL_URL=" + haloExtension.getExternalUrl());
        envs.add(
            "HALO_SECURITY_INITIALIZER_SUPERADMINPASSWORD=" + security.getSuperAdminPassword());
        envs.add(
            "HALO_SECURITY_INITIALIZER_SUPERADMINUSERNAME=" + security.getSuperAdminUsername());
        if (debugPort != null) {
            envs.add("JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,"
                + "address=*:" + debugPort);
        }
        envs.add("HALO_PLUGIN_RUNTIMEMODE=development");
        envs.add("HALO_PLUGIN_FIXEDPLUGINPATH=" + Paths.get(buildPluginDestPath(pluginName)));
        envs.add("HALO_WORKDIR=" + haloWorkDir());
        String configLocation = PathUtils.combinePath(haloWorkDir(), "/config") + "/";
        envs.add(
            "SPRING_CONFIG_IMPORT=optional:file:" + configLocation + ";optional:file:"
                + configLocation + "*/");
        containerCommand.withEnv(envs);
        containerCommand.withImage(getImageId().get());
        containerCommand.withLabels(Map.of(Constant.DEFAULT_CONTAINER_LABEL, "halo-gradle-plugin"));

        List<ExposedPort> exposedPorts = new ArrayList<>(2);
        exposedPorts.add(ExposedPort.parse("8090"));
        if (debugPort != null) {
            exposedPorts.add(ExposedPort.tcp(debugPort));
        }
        containerCommand.withExposedPorts(exposedPorts);

        List<PortBinding> portBindings = new ArrayList<>(2);
        portBindings.add(PortBinding.parse("8090:8090"));
        if (debugPort != null) {
            portBindings.add(PortBinding.parse(debugPort + ":" + debugPort));
        }
        HostConfig hostConfig = new HostConfig();
        hostConfig.withPortBindings(portBindings);

        File projectDir = getProject().getBuildDir();

        List<Bind> binds = new ArrayList<>();
        binds.add(new Bind(projectDir.toString(),
            new Volume(buildPluginDestPath(pluginName) + "build")));
        if (pluginExtension.getWorkDir() != null) {
            binds.add(new Bind(pluginExtension.getWorkDir().toString(),
                new Volume(haloWorkDir())));
        }
        hostConfig.withBinds(binds);

        containerCommand.withHostConfig(hostConfig);
    }

    String haloWorkDir() {
        return haloExtension.getServerWorkDir();
    }

    String buildPluginDestPath(String pluginName) {
        return "/data/plugins/" + pluginName + "/";
    }

    Integer debugPort() {
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
