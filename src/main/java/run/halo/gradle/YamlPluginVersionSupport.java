package run.halo.gradle;

import static org.gradle.api.Project.DEFAULT_VERSION;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.nio.file.Path;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSetContainer;

public class YamlPluginVersionSupport {

    public static void configurePluginYamlVersion(Project project, File manifestFile) {
        File file =
            project.getExtensions().getByType(SourceSetContainer.class)
                .getByName(MAIN_SOURCE_SET_NAME)
                .getOutput().getResourcesDir();
        if (file == null) {
            throw new RuntimeException("Can not find resources dir.");
        }
        Path outputPluginYaml = file.toPath().resolve(manifestFile.getName());
        project.getTasks().getByName("jar").doFirst(task -> {
            rewritePluginYaml(outputPluginYaml.toFile(), project);
        });
    }

    private static void rewritePluginYaml(File outputPluginYaml, Project project) {
        YamlUtils.write(outputPluginYaml, pluginYaml -> {
            JsonNode spec = pluginYaml.get("spec");
            if (spec == null || spec.isNull()) {
                ObjectNode node = (ObjectNode) pluginYaml;
                spec = node.putObject("spec");
            }
            ((ObjectNode) spec).put("version", getProjectVersion(project));
            try {
                System.out.println(YamlUtils.mapper.writeValueAsString(pluginYaml));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            return pluginYaml;
        }, outputPluginYaml);
    }

    private static String getProjectVersion(Project project) {
        String version = (String) project.getVersion();
        if (StringUtils.equals(DEFAULT_VERSION, version)) {
            throw new IllegalStateException("Project version must be set.");
        }
        return version;
    }
}
