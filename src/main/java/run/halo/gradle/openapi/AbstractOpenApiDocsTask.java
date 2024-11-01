package run.halo.gradle.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import run.halo.gradle.docker.DockerExistingImage;
import run.halo.gradle.extension.HaloPluginExtension;
import run.halo.gradle.utils.YamlUtils;

@Getter
public abstract class AbstractOpenApiDocsTask extends DockerExistingImage {

    @Internal
    private final HaloPluginExtension pluginExtension =
        getProject().getExtensions().getByType(HaloPluginExtension.class);

    @Input
    protected final MapProperty<String, String> groupedApiMappings = getProject().getObjects()
        .mapProperty(String.class, String.class);

    @Input
    private final Property<String> apiDocsVersion =
        getProject().getObjects().property(String.class);

    @Internal
    private final Property<String> apiDocsUrl = getProject().getObjects().property(String.class);

    @Internal
    protected final DirectoryProperty outputDir = getProject().getObjects().directoryProperty();

    @Internal
    private final List<SpringDocGroupConfig> springDocGroupConfigs = new ArrayList<>();

    public AbstractOpenApiDocsTask() {
        var openApi = pluginExtension.getOpenApi();
        openApi.getGroupingRules().getAsMap().forEach((group, config) -> {
            springDocGroupConfigs.add(SpringDocGroupConfig.builder()
                .group(group)
                .displayName(config.getDisplayName().get())
                .pathsToMatch(config.getPathsToMatch().get())
                .pathsToExclude(config.getPathsToExclude().get())
                .build());
        });
        groupedApiMappings.convention(openApi.getGroupedApiMappings());
        apiDocsUrl.convention(openApi.getApiDocsUrl());
        outputDir.convention(openApi.getOutputDir());
        apiDocsVersion.convention(openApi.getApiDocsVersion());
    }

    protected JsonNode generateSpringDocApplicationConfig() {
        var springDocYaml = generateSpringDocConfigString(this.springDocGroupConfigs);
        if (StringUtils.isBlank(springDocYaml)) {
            return JsonNodeFactory.instance.missingNode();
        }
        return YamlUtils.read(springDocYaml, JsonNode.class);
    }

    protected static String generateSpringDocConfigString(@Nonnull
    List<SpringDocGroupConfig> configs) {
        if (configs.isEmpty()) {
            return null;
        }
        StringBuilder yamlBuilder = new StringBuilder();
        yamlBuilder.append("springdoc:\n")
            .append("  group-configs:\n");
        for (var entry : configs) {
            var group = entry.group();
            var pathsToMatch = entry.pathsToMatch;
            var pathsToExclude = entry.pathsToExclude();
            var displayName = entry.displayName();
            yamlBuilder.append("    - group: ").append(group).append("\n")
                .append("      displayName: ").append(displayName).append("\n");

            if (!pathsToMatch.isEmpty()) {
                yamlBuilder.append("      paths-to-match:\n");
                for (String path : pathsToMatch) {
                    yamlBuilder.append("        - ").append(path).append("\n");
                }
            }

            if (!pathsToExclude.isEmpty()) {
                yamlBuilder.append("      paths-to-exclude:\n");
                for (String path : pathsToExclude) {
                    yamlBuilder.append("        - ").append(path).append("\n");
                }
            }
        }
        return yamlBuilder.toString();
    }

    @Builder
    protected record SpringDocGroupConfig(String group, String displayName,
                                          List<String> pathsToMatch,
                                          List<String> pathsToExclude) {
        public SpringDocGroupConfig {
            if (StringUtils.isBlank(displayName)) {
                displayName = group;
            }
            if (pathsToMatch == null) {
                pathsToMatch = Collections.emptyList();
            }
            if (pathsToExclude == null) {
                pathsToExclude = Collections.emptyList();
            }
        }
    }
}
