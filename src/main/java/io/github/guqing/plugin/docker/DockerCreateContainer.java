package io.github.guqing.plugin.docker;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

@Getter
@Slf4j
public class DockerCreateContainer extends DockerExistingImage {
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
     * The ID of the container created. The value of this property requires the task action to be executed.
     */
    @Internal
    final Property<String> containerId = getProject().getObjects().property(String.class);

    /**
     * The target platform in the format {@code os[/arch[/variant]]}, for example {@code linux/s390x} or {@code darwin}.
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

    private String getAndValidateContainerId(File file) {
        if (!file.exists()) {
            return null;
        }
        try {
            String containerId = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            try (InspectContainerCmd inspectContainerCmd = getDockerClient().inspectContainerCmd(containerId)) {
                InspectContainerResponse containerResponse = inspectContainerCmd.exec();
                if (containerResponse != null) {
                    return containerResponse.getId();
                }
                FileUtils.delete(file);
                return null;
            } catch (Exception e) {
                log.debug("Failed to inspect container with ID: " + containerId);
            }
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

        containerCommand.withEnv("HALO_EXTERNAL_URL=http://localhost:8090/",
                "HALO_SECURITY_INITIALIZER_SUPERADMINPASSWORD=123456",
                "HALO_SECURITY_INITIALIZER_SUPERADMINUSERNAME=guqing");

        containerCommand.withImage(getImageId().get());
        containerCommand.withLabels(Map.of("container-created-by", "halo-gradle-plugin"));
        containerCommand.withExposedPorts(ExposedPort.parse("8090"));
        containerCommand.withHostConfig(new HostConfig()
                .withPortBindings(PortBinding.parse("8090:8090")));
    }
}
