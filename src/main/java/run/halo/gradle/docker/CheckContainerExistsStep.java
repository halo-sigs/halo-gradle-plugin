package run.halo.gradle.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerConfig;
import run.halo.gradle.Constant;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
public class CheckContainerExistsStep {
    private final String containerId;
    private final DockerClient dockerClient;

    public CheckContainerExistsStep(DockerClient dockerClient, String containerId) {
        this.containerId = containerId;
        this.dockerClient = dockerClient;
    }

    public boolean execute() {
        try (InspectContainerCmd inspectContainerCmd = dockerClient.inspectContainerCmd(containerId)) {
            InspectContainerResponse containerResponse = inspectContainerCmd.exec();
            return validateContainerResponse(containerResponse);
        } catch (Exception e) {
            log.debug("Failed to inspect container with ID: " + containerId);
        }
        return false;
    }

    private boolean validateContainerResponse(InspectContainerResponse containerResponse) {
        if (containerResponse == null) {
            return false;
        }
        String containerId = containerResponse.getId();
        ContainerConfig config = containerResponse.getConfig();
        Map<String, String> labels = config.getLabels();
        if (labels == null || !labels.containsKey(Constant.DEFAULT_CONTAINER_LABEL)) {
            throw new IllegalStateException("Container with ID [" + containerId + "] already exists and not was "
                    + "created by this plugin. Please remove the container before running this task again.");
        }
        return true;
    }
}
