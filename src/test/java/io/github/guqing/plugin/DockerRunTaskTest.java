package io.github.guqing.plugin;

import org.gradle.internal.classpath.ClassPath;
import org.gradle.internal.classpath.DefaultClassPath;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.internal.PluginUnderTestMetadataReading;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * @author guqing
 * @since 2.0.0
 */
public class DockerRunTaskTest {
    private File projectDir;

    private File buildFile;

    private File pluginManifestFile;

    @BeforeEach
    void setup(@TempDir File projectDir) {
        this.projectDir = projectDir;
        this.buildFile = new File(this.projectDir, "build.gradle");
        System.out.println(buildFile);
        this.pluginManifestFile = new File(this.projectDir, "src/main/resources/plugin.yaml");
    }

    private BuildResult runGradle(String... args) {
        return GradleRunner.create().withDebug(true)
            .withProjectDir(this.projectDir)
            .withArguments(args)
            .forwardOutput()
            .withPluginClasspath().build();
    }

    @Test
    @Disabled
    void autoVersionTaskTest() throws IOException {
        createPluginManifestFile();
        try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
            out.println("plugins {");
            out.println("    id 'io.github.guqing.plugin-development'");
            out.println("}");
            out.println("group 'io.github.guqing'");
            out.println("version '1.0.0'");

            out.println("halo {");
            out.println("   watchDomain {");
            out.println("       testDomain {");
            out.println("         files files('src/')");
            out.println("         exclude '**/plugin.yaml'");
            out.println("       }");
            out.println("   }");
            out.println("}");
        }
        ClassPath classPath = DefaultClassPath.of(PluginUnderTestMetadataReading.readImplementationClasspath());
        String asURLs = classPath.getAsURLs().stream().map(URL::toString).collect(Collectors.joining(", "));
        BuildResult buildResult = runGradle("watch", "-Pclasspath="+asURLs);
        System.out.println(buildResult.getOutput());
    }

    private void createPluginManifestFile() throws IOException {
        Path parent = this.pluginManifestFile.toPath().getParent();
        if (!Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        try (PrintWriter out = new PrintWriter(new FileWriter(this.pluginManifestFile))) {
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
