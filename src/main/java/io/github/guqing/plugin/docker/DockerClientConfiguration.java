package io.github.guqing.plugin.docker;

import groovy.transform.EqualsAndHashCode;
import lombok.Data;
import org.gradle.api.file.Directory;

/**
 * @author guqing
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode
public class DockerClientConfiguration {
    public static final String EXTENSION_NAME = "dockerConfig";

    String url = "unix:///var/run/docker.sock";
    Directory certPath;
    String apiVersion;
}
