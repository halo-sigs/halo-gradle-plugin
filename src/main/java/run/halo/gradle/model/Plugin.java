package run.halo.gradle.model;

import java.time.Instant;
import javax.annotation.Nonnull;
import lombok.Data;
import lombok.Getter;

/**
 * @see
 * <a href="https://github.com/halo-dev/halo/blob/main/api/src/main/java/run/halo/app/core/extension/Plugin.java">Plugin Extension</a>
 */
@Data
public class Plugin {

    private Metadata metadata;
    private PluginSpec spec;

    @Getter(onMethod_ = @Nonnull)
    private PluginStatus status = new PluginStatus();

    public void setStatus(PluginStatus status) {
        this.status = (status == null ? new PluginStatus() : status);
    }

    @Data
    public static class PluginSpec {
        private String displayName;

        private String version;

        private String logo;

        private Boolean enabled = false;
    }

    @Data
    public static class PluginStatus {

        private Phase phase;

        private ConditionList conditions = new ConditionList();

        private Instant lastStartTime;

        public void setStatus(ConditionList conditions) {
            this.conditions = (conditions == null ? new ConditionList() : conditions);
        }
    }

    public enum Phase {
        PENDING,
        STARTING,
        CREATED,
        DISABLING,
        DISABLED,
        RESOLVED,
        STARTED,
        STOPPED,
        FAILED,
        UNKNOWN,
        ;
    }
}
