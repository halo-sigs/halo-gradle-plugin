package run.halo.gradle.steps;

import java.io.IOException;
import java.net.URI;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import run.halo.gradle.Assert;
import run.halo.gradle.RetryUtils;
import run.halo.gradle.YamlUtils;

/**
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
public class InitializeHaloStep {
    private final HaloSiteOption haloSiteOption;

    public InitializeHaloStep(HaloSiteOption haloSiteOption) {
        Assert.notNull(haloSiteOption, "haloSiteOption must not be null");
        this.haloSiteOption = haloSiteOption;
    }

    public void execute() {
        try (var client = HttpClients.createDefault()) {
            waitForReadiness(client);
            initializeUserAccount(client);
            System.out.println("Halo 初始化成功，访问： " + requestUri("/console") + "\n" +
                    "用户名：" + haloSiteOption.username() + "\n" +
                    "密码：" + haloSiteOption.password() + "\n");
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void waitForReadiness(CloseableHttpClient client) {
        var httpGet = new HttpGet(requestUri("/actuator/health"));
        RetryUtils.withRetry(20, 400, () -> {
            try {
                var response = client.execute(httpGet);
                return isSuccessful(response);
            } catch (Exception e) {
                // ignore
                return false;
            }
        });
    }

    URI requestUri(String path) {
        return haloSiteOption.externalUrl().resolve(path);
    }

    private static boolean isSuccessful(CloseableHttpResponse response) {
        return response.getCode() >= 200 && response.getCode() < 300;
    }

    private void initializeUserAccount(CloseableHttpClient client)
            throws IOException, ParseException {
        var globalInfoHttpGet = new HttpGet(requestUri("/actuator/globalinfo"));
        var globalInfoResp = client.execute(globalInfoHttpGet);
        var globalInfo =
                YamlUtils.mapper.convertValue(EntityUtils.toString(globalInfoResp.getEntity()),
                        JsonNode.class);
        var userInitialized = globalInfo.get("userInitialized");
        if (userInitialized != null && userInitialized.asBoolean()) {
            return;
        }
        var initializeRequest = getInitializeRequest();
        var response = client.execute(initializeRequest);
        if (isSuccessful(response)) {
            log.info("Initialize system successfully.");
        } else {
            log.error("Initialize system failed: {}", EntityUtils.toString(response.getEntity()));
        }
    }

    private HttpPost getInitializeRequest() {
        var initializeRequest =
                new HttpPost(requestUri("/apis/api.console.halo.run/v1alpha1/system/initialize"));
        var entity = new StringEntity("""
                {
                    "siteTitle": "Halo",
                    "username": "%s",
                    "password": "%s",
                    "email": "admin@halo.run"
                }
                """.formatted(haloSiteOption.username(), haloSiteOption.password()),
                ContentType.APPLICATION_JSON);
        initializeRequest.setEntity(entity);
        return initializeRequest;
    }
}
