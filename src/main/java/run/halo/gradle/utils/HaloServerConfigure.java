package run.halo.gradle.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
public class HaloServerConfigure {
    private final String workDir;

    @Builder.Default
    private final Integer port = 8090;

    @Builder.Default
    private final String externalUrl = "http://localhost:8090";

    private final String fixedPluginPath;

    public JsonNode getServerConfig() {
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

    public String mergeWithUserConfigAsJson(JsonNode userDefined) {
        Assert.notNull(userDefined, "The 'userDefined' must not be null");
        if (userDefined.isMissingNode() || userDefined.isNull()) {
            return getServerConfig().toPrettyString();
        }
        var defaultConfig = getServerConfig();
        try {
            // patch
            JsonMergePatch jsonMergePatch = JsonMergePatch.fromJson(userDefined);
            // apply patch to original
            JsonNode patchedNode = jsonMergePatch.apply(defaultConfig);
            return patchedNode.toPrettyString();
        } catch (JsonPatchException e) {
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
