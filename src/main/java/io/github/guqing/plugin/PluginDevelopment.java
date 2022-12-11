package io.github.guqing.plugin;

import io.github.guqing.plugin.docker.AbstractDockerRemoteApiTask;
import io.github.guqing.plugin.docker.DockerClientConfiguration;
import io.github.guqing.plugin.docker.DockerClientService;
import io.github.guqing.plugin.docker.DockerCreateContainer;
import io.github.guqing.plugin.docker.DockerPullImage;
import io.github.guqing.plugin.docker.DockerRemoveContainer;
import io.github.guqing.plugin.docker.DockerStartContainer;
import io.github.guqing.plugin.docker.DockerStopContainer;
import io.github.guqing.plugin.watch.WatchTask;
import java.io.File;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

/**
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
public class PluginDevelopment implements Plugin<Project> {
    public static final String GROUP = "halo server";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);
        log.info("Halo plugin development gradle plugin run...");
        HaloPluginExtension haloPluginExt = project.getExtensions()
            .create(HaloPluginExtension.EXTENSION_NAME, HaloPluginExtension.class, project);
        // populate plugin manifest info
        File manifestFile = getPluginManifest(project);
        haloPluginExt.setManifestFile(manifestFile);

        PluginManifest pluginManifest = YamlUtils.read(manifestFile, PluginManifest.class);
        haloPluginExt.setRequire(pluginManifest.getSpec().getRequire());
        haloPluginExt.setPluginName(pluginManifest.getMetadata().getName());

        if (StringUtils.isBlank(pluginManifest.getMetadata().getName())) {
            throw new IllegalStateException("Plugin name must not be blank.");
        }

        haloPluginExt.setVersion((String) project.getVersion());
        System.setProperty("halo.plugin.name", pluginManifest.getMetadata().getName());

        project.getTasks()
            .register(PluginComponentsIndexTask.TASK_NAME, PluginComponentsIndexTask.class, it -> {
                it.setGroup(GROUP);
                FileCollection files =
                    project.getExtensions().getByType(SourceSetContainer.class).getByName("main")
                        .getOutput().getClassesDirs();
                it.classesDirs.from(files);
            });
        project.getTasks().getByName("assemble").dependsOn(PluginComponentsIndexTask.TASK_NAME);

        project.getTasks()
            .register(PluginAutoVersionTask.TASK_NAME, PluginAutoVersionTask.class, it -> {
                it.setDescription("Auto populate plugin version to manifest file.");
                it.setGroup(GROUP);
                it.manifest.set(manifestFile);
            });
        project.getTasks().getByName("assemble").dependsOn(PluginAutoVersionTask.TASK_NAME);

        DockerClientConfiguration dockerExtension = project.getExtensions()
            .create("dockerExtension", DockerClientConfiguration.class);
        dockerExtension.setUrl("unix:///var/run/docker.sock");

        final Provider<DockerClientService> serviceProvider = project.getGradle()
            .getSharedServices().registerIfAbsent("docker",
                DockerClientService.class,
                pBuildServiceSpec -> pBuildServiceSpec.parameters(parameters -> {
                    parameters.getUrl().set(dockerExtension.getUrl());
                    parameters.getCertPath().set(dockerExtension.getCertPath());
                    parameters.getApiVersion().set(dockerExtension.getApiVersion());
                }));

        project.getTasks().withType(AbstractDockerRemoteApiTask.class)
            .configureEach(task -> task.getDockerClientService().set(serviceProvider));
        DockerExtension docker = project.getExtensions().create("docker", DockerExtension.class);

        String require = haloPluginExt.getRequire();
        String imageName = docker.getImageName() + ":" + require;
        project.getTasks().create("pullHaloImage", DockerPullImage.class, it -> {
            it.getImage().set(imageName);
            it.setGroup(GROUP);
            it.setDescription("Pull halo server image from docker hub.");
        });

        DockerCreateContainer createContainer =
            project.getTasks().create("createHaloContainer", DockerCreateContainer.class, it -> {
                it.getImageId().set(imageName);
                it.getContainerName().set(docker.getContainerName());
                it.setGroup(GROUP);
                it.setDescription("Create halo server container.");
                it.dependsOn("build", "pullHaloImage");
            });

        project.getTasks().create("stopHalo", DockerStopContainer.class, it -> {
            it.setGroup(GROUP);
            it.getContainerId().set(createContainer.getContainerId());
            it.shouldRunAfter("runHalo");
            it.setDescription("Stop halo server container.");
        });

        project.getTasks().create("removeHalo", DockerRemoveContainer.class, it -> {
            it.setGroup(GROUP);
            it.getContainerId().set(createContainer.getContainerId());
            it.setDescription("Remove halo server container.");
        });
        project.getTasks().getByName("clean").dependsOn("removeHalo");

        project.getTasks().create("haloServer", DockerStartContainer.class, it -> {
            it.setGroup(GROUP);
            it.getContainerId().set(createContainer.getContainerId());
            it.setDescription("Run halo server container.");
            it.dependsOn("createHaloContainer");
            it.finalizedBy("removeHalo");
        });

        project.getTasks().create("watch", WatchTask.class, it -> {
            it.setGroup(GROUP);
            it.getContainerId().set(createContainer.getContainerId());
            it.dependsOn("createHaloContainer");
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

        for (String filename : HaloPluginExtension.MANIFEST) {
            File manifestFile = new File(mainResourceDir, filename);
            if (manifestFile.exists()) {
                return manifestFile;
            }
        }
        throw new IllegalStateException(
            "The plugin manifest file [plugin.yaml] not found in " + mainResourceDir);
    }
}
