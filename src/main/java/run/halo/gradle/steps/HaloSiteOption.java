package run.halo.gradle.steps;

import java.net.URI;
import run.halo.gradle.extension.HaloExtension;

public record HaloSiteOption(String username, String password, URI externalUrl) {

    public static HaloSiteOption from(HaloExtension extension) {
        var username = extension.getSuperAdminUsername();
        var password = extension.getSuperAdminPassword();
        var externalUrl = extension.getExternalUrl();
        return new HaloSiteOption(username, password, URI.create(externalUrl));
    }
}
