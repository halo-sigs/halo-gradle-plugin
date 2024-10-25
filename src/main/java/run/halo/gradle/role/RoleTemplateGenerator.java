package run.halo.gradle.role;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import run.halo.gradle.utils.JsonUtils;

@Slf4j
public class RoleTemplateGenerator {
    private final List<File> schemaJsonFiles;

    public RoleTemplateGenerator(List<File> schemaJsonFiles) {
        this.schemaJsonFiles = schemaJsonFiles;
    }

    public List<Role> createRoles() {
        var apiResources = parseApiResources();
        return createRoles(apiResources);
    }

    private List<Role> createRoles(List<ApiResource> apiResources) {
        var apiGroupResourceMap = new TreeMap<String, List<ResourceRequest>>();
        var nonResourceMap = new TreeMap<String, TreeSet<String>>();

        // categorize api resources
        apiResources.forEach(apiResource -> {
            if (apiResource.isResourceRequest()) {
                var resourceRequest = (ResourceRequest) apiResource;
                apiGroupResourceMap.computeIfAbsent(resourceRequest.apiGroup(),
                        k -> new ArrayList<>())
                    .add(resourceRequest);
            } else {
                var noneResourceRequest = (NoneResourceRequest) apiResource;
                nonResourceMap.computeIfAbsent(noneResourceRequest.resourceUrl(),
                        k -> new TreeSet<>())
                    .add(noneResourceRequest.verb());
            }
        });

        // generate rules
        var roles = new ArrayList<Role>();

        // for resource requests
        apiGroupResourceMap.forEach((apiGroup, resourceRequests) -> {
            var nameRequestsMap = resourceRequests.stream()
                .collect(Collectors.groupingBy(ResourceRequest::name));

            var role = createRole(buildResourceRoleName(apiGroup));

            nameRequestsMap.forEach((name, requests) -> {
                var builder = Role.PolicyRule.builder()
                    .apiGroups(new String[] {apiGroup});
                if (StringUtils.isNotBlank(name)) {
                    builder.resourceNames(new String[] {name});
                }

                // Collect resources and verbs once to avoid multiple stream operations
                var resourcesAndVerbs = requests.stream().collect(Collectors.teeing(
                    Collectors.mapping(request -> {
                        var resourceUrl = request.resource();
                        if (StringUtils.isNotBlank(request.subResource())) {
                            resourceUrl += "/" + request.subResource();
                        }
                        return resourceUrl;
                    }, Collectors.toSet()),
                    Collectors.mapping(ResourceRequest::verb, Collectors.toSet()),
                    ResourcesVerbs::new
                ));

                builder.resources(resourcesAndVerbs.resources().toArray(new String[0]));
                builder.verbs(resourcesAndVerbs.verbs().toArray(new String[0]));

                role.getRules().add(builder.build());
            });

            roles.add(role);
        });

        // for non-resource requests
        nonResourceMap.forEach((url, verbs) -> {
            var role = createRole(buildNonResourceRoleName());
            var builder = Role.PolicyRule.builder()
                .nonResourceURLs(new String[] {url})
                .verbs(verbs.toArray(new String[0]));
            role.getRules().add(builder.build());
            roles.add(role);
        });

        return roles;
    }

    record ResourcesVerbs(Set<String> resources, Set<String> verbs) {
    }

    @NonNull
    private static Role createRole(String roleName) {
        var role = new Role();
        var metadata = role.getMetadata();
        metadata.getLabels().put("halo.run/role-template", "true");
        metadata.getAnnotations().put("rbac.authorization.halo.run/module", "{所属模块}");
        metadata.getAnnotations()
            .put("rbac.authorization.halo.run/display-name", "{角色显示名称}");
        metadata.getAnnotations()
            .put("rbac.authorization.halo.run/ui-permissions", "['{定义 UI 权限}']");
        metadata.setName(roleName);
        return role;
    }

    private static String buildResourceRoleName(String apiGroup) {
        return "rt-" + apiGroup + "-" + RandomStringUtils.randomAlphabetic(6);
    }

    private static String buildNonResourceRoleName() {
        return "rt-" + RandomStringUtils.randomAlphabetic(10);
    }

    private List<ApiResource> parseApiResources() {
        var requests = parseRequestsFromSchemaFiles();
        var apiResources = new ArrayList<ApiResource>();
        requests.forEach(request -> {
            var requestInfo = parseRequestInfo(request);
            if (requestInfo.isResourceRequest()) {
                apiResources.add(ResourceRequest.builder()
                    .apiGroup(requestInfo.getApiGroup())
                    .resource(requestInfo.getResource())
                    .name(requestInfo.getName())
                    .subResource(requestInfo.getSubresource())
                    .verb(requestInfo.getVerb())
                    .build());
            } else {
                apiResources.add(NoneResourceRequest.builder()
                    .resourceUrl(requestInfo.getPath())
                    .verb(requestInfo.getVerb())
                    .build());
            }
        });
        return apiResources;
    }

    private RequestInfo parseRequestInfo(SimpleRequest simpleRequest) {
        return RequestInfoFactory.INSTANCE
            .newRequestInfo(simpleRequest.path(), simpleRequest.method());
    }

    private List<SimpleRequest> parseRequestsFromSchemaFiles() {
        var requests = new ArrayList<SimpleRequest>();
        schemaJsonFiles.forEach(file -> {
            requests.addAll(parseRequestsFromSchemaFile(file));
        });
        return requests;
    }

    /**
     * Example JSON file content:
     * <pre>
     * {
     *   "paths": {
     *     "/apis/api.notification.halo.run/v1alpha1/notifiers/{name}/receiver-config": {
     *       "get": {},
     *       "post": {},
     *     }
     *   }
     * }
     * </pre>
     */
    private List<SimpleRequest> parseRequestsFromSchemaFile(File file) {
        var node = readFileToJson(file);
        if (node == null || !node.has("paths")) {
            return List.of();
        }
        var pathsNode = node.get("paths");
        var requests = new ArrayList<SimpleRequest>();
        pathsNode.fields().forEachRemaining(pathNode -> {
            var requestPath = pathNode.getKey();
            var methodsNode = pathNode.getValue();
            if (methodsNode == null) {
                return;
            }
            methodsNode.fieldNames().forEachRemaining(requestMethod -> {
                requests.add(SimpleRequest.builder()
                    .path(requestPath)
                    .method(requestMethod)
                    .build());
            });
        });
        return requests;
    }

    @Builder
    record SimpleRequest(String path, String method) {
    }

    interface ApiResource {
        boolean isResourceRequest();
    }

    @Builder
    record ResourceRequest(String apiGroup, String resource, String name, String subResource,
                           String verb) implements ApiResource, Comparator<ResourceRequest> {

        @Override
        public boolean isResourceRequest() {
            return true;
        }

        @Override
        public int compare(ResourceRequest o1, ResourceRequest o2) {
            return Comparator.comparing(ResourceRequest::apiGroup)
                .thenComparing(ResourceRequest::resource)
                .compare(o1, o2);
        }
    }

    @Builder
    record NoneResourceRequest(String resourceUrl, String verb)
        implements ApiResource {
        @Override
        public boolean isResourceRequest() {
            return false;
        }
    }

    @Nullable
    ObjectNode readFileToJson(File file) {
        if (!file.exists()) {
            return null;
        }
        try {
            JsonNode jsonNode = JsonUtils.mapper().readTree(file);
            if (jsonNode.isObject()) {
                return (ObjectNode) jsonNode;
            }
            return null;
        } catch (IOException e) {
            // ignore
            log.warn("Failed to read JSON file: {}", file.getAbsolutePath());
        }
        return null;
    }
}
