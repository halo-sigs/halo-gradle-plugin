package run.halo.gradle;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;
import org.gradle.jvm.tasks.Jar;

public class PluginJarManifestCustomizer {

    private static final String DEFAULT_MAIN_CLASS = "run.halo.app.plugin.BasePlugin";

    private final Provider<String> projectName;

    private final Provider<Object> projectVersion;

    private final PluginArchiveSupport support;

    private static final String LIB_DIRECTORY = "lib/";

    /**
     * Returns the target Java version of the project (e.g. as provided by the
     * {@code targetCompatibility} build property).
     */
    Property<JavaVersion> targetJavaVersion;

    final Property<String> mainClass;

    final Project project;

    private FileCollection classpath;

    /**
     * Creates a new {@code BootJar} task.
     */
    public PluginJarManifestCustomizer(Project project) {
        this.project = project;
        this.mainClass = project.getObjects().property(String.class);
        this.targetJavaVersion = project.getObjects().property(JavaVersion.class);
        this.support = new PluginArchiveSupport(mainClass.getOrElse(DEFAULT_MAIN_CLASS));
        this.projectName = project.provider(project::getName);
        this.projectVersion = project.provider(project::getVersion);
    }

    private void configureJarSpec(Jar jarSpec) {
        jarSpec.from("lib", fromCallTo(this::libArchiveFiles));
    }

    public void configureJarArchiveFilesSpec(Jar jarSpec) {
        jarSpec.into("./", fromCallTo(() -> libArchiveFiles().stream()
                .map(project::zipTree)
                .collect(Collectors.toSet())
            )
        );
    }

    private List<File> libArchiveFiles() {
        return StreamSupport.stream(classpathFiles().spliterator(), false)
            .filter(this.support::isZip)
            .toList();
    }

    public void configureManifest(Manifest manifest) {
        this.support.configureManifest(manifest,
            LIB_DIRECTORY,
            targetJavaVersion.get().getMajorVersion(),
            this.projectName.get(),
            this.projectVersion.get()
        );
    }

    private Iterable<File> classpathFiles() {
        return classpathEntries(File::isFile);
    }

    private Iterable<File> classpathEntries(Spec<File> filter) {
        return (this.classpath != null) ? this.classpath.filter(filter) : Collections.emptyList();
    }

    public void classpath(Object... classpath) {
        FileCollection existingClasspath = this.classpath;
        this.classpath = getProject().files(
            (existingClasspath != null) ? existingClasspath : Collections.emptyList(),
            classpath);
    }

    public Project getProject() {
        return this.project;
    }

    /**
     * Syntactic sugar that makes {@link CopySpec#into} calls a little easier to read.
     *
     * @param <T> the result type
     * @param callable the callable
     * @return an action to add the callable to the spec
     */
    private static <T> Action<CopySpec> fromCallTo(Callable<T> callable) {
        return (spec) -> spec.from(callTo(callable));
    }

    /**
     * Syntactic sugar that makes {@link CopySpec#from} calls a little easier to read.
     *
     * @param <T> the result type
     * @param callable the callable
     * @return the callable
     */
    private static <T> Callable<T> callTo(Callable<T> callable) {
        return callable;
    }

    public Property<String> getMainClass() {
        return mainClass;
    }

    public Property<JavaVersion> getTargetJavaVersion() {
        return targetJavaVersion;
    }
}
