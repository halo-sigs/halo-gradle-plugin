package run.halo.gradle.openapi;

import java.io.IOException;
import java.nio.file.Files;
import javax.inject.Inject;
import lombok.Getter;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.openapitools.codegen.DefaultGenerator;
import org.openapitools.codegen.config.CodegenConfigurator;
import run.halo.gradle.extension.HaloPluginExtension;
import run.halo.gradle.utils.FileUtils;

@Getter
public class ApiClientGeneratorTask extends DefaultTask {

    @Input
    private final Property<String> generatorName;

    @Input
    private final MapProperty<String, String> groupedApiMappings;

    @Input
    @Optional
    private final MapProperty<String, Object> additionalProperties;

    @Input
    @Optional
    private final MapProperty<String, String> typeMappings;

    @Internal
    private final DirectoryProperty specDir;

    @Internal
    private final DirectoryProperty outputDir;

    @Inject
    public ApiClientGeneratorTask() {
        this.generatorName = getProject().getObjects().property(String.class);
        this.outputDir = getProject().getObjects().directoryProperty();
        this.specDir = getProject().getObjects().directoryProperty();
        this.additionalProperties =
            getProject().getObjects().mapProperty(String.class, Object.class);
        this.typeMappings = getProject().getObjects().mapProperty(String.class, String.class);
        this.groupedApiMappings = getProject().getObjects().mapProperty(String.class, String.class);

        var pluginExtension = getProject().getExtensions().getByType(HaloPluginExtension.class);

        // convention specDir to openApi.outputDir
        var openApi = pluginExtension.getOpenApi();
        this.specDir.convention(openApi.getOutputDir());
        this.groupedApiMappings.convention(openApi.getGroupedApiMappings());

        // convention generator
        var generator = openApi.getGenerator();
        this.outputDir.convention(generator.getOutputDir());
        this.generatorName.convention(generator.getGeneratorName());
        this.additionalProperties.convention(generator.getAdditionalProperties());
        this.typeMappings.convention(generator.getTypeMappings());
    }

    @TaskAction
    public void generate() throws IOException {
        if (!groupedApiMappings.isPresent()) {
            return;
        }
        var outputPath = outputDir.get().getAsFile().toPath();
        if (Files.exists(outputPath)) {
            FileUtils.deleteRecursively(outputPath);
        }

        for (String value : groupedApiMappings.get().values()) {
            var inputSpec = specDir.file(value).get().getAsFile();

            CodegenConfigurator configurator = new CodegenConfigurator();
            configurator.setInputSpec(inputSpec.getAbsolutePath());
            configurator.setOutputDir(outputPath.toString());
            configurator.setGeneratorName(generatorName.get());
            configurator.setAdditionalProperties(additionalProperties.get());
            configurator.setTypeMappings(typeMappings.get());

            DefaultGenerator generator = new DefaultGenerator();
            generator.opts(configurator.toClientOptInput()).generate();
        }
    }
}
