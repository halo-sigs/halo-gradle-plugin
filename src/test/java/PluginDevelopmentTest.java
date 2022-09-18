import groovyjarjarasm.asm.Opcodes;
import io.github.guqing.plugin.Assert;
import io.github.guqing.plugin.HaloServerTask;
import io.github.guqing.plugin.InstallDefaultThemeTask;
import io.github.guqing.plugin.InstallHaloTask;
import io.github.guqing.plugin.PluginComponentsIndexTask;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.query.ArtifactResolutionQuery;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.artifacts.result.ArtifactResolutionResult;
import org.gradle.api.artifacts.result.ComponentArtifactsResult;
import org.gradle.api.artifacts.result.ComponentResult;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.artifacts.DefaultModule;
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier;
import org.gradle.api.internal.artifacts.Module;
import org.gradle.api.internal.artifacts.component.DefaultComponentIdentifierFactory;
import org.gradle.api.internal.artifacts.query.DefaultArtifactResolutionQuery;
import org.gradle.api.internal.artifacts.repositories.DefaultMavenLocalArtifactRepository;
import org.gradle.api.internal.artifacts.result.DefaultUnresolvedComponentResult;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier;
import org.gradle.maven.MavenModule;
import org.gradle.maven.MavenPomArtifact;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.AnnotationNode;

/**
 * @author guqing
 * @since 2.0.0
 */
public class PluginDevelopmentTest {

    @Test
    public void greeterPluginAddsGreetingTaskToProject() {
        Project project = ProjectBuilder.builder()
            .withProjectDir(
                Paths.get("/Users/guqing/Develop/workspace/plugins/plugin-links").toFile())
            .build();
        project.getPluginManager().apply("io.github.guqing.plugin-development");

        GradleRunner.create()
            .withProjectDir(
                Paths.get("/Users/guqing/Develop/workspace/plugins/plugin-links").toFile())
            .withArguments(InstallHaloTask.TASK_NAME)
            .withDebug(true)
            .forwardOutput()
            .build();
    }

    @Test
    void test() throws URISyntaxException, IOException {
        Project project = ProjectBuilder.builder()
            .withProjectDir(
                Paths.get("/Users/guqing/Develop/workspace/plugins/plugin-links").toFile())
            .withName("plugin-links")
            .build();
        project.getPluginManager().apply(JavaPlugin.class);
        project.getPluginManager().apply("io.github.guqing.plugin-development");

        // DefaultModule jsonModule = new DefaultModule("org.apache.commons", "commons-lang3", "3.12.0");
//        Configuration configuration = project.getConfigurations().create("installHalo");
//
//        Dependency dependency = project.getDependencies()
//            .create("io.github.guqing:halo:2.0.0-SNAPSHOT");
//        configuration.withDependencies(dependencies -> {
//            dependencies.add(dependency);
//        });
//        for (File file : configuration.files(dependency)) {
//            System.out.println(file);
//        }

//        configuration.files(project.getDependencies().create("org.json:json:20220320")).forEach(file -> {
//            System.out.println(file);
//        });

//        SourceSetContainer sourceSetContainer =
//            (SourceSetContainer) project.getProperties().get("sourceSets");
//        List<File> javaSourceFiles = sourceSetContainer.stream()
//            .filter(set -> "main".equals(set.getName()))
//            .map(SourceSet::getAllJava)
//            .map(FileTree::getFiles)
//            .flatMap(Set::stream)
//            .toList();
//        for (File file : javaSourceFiles) {
//            System.out.println(file);
//        }
        FileCollection files =
            project.getExtensions().getByType(SourceSetContainer.class).getByName("main")
                .getOutput()
                .getClassesDirs();
        String buildPath = files.getAsPath();

//        if (!Files.exists(componentsIdxFilePath)) {
//            Files.createFile(componentsIdxFilePath);
//        }
//        DirectoryProperty buildDirectory = project.getLayout().getBuildDirectory();
//        Provider<Directory> main = buildDirectory.dir("main");
//        System.out.println(main.get());
    }
}
