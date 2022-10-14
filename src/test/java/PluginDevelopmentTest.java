import io.github.guqing.plugin.PluginComponentsIndexTask;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.component.Artifact;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author guqing
 * @since 2.0.0
 */
@Disabled
public class PluginDevelopmentTest {

    @Test
    public void greeterPluginAddsGreetingTaskToProject() {
        Project project = ProjectBuilder.builder()
            .withProjectDir(
                Paths.get("/Users/guqing/Development/workspace/plugins/plugin-comment-widget")
                    .toFile())
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
                Paths.get("/Users/guqing/Development/workspace/plugins/plugin-links")
                    .toFile())
            .withName("plugin-links")
            .build();
        project.getPluginManager().apply(JavaPlugin.class);
        project.getPluginManager().apply("io.github.guqing.plugin-development");

        project.getRepositories().add(project.getRepositories().mavenLocal());

        project.getRepositories().add(project.getRepositories().maven(rep -> {
            rep.setUrl("https://repo.spring.io/milestone");
            rep.setName("SpringMilestone");
        }));
        project.getRepositories().add(project.getRepositories().mavenCentral());
        MavenArtifactRepository mavenArtifactRepository = project.getRepositories()
            .maven(rep -> {
                rep.setName("SonatypeSnapShotsPackages");
                rep.setUrl("https://s01.oss.sonatype.org/content/repositories/snapshots/");
            });
        project.getRepositories().add(mavenArtifactRepository);

        Configuration compileOnly = project.getConfigurations().getByName("compileOnly");
        DependencySet compileDeps = compileOnly.getDependencies();
        Dependency dependency = project.getDependencies()
            .create("io.github.guqing:halo:2.0.0-SNAPSHOT:boot");
//        Dependency dependency = project.getDependencies()
//            .create("org.json:json:20220924");
        compileDeps.add(dependency);

//        ModuleIdentifier haloIdentifier = DefaultModuleIdentifier.newId("io.github.guqing", "halo");
//        DefaultModuleComponentIdentifier boot = new DefaultModuleComponentIdentifier(
//            haloIdentifier, "2.0.0-SNAPSHOT");
//        ArtifactResolutionResult execute = project.getDependencies()
//            .createArtifactResolutionQuery()
//            .forComponents(boot)
//            .withArtifacts(Component.class, List.of(BootArtifact.class))
//            .execute();
//        for (ComponentResult component : execute.getComponents()) {
//            System.out.println(component.getId());
//        }
        compileOnly.setCanBeResolved(true);
        compileOnly.files(file -> file.equals(dependency))
            .stream()
            .filter(file -> file.getName()
                .endsWith(dependency.getName() + "-" + dependency.getVersion() + "-boot.jar"))
            .forEach(file -> {
                System.out.println(file);
            });

        // project.getDependencies().add("compileOnly", "org.json:json:20220924");

//        project.dependencies.add 'compile', project.dependencies.project(':mySecondLib')
//        project.getGradle().removeListener(this);

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
//        Set<File> files =
//            project.getExtensions().getByType(SourceSetContainer.class).getByName("main")
//                .getRuntimeClasspath()
//                .getFiles();
//        files.forEach(file -> {
//            System.out.println(file);
//        });

//        if (!Files.exists(componentsIdxFilePath)) {
//            Files.createFile(componentsIdxFilePath);
//        }
//        DirectoryProperty buildDirectory = project.getLayout().getBuildDirectory();
//        Provider<Directory> main = buildDirectory.dir("main");
//        System.out.println(main.get());
    }

    public interface BootArtifact extends Artifact {

    }
}
