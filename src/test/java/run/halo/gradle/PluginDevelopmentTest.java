package run.halo.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author guqing
 * @since 2.0.0
 */
public class PluginDevelopmentTest {

    private File projectDir;

    private File buildFile;

    private File pluginManifestFile;

    @BeforeEach
    void setup(@TempDir File projectDir) {
        this.projectDir = projectDir;
        this.buildFile = new File(this.projectDir, "build.gradle");
        this.pluginManifestFile = new File(this.projectDir, "src/main/resources/plugin.yaml");
    }

    private BuildResult runGradle(String... args) {
        return GradleRunner.create().withDebug(true)
            .withProjectDir(this.projectDir)
            .withArguments(args)
            .withPluginClasspath().build();
    }

    @Test
    void autoVersionTaskTest() throws IOException {
        createPluginManifestFile();
        try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
            out.println("plugins {");
            out.println("    id 'io.github.guqing.plugin-development'");
            out.println("}");
            out.println("group 'io.github.guqing'");
            out.println("version '1.0.0'");
        }
        BuildResult buildResult = runGradle(PluginAutoVersionTask.TASK_NAME, "-s");
        System.out.println(buildResult.getOutput());

        File outputPluginYaml = new File(projectDir, "build/resources/main/plugin.yaml");
        String pluginYaml =
            String.join("\n", Files.readAllLines(outputPluginYaml.toPath()));
        assertEquals("""
            ---
            apiVersion: "plugin.halo.run/v1alpha1"
            kind: "Plugin"
            metadata:
              name: "PluginSitemap"
            spec:
              enabled: true
              version: "1.0.0"
              requires: ">=2.0.0-beta.2"
            """, pluginYaml + "\n");
    }


    @Test
    void defaultThemeInstallTask() throws IOException {
        createPluginManifestFile();
        try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
            out.println("plugins {");
            out.println("    id 'io.github.guqing.plugin-development'");
            out.println("}");
            out.println("group 'io.github.guqing'");
            out.println("version '1.0.0'");
        }
        BuildResult buildResult = runGradle(InstallDefaultThemeTask.TASK_NAME, "-s");
        System.out.println(buildResult.getOutput());
        try (Stream<Path> paths = Files.list(
            Path.of(this.projectDir.getAbsolutePath(), "workplace/themes"))) {
            Path themePath = paths.filter(path -> path.endsWith("theme-earth"))
                .findFirst()
                .orElseThrow();
            // not empty
            assertFalse(FileUtils.isEmpty(themePath));
        }
    }

    private void createPluginManifestFile() throws IOException {
        Path parent = this.pluginManifestFile.toPath().getParent();
        if (!Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        try (PrintWriter out = new PrintWriter(new FileWriter(this.pluginManifestFile))) {
            /*
             * apiVersion: plugin.halo.run/v1alpha1
             * kind: Plugin
             * metadata:
             *   name: PluginSitemap
             * spec:
             *   enabled: true
             *   version: 1.0.0-alpha.3
             *   requires: ">=2.0.0-beta.2"
             */
            out.println("apiVersion: plugin.halo.run/v1alpha1");
            out.println("kind: Plugin");
            out.println("metadata:");
            out.println("  name: PluginSitemap");
            out.println("spec:");
            out.println("  enabled: true");
            out.println("  version: 1.0.0-alpha.3");
            out.println("  requires: \">=2.0.0-beta.2\"");
        }
    }
}
