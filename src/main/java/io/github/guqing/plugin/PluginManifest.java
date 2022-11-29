package io.github.guqing.plugin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * @author guqing
 * @since 2.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginManifest {
    private String kind;

    private String apiVersion;

    private Metadata metadata;

    private PluginSpec spec;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PluginSpec {
        private String version;
        private String require;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        private String name;
    }
}
