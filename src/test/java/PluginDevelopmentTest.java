
import groovy.swing.impl.DefaultAction;
import io.github.guqing.plugin.HaloPluginEnv;
import io.github.guqing.plugin.HaloServerTask;
import java.nio.file.Paths;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskContainer;
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
            .withProjectDir(Paths.get("/Users/guqing/Develop/workspace/spring-demo").toFile())
            .build();
    }

    @Test
    void test() {
        Project project = ProjectBuilder.builder()
            .withProjectDir(Paths.get("/Users/guqing/Develop/workspace/spring-demo").toFile())
            .build();
        project.getPluginManager().apply("io.github.guqing.plugin-development");
        HaloPluginEnv pluginEnv =
            (HaloPluginEnv) project.getExtensions().getByName(HaloPluginEnv.EXTENSION_NAME);
        System.out.println(pluginEnv.getWorkDir());
    }
}
