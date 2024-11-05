package run.halo.gradle.model;

import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @see
 * <a href="https://github.com/halo-dev/halo/blob/main/api/src/main/java/run/halo/app/infra/Condition.java">Condition</a>
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(exclude = "lastTransitionTime")
public class Condition {
    private String type;
    private Instant lastTransitionTime;
    private String message = "";
    private String reason = "";
}
