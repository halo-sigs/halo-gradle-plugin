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
    public static final String TASK_NAME = "server";

    @Input
    final Property<Path> haloHome = getProject().getObjects().property(Path.class);

    public Property<Path> getHaloHome() {
        return haloHome;
    }

    public void sourceResources(SourceSet sourceSet) {
        File resourcesDir = sourceSet.getOutput().getResourcesDir();
        Set<File> srcDirs = sourceSet.getResources().getSrcDirs();
        setClasspath(getProject().files(srcDirs, getClasspath())
            .filter((file) -> !file.equals(resourcesDir)));
    }

    @Override
    public void exec() {
        classpath(Paths.get(haloHome.get().resolve("halo.jar").toString()));
        args(haloHome.get().resolve("halo.jar").toFile(),
            "--halo.work-dir=" + haloHome.get(),
            "--halo.plugin.runtime-mode=development",
            "--halo.plugin.plugins-root=/Users/guqing/Development/workspace/plugins",
            "--initial-extension-locations=classpath:plugin.yaml",
            "--springdoc.api-docs.enabled=true",
            "--springdoc.swagger-ui.enabled=true");
        super.exec();
    }
}
