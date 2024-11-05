package run.halo.gradle.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URI;
import lombok.experimental.UtilityClass;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import run.halo.gradle.utils.JsonUtils;
import run.halo.gradle.utils.RetryUtils;

@UtilityClass
public class HttpUtils {

    public static String getEntityString(CloseableHttpResponse response) {
        try (response) {
            return EntityUtils.toString(response.getEntity());
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isSuccessful(HttpResponse response) {
        return response.getCode() >= 200 && response.getCode() < 300;
    }

    public static void waitForReadiness(URI externalUrl, CloseableHttpClient client) {
        var httpGet = new HttpGet(externalUrl.resolve("/actuator/health"));
        RetryUtils.withRetry(20, 400, () -> {
            try {
                var response = client.execute(httpGet);
                var body = getEntityString(response);
                return isSuccessful(response) && isUp(body);
            } catch (IOException e) {
                // ignore
                return false;
            }
        });
    }

    static boolean isUp(String body) throws JsonProcessingException {
        return JsonUtils.mapper().readTree(body)
            .path("status").asText()
            .equalsIgnoreCase("UP");
    }
}
