package run.halo.gradle;

import lombok.extern.slf4j.Slf4j;
import run.halo.gradle.docker.DockerStartContainer;

/**
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
public class HaloServerTask extends DockerStartContainer {
    public static final String TASK_NAME = "haloServer";

    @Override
    public void runRemoteCommand() {
        super.runRemoteCommand();
    }
}
