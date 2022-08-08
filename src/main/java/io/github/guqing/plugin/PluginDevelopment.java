package io.github.guqing.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * @author guqing
 * @since 2.0.0
 */
public class PluginDevelopment implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        System.out.println("Halo plugin development gradle plugin run...");
        project.getTasks().register("haloRun", HaloRunTask.class);
    }
}
