package run.halo.gradle;

import lombok.Data;
import org.apache.commons.lang3.SystemUtils;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.provider.Property;

/**
 * 可以通过 {@link ExtensionContainer} 来创建和管理 Extension，{@link ExtensionContainer} 对象可以
 * 通过 {@link Project} 对象的 {@link Project#getExtensions} 方法获取：
 * <pre>
 *     project.getExtensions().create("halo", HaloExtension)
 * </pre>
 *
 * @author guqing
 * @since 0.0.1
 */
@Data
public class HaloExtension {
    public static final String EXTENSION_NAME = "halo";

    private String imageName = "halohub/halo";

    private String containerName = "halo-for-plugin-development";

    private String serverWorkDir = "/root/.halo2";

    private String version = "2.5.2";

    private String externalUrl = "http://localhost:8090";

    private Docker docker;

    private HaloSecurity security = new HaloSecurity();

    public HaloExtension(ObjectFactory objectFactory) {
        this.docker = new Docker(objectFactory);
    }

    @Data
    public static class HaloSecurity {
        String superAdminUsername = "admin";
        String superAdminPassword = "admin";
    }

    public static class Docker {

        static final String DEFAULT_DOCKER_HOST = "unix:///var/run/docker.sock";

        static final String WINDOWS_DEFAULT_DOCKER_HOST = "npipe:////./pipe/docker_engine";

        private final Property<String> url;

        private final Property<String> apiVersion;

        public Docker(ObjectFactory objectFactory) {
            url = objectFactory.property(String.class);
            url.convention(getDefaultDockerUrl());

            apiVersion = objectFactory.property(String.class);
        }

        /**
         * The remote API version. For most cases this can be left null.
         */
        public final Property<String> getApiVersion() {
            return apiVersion;
        }

        /**
         * The server URL to connect to via Docker’s remote API.
         * <p>
         * Defaults to {@code unix:///var/run/docker.sock} for Unix systems
         * and {@code tcp://127.0.0.1:2375} for Windows systems.
         */
        public final Property<String> getUrl() {
            return url;
        }

        String getDefaultDockerUrl() {
            return SystemUtils.IS_OS_WINDOWS ? WINDOWS_DEFAULT_DOCKER_HOST : DEFAULT_DOCKER_HOST;
        }
    }
}
