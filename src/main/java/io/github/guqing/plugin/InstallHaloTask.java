package io.github.guqing.plugin;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
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
        URL website = new URL(downloadUrl(pluginEnv.getRequire()));
        try (BufferedInputStream in = new BufferedInputStream(website.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(targetJarPath.toFile())) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            // handle exception
            try {
                Files.deleteIfExists(targetJarPath);
            } catch (IOException ex) {
                // ignore
            }
            throw new IllegalStateException("Failed to download halo.jar from " + website, e);
        }
    }

    private String downloadUrl(String version) {
        // https://docs.gradle.org/7.4/userguide/build_environment.html#gradle_system_properties
        return "http://image-guqing.test.upcdn.net/halo-2.0.0-SNAPSHOT.jar";
//        String repository = StringUtils.appendIfMissing(serverRepository.get(), "/");
//        String v = StringUtils.removeStart(version, "v");
//        Semver semver = new Semver(v);
//        return repository + "halo-" + semver.getValue() + ".jar";
    }

    public Property<String> getServerRepository() {
        return serverRepository;
    }

    public Property<Configuration> getConfigurationProperty() {
        return configurationProperty;
    }
}
