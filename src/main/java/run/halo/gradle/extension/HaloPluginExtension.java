package run.halo.gradle.extension;

import java.io.File;
import lombok.Data;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import run.halo.gradle.openapi.OpenApiExtension;
import run.halo.gradle.watch.WatchTarget;

@Data
public class HaloPluginExtension {
    public static final String[] MANIFEST = {"plugin.yaml", "plugin.yml"};
    public static final String EXTENSION_NAME = "haloPlugin";

    private DirectoryProperty workDir;

    private String pluginName;

    private String requires;

    private String version;

    private final Property<String> mainClass;

    private OpenApiExtension openApi;

    private File manifestFile;

    private NamedDomainObjectContainer<WatchTarget> watchDomains;

    public HaloPluginExtension(Project project) {
        this.watchDomains = project.container(WatchTarget.class);
        this.mainClass = project.getObjects().property(String.class);
        this.workDir = project.getObjects().directoryProperty();
        this.openApi = project.getObjects()
            .newInstance(OpenApiExtension.class, project.getExtensions());

        this.workDir.convention(project.getLayout().getProjectDirectory().dir("workplace"));
    }

    public void openApi(Action<OpenApiExtension> action) {
        action.execute(openApi);
    }

    public String getRequires() {
        if (this.requires == null || "*".equals(requires)) {
            return "latest";
        }
        return requires;
    }

    public void watchDomain(Action<NamedDomainObjectContainer<WatchTarget>> action) {
        action.execute(watchDomains);
    }
}
