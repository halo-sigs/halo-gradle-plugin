package run.halo.gradle.openapi;

import java.util.List;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.gradle.api.Named;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

@Getter
@EqualsAndHashCode(of = "name")
public class GroupedOpenApiExtension implements Named {
    private final String name;
    private final Property<String> displayName;
    private final ListProperty<String> pathsToMatch;
    private final ListProperty<String> pathsToExclude;

    @Inject
    public GroupedOpenApiExtension(String name, ObjectFactory objects) {
        this.name = name;
        this.displayName = objects.property(String.class);
        this.pathsToMatch = objects.listProperty(String.class);
        this.pathsToExclude = objects.listProperty(String.class);

        // init
        this.pathsToMatch.convention(List.of());
        this.pathsToExclude.convention(List.of());
    }

    @Override
    @Nonnull
    public String getName() {
        return name;
    }
}
