package run.halo.gradle.docker;

import static org.assertj.core.api.Assertions.assertThat;

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
}
