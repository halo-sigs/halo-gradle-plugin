package run.halo.gradle.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import groovy.transform.CompileStatic;
import java.io.Closeable;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CompileStatic
public class DockerStartContainer extends DockerExistingContainer {
    @Override
    public void runRemoteCommand() {
        log.info("Starting container with ID [{}].", containerId.get());
        try (StartContainerCmd containerCommand = getDockerClient()
                .startContainerCmd(containerId.get())) {
            containerCommand.exec();
            waitForContainerRunning();
        }
    }

    private void waitForContainerRunning() {
        try {
            WaitingConsumer waitingConsumer = new WaitingConsumer();
            ToStringConsumer toStringConsumer = new ToStringConsumer();
            Consumer<OutputFrame> outputFrameConsumer = toStringConsumer.andThen(waitingConsumer);
            attachConsumer(getDockerClient(), getContainerId().get(), outputFrameConsumer,
                    OutputFrame.OutputType.STDOUT);
            waitingConsumer.waitUntil(frame -> false);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private static Closeable attachConsumer(
            DockerClient dockerClient,
            String containerId,
            Consumer<OutputFrame> consumer,
            OutputFrame.OutputType... types
    ) {
        final LogContainerCmd cmd = dockerClient
                .logContainerCmd(containerId)
                .withFollowStream(true)
                .withSince(0);

        final FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        for (OutputFrame.OutputType type : types) {
            callback.addConsumer(type, consumer);
            if (type == OutputFrame.OutputType.STDOUT) {
                cmd.withStdOut(true);
            }
            if (type == OutputFrame.OutputType.STDERR) {
                cmd.withStdErr(true);
            }
        }

        return cmd.exec(callback);
    }

}
