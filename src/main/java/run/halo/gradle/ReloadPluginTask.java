package run.halo.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import run.halo.gradle.extension.HaloExtension;
import run.halo.gradle.extension.HaloPluginExtension;
import run.halo.gradle.steps.PluginClient;

/**
 * A task to reload plugin by name.
 *
 * @author guqing
 * @see HaloExtension
 * @see HaloPluginExtension
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Not worth caching")
public class ReloadPluginTask extends DefaultTask {
    public static final String TASK_NAME = "reloadPlugin";

    private final PluginClient pluginClient;

    public ReloadPluginTask() {
        this.pluginClient = new PluginClient(getProject());
    }

    @TaskAction
    public void reloadPlugin() {
        pluginClient.reloadPlugin();
    }
}
