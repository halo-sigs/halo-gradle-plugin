package io.github.guqing.plugin;

import static org.gradle.api.Project.DEFAULT_VERSION;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;

/**
 * <p>Read plugin manifest file and populate <code>spec.version</code> to manifest
 * by <code>MANIFEST.MF</code> file.</p>
 *
 * @author guqing
 * @since 1.0.0
 */
public class PluginAutoVersionTask extends DefaultTask {
    public static final String TASK_NAME = "populateVersion";

    @Input
    final Property<File> manifest = getProject().getObjects().property(File.class);

    @InputFiles
    Property<File> resourcesDir = getProject().getObjects().property(File.class);

    @TaskAction
    public void autoPopulateVersion() throws IOException {
        File outputResourceDir = resourcesDir.get();
        if (!Files.exists(outputResourceDir.toPath())) {
            Files.createDirectories(outputResourceDir.toPath());
        }
        File outputPluginYaml = new File(outputResourceDir, manifest.get().getName());
        String projectVersion = getProjectVersion();
        System.out.println("-------->project version: " + projectVersion);
        YamlUtils.write(manifest.get(), pluginYaml -> {
            try {
                System.out.println(YamlUtils.mapper.writeValueAsString(pluginYaml));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            JsonNode spec = pluginYaml.get("spec");
            if (spec == null) {
                ObjectNode node = (ObjectNode) pluginYaml;
                spec = node.putObject("spec");
            }
            ((ObjectNode) spec).put("version", projectVersion);
            return pluginYaml;
        }, outputPluginYaml);
        System.out.println(
            "-------->output plugin yaml: " + Files.readString(outputPluginYaml.toPath()));
    }

    private String getProjectVersion() {
        String version = (String) getProject().getVersion();
        if (StringUtils.equals(DEFAULT_VERSION, version)) {
            throw new IllegalStateException("Project version must be set.");
        }
        return version;
    }

    public Property<File> getManifest() {
        return manifest;
    }

    public Property<File> getResourcesDir() {
        return resourcesDir;
    }
}
