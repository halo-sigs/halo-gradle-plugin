package io.github.guqing.plugin;

import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.PullResponseItem;
import io.github.guqing.plugin.docker.AbstractDockerRemoteApiTask;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Action;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

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

    @Override
    public void runRemoteCommand() {
        log.info("Pulling image '${image.get()}'.");

        try (CreateContainerCmd containerCmd = getDockerClient().createContainerCmd(image.get())) {
            containerCmd.withName("halo-for-plugin");

            if (platform.getOrNull() != null) {
                containerCmd.withPlatform(platform.get());
            }
            containerCmd.withEnv("HALO_EXTERNAL_URL=http://localhost:8090/",
                    "HALO_SECURITY_INITIALIZER_SUPERADMINPASSWORD=123456", "HALO_SECURITY_INITIALIZER_SUPERADMINUSERNAME=guqing");
            HostConfig hostConfig = containerCmd.getHostConfig();
            if (hostConfig == null) {
                containerCmd.withHostConfig(new HostConfig());
                hostConfig = containerCmd.getHostConfig();
            }
            hostConfig.withPortBindings(PortBinding.parse("8090:8090"));
            containerCmd.withExposedPorts(ExposedPort.parse("8090"));
            CreateContainerResponse containerResponse = containerCmd.exec();

            try (StartContainerCmd startContainerCmd = getDockerClient()
                    .startContainerCmd(containerResponse.getId())) {
                startContainerCmd.exec();

//                WaitContainerResultCallback resultCallback = new WaitContainerResultCallback();
//                WaitContainerCmd waitContainerCmd = getDockerClient().waitContainerCmd(containerResponse.getId());
//                waitContainerCmd.exec(resultCallback);
//                resultCallback.awaitCompletion();
//                waitContainerCmd.close();
            }
        }
    }

    private PullImageResultCallback createCallback(Action<? super Object> nextHandler) {
        return new PullImageResultCallback() {
            @Override
            public void onNext(PullResponseItem item) {
                if (nextHandler != null) {
                    try {
                        nextHandler.execute(item);
                    } catch (Exception e) {
                        log.error("Failed to handle pull response", e);
                        return;
                    }
                }
                super.onNext(item);
            }
        };
    }

    public Property<String> getPlatform() {
        return platform;
    }

    public Property<String> getImage() {
        return image;
    }
}
