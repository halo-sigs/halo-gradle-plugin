package run.halo.gradle.docker;

import com.github.dockerjava.api.command.AttachContainerCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.WaitResponse;
import groovy.transform.CompileStatic;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

@Slf4j
@CompileStatic
public class DockerStartContainer extends DockerExistingContainer {

    private int exitCode;

    @Input
    @Optional
    @Getter
    private final Property<Integer> awaitStatusTimeout =
        getProject().getObjects().property(Integer.class);

    @Override
    public void runRemoteCommand() {
        log.info("Starting container with ID [{}].", containerId.get());
        try (StartContainerCmd containerCommand = getDockerClient()
            .startContainerCmd(containerId.get())) {
            containerCommand.exec();
        } catch (Exception e) {
            throw new GradleException("Failed to start container", e);
        }

        try (AttachContainerCmd attachContainerCmd = getDockerClient().attachContainerCmd(
            containerId.get())) {
            final FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
            ToStringConsumer toStringConsumer = new ToStringConsumer();
            callback.addConsumer(OutputFrame.OutputType.STDOUT, toStringConsumer);
            callback.addConsumer(OutputFrame.OutputType.STDERR, toStringConsumer);
            attachContainerCmd.withStdErr(true)
                .withStdOut(true)
                .withFollowStream(true)
                .withLogs(true)
                .exec(callback);
        } catch (Exception e) {
            throw new GradleException("Failed to attach to container", e);
        }

        try (WaitContainerCmd containerCommand =
                 getDockerClient().waitContainerCmd(getContainerId().get())) {
            WaitContainerResultCallback callback =
                containerCommand.exec(createCallback(getNextHandler()));
            exitCode = awaitStatusTimeout.getOrNull() != null ? callback.awaitStatusCode(
                awaitStatusTimeout.get(), TimeUnit.SECONDS) : callback.awaitStatusCode();
            getLogger().quiet("Container exited with code " + getExitCode());
        } catch (Exception e) {
            throw new GradleException("Failed to wait for container to exit", e);
        }
    }

    @Internal
    public int getExitCode() {
        return exitCode;
    }

    private WaitContainerResultCallback createCallback(final Action<Object> nextHandler) {
        return new WaitContainerResultCallback() {
            @Override
            public void onNext(WaitResponse waitResponse) {
                if (nextHandler != null) {
                    try {
                        nextHandler.execute(waitResponse);
                    } catch (Exception e) {
                        getLogger().error("Failed to handle wait response", e);
                        return;
                    }
                }
                super.onNext(waitResponse);
            }
        };
    }
}
