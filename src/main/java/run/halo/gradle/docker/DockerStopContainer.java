package run.halo.gradle.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.StopContainerCmd;
import groovy.transform.CompileStatic;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

@Getter
@Slf4j
@CompileStatic
public class DockerStopContainer extends DockerExistingContainer {

    /**
     * Stop timeout in seconds.
     */
    @Input
    @Optional
    final Property<Integer> waitTime = getProject().getObjects().property(Integer.class);

    @Override
    public void runRemoteCommand() {
        log.info("Stopping container with ID [{}].", containerId.get());
        runRemoteCommand(getDockerClient(), containerId.get(), waitTime.getOrNull());
    }

    // overloaded method used by sub-classes and ad-hoc processes
    static void runRemoteCommand(DockerClient dockerClient, String containerId,
                                 Integer optionalTimeout) {
        StopContainerCmd stopContainerCmd = dockerClient.stopContainerCmd(containerId);
        if (optionalTimeout != null) {
            stopContainerCmd.withTimeout(optionalTimeout);
        }
        stopContainerCmd.exec();
    }
}
