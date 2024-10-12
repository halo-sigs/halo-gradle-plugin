package run.halo.gradle.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import run.halo.gradle.utils.Assert;
import run.halo.gradle.utils.RetryUtils;
import run.halo.gradle.utils.YamlUtils;

/**
 * @author guqing
 * @since 0.0.1
 */
@Slf4j
public class SetupHaloStep {
    private final HaloSiteOption haloSiteOption;

    public SetupHaloStep(HaloSiteOption haloSiteOption) {
        Assert.notNull(haloSiteOption, "haloSiteOption must not be null");
        this.haloSiteOption = haloSiteOption;
    }

    public void execute() {
        try (var client = HttpClients.createDefault()) {
            waitForReadiness(client);
            setup(client);
            System.out.printf("Halo 初始化成功，访问： %s\n用户名：%s\n密码：%s%n",
                requestUri("/console"),
                haloSiteOption.username(),
                haloSiteOption.password()
            );
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private void waitForReadiness(CloseableHttpClient client) {
        var httpGet = new HttpGet(requestUri("/actuator/health"));
        RetryUtils.withRetry(20, 400, () -> {
            try {
                var response = client.execute(httpGet);
                var body = EntityUtils.toString(response.getEntity());
                return isSuccessful(response) && isUp(body);
            } catch (Exception e) {
                // ignore
                return false;
            }
        });
    }

    static boolean isUp(String body) throws JsonProcessingException {
        return YamlUtils.mapper.readTree(body)
            .path("status").asText()
            .equalsIgnoreCase("UP");
    }

    private void setup(CloseableHttpClient client) throws IOException, ParseException {
        var globalInfo = fetchGlobalInfo(client);
        if (isUserInitialized(globalInfo)) {
            return;
        }
        executeSetupRequest(client);
    }

    private void executeSetupRequest(CloseableHttpClient client)
        throws IOException, ParseException {
        var initializeRequest = getSetupRequest();
        var response = client.execute(initializeRequest);
        if (isSuccessful(response)) {
            log.info("Initialize system successfully.");
        } else {
            log.error("Initialize system failed: {}", EntityUtils.toString(response.getEntity()));
        }
    }

    private JsonNode fetchGlobalInfo(CloseableHttpClient client)
        throws IOException, ParseException {
        var globalInfoHttpGet = new HttpGet(requestUri("/actuator/globalinfo"));
        var globalInfoResp = client.execute(globalInfoHttpGet);
        var bodyStr = EntityUtils.toString(globalInfoResp.getEntity());
        return YamlUtils.mapper.readTree(bodyStr);
    }

    private boolean isUserInitialized(JsonNode globalInfo) {
        var userInitialized = globalInfo.get("userInitialized");
        return userInitialized != null && userInitialized.asBoolean();
    }

    private HttpPost getSetupRequest() {
        var setupRequest = new HttpPost(requestUri("/system/setup"));
        var entity = new UrlEncodedFormEntity(List.of(
            new BasicNameValuePair("siteTitle", "Halo"),
            new BasicNameValuePair("username", haloSiteOption.username()),
            new BasicNameValuePair("password", haloSiteOption.password()),
            new BasicNameValuePair("email", "custom@halo.run")
        ));
        setupRequest.setEntity(entity);
        setupRequest.addHeader(HttpHeaders.ACCEPT, "application/json");
        return setupRequest;
    }

    URI requestUri(String path) {
        return haloSiteOption.externalUrl().resolve(path);
    }

    private static boolean isSuccessful(CloseableHttpResponse response) {
        return response.getCode() >= 200 && response.getCode() < 300;
    }
}
