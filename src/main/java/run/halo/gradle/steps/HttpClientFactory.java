package run.halo.gradle.steps;

import java.util.ArrayList;
import java.util.Base64;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicHeader;
import run.halo.gradle.Assert;

/**
 * @author guqing
 * @since 2.0.0
 */
public class HttpClientFactory {
    private final HaloSiteOption haloSiteOption;

    public HttpClientFactory(HaloSiteOption haloSiteOption) {
        Assert.notNull(haloSiteOption, "haloSiteOption must not be null");
        this.haloSiteOption = haloSiteOption;
    }

    public CloseableHttpClient create() {
        var headers = new ArrayList<Header>();
        headers.add(new BasicHeader("Authorization", getBasicAuthenticationHeader(haloSiteOption)));
        return HttpClients.custom()
                .setDefaultHeaders(headers)
                .build();
    }

    private static String getBasicAuthenticationHeader(HaloSiteOption haloSiteOption) {
        String valueToEncode = haloSiteOption.username() + ":" + haloSiteOption.password();
        return "Basic " + Base64.getEncoder()
                .encodeToString(valueToEncode.getBytes());
    }
}
