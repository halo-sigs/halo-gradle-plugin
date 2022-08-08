package io.github.guqing.plugin;

import java.nio.file.Path;
import org.gradle.api.Project;

public class HaloPluginEnv {
    public static final String EXTENSION_NAME = "haloPluginEnv";

    private final Project project;

    public HaloPluginEnv(Project project) {
        this.project = project;
    }

    private Path workDir;

    public void setWorkDir(Path workDir) {
        this.workDir = workDir;
    }

    public Path getWorkDir() {
        return workDir == null ? project.getProjectDir()
            .toPath().resolve("work") : workDir;
    }

    public Project getProject() {
        return project;
    }
}
