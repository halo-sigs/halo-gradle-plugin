package io.github.guqing.plugin;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.SourceSet;

/**
 * @author guqing
 * @since 2.0.0
 */
public class HaloServerTask extends JavaExec {
    public static final String TASK_NAME = "haloServer";

    @Input
    final Property<Path> haloHome = getProject().getObjects().property(Path.class);

    @Input
    final Property<File> manifest = getProject().getObjects().property(File.class);

    @Input
    final Property<HaloPluginExtension> pluginEnvProperty =
        getProject().getObjects().property(HaloPluginExtension.class);

    public Property<Path> getHaloHome() {
        return haloHome;
    }

    public Property<File> getManifest() {
        return manifest;
    }

    public void sourceResources(SourceSet sourceSet) {
        File resourcesDir = sourceSet.getOutput().getResourcesDir();
        Set<File> srcDirs = sourceSet.getResources().getSrcDirs();
        setClasspath(getProject().files(srcDirs, getClasspath())
            .filter((file) -> !file.equals(resourcesDir)));
    }

    public Property<HaloPluginExtension> getPluginEnvProperty() {
        return pluginEnvProperty;
    }

    @Override
    public void exec() {
        HaloPluginExtension haloPluginEnv = pluginEnvProperty.get();
        classpath(Paths.get(haloHome.get().resolve("halo.jar").toString()));
        args(haloHome.get().resolve("halo.jar").toFile(),
            "--halo.work-dir=" + haloHome.get(),
            "--halo.security.super-admin-username=" + haloPluginEnv.getSecurity()
                .getSuperAdminUsername(),
            "--halo.security.super-admin-password=" + haloPluginEnv.getSecurity()
                .getSuperAdminPassword(),
            "--halo.plugin.fixed-plugin-path=" + getProject().getProjectDir(),
            "--halo.plugin.runtime-mode=development",
            "--halo.plugin.plugins-root=" + haloHome.get().resolve("plugins"),
            "--initial-extension-locations=" + manifest.get(),
            "--springdoc.api-docs.enabled=true",
            "--springdoc.swagger-ui.enabled=true");
        super.exec();
    }
}
