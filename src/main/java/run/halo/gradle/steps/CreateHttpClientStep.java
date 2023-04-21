package run.halo.gradle.steps;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.http.HttpClient;
import run.halo.gradle.Assert;

/**
 * @author guqing
 * @since 2.0.0
 */
public class CreateHttpClientStep {
    private final HttpClient client;

    public CreateHttpClientStep(String username, String password) {
        Assert.notNull(username, "username must not be null");
        Assert.notNull(password, "password must not be null");
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

    public HttpClient create() {
        return this.client;
    }
}
