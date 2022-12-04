package io.github.guqing.plugin.docker;

import com.github.dockerjava.api.command.RemoveContainerCmd;
import groovy.transform.CompileStatic;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

@Getter
@Slf4j
@CompileStatic
public class DockerRemoveContainer extends DockerExistingContainer {
    @Input
    @Optional
    final Property<Boolean> force = getProject().getObjects().property(Boolean.class);

    /**
     * Stop timeout in seconds.
     */
    @Input
    @Optional
    final Property<Integer> waitTime = getProject().getObjects().property(Integer.class);

    @Override
    public void runRemoteCommand() {
        RemoveContainerCmd containerCommand =
            getDockerClient().removeContainerCmd(containerId.get());
        configureContainerCommandConfig(containerCommand);
        log.info("Removing container with ID [{}].", containerId.get());
        containerCommand.exec();
    }

    private void configureContainerCommandConfig(RemoveContainerCmd containerCommand) {
        if (force.getOrNull() != null) {
            containerCommand.withForce(force.get());
        }
    }
}
