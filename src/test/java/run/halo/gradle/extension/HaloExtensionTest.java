package run.halo.gradle.extension;

import static org.assertj.core.api.Assertions.assertThat;

import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import run.halo.gradle.steps.HaloSiteOption;

class HaloExtensionTest {

    @Test
    void defaultExternalUrlUsesDefaultPort() {
        var extension = newExtension();

        assertThat(extension.getExternalUrl()).isEqualTo("http://localhost:8090");
        assertThat(HaloSiteOption.from(extension).externalUrl())
            .hasToString("http://localhost:8090");
    }

    @Test
    void externalUrlIsDerivedFromConfiguredPort() {
        var extension = newExtension();
        extension.setPort(8091);

        assertThat(extension.getExternalUrl()).isEqualTo("http://localhost:8091");
        assertThat(HaloSiteOption.from(extension).externalUrl())
            .hasToString("http://localhost:8091");
    }

    @Test
    void configuredExternalUrlOverridesDerivedUrl() {
        var extension = newExtension();
        extension.setPort(8091);
        extension.setExternalUrl("https://halo.example.com");

        assertThat(extension.getExternalUrl()).isEqualTo("https://halo.example.com");
        assertThat(HaloSiteOption.from(extension).externalUrl())
            .hasToString("https://halo.example.com");
    }

    @Test
    void blankExternalUrlFallsBackToConfiguredPort() {
        var extension = newExtension();
        extension.setPort(8092);
        extension.setExternalUrl(" ");

        assertThat(extension.getExternalUrl()).isEqualTo("http://localhost:8092");
        assertThat(HaloSiteOption.from(extension).externalUrl())
            .hasToString("http://localhost:8092");
    }

    private static HaloExtension newExtension() {
        return new HaloExtension(ProjectBuilder.builder().build().getObjects());
    }
}
