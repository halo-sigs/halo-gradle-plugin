package run.halo.gradle.steps;

import java.net.URI;
import org.apache.commons.lang3.StringUtils;
import run.halo.gradle.extension.HaloExtension;

public record HaloSiteOption(String username, String password, URI externalUrl) {

    public static HaloSiteOption from(HaloExtension extension) {
        var username = extension.getSuperAdminUsername();
        var password = extension.getSuperAdminPassword();
        var externalUrl =
            StringUtils.defaultIfBlank(extension.getExternalUrl(), "http://localhost:8090");
        return new HaloSiteOption(username, password, URI.create(externalUrl));
    }
}
