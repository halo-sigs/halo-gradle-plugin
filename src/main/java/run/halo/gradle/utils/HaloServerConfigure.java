package run.halo.gradle.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

@Builder
@Getter
public class HaloServerConfigure {
    private final String workDir;

    @Builder.Default
    private final Integer port = 8090;

    @Builder.Default
    private final String externalUrl = "http://localhost:8090";

    private final String fixedPluginPath;

    @Singular("otherConfig")
    private List<JsonNode> otherConfigs;

    public String toApplicationJsonString() {
        return toApplicationJson().toPrettyString();
    }

    public JsonNode toApplicationJson() {
        JsonNode finalConfig = getServerConfig();
        for (JsonNode userDefinedConfig : otherConfigs) {
            if (userDefinedConfig.isMissingNode()) {
                continue;
            }
            finalConfig = mergeJson(finalConfig, userDefinedConfig);
        }
        return finalConfig;
    }

    private JsonNode getServerConfig() {
        try {
            return YamlUtils.mapper.readValue(renderConfig(), JsonNode.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String renderConfig() {
        var props = new HashMap<String, String>();
        props.put("port", port.toString());
        props.put("externalUrl", externalUrl);
        props.put("fixedPluginPath", fixedPluginPath);
        props.put("workDir", workDir);

        return replaceTemplate("""
            server:
              port: {port}
            spring:
              thymeleaf:
                cache: false
              web:
                resources:
                  cache:
                    cachecontrol:
                      no-cache: true
                    use-last-modified: false
            halo:
              security:
                basic-auth:
                  disabled: false
              external-url: {externalUrl}
              plugin:
                runtime-mode: development
                fixed-plugin-path:
                  - {fixedPluginPath}
              work-dir: {workDir}
            logging:
              level:
                run.halo.app: DEBUG
                org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler: DEBUG
            springdoc:
              cache:
                disabled: true
              api-docs:
                enabled: true
                version: OPENAPI_3_0
              swagger-ui:
                enabled: true
              show-actuator: true
            management:
              endpoints:
                web:
                  exposure:
                    include: "*"
            """, props);
    }

    public static String replaceTemplate(String template, Map<String, String> valuesMap) {
        StringBuilder result = new StringBuilder();
        int cursor = 0;
        while (cursor < template.length()) {
            int start = template.indexOf('{', cursor);
            if (start == -1) {
                result.append(template.substring(cursor));
                break;
            }
            int end = template.indexOf('}', start);
            if (end == -1) {
                throw new IllegalArgumentException("Unmatched '{' at position " + start);
            }

            result.append(template, cursor, start);
            String key = template.substring(start + 1, end);
            String replacement = valuesMap.getOrDefault(key, "{" + key + "}");
            result.append(replacement);
            cursor = end + 1;
        }
        return result.toString();
    }

    public static JsonNode mergeJson(JsonNode existing, JsonNode patch) {
        Assert.notNull(existing, "The 'existing' must not be null");
        Assert.notNull(patch, "The 'patch' must not be null");
        try {
            // patch
            JsonMergePatch jsonMergePatch = JsonMergePatch.fromJson(patch);
            // apply patch to original
            return jsonMergePatch.apply(existing);
        } catch (JsonPatchException e) {
            System.out.println(existing);
            System.out.println(patch);
            throw new RuntimeException(e);
        }
    }

    public static String buildPluginDestPath(String pluginName) {
        return "/data/plugins/" + pluginName + "/";
    }

    public static String buildPluginConfigYamlPath(String haloWorkDir, String pluginName) {
        return PathUtils.combinePath(haloWorkDir, "plugins/configs/" + pluginName + ".yaml");
    }
}
