package run.halo.gradle.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import org.junit.jupiter.api.Nested;
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

    @Nested
    class ConsoleOutputFormatterTest {

        @Test
        void outputTest() {
            var result = SetupHaloStep.ConsoleOutputFormatter.printFormatted(
                "Halo 初始化成功",
                URI.create("http://localhost:8090/console?language=zh-CN"),
                "admin",
                "admin",
                URI.create("http://localhost:8090/swagger-ui.html")
            );
            assertThat(result).isEqualTo(
                """
                    \u001B[32m=======================================================================\u001B[0m
                    Halo 初始化成功                                                       \s
                    访问地址：http://localhost:8090/console?language=zh-CN                \s
                    用户名：admin                                                         \s
                    密码：admin                                                           \s
                    API 文档：http://localhost:8090/swagger-ui.html                       \s
                    插件开发文档：https://docs.halo.run/developer-guide/plugin/introduction
                    \u001B[32m=======================================================================\u001B[0m
                    """);
        }
    }
}
