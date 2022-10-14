package io.github.guqing.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Data;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.ExtensionContainer;

/**
 * 可以通过 {@link ExtensionContainer} 来创建和管理 Extension，{@link ExtensionContainer} 对象可以
 * 通过 {@link Project} 对象的 {@link Project#getExtensions} 方法获取：
 * <pre>
 *     project.getExtensions().create("haloPluginExt", HaloPluginExtension)
 * </pre>
 *
 * @author guqing
 * @since 0.0.1
 */
@Data
public class HaloPluginExtension {
    public static final String[] MANIFEST = {"plugin.yaml", "plugin.yml"};
    public static final String EXTENSION_NAME = "haloPlugin";
    private static final String DEFAULT_REPOSITORY = "https://dl.halo.run/release";
    public static final String DEFAULT_BOOT_JAR = "io.github.guqing:halo:%s:boot";
    private final Project project;

    public HaloPluginExtension(Project project) {
        this.project = project;
    }

    private Path workDir;

    private String serverRepository = DEFAULT_REPOSITORY;

    private File manifestFile;

    private String require;

    private String version;

    private Dependency haloBootJar;

    private HaloSecurity security = new HaloSecurity();

    public Path getWorkDir() {
        Path path = workDir == null ? project.getProjectDir()
            .toPath().resolve("workplace") : workDir;
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return path;
    }

    public String getRequire() {
        if (this.require == null || "*".equals(require)) {
            return "2.0.0-SNAPSHOT";
        }
        return require;
    }

    @Data
    public static class HaloSecurity {
        String superAdminUsername = "admin";
        String superAdminPassword = "123456";
    }
}
