package io.github.guqing.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;

/**
 * @author guqing
 * @since 2.0.0
 */
public class PluginDevelopment implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        System.out.println("Halo plugin development gradle plugin run...");
//        project.getTasks().register("haloRun", HaloRunTask.class);
        HaloPluginEnv pluginEnv = project.getExtensions()
            .create(HaloPluginEnv.EXTENSION_NAME, HaloPluginEnv.class, project);


        Configuration serverRuntime =
            project.getConfigurations().create("haloServerRuntimeOnly", c -> {
                c.withDependencies(deps -> {
                    String warNotation =
                        "org.jenkins-ci.main:jenkins-war:${ext.jenkinsVersion.get()}@war";
                    deps.add(project.getDependencies().create(warNotation));
                });
            });

        project.getTasks().register(HaloServerTask.TASK_NAME, HaloServerTask.class, it -> {
            it.setDescription("Run Halo server locally with the plugin being developed");
            it.setGroup("Halo Server");
            it.haloServerRuntime.set(serverRuntime);
            it.haloHome.set(pluginEnv.getWorkDir().toFile());
            String sysPropPort = System.getProperty("server.port");
            if (sysPropPort != null) {
                it.port.convention(sysPropPort);
            }
            String propPort = (String) project.findProperty("server.port");
            if (propPort != null) {
                it.port.convention(propPort);
            }
        });
    }
}
