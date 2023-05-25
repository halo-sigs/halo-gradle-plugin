package run.halo.gradle.docker;

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
    String url;
    Directory certPath;
    String apiVersion;
}
