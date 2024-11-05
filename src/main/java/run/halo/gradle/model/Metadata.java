package run.halo.gradle.model;

import java.time.Instant;
import java.util.Map;
import lombok.Data;

@Data
public class Metadata {

    private String name;

    private Map<String, String> labels;

    private Map<String, String> annotations;

    private Long version;

    private Instant creationTimestamp;
}
