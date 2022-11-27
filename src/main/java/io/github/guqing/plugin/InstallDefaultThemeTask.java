package io.github.guqing.plugin;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

public class InstallDefaultThemeTask extends DefaultTask {
    public static final String TASK_NAME = "defaultThemeInstall";
    private static final String THEME_TMP_PREFIX = "halo-theme-";
    public static final String DEFAULT_THEME_DIR = "themes/theme-earth";
    @Input
    final Property<String> themeUrl = getProject().getObjects().property(String.class);

    @TaskAction
    public void installTheme() throws IOException {
        HaloPluginExtension pluginEnv = (HaloPluginExtension) getProject()
            .getExtensions()
            .getByName(HaloPluginExtension.EXTENSION_NAME);
        Path targetThemePath = pluginEnv.getWorkDir()
            .resolve(DEFAULT_THEME_DIR);
        if (!Files.exists(targetThemePath)) {
            Files.createDirectories(targetThemePath);
        }
        if (!FileUtils.isEmpty(targetThemePath)) {
            return;
        }
        Path tempDirectory = Files.createTempDirectory(THEME_TMP_PREFIX);
        Path defaultThemeZipPath = tempDirectory.resolve("theme-earth.zip");
        URL website = new URL(themeUrl.get());
        try (BufferedInputStream in = new BufferedInputStream(website.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(defaultThemeZipPath.toFile())) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
            installTheme(new FileInputStream(defaultThemeZipPath.toFile()), targetThemePath);
        } catch (IOException e) {
            FileUtils.deleteRecursively(tempDirectory);
            throw new IllegalStateException(
                "Failed to download default theme [theme-earth] from " + website, e);
        }
    }

    private Path installTheme(InputStream inputStream, Path themeTargetPath) throws IOException {
        Path themePath;
        Path tempDirectory = null;
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            tempDirectory = Files.createTempDirectory(THEME_TMP_PREFIX);
            FileUtils.unzip(zipInputStream, tempDirectory);
            try (Stream<Path> walk = Files.walk(tempDirectory, 3)) {
                themePath = walk.filter(path ->
                        path.endsWith("theme.yaml") || path.endsWith("theme.yml"))
                    .map(Path::getParent).findAny().orElseThrow(() -> new IllegalArgumentException(
                        "It's an invalid zip format for the theme, manifest "
                            + "file [theme.yaml] is required."));
            }
            // install theme to theme work dir
            FileUtils.copyRecursively(themePath, themeTargetPath);
            return themePath;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize theme", e);
        } finally {
            FileUtils.deleteRecursively(tempDirectory);
        }
    }

    public Property<String> getThemeUrl() {
        return themeUrl;
    }
}
