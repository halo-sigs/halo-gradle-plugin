package io.github.guqing.plugin.docker;

import com.github.dockerjava.api.command.InspectImageCmd;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.PullResponseItem;
import groovy.transform.CompileStatic;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.Action;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

@Slf4j
@Getter
@CompileStatic
public class DockerPullImage extends AbstractDockerRemoteApiTask {

    /**
     * The image including repository, image name and tag to be pulled e.g. {@code vieux/apache:2.0}.
     *
     * @since 6.0.0
     */
    @Input
    protected final Property<String> image = getProject().getObjects().property(String.class);

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
        log.info("Pulling image '{}'.", image.get());
        if (checkImageExits()) {
            log.info("Image [{}] already exists, skipping pull.", image.get());
            return;
        }
        try (PullImageCmd pullImageCmd = getDockerClient().pullImageCmd(image.get())) {
            if (platform.getOrNull() != null) {
                pullImageCmd.withPlatform(platform.get());
            }

            PullImageResultCallback callback = createCallback(getNextHandler());
            pullImageCmd.exec(callback).awaitCompletion();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkImageExits() {
        try (InspectImageCmd inspectImageCmd = getDockerClient().inspectImageCmd(image.get())) {
            InspectImageResponse response = inspectImageCmd.exec();
            if (response != null) {
                return true;
            }
        } catch (NotFoundException e) {
            // ignore this
        }
        return false;
    }

    private PullImageResultCallback createCallback(Action<? super Object> nextHandler) {
        return new PullImageResultCallback() {
            @Override
            public void onNext(PullResponseItem item) {
                if (nextHandler != null) {
                    try {
                        nextHandler.execute(item);
                    } catch (Exception e) {
                        log.error("Failed to handle pull response [{}]", e.getMessage(), e);
                        return;
                    }
                }
                super.onNext(item);
            }
        };
    }
}

