package run.halo.gradle.openapi;

import static java.util.Collections.emptyMap;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;
import javax.inject.Inject;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.UtilityClass;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import run.halo.gradle.extension.HaloExtension;

@Data
@ToString
public class OpenApiExtension {
    private static final int DEFAULT_WAIT_TIME_IN_SECONDS = 30;

    private DirectoryProperty outputDir;
    private MapProperty<String, String> groupedApiMappings;
    private MapProperty<String, String> requestHeaders;
    private Property<Integer> waitTimeInSeconds;
    private Property<String> trustStore;
    private Property<char[]> trustStorePassword;
    private Property<String> apiDocsUrl;
    private Property<Integer> apiDocsPort;
    private Property<String> apiDocsVersion;

    private ApiClientExtension generator;

    private NamedDomainObjectContainer<GroupedOpenApiExtension> groupingRules;

    @Inject
    public OpenApiExtension(ExtensionContainer extensions,
        ObjectFactory objectFactory,
        ProjectLayout layout) {
        this.outputDir = objectFactory.directoryProperty();
        this.groupedApiMappings = objectFactory.mapProperty(String.class, String.class);
        this.requestHeaders = objectFactory.mapProperty(String.class, String.class);
        this.waitTimeInSeconds = objectFactory.property(Integer.class);
        this.trustStore = objectFactory.property(String.class);
        this.trustStorePassword = objectFactory.property(char[].class);
        this.apiDocsUrl = objectFactory.property(String.class);
        this.apiDocsPort = objectFactory.property(Integer.class);
        this.apiDocsVersion = objectFactory.property(String.class);

        this.generator = objectFactory.newInstance(ApiClientExtension.class);
        this.groupingRules = objectFactory.domainObjectContainer(GroupedOpenApiExtension.class,
            name -> objectFactory.newInstance(GroupedOpenApiExtension.class, name));

        // init
        outputDir.convention(layout.getBuildDirectory().dir("api-docs/openapi/v3_0"));
        groupedApiMappings.convention(emptyMap());
        requestHeaders.convention(emptyMap());
        waitTimeInSeconds.convention(DEFAULT_WAIT_TIME_IN_SECONDS);
        this.apiDocsPort.convention(AvailablePortFinder.findRandomAvailablePort());
        this.apiDocsVersion.convention("OPENAPI_3_0");
        conventionApiDocsUrl(extensions);
    }

    public void generator(Action<ApiClientExtension> action) {
        action.execute(this.generator);
    }

    public void groupingRules(Action<NamedDomainObjectContainer<GroupedOpenApiExtension>> action) {
        action.execute(this.groupingRules);
    }

    private void conventionApiDocsUrl(ExtensionContainer extensions) {
        var haloExtension = extensions.getByType(HaloExtension.class);
        var externalUri = URI.create(haloExtension.getExternalUrl());
        try {
            var uri = new URI(externalUri.getScheme(), null, externalUri.getHost(),
                this.apiDocsPort.get(), null, null, null);
            apiDocsUrl.convention(uri.toString());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Failed to create API docs URL", e);
        }
    }

    @UtilityClass
    static class AvailablePortFinder {
        private static final int MIN_PORT = 30000;
        private static final int MAX_PORT = 65534;

        public static int findRandomAvailablePort() {
            var random = new Random();
            int port;
            boolean isAvailable;
            do {
                port = random.nextInt((MAX_PORT - MIN_PORT) + 1) + MIN_PORT;
                isAvailable = isPortAvailable(port);
            } while (!isAvailable);

            return port;
        }

        private static boolean isPortAvailable(int port) {
            try (ServerSocket serverSocket = new ServerSocket(port);
                 DatagramSocket datagramSocket = new DatagramSocket(port)) {
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }
}
