package run.halo.gradle;

import org.gradle.api.java.archives.Manifest;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

public class PluginJarManifestCustomizer {

    private static final String DEFAULT_MAIN_CLASS = "run.halo.app.plugin.BasePlugin";

    private final Provider<String> projectName;

    private final Provider<Object> projectVersion;

    private final PluginArchiveSupport support;

    private final ResolvedDependencies resolvedDependencies = new ResolvedDependencies();

    private static final String LIB_DIRECTORY = "lib/";

    /**
     * Returns the target Java version of the project (e.g. as provided by the
     * {@code targetCompatibility} build property).
     */
    Property<JavaVersion> targetJavaVersion;

    final Property<String> mainClass;

    /**
     * Creates a new {@code BootJar} task.
     */
    public PluginJarManifestCustomizer(Project project) {
        this.mainClass = project.getObjects().property(String.class);
        this.targetJavaVersion = project.getObjects().property(JavaVersion.class);
        this.support = new PluginArchiveSupport(mainClass.getOrElse(DEFAULT_MAIN_CLASS));
        project.getConfigurations().all((configuration) -> {
            ResolvableDependencies incoming = configuration.getIncoming();
            incoming.afterResolve((resolvableDependencies) -> {
                if (resolvableDependencies == incoming) {
                    this.resolvedDependencies.processConfiguration(project, configuration);
                }
            });
        });
        this.projectName = project.provider(project::getName);
        this.projectVersion = project.provider(project::getVersion);
    }

    public void configureManifest(Manifest manifest) {
        this.support.configureManifest(manifest,
            LIB_DIRECTORY,
            targetJavaVersion.get().getMajorVersion(),
            this.projectName.get(),
            this.projectVersion.get()
        );
    }

    public Property<String> getMainClass() {
        return mainClass;
    }

    public Property<JavaVersion> getTargetJavaVersion() {
        return targetJavaVersion;
    }
}
