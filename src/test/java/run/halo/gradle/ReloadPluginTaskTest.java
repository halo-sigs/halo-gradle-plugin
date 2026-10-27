package run.halo.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import run.halo.gradle.docker.DockerCreateContainer;
import run.halo.gradle.utils.HaloServerConfigure;
import run.halo.gradle.utils.JsonUtils;

class ReloadPluginTaskTest {

    @TempDir
    Path projectDir;

    private HttpServer server;
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private String inspectionBody;
    private int inspectionStatus = 200;

    @BeforeEach
    void setup() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handleRequest);
        server.start();
        Files.writeString(projectDir.resolve("settings.gradle"),
            "rootProject.name = 'reload-test'");
        Path manifest = projectDir.resolve("src/main/resources/plugin.yaml");
        Files.createDirectories(manifest.getParent());
        Files.writeString(manifest, """
            apiVersion: plugin.halo.run/v1alpha1
            kind: Plugin
            metadata:
              name: reload-test
            spec:
              version: 1.0.0
              requires: ">=2.0.0"
            """);
        setContainerState(true, serverUrl());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void reloadsRecordedContainerWithoutRepeatingPort() throws IOException {
        writeBuild("halo { port = 1 }");
        recordContainer("running-halo");

        var result = runner().build();

        assertThat(result.getOutput()).contains("BUILD SUCCESSFUL");
        assertThat(requests).contains("GET /v1.42/containers/running-halo/json", reloadRequest());
        assertThat(requests.stream().filter(reloadRequest()::equals).count()).isEqualTo(1);
    }

    @Test
    void usesConfiguredAddressWithoutContainerRecord() throws IOException {
        writeBuild("halo { port = " + server.getAddress().getPort() + " }");

        runner().build();

        assertThat(requests).contains(reloadRequest());
        assertThat(requests).noneMatch(request -> request.contains("/containers/"));
    }

    @Test
    void explicitPortOverridesStaleContainerRecord() throws IOException {
        writeBuild("halo { port = 1 }");
        recordContainer("removed-halo");
        inspectionStatus = 404;
        inspectionBody = "{\"message\":\"No such container\"}";

        runner("-Pport=" + server.getAddress().getPort()).build();

        assertThat(requests).contains(reloadRequest());
        assertThat(requests).noneMatch(request -> request.contains("/containers/"));
    }

    @Test
    void stoppedContainerDoesNotFallBackToConfiguredAddress() throws IOException {
        writeBuild("halo { port = " + server.getAddress().getPort() + " }");
        recordContainer("stopped-halo");
        setContainerState(false, serverUrl());

        var result = runner().buildAndFail();

        assertThat(result.getOutput()).contains("Recorded Halo container is not running");
        assertThat(requests).doesNotContain(reloadRequest());
    }

    @Test
    void removedContainerDoesNotFallBackToConfiguredAddress() throws IOException {
        writeBuild("halo { port = " + server.getAddress().getPort() + " }");
        recordContainer("removed-halo");
        inspectionStatus = 404;
        inspectionBody = "{\"message\":\"No such container\"}";

        var result = runner().buildAndFail();

        assertThat(result.getOutput()).contains("Recorded Halo container no longer exists");
        assertThat(requests).doesNotContain(reloadRequest());
    }

    @Test
    void invalidContainerAddressDoesNotFallBackToConfiguredAddress() throws IOException {
        writeBuild("halo { port = " + server.getAddress().getPort() + " }");
        recordContainer("invalid-halo");
        setContainerState(true, "invalid-address");

        var result = runner().buildAndFail();

        assertThat(result.getOutput()).contains("Cannot determine the recorded Halo container URL");
        assertThat(requests).doesNotContain(reloadRequest());
    }

    @Test
    void dockerFailureDoesNotFallBackToConfiguredAddress() throws IOException {
        writeBuild("halo { port = " + server.getAddress().getPort() + " }");
        recordContainer("unavailable-halo");
        inspectionStatus = 500;
        inspectionBody = "{\"message\":\"Docker unavailable\"}";

        runner().buildAndFail();

        assertThat(requests).contains("GET /v1.42/containers/unavailable-halo/json");
        assertThat(requests).doesNotContain(reloadRequest());
    }

    @Test
    void emptyRecordDoesNotFallBackToConfiguredAddress() throws IOException {
        writeBuild("halo { port = " + server.getAddress().getPort() + " }");
        recordContainer(" ");

        var result = runner().buildAndFail();

        assertThat(result.getOutput()).contains("Recorded Halo container ID is empty");
        assertThat(requests).isEmpty();
    }

    @Test
    void malformedContainerConfigurationIsNotExposedInError() throws IOException {
        writeBuild("halo { port = " + server.getAddress().getPort() + " }");
        recordContainer("invalid-halo");
        inspectionBody = JsonUtils.mapper().writeValueAsString(Map.of(
            "State", Map.of("Running", true),
            "Config", Map.of("Env", List.of("SPRING_APPLICATION_JSON={secret-marker"))));

        var result = runner().buildAndFail();

        assertThat(result.getOutput())
            .contains("Cannot determine the recorded Halo container URL")
            .doesNotContain("secret-marker");
        assertThat(requests).doesNotContain(reloadRequest());
    }

    private void writeBuild(String configuration) throws IOException {
        Files.writeString(projectDir.resolve("build.gradle"), """
            plugins {
                id 'run.halo.plugin.devtools'
            }
            halo {
                docker {
                    url = 'tcp://127.0.0.1:%d'
                    apiVersion = '1.42'
                }
            }
            %s
            afterEvaluate {
                tasks.named('reloadPlugin') {
                    setDependsOn([])
                }
            }
            """.formatted(server.getAddress().getPort(), configuration));
    }

    private void recordContainer(String id) throws IOException {
        Path record = projectDir.resolve(DockerCreateContainer.containerIdFilePath(":"));
        Files.createDirectories(record.getParent());
        Files.writeString(record, id);
    }

    private void setContainerState(boolean running, String url) throws IOException {
        String applicationJson = HaloServerConfigure.builder().externalUrl(url).build()
            .toApplicationJsonString();
        inspectionBody = JsonUtils.mapper().writeValueAsString(Map.of(
            "State", Map.of("Running", running),
            "Config", Map.of("Env", List.of("SPRING_APPLICATION_JSON=" + applicationJson))));
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        try (exchange) {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            requests.add(method + " " + path);
            exchange.getRequestBody().readAllBytes();
            boolean inspect = path.startsWith("/v1.42/containers/");
            String response = inspect ? inspectionBody : """
                {"metadata":{"name":"reload-test","version":%d},"spec":{"enabled":false}}
                """.formatted("PUT".equals(method) ? 1 : 2);
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(inspect ? inspectionStatus : 200, body.length);
            exchange.getResponseBody().write(body);
        }
    }

    private String serverUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static String reloadRequest() {
        return "PUT /apis/api.console.halo.run/v1alpha1/plugins/reload-test/reload";
    }

    private GradleRunner runner(String... options) {
        var arguments = new ArrayList<>(List.of("reloadPlugin", "--stacktrace"));
        arguments.addAll(List.of(options));
        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .withArguments(arguments);
    }
}
