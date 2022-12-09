package io.github.guqing.plugin.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.guqing.plugin.Assert;
import io.github.guqing.plugin.YamlUtils;
import io.github.guqing.plugin.model.ObjectNodeListResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
public class InitializeHaloStep {
    private final HttpClient client;
    private final String host;

    public InitializeHaloStep(String host, String username, String password) {
        Assert.notNull(username, "username must not be null");
        Assert.notNull(password, "password must not be null");
        this.host = StringUtils.defaultString(host, "http://localhost:8090");
        this.client = HttpClient.newBuilder()
                .authenticator(new Authenticator() {

                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                username,
                                password.toCharArray());
                    }
                })
                .build();
    }

    public void execute() {
        try {
            initializeHalo(client);
            initializeTheme(client);
            createMenu(client);
        } catch (IOException | URISyntaxException | InterruptedException e) {
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

    private void initializeHalo(HttpClient client) throws IOException, InterruptedException {
        HttpRequest checkSystemStates = HttpRequest.newBuilder()
                .uri(buildUri("/api/v1alpha1/configmaps/system-states"))
                .GET()
                .build();
        HttpResponse<String> checkResponse = client.send(checkSystemStates,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (isSuccessful(checkResponse)) {
            return;
        }
        HttpRequest createSystemStateConfig = HttpRequest.newBuilder()
                .uri(buildUri("/api/v1alpha1/configmaps"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                            "data": {
                                "states": "{\\"isSetup\\":true}"
                            },
                            "apiVersion": "v1alpha1",
                            "kind": "ConfigMap",
                            "metadata": {
                                "name": "system-states"
                            }
                        }
                        """))
                .build();
        HttpResponse<String> createResponse = client.send(createSystemStateConfig,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (!isSuccessful(createResponse)) {
            throw new RuntimeException(createResponse.body());
        }
    }

    private void initializeTheme(HttpClient client) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest listUninstalledTheme = HttpRequest.newBuilder()
                .uri(buildUri("/apis/api.console.halo.run/v1alpha1/themes?uninstalled=true"))
                .GET()
                .build();
        HttpResponse<String> listUninstalledThemResp = client.send(listUninstalledTheme,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (!isSuccessful(listUninstalledThemResp)) {
            throw new RuntimeException(listUninstalledThemResp.body());
        }
        String body = listUninstalledThemResp.body();
        ObjectNodeListResult listResult = YamlUtils.mapper.readValue(body, ObjectNodeListResult.class);
        if (listResult == null || listResult.getItems().isEmpty()) {
            return;
        }
        ObjectNode item = listResult.getItems().get(0);
        createTheme(client, item.toString());
    }

    private void createTheme(HttpClient client, String payload) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest installRequest = HttpRequest.newBuilder()
                .uri(new URI(
                        "http://localhost:8090/apis/theme.halo.run/v1alpha1/themes"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> response = client.send(installRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (!isSuccessful(response)) {
            throw new RuntimeException(response.body());
        }
        String body = response.body();
        ObjectNode theme = YamlUtils.mapper.readValue(body, ObjectNode.class);
        JsonNode nameNode = theme.at("/metadata/name");
        if (nameNode == null) {
            throw new IllegalStateException("Unexpected theme name from [" + theme + "]");
        }
        HttpRequest reloadSettingReq = HttpRequest.newBuilder()
                .uri(buildUri("/apis/api.console.halo.run/v1alpha1/themes/" + nameNode.asText() + "/reload"))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> reloadResponse = client.send(reloadSettingReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (!isSuccessful(reloadResponse)) {
            throw new IllegalStateException("Reload theme setting failed: " + reloadResponse.body());
        }
    }

    private void createMenu(HttpClient client) throws IOException, InterruptedException {
        UUID menuItemUid = UUID.randomUUID();
        HttpRequest installRequest = HttpRequest.newBuilder()
                .uri(buildUri("/api/v1alpha1/menus"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                            "spec": {
                                "displayName": "默认",
                                "menuItems": ["%s"]
                            },
                            "apiVersion": "v1alpha1",
                            "kind": "Menu",
                            "metadata": {
                                "name": "",
                                "generateName": "menu-"
                            }
                        }
                        """.formatted(menuItemUid)))
                .build();
        HttpResponse<String> response = client.send(installRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (!isSuccessful(response)) {
            throw new RuntimeException(response.body());
        }
        HttpRequest createMenuItem = HttpRequest.newBuilder()
                .uri(buildUri("/api/v1alpha1/menuitems"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                            "spec": {
                                "displayName": "首页",
                                "href": "/index",
                                "children": [],
                                "priority": 0
                            },
                            "apiVersion": "v1alpha1",
                            "kind": "MenuItem",
                            "metadata": {
                                "name": "%s"
                            }
                        }
                        """.formatted(menuItemUid)))
                .build();
        HttpResponse<String> menuItemResponse = client.send(createMenuItem,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (!isSuccessful(menuItemResponse)) {
            throw new RuntimeException(menuItemResponse.body());
        }
    }
}
