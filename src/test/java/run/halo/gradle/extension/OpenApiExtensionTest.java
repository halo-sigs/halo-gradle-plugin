package run.halo.gradle.extension;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

class OpenApiExtensionTest {

    @Test
    void apiDocsUrlDefaultsToApiDocsPortWhenUsingTemporaryServer() {
        var project = newProject();
        var halo = haloExtension(project);
        halo.setPort(8091);

        var openApi = openApiExtension(project);

        URI apiDocsUri = URI.create(openApi.getApiDocsUrl().get());
        assertThat(apiDocsUri.getScheme()).isEqualTo("http");
        assertThat(apiDocsUri.getHost()).isEqualTo("localhost");
        assertThat(apiDocsUri.getPort()).isEqualTo(openApi.getApiDocsPort().get());
        assertThat(apiDocsUri.getPort()).isNotEqualTo(8091);
    }

    @Test
    void apiDocsUrlDefaultsToHaloExternalUrlWhenUsingExistingServer() {
        var project = newProject();
        var halo = haloExtension(project);

        var openApi = openApiExtension(project);
        openApi.getUseExistingServer().set(true);
        halo.setPort(8091);

        assertThat(openApi.getApiDocsUrl().get()).isEqualTo("http://localhost:8091");
    }

    @Test
    void explicitApiDocsUrlOverridesExistingServerDefault() {
        var project = newProject();
        haloExtension(project).setPort(8091);

        var openApi = openApiExtension(project);
        openApi.getUseExistingServer().set(true);
        openApi.getApiDocsUrl().set("http://localhost:19090");

        assertThat(openApi.getApiDocsUrl().get()).isEqualTo("http://localhost:19090");
    }

    private static Project newProject() {
        return ProjectBuilder.builder().build();
    }

    private static HaloExtension haloExtension(Project project) {
        return project.getExtensions()
            .create(HaloExtension.EXTENSION_NAME, HaloExtension.class, project.getObjects());
    }

    private static OpenApiExtension openApiExtension(Project project) {
        var pluginExtension = project.getExtensions()
            .create(HaloPluginExtension.EXTENSION_NAME, HaloPluginExtension.class, project);
        return pluginExtension.getOpenApi();
    }
}
