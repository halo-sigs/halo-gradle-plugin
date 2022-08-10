import io.github.guqing.plugin.HaloServerTask;
import java.nio.file.Paths;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPlugin;
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
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("io.github.guqing.plugin-development");
//        Task haloRun = project.getTasks().getByName("haloRun");
//        System.out.println(haloRun);
        Task task = project.getTasks().getByName(HaloServerTask.TASK_NAME);
        GradleRunner.create()
            .withProjectDir(
                Paths.get("/Users/guqing/Development/workspace/plugins/plugin-journals").toFile())
            .withArguments(HaloServerTask.TASK_NAME)
            .withDebug(true)
            .forwardOutput()
            .build();
    }

    @Test
    void test() {
        Project project = ProjectBuilder.builder()
            .withProjectDir(Paths.get("/Users/guqing/Development/workspace/plugins/plugin-journals").toFile())
            .withName("plugin-journals")
            .build();
        project.getPluginManager().apply(JavaPlugin.class);
        project.getPluginManager().apply("io.github.guqing.plugin-development");
//        SourceSetContainer sourceSetContainer =
//            (SourceSetContainer) project.getProperties().get("sourceSets");
//        List<File> mainResourceDir = sourceSetContainer.stream()
//            .filter(set -> "main".equals(set.getName()))
//            .map(SourceSet::getResources)
//            .map(SourceDirectorySet::getSrcDirs)
//            .flatMap(Set::stream).toList();
//
//        System.out.println(mainResourceDir);

//        HaloPluginEnv pluginEnv =
//            (HaloPluginEnv) project.getExtensions().getByName(HaloPluginEnv.EXTENSION_NAME);
//        Task task = project.getTasks().getByName("haloRun");
    }
}
