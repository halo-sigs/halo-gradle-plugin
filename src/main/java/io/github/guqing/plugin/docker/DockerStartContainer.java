package io.github.guqing.plugin.docker;

import com.github.dockerjava.api.command.StartContainerCmd;
import groovy.transform.CompileStatic;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CompileStatic
class DockerStartContainer extends DockerExistingContainer {
    @Override
    public void runRemoteCommand() {
        log.info("Starting container with ID [{}].", containerId.get());
        try (StartContainerCmd containerCommand = getDockerClient()
            .startContainerCmd(containerId.get())) {
            containerCommand.exec();
        }
    }
}
