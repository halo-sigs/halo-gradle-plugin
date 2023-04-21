package run.halo.gradle;

import lombok.Data;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

@Data
public class DockerExtension {
    public static final String EXTENSION_NAME = "docker";

    private String DEFAULT_HALO_IMAGE_NAME = "halohub/halo";
    private String imageName = DEFAULT_HALO_IMAGE_NAME;
    private String containerName = "halo-for-plugin-development";

    /**
     * The server URL to connect to via Docker’s remote API.
     * <p>
     * Defaults to {@code unix:///var/run/docker.sock} for Unix systems and {@code tcp://127.0.0.1:2375} for Windows systems.
     */
    public final Property<String> getUrl() {
        return url;
    }

    private final Property<String> url;

    /**
     * The remote API version. For most cases this can be left null.
     */
    public final Property<String> getApiVersion() {
        return apiVersion;
    }

    private final Property<String> apiVersion;

    public DockerExtension(ObjectFactory objectFactory) {
        url = objectFactory.property(String.class);
        url.convention(getDefaultDockerUrl());

        apiVersion = objectFactory.property(String.class);
    }

    public String getDefaultDockerUrl() {
        return "tcp://127.0.0.1:2375";
    }

}
