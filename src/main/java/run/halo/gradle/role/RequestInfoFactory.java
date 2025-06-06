package run.halo.gradle.role;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * @see
 * <a href="https://github.com/halo-dev/halo/blob/17e9f2be1f63cbfe328d806987a284c702be79fe/application/src/main/java/run/halo/app/security/authorization/RequestInfoFactory.java#L17">Halo RequestInfoFactory</a>
 */
public class RequestInfoFactory {
    public static final RequestInfoFactory INSTANCE =
        new RequestInfoFactory(Set.of("api", "apis"), Set.of("api"));

    /**
     * without leading and trailing slashes.
     */
    final Set<String> apiPrefixes;

    /**
     * without leading and trailing slashes.
     */
    final Set<String> grouplessApiPrefixes;

    /**
     * special verbs no subresources.
     */
    final Set<String> specialVerbs;

    public RequestInfoFactory(Set<String> apiPrefixes, Set<String> grouplessApiPrefixes) {
        this(apiPrefixes, grouplessApiPrefixes, Set.of("proxy", "watch"));
    }

    public RequestInfoFactory(Set<String> apiPrefixes, Set<String> grouplessApiPrefixes,
        Set<String> specialVerbs) {
        this.apiPrefixes = apiPrefixes;
        this.grouplessApiPrefixes = grouplessApiPrefixes;
        this.specialVerbs = specialVerbs;
    }

    public RequestInfo newRequestInfo(String requestPath, String requestMethod) {
        // non-resource request default
        RequestInfo requestInfo =
            new RequestInfo(false, requestPath, requestMethod.toLowerCase());

        String[] currentParts = splitPath(requestPath);

        if (currentParts.length < 3) {
            // return a non-resource request
            return requestInfo;
        }

        if (!apiPrefixes.contains(currentParts[0])) {
            // return a non-resource request
            return requestInfo;
        }
        requestInfo.apiPrefix = currentParts[0];
        currentParts = Arrays.copyOfRange(currentParts, 1, currentParts.length);

        if (!grouplessApiPrefixes.contains(requestInfo.apiPrefix)) {
            // one part (APIPrefix) has already been consumed, so this is actually "do we have
            // four parts?"
            if (currentParts.length < 3) {
                // return a non-resource request
                return requestInfo;
            }

            requestInfo.apiGroup = StringUtils.defaultString(currentParts[0]);
            currentParts = Arrays.copyOfRange(currentParts, 1, currentParts.length);
        }
        requestInfo.isResourceRequest = true;
        requestInfo.apiVersion = currentParts[0];
        currentParts = Arrays.copyOfRange(currentParts, 1, currentParts.length);
        // handle input of form /{specialVerb}/*
        Set<String> specialVerbs = Set.of("proxy", "watch");
        if (specialVerbs.contains(currentParts[0])) {
            if (currentParts.length < 2) {
                throw new IllegalArgumentException(
                    String.format("unable to determine kind and namespace from url, %s",
                        requestPath));
            }
            requestInfo.verb = currentParts[0];
            currentParts = Arrays.copyOfRange(currentParts, 1, currentParts.length);
        } else {
            requestInfo.verb = switch (requestMethod.toUpperCase()) {
                case "POST" -> "create";
                case "GET", "HEAD" -> "get";
                case "PUT" -> "update";
                case "PATCH" -> "patch";
                case "DELETE" -> "delete";
                default -> "";
            };
        }
        // URL forms: /namespaces/{namespace}/{kind}/*, where parts are adjusted to be relative
        // to kind
        Set<String> namespaceSubresources = Set.of("status", "finalize");
        if (Objects.equals(currentParts[0], "namespaces")) {
            if (currentParts.length > 1) {
                requestInfo.namespace = currentParts[1];

                // if there is another step after the namespace name and it is not a known
                // namespace subresource
                // move currentParts to include it as a resource in its own right
                if (currentParts.length > 2 && !namespaceSubresources.contains(currentParts[2])) {
                    currentParts = Arrays.copyOfRange(currentParts, 2, currentParts.length);
                }
            }
        } else if ("userspaces".equals(currentParts[0])) {
            if (currentParts.length > 1) {
                requestInfo.userspace = currentParts[1];

                // if there is another step after the userspace name
                // move currentParts to include it as a resource in its own right
                if (currentParts.length > 2) {
                    currentParts = Arrays.copyOfRange(currentParts, 2, currentParts.length);
                }
            }
        } else {
            requestInfo.userspace = "";
            requestInfo.namespace = "";
        }

        // parsing successful, so we now know the proper value for .Parts
        requestInfo.parts = currentParts;
        // special verbs no subresources
        // parts look like: resource/resourceName/subresource/other/stuff/we/don't/interpret
        if (requestInfo.parts.length >= 3 && !specialVerbs.contains(
            requestInfo.verb)) {
            requestInfo.subresource = requestInfo.parts[2];
            // if there is another step after the subresource name, and it is not a known
            if (requestInfo.parts.length >= 4) {
                requestInfo.subName = requestInfo.parts[3];
            }
        }

        if (requestInfo.parts.length >= 2) {
            requestInfo.name = requestInfo.parts[1];
        }

        if (requestInfo.parts.length >= 1) {
            requestInfo.resource = requestInfo.parts[0];
        }

        // has name and no subresource but verb=create, then this is a non-resource request
        if (StringUtils.isNotBlank(requestInfo.name) && StringUtils.isBlank(requestInfo.subresource)
            && "create".equals(requestInfo.verb)) {
            requestInfo.isResourceRequest = false;
        }

        // if there's no name on the request, and we thought it was a get before, then the actual
        // verb is a list or a watch
        if (requestInfo.name.isEmpty() && "get".equals(requestInfo.verb)) {
            requestInfo.verb = "list";
        }
        return requestInfo;
    }

    private String[] splitPath(String path) {
        path = StringUtils.strip(path, "/");
        if (StringUtils.isEmpty(path)) {
            return new String[] {};
        }
        return StringUtils.split(path, "/");
    }
}
