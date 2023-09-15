package run.halo.gradle.steps;

import java.io.IOException;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.core5.http.HttpResponse;
import run.halo.gradle.Assert;

/**
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
public class ReloadPluginStep {
    private final HttpClientFactory httpClientFactory;
    private final URI externalUrl;

    public ReloadPluginStep(HaloSiteOption haloSiteOption) {
        Assert.notNull(haloSiteOption, "The haloSiteOption must not be null.");
        this.httpClientFactory = new HttpClientFactory(haloSiteOption);
        this.externalUrl = haloSiteOption.externalUrl();
    }

    public void execute(String pluginName) {
        try (var client = httpClientFactory.create()) {
            reloadPlugin(client, pluginName);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isSuccessful(HttpResponse response) {
        return response.getCode() >= 200 && response.getCode() < 300;
    }

    private void reloadPlugin(HttpClient client, String pluginName)
            throws IOException, InterruptedException {
        var httpPut =
                new HttpPut(externalUrl.resolve("/apis/api.console.halo.run/v1alpha1/plugins/" + pluginName + "/reload"));
        HttpResponse response = client.execute(httpPut);
        if (isSuccessful(response)) {
            log.info("Reload plugin successfully.");
        } else {
            throw new RuntimeException("Reload plugin failed, " + response.getReasonPhrase());
        }
    }
}
