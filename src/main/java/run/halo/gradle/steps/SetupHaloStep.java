package run.halo.gradle.steps;

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
            HttpUtils.waitForReadiness(haloSiteOption.externalUrl(), client);
            setup(client);
            var output = ConsoleOutputFormatter.printFormatted(
                "> Halo 启动成功！",
                requestUri("/console?language=zh-CN"),
                haloSiteOption.username(),
                haloSiteOption.password(),
                requestUri("swagger-ui.html")
            );
            System.out.println(output);
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
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
            new BasicNameValuePair("email", "custom@halo.run"),
            new BasicNameValuePair("externalUrl", haloSiteOption.externalUrl().toString())
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

    static class ConsoleOutputFormatter {

        public static String printFormatted(String title, URI url, String username,
            String password, URI apiDoc) {
            String[] lines = {
                String.format("%s", title),
                String.format("访问地址：%s", url),
                String.format("用户名：%s", username),
                String.format("密码：%s", password),
                String.format("API 文档：%s", apiDoc),
                "插件开发文档：https://docs.halo.run/developer-guide/plugin/introduction"
            };

            int maxWidth = 0;
            for (String line : lines) {
                int width = getDisplayWidth(line);
                if (width > maxWidth) {
                    maxWidth = width;
                }
            }

            // ANSI escape sequence for setting text color to green
            final String ANSI_GREEN = "\u001B[32m";
            final String ANSI_RESET = "\u001B[0m";

            String border = repeatChar('=', maxWidth);

            // Output top border
            var sb = new StringBuilder();
            sb.append(ANSI_GREEN).append(border).append(ANSI_RESET).append("\n");
            for (String line : lines) {
                String paddedLine = padRight(line, maxWidth);
                sb.append(paddedLine).append("\n");
            }
            // Output bottom border
            sb.append(ANSI_GREEN).append(border).append(ANSI_RESET).append("\n");
            return sb.toString();
        }

        public static int getDisplayWidth(String s) {
            int width = 0;
            for (int offset = 0; offset < s.length(); ) {
                int codePoint = s.codePointAt(offset);
                int charCount = Character.charCount(codePoint);
                if (isFullWidth(codePoint)) {
                    width += 2;
                } else {
                    width += 1;
                }
                offset += charCount;
            }
            return width;
        }

        public static boolean isFullWidth(int codePoint) {
            return (codePoint >= 0x1100 && codePoint <= 0x115F) || // Hangul Jamo
                (codePoint >= 0x2E80 && codePoint <= 0xA4CF) ||
                // CJK Radicals Supplement..Yi Radicals
                (codePoint >= 0xAC00 && codePoint <= 0xD7A3) || // Hangul Syllables
                (codePoint >= 0xF900 && codePoint <= 0xFAFF) || // CJK Compatibility Ideographs
                (codePoint >= 0xFE10 && codePoint <= 0xFE19) || // Vertical forms
                (codePoint >= 0xFE30 && codePoint <= 0xFE6F) || // CJK Compatibility Forms
                (codePoint >= 0xFF00 && codePoint <= 0xFF60) || // Fullwidth Forms
                (codePoint >= 0xFFE0 && codePoint <= 0xFFE6);   // Fullwidth Symbol Variants
        }

        public static String padRight(String s, int totalWidth) {
            int displayWidth = getDisplayWidth(s);
            int paddingWidth = totalWidth - displayWidth;
            return s + " ".repeat(Math.max(0, paddingWidth));
        }

        public static String repeatChar(char ch, int count) {
            return String.valueOf(ch).repeat(Math.max(0, count));
        }
    }
}
