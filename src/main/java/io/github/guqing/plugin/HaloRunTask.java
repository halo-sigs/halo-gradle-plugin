package io.github.guqing.plugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * @author guqing
 * @since 2.0.0
 */
public class HaloRunTask extends DefaultTask {
    private static final String JAR_PATH = "/Users/guqing/Development/workspace/spring-demo/build/libs/spring-demo-0.0.1-SNAPSHOT.jar";

    @TaskAction
    public void runHalo() {
        getProject().getExtensions().getByName(HaloPluginEnv.EXTENSION_NAME);
        getProject().exec(action -> {
            action.commandLine("/usr/bin/java", "-jar",
                JAR_PATH);
        });
    }
}
