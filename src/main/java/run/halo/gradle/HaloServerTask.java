package run.halo.gradle;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.tasks.TaskAction;
import run.halo.gradle.docker.DockerStartContainer;
import run.halo.gradle.steps.PluginClient;
import run.halo.gradle.steps.SetupHaloStep;

/**
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
public class HaloServerTask extends DockerStartContainer {
    public static final String TASK_NAME = "haloServer";
    private final AtomicBoolean isInitializing = new AtomicBoolean(false);
    private final PluginClient pluginClient;

    public HaloServerTask() {
        this.pluginClient = new PluginClient(getProject());
    }

    @Override
    @TaskAction
    public void runRemoteCommand() {
        if (isInitializing.compareAndSet(false, true)) {
            CompletableFuture<Void> initializeFuture = CompletableFuture.runAsync(() -> {
                new SetupHaloStep(pluginClient.getSiteOption()).execute();
                pluginClient.checkPluginState();
            });
            initializeFuture.exceptionally(e -> {
                log.error(e.getMessage(), e);
                return null;
            });
        }
        super.runRemoteCommand();
    }
}
