package run.halo.gradle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

/**
 * @author guqing
 * @since 2.0.0
 */
public class InstallHaloTask extends DefaultTask {
    public static final String TASK_NAME = "serverInstall";

    @Input
    final Property<String> serverRepository = getProject().getObjects().property(String.class);

    @Input
    final Property<Dependency> serverBootJar = getProject().getObjects().property(Dependency.class);

    @Input
    final Property<Configuration> configurationProperty = getProject().getObjects()
        .property(Configuration.class);

    @TaskAction
    public void downloadJar() throws IOException {
        HaloPluginExtension pluginEnv = (HaloPluginExtension) getProject()
            .getExtensions()
            .getByName(HaloPluginExtension.EXTENSION_NAME);
        Path targetJarPath = pluginEnv.getWorkDir().resolve("halo.jar");
        if (Files.exists(targetJarPath)) {
            return;
        }

        getProject().getRepositories().maven(repo -> {
            repo.setName("HaloPackages");
            repo.setUrl(serverRepository.get());
        });

        Configuration compileOnly = getProject().getConfigurations().getByName("compileOnly");
        compileOnly.setCanBeResolved(true);
        DependencySet compileDeps = compileOnly.getDependencies();

        Dependency dependency = serverBootJar.get();
        compileDeps.add(dependency);

        File boorJar = compileOnly.files(file -> file.equals(dependency))
            .stream()
            .filter(file -> file.getName().endsWith(getBootJarName(dependency)))
            .findAny()
            .orElseThrow(() -> new RuntimeException(
                String.format("Halo boot jar [%s] not found", getBootJarName(dependency))));

        Files.copy(new FileInputStream(boorJar), targetJarPath);
    }

    private static String getBootJarName(Dependency dependency) {
        return dependency.getName() + "-" + dependency.getVersion() + "-boot.jar";
    }

    public Property<String> getServerRepository() {
        return serverRepository;
    }

    public Property<Configuration> getConfigurationProperty() {
        return configurationProperty;
    }

    public Property<Dependency> getServerBootJar() {
        return serverBootJar;
    }
}
