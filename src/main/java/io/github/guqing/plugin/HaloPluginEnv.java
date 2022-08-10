package io.github.guqing.plugin;

import java.nio.file.Path;
import lombok.Data;
import org.gradle.api.Project;

@Data
public class HaloPluginEnv {
    public static final String EXTENSION_NAME = "haloPluginEnv";

    private final Project project;

    public HaloPluginEnv(Project project) {
        this.project = project;
    }

    private Path workDir;

    private String require;

    private String version;

    public Path getWorkDir() {
        return workDir == null ? project.getProjectDir()
            .toPath().resolve("work") : workDir;
    }
}
