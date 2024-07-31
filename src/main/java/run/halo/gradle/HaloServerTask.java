package run.halo.gradle;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.tasks.TaskAction;
import run.halo.gradle.docker.DockerStartContainer;
import run.halo.gradle.extension.HaloExtension;
import run.halo.gradle.steps.HaloSiteOption;
import run.halo.gradle.steps.InitializeHaloStep;

/**
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
public class HaloServerTask extends DockerStartContainer {
    public static final String TASK_NAME = "haloServer";
    private final HaloExtension haloExtension =
            getProject().getExtensions().getByType(HaloExtension.class);

    @Override
    @TaskAction
    public void runRemoteCommand() {
        var haloSiteOption = HaloSiteOption.from(haloExtension);
        CompletableFuture<Void> initializeFuture = CompletableFuture.runAsync(
                () -> new InitializeHaloStep(haloSiteOption).execute());

        initializeFuture.exceptionally(e -> {
            log.error(e.getMessage(), e);
            return null;
        });
        super.runRemoteCommand();
    }
}
