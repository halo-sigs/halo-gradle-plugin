package run.halo.gradle;

import java.net.http.HttpClient;
import lombok.Getter;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import run.halo.gradle.steps.CreateHttpClientStep;
import run.halo.gradle.steps.ReloadPluginStep;

/**
 * A task to reload plugin by name.
 *
 * @author guqing
 * @see ReloadPluginStep
 * @see HaloExtension
 * @see HaloPluginExtension
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Not worth caching")
public class ReloadPluginTask extends DefaultTask {
    public static final String TASK_NAME = "reloadPlugin";

    @Input
    @Getter
    private final Property<String> pluginName = getProject().getObjects().property(String.class);

    private final ReloadPluginStep reloadPluginStep;

    public ReloadPluginTask() {
        HaloExtension haloExt = getProject().getExtensions().getByType(HaloExtension.class);
        reloadPluginStep = new ReloadPluginStep(haloExt.getExternalUrl(), createHttpClient());
    }

    @TaskAction
    public void reloadPlugin() {
        reloadPluginStep.execute(pluginName.get());
    }

    private HttpClient createHttpClient() {
        HaloExtension haloExt = getProject().getExtensions().getByType(HaloExtension.class);
        String username = haloExt.getSuperAdminUsername();
        String password = haloExt.getSuperAdminPassword();
        return new CreateHttpClientStep(username, password).create();
    }
}
