package run.halo.gradle.docker;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.Test;

class DockerCreateContainerTest {

    @Test
    void shouldBuildContainerIdFilePathFromProjectPath() {
        assertThat(DockerCreateContainer.containerIdFilePath(":"))
            .isEqualTo(".gradle/halo-devtools/containers/e7ac0786668e0ff0-containerId.txt");
        assertThat(DockerCreateContainer.containerIdFilePath(":app"))
            .isEqualTo(".gradle/halo-devtools/containers/fe6d6407b0d4f368-containerId.txt");
        assertThat(DockerCreateContainer.containerIdFilePath(":plugins:app"))
            .isEqualTo(".gradle/halo-devtools/containers/ec3dced475b9eeff-containerId.txt");
    }

    @Test
    void shouldBuildDefaultContainerNameFromProjectNameAndRootDir() {
        String containerName = DockerCreateContainer.defaultContainerName(
            "plugin-links",
            new File("/workspace/plugin-links"),
            ":"
        );

        assertThat(containerName).matches("halo-plugin-links-[0-9a-f]{16}");
    }

    @Test
    void shouldUseDifferentContainerNamesForDifferentRootDirs() {
        String firstContainerName = DockerCreateContainer.defaultContainerName(
            "plugin-links",
            new File("/workspace/plugin-links"),
            ":"
        );
        String secondContainerName = DockerCreateContainer.defaultContainerName(
            "plugin-links",
            new File("/workspace/another-plugin-links"),
            ":"
        );

        assertThat(firstContainerName).isNotEqualTo(secondContainerName);
    }

    @Test
    void shouldSanitizeProjectNameForDefaultContainerName() {
        String containerName = DockerCreateContainer.defaultContainerName(
            "Halo 插件 / dev",
            new File("/workspace/plugin"),
            ":app"
        );

        assertThat(containerName).matches("halo-Halo-dev-[0-9a-f]{16}");
    }

    @Test
    void shouldFallbackWhenProjectNameCannotBeUsedInDefaultContainerName() {
        String containerName = DockerCreateContainer.defaultContainerName(
            "插件",
            new File("/workspace/plugin"),
            ":app"
        );

        assertThat(containerName).matches("halo-project-[0-9a-f]{16}");
    }
}
