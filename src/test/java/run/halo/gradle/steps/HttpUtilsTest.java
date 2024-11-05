package run.halo.gradle.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HttpUtils}.
 *
 * @author guqing
 * @since 0.4.0
 */
class HttpUtilsTest {

    @Test
    void isUp() throws JsonProcessingException {
        var bodyStr = """
            {
              "status": "UP",
              "groups": [
                "liveness",
                "readiness"
              ]
            }
            """;
        assertThat(HttpUtils.isUp(bodyStr)).isTrue();
    }
}