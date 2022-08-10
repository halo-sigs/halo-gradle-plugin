package io.github.guqing.plugin;

import java.io.File;
import java.nio.file.Path;
import lombok.Data;
import org.gradle.api.Project;

@Data
public class HaloPluginEnv {
    public static final String[] MANIFEST = {"plugin.yaml", "plugin.yml"};
    public static final String EXTENSION_NAME = "haloPluginEnv";
    private static final String DEFAULT_REPOSITORY = "https://dl.halo.run/release";
    private final Project project;

    public HaloPluginEnv(Project project) {
        this.project = project;
    }

    private Path workDir;

    private String serverRepository = DEFAULT_REPOSITORY;

    private File manifestFile;

    private String require;

    private String version;

    public Path getWorkDir() {
        return workDir == null ? project.getProjectDir()
            .toPath().resolve("work") : workDir;
    }

    public String getRequire() {
        if (this.require == null || "*".equals(require)) {
            return "1.5.3";
        }
        return require;
    }
}
