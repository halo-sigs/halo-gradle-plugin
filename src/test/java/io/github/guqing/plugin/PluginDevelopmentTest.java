package io.github.guqing.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
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
        return GradleRunner.create().withDebug(true).withProjectDir(this.projectDir)
            .withArguments(args)
            .withPluginClasspath().build();
    }

    @Test
    void autoVersionTaskTest() throws IOException {
        createPluginManifestFile();
        try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
            out.println("plugins {");
            out.println("    id 'io.github.guqing.plugin-development'");
            out.println("    id 'com.coditory.manifest' version '0.2.1'");
            out.println("}");
            out.println("group 'io.github.guqing'");
            out.println("version '1.0.0'");
            out.println("manifest {");
            out.println("    buildAttributes = false");
            out.println("    implementationAttributes = true");
            out.println("    scmAttributes = false");
            out.println("}");
        }
        BuildResult buildResult = runGradle(PluginAutoVersionTask.TASK_NAME, "-s");
        System.out.println(buildResult.getOutput());
        String pluginYaml = String.join("\n", Files.readAllLines(this.pluginManifestFile.toPath()));
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
