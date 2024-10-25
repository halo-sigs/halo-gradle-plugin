package run.halo.gradle.role;

import java.util.Objects;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

/**
 * @see
 * <a href="https://github.com/halo-dev/halo/blob/17e9f2be1f63cbfe328d806987a284c702be79fe/application/src/main/java/run/halo/app/security/authorization/RequestInfo.java#L17">Halo RequestInfo</a>
 */
@Getter
@ToString
public class RequestInfo {
    boolean isResourceRequest;
    final String path;
    String namespace;
    String userspace;
    String verb;
    String apiPrefix;
    String apiGroup;
    String apiVersion;
    String resource;

    String name;

    String subresource;

    String subName;

    String[] parts;

    public RequestInfo(boolean isResourceRequest, String path, String verb) {
        this(isResourceRequest, path, null, null, verb, null, null, null, null, null, null, null,
            null);
    }

    public RequestInfo(boolean isResourceRequest, String path, String namespace, String userspace,
        String verb,
        String apiPrefix,
        String apiGroup,
        String apiVersion, String resource, String name, String subresource, String subName,
        String[] parts) {
        this.isResourceRequest = isResourceRequest;
        this.path = StringUtils.defaultString(path);
        this.namespace = StringUtils.defaultString(namespace);
        this.userspace = StringUtils.defaultString(userspace);
        this.verb = StringUtils.defaultString(verb);
        this.apiPrefix = StringUtils.defaultString(apiPrefix);
        this.apiGroup = StringUtils.defaultString(apiGroup);
        this.apiVersion = StringUtils.defaultString(apiVersion);
        this.resource = StringUtils.defaultString(resource);
        this.subresource = StringUtils.defaultString(subresource);
        this.subName = StringUtils.defaultString(subName);
        this.name = StringUtils.defaultString(name);
        this.parts = Objects.requireNonNullElseGet(parts, () -> new String[] {});
    }
}
