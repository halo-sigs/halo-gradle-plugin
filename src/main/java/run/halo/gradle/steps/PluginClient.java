package run.halo.gradle.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import lombok.Getter;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.gradle.api.Project;
import run.halo.gradle.extension.HaloExtension;
import run.halo.gradle.extension.HaloPluginExtension;
import run.halo.gradle.model.Plugin;
import run.halo.gradle.utils.JsonUtils;
import run.halo.gradle.utils.RetryUtils;

@Getter
public class PluginClient {
    private final String pluginName;
    private final HttpClientFactory clientFactory;
    private final HaloSiteOption siteOption;
    private final HaloPluginExtension pluginExtension;
    private final URI baseUri;

    public PluginClient(Project project) {
        var haloExt = project.getExtensions().getByType(HaloExtension.class);
        this.pluginExtension = project.getExtensions().getByType(HaloPluginExtension.class);
        this.pluginName = pluginExtension.getPluginName();
        this.siteOption = HaloSiteOption.from(haloExt);
        this.baseUri = siteOption.externalUrl();
        this.clientFactory = new HttpClientFactory(siteOption);
    }

    public void reloadPlugin() {
        try (var client = clientFactory.create()) {
            var httpPut =
                buildPut("/apis/api.console.halo.run/v1alpha1/plugins/" + pluginName + "/reload");
            var response = client.execute(httpPut);
            if (!HttpUtils.isSuccessful(response)) {
                throw new RuntimeException("Reload plugin failed, " + response.getReasonPhrase());
            }
            var plugin = pluginResponseToEntity(response);
            var reloadedVersion = plugin.getMetadata().getVersion();

            RetryUtils.withRetry(10, 100, () -> {
                var latestPlugin = getPlugin();
                if (latestPlugin == null) {
                    return false;
                }
                var latestVersion = latestPlugin.getMetadata().getVersion();
                return latestVersion > reloadedVersion;
            });
            CheckPluginStateHelper.checkState(this::getPlugin);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Plugin getPlugin() {
        var httpGet = buildGet("/apis/plugin.halo.run/v1alpha1/plugins/" + pluginName);
        try (var client = clientFactory.create()) {
            var response = client.execute(httpGet);
            if (!HttpUtils.isSuccessful(response)) {
                throw new FailedToGetPluginException(
                    "Failed to get plugin: " + response.getReasonPhrase());
            }
            var entity = HttpUtils.getEntityString(response);
            return JsonUtils.mapper().readValue(entity, Plugin.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void checkPluginState() {
        CheckPluginStateHelper.checkState(this::getPlugin);
    }

    private static Plugin pluginResponseToEntity(CloseableHttpResponse response) {
        var entity = HttpUtils.getEntityString(response);
        try {
            return JsonUtils.mapper().readValue(entity, Plugin.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse plugin response");
        }
    }

    private HttpGet buildGet(String path) {
        return new HttpGet(baseUri.resolve(path));
    }

    private HttpPut buildPut(String path) {
        return new HttpPut(baseUri.resolve(path));
    }

    public static class FailedToGetPluginException extends RuntimeException {
        public FailedToGetPluginException(String message) {
            super(message);
        }
    }
}
