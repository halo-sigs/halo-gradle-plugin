package run.halo.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HaloPortPropertyTest {

    @TempDir
    Path projectDir;

    @BeforeEach
    void setup() throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'port-test'");
        Path manifest = projectDir.resolve("src/main/resources/plugin.yaml");
        Files.createDirectories(manifest.getParent());
        Files.writeString(manifest, """
            apiVersion: plugin.halo.run/v1alpha1
            kind: Plugin
            metadata:
              name: port-test
            spec:
              version: 1.0.0
              requires: ">=2.0.0"
            """);
    }

    @Test
    void defaultsTo8090() throws IOException {
        writeBuild("");

        assertConfiguration(runner().build().getOutput(), 8090, "http://localhost:8090");
    }

    @Test
    void usesDslPortWhenProjectPropertyIsAbsent() throws IOException {
        writeBuild("halo { port = 8092 }");

        assertConfiguration(runner().build().getOutput(), 8092, "http://localhost:8092");
    }

    @Test
    void projectPropertyOverridesDslPortForAllClients() throws IOException {
        writeBuild("halo { port = 8092 }");

        assertConfiguration(runner("-Pport=8091").build().getOutput(),
            8091, "http://localhost:8091");
    }

    @Test
    void preservesExplicitExternalUrl() throws IOException {
        writeBuild("halo { externalUrl = 'https://halo.example.com' }");

        assertConfiguration(runner("-Pport=8091").build().getOutput(),
            8091, "https://halo.example.com");
    }

    @Test
    void rejectsInvalidPorts() throws IOException {
        writeBuild("");

        for (String port : List.of("abc", "0", "65536")) {
            assertThat(runner("-Pport=" + port).buildAndFail().getOutput())
                .contains("The 'port' project property must be an integer between 1 and 65535.");
        }
    }

    private void writeBuild(String configuration) throws IOException {
        Files.writeString(projectDir.resolve("build.gradle"), """
            plugins {
                id 'run.halo.plugin.devtools'
            }
            """ + configuration + "\n" + """
            tasks.register('checkPort') {
                doLast {
                    println "HALO_PORT=${halo.port}"
                    ['haloServer', 'watch', 'reloadPlugin'].each { name ->
                        def task = tasks.getByName(name)
                        def field = task.class.superclass.getDeclaredField('pluginClient')
                        field.setAccessible(true)
                        println "${name}_URL=${field.get(task).baseUri}"
                    }
                }
            }
            """);
    }

    private GradleRunner runner(String... options) {
        var arguments = new ArrayList<>(List.of("checkPort", "--stacktrace"));
        arguments.addAll(List.of(options));
        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments(arguments);
    }

    private static void assertConfiguration(String output, int port, String url) {
        assertThat(output).contains("HALO_PORT=" + port);
        for (String task : List.of("haloServer", "watch", "reloadPlugin")) {
            assertThat(output).contains(task + "_URL=" + url);
        }
    }
}
