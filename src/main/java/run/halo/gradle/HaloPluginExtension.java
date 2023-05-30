package run.halo.gradle;

import java.io.File;
import java.nio.file.Path;
import lombok.Data;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import run.halo.gradle.watch.WatchTarget;

@Data
public class HaloPluginExtension {
    public static final String[] MANIFEST = {"plugin.yaml", "plugin.yml"};
    public static final String EXTENSION_NAME = "haloPlugin";

    private Path workDir;

    private String pluginName;

    private String requires;

    private String version;

    private final Property<String> mainClass;

    /**
     * Returns the fully-qualified name of the plugin's main class.
     *
     * @return the fully-qualified name of the plugin's main class
     */
    public Property<String> getMainClass() {
        return this.mainClass;
    }

    private File manifestFile;

    private NamedDomainObjectContainer<WatchTarget> watchDomains;

    public HaloPluginExtension(Project project) {
        this.watchDomains = project.container(WatchTarget.class);
        this.mainClass = project.getObjects().property(String.class);
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
