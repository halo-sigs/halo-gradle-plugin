package run.halo.gradle;

import static org.gradle.api.plugins.JavaPlugin.JAR_TASK_NAME;
import static run.halo.gradle.ResolvePluginMainClassName.TASK_NAME;

import java.io.File;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.build.event.BuildEventListenerRegistryInternal;
import org.gradle.jvm.tasks.Jar;
import run.halo.gradle.docker.AbstractDockerRemoteApiTask;
import run.halo.gradle.docker.DockerClientService;
import run.halo.gradle.docker.DockerCreateContainer;
import run.halo.gradle.docker.DockerPullImage;
import run.halo.gradle.docker.DockerRemoveContainer;
import run.halo.gradle.docker.DockerStartContainer;
import run.halo.gradle.docker.DockerStopContainer;
import run.halo.gradle.watch.WatchTask;


/**
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
public class HaloDevtoolsPlugin implements Plugin<Project> {
    public static final String GROUP = "halo server";

    /**
     * The name of the {@code developmentOnly} configuration.
     *
     * @since 2.3.0
     */
    public static final String DEVELOPMENT_ONLY_CONFIGURATION_NAME = "developmentOnly";

    /**
     * The name of the {@code productionRuntimeClasspath} configuration.
     */
    public static final String PRODUCTION_RUNTIME_CLASSPATH_CONFIGURATION_NAME =
        "productionRuntimeClasspath";


    @Getter
    private final BuildEventListenerRegistryInternal buildEvents;

    @Inject
    public HaloDevtoolsPlugin(BuildEventListenerRegistryInternal buildEvents) {
        this.buildEvents = buildEvents;
    }

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);
        log.info("Halo plugin development gradle plugin run...");
        HaloExtension haloExtension = project.getExtensions()
            .create(HaloExtension.EXTENSION_NAME, HaloExtension.class, project.getObjects());

        HaloPluginExtension pluginExtension = project.getExtensions()
            .create(HaloPluginExtension.EXTENSION_NAME, HaloPluginExtension.class, project);
        pluginExtension.setWorkDir(project.getProjectDir().toPath().resolve("workplace"));
        // populate plugin manifest info
        File manifestFile = getPluginManifest(project);
        pluginExtension.setManifestFile(manifestFile);

        PluginManifest pluginManifest = YamlUtils.read(manifestFile, PluginManifest.class);
        pluginExtension.setRequires(pluginManifest.getSpec().getRequires());
        pluginExtension.setPluginName(pluginManifest.getMetadata().getName());

        if (StringUtils.isBlank(pluginManifest.getMetadata().getName())) {
            throw new IllegalStateException("Plugin name must not be blank.");
        }

        pluginExtension.setVersion((String) project.getVersion());
        System.setProperty("halo.plugin.name", pluginManifest.getMetadata().getName());

        Action<Task> yamlVersionAction =
            YamlPluginVersionSupport.configurePluginYamlVersion(project, manifestFile);

        project.getTasks()
            .register(PluginComponentsIndexTask.TASK_NAME, PluginComponentsIndexTask.class,
                it -> {
                    it.setGroup(GROUP);
                    FileCollection files =
                        project.getExtensions().getByType(SourceSetContainer.class)
                            .getByName("main")
                            .getOutput().getClassesDirs();
                    it.classesDirs.from(files);
                    it.doFirst(yamlVersionAction);
                });
        project.getTasks().getByName("jar").dependsOn(PluginComponentsIndexTask.TASK_NAME);

        TaskProvider<ResolvePluginMainClassName> resolvePluginMainClassName =
            configureResolvePluginMainClassNameTask(project);

        configurePluginJarTask(project, resolvePluginMainClassName);

        HaloExtension.Docker dockerExtension = haloExtension.getDocker();

        final Provider<DockerClientService> serviceProvider = project.getGradle()
            .getSharedServices().registerIfAbsent("docker",
                DockerClientService.class,
                pBuildServiceSpec -> pBuildServiceSpec.parameters(parameters -> {
                    parameters.getUrl().set(dockerExtension.getUrl());
                    parameters.getApiVersion().set(dockerExtension.getApiVersion());
                }));

        String imageName = haloExtension.getImageName() + ":" + haloExtension.getVersion();
        project.getTasks().create("pullHaloImage", DockerPullImage.class, it -> {
            it.getImage().set(imageName);
            it.setGroup(GROUP);
            it.setDescription("Pull halo server image from docker hub.");
        });

        DockerCreateContainer createContainer =
            project.getTasks().create("createHaloContainer", DockerCreateContainer.class, it -> {
                it.getImageId().set(imageName);
                it.getContainerName().set(haloExtension.getContainerName());
                it.setGroup(GROUP);
                it.setDescription("Create halo server container.");
                it.dependsOn("build", "pullHaloImage");
            });

        project.getTasks().create("stopHalo", DockerStopContainer.class, it -> {
            it.setGroup(GROUP);
            it.getContainerId().set(createContainer.getContainerId());
            it.shouldRunAfter(HaloServerTask.TASK_NAME);
            it.setDescription("Stop halo server container.");
        });

        project.getTasks().create("removeHalo", DockerRemoveContainer.class, it -> {
            it.setGroup(GROUP);
            it.getContainerId().set(createContainer.getContainerId());
            it.setDescription("Remove halo server container.");
        });
        project.getTasks().getByName("clean").dependsOn("removeHalo");

        project.getTasks()
            .create(HaloServerTask.TASK_NAME, DockerStartContainer.class, it -> {
                it.setGroup(GROUP);
                it.getContainerId().set(createContainer.getContainerId());
                it.dependsOn("createHaloContainer");
                it.setDescription("Run halo server container.");
            });

        project.getTasks().create("watch", WatchTask.class, it -> {
            it.setGroup(GROUP);
            it.getContainerId().set(createContainer.getContainerId());
            it.dependsOn("createHaloContainer");
        });

        project.getTasks().withType(AbstractDockerRemoteApiTask.class)
            .configureEach(task -> task.getDockerClientService().set(serviceProvider));

        Provider<HaloServerBuildOperationListener> haloServerBuildOperationListenerProvider =
            project.getGradle().getSharedServices()
                .registerIfAbsent("halo-server-build-operation-listener",
                    HaloServerBuildOperationListener.class,
                    builderServiceSpec -> {
                        builderServiceSpec.parameters(parameters -> {
                            parameters.getDockerClientService().set(serviceProvider);
                            // TODO use a better way to get container id
                            File containerIdFile =
                                new File(project.getLayout().getBuildDirectory().getAsFile().get(),
                                    ".docker/createHaloContainer-containerId.txt");
                            parameters.getContainerId().set(containerIdFile);
                        });
                    });
        buildEvents.onOperationCompletion(haloServerBuildOperationListenerProvider);
    }

    private TaskProvider<ResolvePluginMainClassName> configureResolvePluginMainClassNameTask(
        Project project) {
        return project.getTasks()
            .register(TASK_NAME, ResolvePluginMainClassName.class,
                (resolveMainClassName) -> {
                    resolveMainClassName.setDescription(
                        "Resolves the name of the plugin's main class.");
                    resolveMainClassName.setGroup(GROUP);
                    Callable<FileCollection> classpath = () -> project.getExtensions()
                        .getByType(SourceSetContainer.class)
                        .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
                        .getOutput();
                    resolveMainClassName.setClasspath(classpath);
                    resolveMainClassName.getConfiguredMainClassName()
                        .convention(project.provider(() -> {
                            HaloPluginExtension haloPluginExtension = project.getExtensions()
                                .getByType(HaloPluginExtension.class);
                            return haloPluginExtension.getMainClass().getOrNull();
                        }));
                    resolveMainClassName.getOutputFile()
                        .set(project.getLayout().getBuildDirectory().file("resolvedMainClassName"));
                });
    }

    private void configurePluginJarTask(Project project,
        TaskProvider<ResolvePluginMainClassName> resolveMainClassName) {
        configureDevelopmentOnlyConfiguration(project);
        SourceSet mainSourceSet = javaPluginExtension(project).getSourceSets()
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        Configuration developmentOnly = project.getConfigurations()
            .getByName(DEVELOPMENT_ONLY_CONFIGURATION_NAME);
        Configuration productionRuntimeClasspath = project.getConfigurations()
            .getByName(PRODUCTION_RUNTIME_CLASSPATH_CONFIGURATION_NAME);
        Callable<FileCollection> classpath = () -> mainSourceSet.getRuntimeClasspath()
            .minus((developmentOnly.minus(productionRuntimeClasspath)));
        project.getTasks().named(JAR_TASK_NAME, Jar.class)
            .configure((jar) -> {
                var customizer = new PluginJarManifestCustomizer(project);
                Provider<String> manifestStartClass = project
                    .provider(() -> (String) jar.getManifest().getAttributes()
                        .get("Plugin-Main-Class")
                    );
                customizer.getMainClass()
                    .convention(resolveMainClassName
                        .flatMap((resolver) -> manifestStartClass.isPresent()
                            ? manifestStartClass :
                            resolveMainClassName.get().readMainClassName())
                    );
                customizer.getTargetJavaVersion()
                    .set(project.provider(
                        () -> javaPluginExtension(project).getTargetCompatibility()));
                customizer.classpath(classpath);
                customizer.configureManifest(jar.getManifest());
                jar.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
                customizer.configureJarArchiveFilesSpec(jar);
            });
    }

    private void configureDevelopmentOnlyConfiguration(Project project) {
        Configuration developmentOnly = project.getConfigurations()
            .create(DEVELOPMENT_ONLY_CONFIGURATION_NAME);
        Configuration runtimeClasspath = project.getConfigurations()
            .getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
        Configuration productionRuntimeClasspath = project.getConfigurations()
            .create(PRODUCTION_RUNTIME_CLASSPATH_CONFIGURATION_NAME);
        AttributeContainer attributes = productionRuntimeClasspath.getAttributes();
        ObjectFactory objectFactory = project.getObjects();
        attributes.attribute(Usage.USAGE_ATTRIBUTE,
            objectFactory.named(Usage.class, Usage.JAVA_RUNTIME));
        attributes.attribute(
            Bundling.BUNDLING_ATTRIBUTE, objectFactory.named(Bundling.class, Bundling.EXTERNAL));
        attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
            objectFactory.named(LibraryElements.class, LibraryElements.JAR));
        productionRuntimeClasspath.setVisible(false);
        productionRuntimeClasspath.setExtendsFrom(runtimeClasspath.getExtendsFrom());
        productionRuntimeClasspath.setCanBeResolved(runtimeClasspath.isCanBeResolved());
        productionRuntimeClasspath.setCanBeConsumed(runtimeClasspath.isCanBeConsumed());
        runtimeClasspath.extendsFrom(developmentOnly);
    }

    private JavaPluginExtension javaPluginExtension(Project project) {
        return project.getExtensions().getByType(JavaPluginExtension.class);
    }

    private File getPluginManifest(Project project) {
        SourceSetContainer sourceSetContainer =
            (SourceSetContainer) project.getProperties().get("sourceSets");
        File mainResourceDir = sourceSetContainer.stream()
            .filter(set -> "main".equals(set.getName()))
            .map(SourceSet::getResources)
            .map(SourceDirectorySet::getSrcDirs)
            .flatMap(Set::stream)
            .findFirst()
            .orElseThrow();

        for (String filename : HaloPluginExtension.MANIFEST) {
            File manifestFile = new File(mainResourceDir, filename);
            if (manifestFile.exists()) {
                return manifestFile;
            }
        }
        throw new IllegalStateException(
            "The plugin manifest file [plugin.yaml] not found in " + mainResourceDir);
    }
}
