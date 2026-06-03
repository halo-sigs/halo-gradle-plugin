package run.halo.gradle.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import run.halo.gradle.extension.HaloExtension;
import run.halo.gradle.extension.HaloPluginExtension;

class OpenApiDocsGeneratorTaskTest {

    @TempDir
    File projectDir;

    @Test
    void shouldGenerateDocsFromExistingServerWithoutDockerClientService() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            var executor = Executors.newSingleThreadExecutor();
            try {
                var requestLine = executor.submit(() -> handleOpenApiRequest(serverSocket));

                var project = ProjectBuilder.builder().withProjectDir(projectDir).build();
                project.getExtensions()
                    .create(HaloExtension.EXTENSION_NAME, HaloExtension.class,
                        project.getObjects());
                var pluginExtension = project.getExtensions()
                    .create(HaloPluginExtension.EXTENSION_NAME, HaloPluginExtension.class, project);
                pluginExtension.setPluginName("test-plugin");

                var openApi = pluginExtension.getOpenApi();
                openApi.getUseExistingServer().set(true);
                openApi.getApiDocsUrl().set("http://localhost:" + serverSocket.getLocalPort());
                openApi.getGroupedApiMappings()
                    .put("/v3/api-docs/extensionApis", "extensionApis.json");

                var task = project.getTasks()
                    .create(OpenApiDocsGeneratorTask.TASK_NAME, OpenApiDocsGeneratorTask.class);

                task.runRemoteCommand();

                var outputFile = openApi.getOutputDir().file("extensionApis.json").get().getAsFile();
                assertThat(Files.readString(outputFile.toPath()))
                    .contains("\"openapi\" : \"3.0.1\"");
                assertThat(requestLine.get()).startsWith("GET /v3/api-docs/extensionApis ");
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private static String handleOpenApiRequest(ServerSocket serverSocket) throws Exception {
        try (var socket = serverSocket.accept();
             var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),
                 StandardCharsets.UTF_8))) {
            var requestLine = reader.readLine();
            String line;
            while ((line = reader.readLine()) != null && !line.isBlank()) {
                // Drain headers before writing the response.
            }

            byte[] body = "{\"openapi\":\"3.0.1\"}".getBytes(StandardCharsets.UTF_8);
            byte[] headers = ("HTTP/1.1 200 OK\r\n"
                + "Content-Type: application/json\r\n"
                + "Content-Length: " + body.length + "\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8);
            socket.getOutputStream().write(headers);
            socket.getOutputStream().write(body);
            return requestLine;
        }
    }
}
