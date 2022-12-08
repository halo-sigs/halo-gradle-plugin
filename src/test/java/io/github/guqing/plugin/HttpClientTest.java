package io.github.guqing.plugin;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.junit.jupiter.api.Test;

public class HttpClientTest {

    @Test
    void test() throws URISyntaxException, IOException, InterruptedException {
        String pluginName = "plugin-colorless";

        HttpClient client = HttpClient.newBuilder()
            .authenticator(new Authenticator() {

                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                        "guqing",
                        "123456".toCharArray());
                }
            })
            .build();
        boolean b = checkPluginExists(client, pluginName);
        if (!b) {
            // todo change to upload plugin
            // installPlugin(client, null);
        } else {
            uninstallPlugin(client, pluginName);
            System.out.println("卸载完成");
        }
    }

    private void installPlugin(HttpClient client, File pluginFile)
        throws IOException, URISyntaxException, InterruptedException {
        String multipartFormDataBoundary = "Java11HttpClientFormBoundary";
        HttpRequest installRequest = HttpRequest.newBuilder()
            .uri(new URI(
                "http://localhost:8090/apis/api.console.halo.run/v1alpha1/plugins/install"))
            .header("Content-Type", "multipart/form-data; boundary=Java11HttpClientFormBoundary")
            .POST(HttpRequest.BodyPublishers.ofInputStream(() -> {
                try (HttpEntity file = MultipartEntityBuilder.create()
                    .addPart("file", new FileBody(pluginFile, ContentType.DEFAULT_BINARY))
                    .setBoundary(multipartFormDataBoundary) //要设置，否则阻塞
                    .build()) {
                    return file.getContent();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }))
            .build();
        HttpResponse<String> response =
            client.send(installRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println(response);
        System.out.println(response.body());
    }

    private void uninstallPlugin(HttpClient client, String pluginName)
        throws URISyntaxException, IOException, InterruptedException {
        HttpRequest uninstallRequest = HttpRequest.newBuilder()
            .uri(new URI(
                "http://localhost:8090/apis/plugin.halo.run/v1alpha1/plugins/" + pluginName))
            .DELETE()
            .build();
        HttpResponse<String> response =
            client.send(uninstallRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println(response);
        System.out.println(response.body());
    }

    private boolean checkPluginExists(HttpClient client, String pluginName)
        throws URISyntaxException, IOException, InterruptedException {
        HttpRequest getPluginRequest = HttpRequest.newBuilder()
            .uri(new URI(
                "http://localhost:8090/apis/plugin.halo.run/v1alpha1/plugins/" + pluginName))
            .GET()
            .build();
        HttpResponse<String> response = client.send(getPluginRequest,
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return !Objects.equals(404, response.statusCode());
    }
}
