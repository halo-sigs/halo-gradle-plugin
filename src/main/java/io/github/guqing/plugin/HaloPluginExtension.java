package io.github.guqing.plugin;

import io.github.guqing.plugin.watch.WatchTarget;
import lombok.Data;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;

import java.io.File;
import java.nio.file.Path;

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
    public static final String EXTENSION_NAME = "halo";

    private final Project project;

    private Path workDir;

    private File manifestFile;

    private String pluginName;
    private String require;

    private String version;

    private String host = "http://localhost:8090";

    private NamedDomainObjectContainer<WatchTarget> watchDomains;

    private DockerExtension docker = new DockerExtension();

    private HaloSecurity security = new HaloSecurity();

    public HaloPluginExtension(Project project) {
        this.project = project;
    }

    public void watchDomain(Action<NamedDomainObjectContainer<WatchTarget>> action) {
        action.execute(watchDomains);
    }

    public String getRequire() {
        if (this.require == null || "*".equals(require)) {
            return "2.0.0";
        }
        return require;
    }

    @Data
    public static class HaloSecurity {
        String superAdminUsername = "admin";
        String superAdminPassword = "123456";
    }
}
