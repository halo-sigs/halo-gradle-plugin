package run.halo.gradle.steps;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import run.halo.gradle.Assert;
import run.halo.gradle.RetryUtils;

/**
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
public class ReloadPluginStep {
    private final HttpClient client;
    private final String host;

    public ReloadPluginStep(String host, HttpClient client) {
        Assert.notNull(host, "The host must not be null.");
        Assert.notNull(client, "The httpClient must not be null.");
        this.client = client;
        this.host = host;
    }

    public void execute(String pluginName, File file) {
        try {
            boolean exists = checkPluginExists(client, pluginName);
            if (exists) {
                uninstallPlugin(client, pluginName);
            }
            installPlugin(client, file);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private URI buildUri(String endpoint) {
        String path = StringUtils.prependIfMissing(endpoint, "/");
        String hostPrepared = StringUtils.removeEnd(host, "/");
        try {
            return new URI(hostPrepared + path);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isSuccessful(HttpResponse<String> response) {
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }

    private boolean is404(HttpResponse<String> response) {
        return response.statusCode() == 404;
    }

    private void installPlugin(HttpClient client, File pluginFile)
        throws IOException, InterruptedException {
        String multipartFormDataBoundary = "Java11HttpClientFormBoundary";
        HttpRequest installRequest = HttpRequest.newBuilder()
            .uri(buildUri("/apis/api.console.halo.run/v1alpha1/plugins/install"))
            .header("Content-Type", "multipart/form-data; boundary=Java11HttpClientFormBoundary")
            .POST(HttpRequest.BodyPublishers.ofInputStream(() -> {
                try (HttpEntity file = MultipartEntityBuilder.create()
                    .addPart("file", new FileBody(pluginFile, ContentType.DEFAULT_BINARY))
                    //要设置，否则阻塞
                    .setBoundary(multipartFormDataBoundary)
                    .build()) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    file.writeTo(byteArrayOutputStream);
                    return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }))
            .build();
        HttpResponse<String> response =
            client.send(installRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (isSuccessful(response)) {
            log.info("Install plugin successfully.");
        } else {
            log.error("Install plugin failed, [{}].", response.body());
        }
    }

    private void uninstallPlugin(HttpClient client, String pluginName)
        throws IOException, InterruptedException {
        HttpRequest uninstallRequest = HttpRequest.newBuilder()
            .uri(buildUri("/apis/plugin.halo.run/v1alpha1/plugins/" + pluginName))
            .DELETE()
            .build();
        HttpResponse<String> response =
            client.send(uninstallRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (isSuccessful(response)) {
            waitForUninstall(client, pluginName);
            log.info("Uninstall plugin successfully.");
        } else {
            throw new IllegalStateException("Uninstall plugin failed," + response.body());
        }
    }

    private void waitForUninstall(HttpClient client, String pluginName) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(buildUri("/apis/plugin.halo.run/v1alpha1/plugins/" + pluginName))
            .GET()
            .build();
        RetryUtils.withRetry(20, 500, () -> {
            HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());
            return is404(response);
        });
    }

    private boolean checkPluginExists(HttpClient client, String pluginName)
        throws IOException, InterruptedException {
        HttpRequest getPluginRequest = HttpRequest.newBuilder()
            .uri(buildUri("/apis/plugin.halo.run/v1alpha1/plugins/" + pluginName))
            .GET()
            .build();
        HttpResponse<String> response = client.send(getPluginRequest,
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return isSuccessful(response);
    }
}
