package io.github.guqing.plugin;

import static java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION;
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.SourceSetContainer;
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

    @TaskAction
    public void autoPopulateVersion() {
        String projectVersion = getProjectVersion();
        YamlUtils.write(manifest.get(), pluginYaml -> {
            JsonNode spec = pluginYaml.get("spec");
            if (spec == null) {
                ObjectNode node = (ObjectNode) pluginYaml;
                spec = node.putObject("spec");
            }
            ((ObjectNode) spec).put("version", projectVersion);
            return pluginYaml;
        });
    }

    private String getProjectVersion() {
        File projectManifestMfFile = getProjectManifestMfFile();
        Manifest manifestMf = toManifest(projectManifestMfFile);
        String projectVersion =
            manifestMf.getMainAttributes().getValue(IMPLEMENTATION_VERSION.toString());
        if (StringUtils.isBlank(projectVersion)
            || StringUtils.equalsIgnoreCase(projectVersion, "unspecified")) {
            throw new IllegalStateException(
                "Project version can not be blank, please set the value for attribute ["
                    + IMPLEMENTATION_VERSION + "] in MANIFEST.MF file.");
        }
        return projectVersion;
    }

    private Manifest toManifest(File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            return new Manifest(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Unable to open manifest file: " + file, e);
        }
    }

    private File getProjectManifestMfFile() {
        SourceSetContainer sourceSetContainer =
            (SourceSetContainer) getProject().getProperties().get("sourceSets");
        File resourcesDir = sourceSetContainer.getByName(MAIN_SOURCE_SET_NAME)
            .getOutput()
            .getResourcesDir();
        if (resourcesDir == null) {
            throw new IllegalStateException("Cannot find resources directory.");
        }
        return new File(resourcesDir, "META-INF/MANIFEST.MF");
    }

    public Property<File> getManifest() {
        return manifest;
    }
}
