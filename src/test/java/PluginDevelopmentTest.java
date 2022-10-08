import io.github.guqing.plugin.PluginComponentsIndexTask;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Set;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

/**
 * @author guqing
 * @since 2.0.0
 */
public class PluginDevelopmentTest {

    @Test
    public void greeterPluginAddsGreetingTaskToProject() {
        Project project = ProjectBuilder.builder()
            .withProjectDir(
                Paths.get("/Users/guqing/Development/workspace/plugins/plugin-comment-widget").toFile())
            .build();
        project.getPluginManager().apply("io.github.guqing.plugin-development");

        GradleRunner.create()
            .withProjectDir(
                Paths.get("/Users/guqing/Develop/workspace/plugins/plugin-links").toFile())
            .withArguments(PluginComponentsIndexTask.TASK_NAME)
            .withDebug(true)
            .forwardOutput()
            .build();
    }

    @Test
    void test() throws URISyntaxException, IOException {
        Project project = ProjectBuilder.builder()
            .withProjectDir(
                Paths.get("/Users/guqing/Development/workspace/plugins/plugin-comment-widget").toFile())
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
        Set<File> files =
            project.getExtensions().getByType(SourceSetContainer.class).getByName("main")
                .getRuntimeClasspath()
                .getFiles();
        files.forEach(file -> {
            System.out.println(file);
        });

//        if (!Files.exists(componentsIdxFilePath)) {
//            Files.createFile(componentsIdxFilePath);
//        }
//        DirectoryProperty buildDirectory = project.getLayout().getBuildDirectory();
//        Provider<Directory> main = buildDirectory.dir("main");
//        System.out.println(main.get());
    }
}
