package io.github.guqing.plugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @author guqing
 * @since 2.0.0
 */
public class DemoTask extends DefaultTask {

    @TaskAction
    public void demo() throws IOException {
        Path path = Paths.get("/Users/guqing/build.txt");
        if(!Files.exists(path)) {
            Files.createFile(path);
        }
        Files.writeString(path, "hello world", StandardOpenOption.APPEND);
    }
}
