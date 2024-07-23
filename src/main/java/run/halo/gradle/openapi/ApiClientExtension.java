package run.halo.gradle.openapi;

import java.util.HashMap;
import javax.inject.Inject;
import lombok.Data;
import lombok.ToString;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

@Data
@ToString
public class ApiClientExtension {
    private DirectoryProperty outputDir;
    private Property<String> generatorName;
    private MapProperty<String, Object> additionalProperties;
    private MapProperty<String, String> typeMappings;

    @Inject
    public ApiClientExtension(ObjectFactory objectFactory, ProjectLayout layout) {
        this.outputDir = objectFactory.directoryProperty();
        this.generatorName = objectFactory.property(String.class);
        this.additionalProperties = objectFactory.mapProperty(String.class, Object.class);
        this.typeMappings = objectFactory.mapProperty(String.class, String.class);

        // init
        outputDir.convention(layout.getProjectDirectory()
            .dir("console/src/api/generated"));
        this.generatorName.convention("typescript-axios");

        var properties = new HashMap<String, Object>();
        properties.put("useES6", true);
        properties.put("useSingleRequestParameter", true);
        properties.put("withSeparateModelsAndApi", true);
        properties.put("apiPackage", "api");
        properties.put("modelPackage", "models");
        this.additionalProperties.convention(properties);

        var typeMappings = new HashMap<String, String>();
        this.typeMappings.convention(typeMappings);
    }
}
