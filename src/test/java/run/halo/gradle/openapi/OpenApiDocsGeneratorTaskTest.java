package run.halo.gradle.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link OpenApiDocsGeneratorTask}.
 *
 * @author guqing
 * @since 0.0.10
 */
class OpenApiDocsGeneratorTaskTest {

    @Test
    void generateSpringDocConfigStringTest() {
        var result = OpenApiDocsGeneratorTask.generateSpringDocConfigString(List.of());
        assertThat(result).isNull();

        var config = OpenApiDocsGeneratorTask.SpringDocGroupConfig.builder()
            .group("postShareLinkExtensionV1alpha1Api")
            .displayName("Extension API for Post Share Link")
            .pathsToMatch(List.of("/api/v1alpha1/share/**"))
            .pathsToExclude(List.of("/api/v1alpha1/do-not-share/**"))
            .build();
        result = OpenApiDocsGeneratorTask.generateSpringDocConfigString(List.of(config));
        assertThat(result).isEqualTo("""
            springdoc:
              group-configs:
                - group: postShareLinkExtensionV1alpha1Api
                  displayName: Extension API for Post Share Link
                  paths-to-match:
                    - /api/v1alpha1/share/**
                  paths-to-exclude:
                    - /api/v1alpha1/do-not-share/**
            """);
    }
}