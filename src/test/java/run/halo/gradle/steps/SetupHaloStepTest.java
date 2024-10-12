package run.halo.gradle.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SetupHaloStep}.
 *
 * @author guqing
 * @since 0.2.0
 */
class SetupHaloStepTest {

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
        var result = SetupHaloStep.isUp(bodyStr);
        assert result;
    }
}
