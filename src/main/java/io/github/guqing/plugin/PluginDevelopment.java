package io.github.guqing.plugin;

import java.io.File;
import java.util.Set;
import lombok.val;
import org.gradle.BuildAdapter;
import org.gradle.BuildListener;
import org.gradle.BuildResult;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.initialization.Settings;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.composite.internal.DefaultIncludedBuildTaskGraph;

/**
 * @author guqing
 * @since 2.0.0
 */
public class PluginDevelopment implements Plugin<Project> {
    public static final String HALO_SERVER_DEPENDENCY_CONFIGURATION_NAME = "haloServer";
    public static final String GROUP = "halo server";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);
        System.out.println("Halo plugin development gradle plugin run...");

        HaloPluginEnv pluginEnv = project.getExtensions()
            .create(HaloPluginEnv.EXTENSION_NAME, HaloPluginEnv.class, project);
        // populate plugin manifest info
        File manifestFile = getPluginManifest(project);
        pluginEnv.setManifestFile(manifestFile);

        PluginManifest pluginManifest = YamlUtils.read(manifestFile, PluginManifest.class);
        pluginEnv.setRequire(pluginManifest.getSpec().getRequire());
        pluginEnv.setVersion(pluginManifest.getSpec().getVersion());

        project.getTasks()
            .register(PluginComponentsIndexTask.TASK_NAME, PluginComponentsIndexTask.class, it -> {
                it.setGroup(GROUP);
                FileCollection files =
                    project.getExtensions().getByType(SourceSetContainer.class).getByName("main")
                        .getOutput().getClassesDirs();
                it.classesDirs.from(files);
            });
        project.getTasks().getByName("build")
            .finalizedBy(PluginComponentsIndexTask.TASK_NAME);

        project.getTasks().register(InstallHaloTask.TASK_NAME, InstallHaloTask.class, it -> {
            it.setDescription("Install Halo server executable jar locally.");
            it.setGroup(GROUP);
            Configuration configuration =
                project.getConfigurations().create(HALO_SERVER_DEPENDENCY_CONFIGURATION_NAME);
            it.configurationProperty.set(configuration);
            it.serverRepository.set(pluginEnv.getServerRepository());
        });

        project.getTasks().register(HaloServerTask.TASK_NAME, HaloServerTask.class, it -> {
            it.setDescription("Run Halo server locally with the plugin being developed");
            it.setGroup(GROUP);
            it.pluginEnvProperty.set(pluginEnv);
            it.haloHome.set(pluginEnv.getWorkDir());
            it.manifest.set(pluginEnv.getManifestFile());
            it.dependsOn(InstallHaloTask.TASK_NAME);
        });
    }

    private File getPluginManifest(Project project) {
        SourceSetContainer sourceSetContainer =
            (SourceSetContainer) project.getProperties().get("sourceSets");
        File mainResourceDir = sourceSetContainer.stream()
            .filter(set -> "main".equals(set.getName()))
            .map(SourceSet::getResources)
            .map(SourceDirectorySet::getSrcDirs)
            .flatMap(Set::stream)
            .findFirst()
            .orElseThrow();

        for (String filename : HaloPluginEnv.MANIFEST) {
            File manifestFile = new File(mainResourceDir, filename);
            if (manifestFile.exists()) {
                return manifestFile;
            }
        }
        throw new IllegalStateException(
            "The plugin manifest file [plugin.yaml] not found in " + mainResourceDir);
    }
}
