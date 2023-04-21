package run.halo.gradle;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import io.github.guqing.plugin.docker.*;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import java.io.Closeable;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import run.halo.gradle.docker.AbstractDockerRemoteApiTask;
import run.halo.gradle.docker.FrameConsumerResultCallback;
import run.halo.gradle.docker.OutputFrame;
import run.halo.gradle.docker.ToStringConsumer;
import run.halo.gradle.docker.WaitingConsumer;

/**
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
public class DockerRunTask extends AbstractDockerRemoteApiTask {
    /**
     * The image including repository, image name and tag to be pulled e.g. {@code vieux/apache:2.0}.
     *
     * @since 6.0.0
     */
    @Input
    final Property<String> image = getProject().getObjects().property(String.class);

    /**
     * The target platform in the format {@code os[/arch[/variant]]}, for example {@code linux/s390x} or {@code darwin}.
     *
     * @since 7.1.0
     */
    @Input
    @Optional
    final Property<String> platform = getProject().getObjects().property(String.class);

    private String containerId;

    public DockerRunTask() {
        doLast(action -> {
            removeContainer(containerId);
            System.out.println("DockerRunTask.doLast");
        });
    }

    @Override
    public void runRemoteCommand() {
        log.info("Pulling image '${image.get()}'.");

        CreateContainerCmd containerCmd = getDockerClient().createContainerCmd(image.get());
        containerCmd.withName("halo-for-plugin");

        if (platform.getOrNull() != null) {
            containerCmd.withPlatform(platform.get());
        }
        containerCmd.withEnv("HALO_EXTERNAL_URL=http://localhost:8090/",
            "HALO_SECURITY_INITIALIZER_SUPERADMINPASSWORD=123456",
            "HALO_SECURITY_INITIALIZER_SUPERADMINUSERNAME=admin");
        HostConfig hostConfig = containerCmd.getHostConfig();
        if (hostConfig == null) {
            containerCmd.withHostConfig(new HostConfig());
            hostConfig = containerCmd.getHostConfig();
        }
        hostConfig.withPortBindings(PortBinding.parse("8090:8090"));
        containerCmd.withExposedPorts(ExposedPort.parse("8090"));
        CreateContainerResponse containerResponse = containerCmd.exec();
        containerId = containerResponse.getId();

        try (StartContainerCmd startContainerCmd = getDockerClient()
            .startContainerCmd(containerResponse.getId())) {
            startContainerCmd.exec();
            WaitingConsumer waitingConsumer = new WaitingConsumer();
            ToStringConsumer toStringConsumer = new ToStringConsumer();
            Consumer<OutputFrame> outputFrameConsumer = toStringConsumer.andThen(waitingConsumer);
            attachConsumer(getDockerClient(), containerResponse.getId(), outputFrameConsumer, true,
                OutputFrame.OutputType.STDOUT);
            waitingConsumer.waitUntil(frame -> false);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            removeContainer(containerResponse.getId());
            containerCmd.close();
        }
    }

    private void removeContainer(String containerId) {
        if (containerId == null) {
            return;
        }
        try (StopContainerCmd stopContainerCmd = getDockerClient().stopContainerCmd(containerId);
             RemoveContainerCmd removeContainerCmd = getDockerClient().removeContainerCmd(
                 containerId)) {
            stopContainerCmd.exec();
            removeContainerCmd.exec();
        }
    }

    private static Closeable attachConsumer(
        DockerClient dockerClient,
        String containerId,
        Consumer<OutputFrame> consumer,
        boolean followStream,
        OutputFrame.OutputType... types
    ) {
        final LogContainerCmd cmd = dockerClient
            .logContainerCmd(containerId)
            .withFollowStream(followStream)
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

    public Property<String> getPlatform() {
        return platform;
    }

    public Property<String> getImage() {
        return image;
    }
}
