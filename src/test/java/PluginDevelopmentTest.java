
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.testfixtures.ProjectBuilder;
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
        Task haloRun = project.getTasks().getByName("haloRun");
        System.out.println(haloRun);
    }
}
