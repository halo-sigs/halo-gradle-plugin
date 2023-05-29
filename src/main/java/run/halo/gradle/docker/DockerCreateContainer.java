package run.halo.gradle.docker;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Volume;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import run.halo.gradle.Constant;
import run.halo.gradle.HaloPluginExtension;

@Getter
@Slf4j
public class DockerCreateContainer extends DockerExistingImage {
    @Internal
    private final HaloPluginExtension pluginExtension =
        getProject().getExtensions().getByType(HaloPluginExtension.class);

    @Input
    @Optional
    final Property<String> containerName = getProject().getObjects().property(String.class);

    @Input
    @Optional
    final Property<String> workingDir = getProject().getObjects().property(String.class);

    /**
     * Output file containing the container ID of the container created.
     * Defaults to "$buildDir/.docker/$taskpath-containerId.txt".
     * If path contains ':' it will be replaced by '_'.
     */
    @OutputFile
    final RegularFileProperty containerIdFile = getProject().getObjects().fileProperty();

    /**
     * The ID of the container created. The value of this property requires the task action to be
     * executed.
     */
    @Internal
    final Property<String> containerId = getProject().getObjects().property(String.class);

    /**
     * The target platform in the format {@code os[/arch[/variant]]}, for example {@code linux
     * /s390x} or {@code darwin}.
     *
     * @since 7.1.0
     */
    @Input
    @Optional
    final Property<String> platform = getProject().getObjects().property(String.class);

    public DockerCreateContainer() {
        containerId.convention(containerIdFile.map(it -> {
            File file = it.getAsFile();
            String containerId = getAndValidateContainerId(file);
            if (containerId == null) {
                containerIdFile.fileValue(null);
            }
            return StringUtils.defaultString(containerId);
        }));

        String safeTaskPath = getPath().replaceFirst("^:", "").replaceAll(":", "_");
        containerIdFile.convention(
            getProject().getLayout().getBuildDirectory()
                .file(".docker/" + safeTaskPath + "-containerId.txt"));
    }

    @Override
    public void runRemoteCommand() throws Exception {
        String imageId = getImageId().get();
        removeContainerIfPresent();

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

    private void removeContainerIfPresent() {
        try {
            List<String> containerIds =
                Files.readAllLines(containerIdFile.get().getAsFile().toPath());
            if (containerIds.isEmpty()) {
                return;
            }
            String containerIdValue = containerIds.get(0);
            if (StringUtils.isBlank(containerIdValue)) {
                return;
            }
            try (RemoveContainerCmd cmd = getDockerClient().removeContainerCmd(containerIdValue)
                .withForce(true)) {
                cmd.exec();
            } catch (Exception e) {
                log.debug("Failed to remove container with ID: " + containerIdValue);
            }
        } catch (IOException e) {
            // ignore
        }
    }

    private String getAndValidateContainerId(File file) {
        if (!file.exists()) {
            return null;
        }
        try {
            String containerId = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            boolean exists = new CheckContainerExistsStep(getDockerClient(), containerId).execute();
            if (exists) {
                return containerId;
            }

            Files.writeString(file.toPath(), "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
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

        HaloPluginExtension.HaloSecurity security = pluginExtension.getSecurity();
        String pluginName = pluginExtension.getPluginName();

        // Set environment variables and port bindings
        Integer debugPort = debugPort();
        List<String> envs = new ArrayList<>();
        envs.add("HALO_EXTERNAL_URL=" + pluginExtension.getHost());
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
        containerCommand.withEnv(envs);
        System.out.println(envs);
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
        hostConfig.withBinds(new Bind(projectDir.toString(),
            new Volume(buildPluginDestPath(pluginName) + "build"))
        );

        containerCommand.withHostConfig(hostConfig);
    }

    String buildPluginDestPath(String pluginName) {
        return "/data/plugins/" + pluginName + "/";
    }

    Integer debugPort() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        List<String> inputArguments = runtimeMXBean.getInputArguments();
        System.out.println("-->" + inputArguments);
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
