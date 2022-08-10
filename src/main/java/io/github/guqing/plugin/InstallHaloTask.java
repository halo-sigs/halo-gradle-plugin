package io.github.guqing.plugin;

import com.vdurmont.semver4j.Semver;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

/**
 * @author guqing
 * @since 2.0.0
 */
public class InstallHaloTask extends DefaultTask {
    public static final String TASK_NAME = "install";

    @Input
    final Property<String> serverRepository = getProject().getObjects().property(String.class);

    @TaskAction
    public void downloadJar() throws MalformedURLException {
        HaloPluginEnv pluginEnv = (HaloPluginEnv) getProject()
            .getExtensions()
            .getByName(HaloPluginEnv.EXTENSION_NAME);
        Path targetJarPath = pluginEnv.getWorkDir().resolve("halo.jar");
        if (Files.exists(targetJarPath)) {
            return;
        }
        URL website = new URL(downloadUrl(pluginEnv.getRequire()));
        try (InputStream in = website.openStream()) {
            Files.copy(in, targetJarPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(targetJarPath);
            } catch (IOException ex) {
                // ignore
            }
            throw new IllegalStateException("Failed to download halo.jar from " + website, e);
        }
    }

    private String downloadUrl(String version) {
        String repository = StringUtils.appendIfMissing(serverRepository.get(), "/");
        String v = StringUtils.removeStart(version, "v");
        Semver semver = new Semver(v);
        return repository + "halo-" + semver.getValue() + ".jar";
    }

    public Property<String> getServerRepository() {
        return serverRepository;
    }
}
