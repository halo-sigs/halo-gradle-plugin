package io.github.guqing.plugin.docker

import com.github.dockerjava.api.command.PullImageCmd
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.PullResponseItem
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

@CompileStatic
class DockerPullImage extends AbstractDockerRemoteApiTask {

    /**
     * The image including repository, image name and tag to be pulled e.g. {@code vieux/apache:2.0}.
     *
     * @since 6.0.0
     */
    @Input
    final Property<String> image = project.objects.property(String)

    /**
     * The target platform in the format {@code os[/arch[/variant]]}, for example {@code linux/s390x} or {@code darwin}.
     *
     * @since 7.1.0
     */
    @Input
    @Optional
    final Property<String> platform = project.objects.property(String)

    @Override
    void runRemoteCommand() {
        logger.quiet "Pulling image '${image.get()}'."

        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image.get())

        if(platform.getOrNull()) {
            pullImageCmd.withPlatform(platform.get())
        }

        PullImageResultCallback callback = createCallback(nextHandler)
        pullImageCmd.exec(callback).awaitCompletion()
    }

    private PullImageResultCallback createCallback(Action nextHandler) {
        new PullImageResultCallback() {
            @Override
            void onNext(PullResponseItem item) {
                if (nextHandler) {
                    try {
                        nextHandler.execute(item)
                    } catch (Exception e) {
                        logger.error('Failed to handle pull response', e)
                        return
                    }
                }
                super.onNext(item)
            }
        }
    }
}
