package run.halo.gradle.role;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import lombok.Getter;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import run.halo.gradle.extension.HaloPluginExtension;

@Getter
public class RoleTemplateGenerateTask extends DefaultTask {

    @Internal
    final ListProperty<File> schemaJsonFiles;

    @InputFile
    final RegularFileProperty outputFile;

    @Inject
    public RoleTemplateGenerateTask(ObjectFactory objects) {
        this.schemaJsonFiles = objects.listProperty(File.class);
        this.outputFile = objects.fileProperty();

        var pluginExtension = getProject().getExtensions().getByType(HaloPluginExtension.class);
        var openApi = pluginExtension.getOpenApi();
        var schemaOutputDir = openApi.getOutputDir();
        Provider<List<File>> schemaJsonFileProvider = openApi.getGroupedApiMappings()
            .map(fileMapping -> fileMapping.values()
                .stream()
                .map(schemaOutputDir::dir)
                .map(Provider::getOrNull)
                .filter(Objects::nonNull)
                .map(Directory::getAsFile)
                .toList()
            );
        this.schemaJsonFiles.set(schemaJsonFileProvider);

        var resultFile = pluginExtension.getWorkDir().file("roleTemplates.yaml")
            .map(file -> {
                var filePath = file.getAsFile().toPath();
                if (Files.notExists(filePath)) {
                    try {
                        Files.createFile(filePath);
                    } catch (IOException e) {
                        throw new UncheckedIOException("Error creating file: " + filePath, e);
                    }
                }
                return file;
            });
        this.outputFile.convention(resultFile);
    }

    @TaskAction
    public void generate() {
        var roles = new RoleTemplateGenerator(schemaJsonFiles.get()).createRoles();
        var yaml = writeListAsString(roles);

        // Write to file
        try {
            Files.writeString(outputFile.get().getAsFile().toPath(), yaml);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String writeListAsString(List<Role> roles) {
        StringWriter writer = new StringWriter();
        try {
            RoleYamlWriter.mapper.writer().writeValues(writer).writeAll(roles);
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class RoleYamlWriter {
        public static final ObjectMapper mapper;

        static {
            YAMLFactory yamlFactory = new YAMLFactory()
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.SPLIT_LINES)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR);

            mapper = new ObjectMapper(yamlFactory);
            mapper.registerModule(new JavaTimeModule());
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    }
}
