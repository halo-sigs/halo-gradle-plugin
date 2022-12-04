package io.github.guqing.plugin;

import lombok.Data;

@Data
public class DockerExtension {
    private String DEFAULT_HALO_IMAGE_NAME = "halohub/halo";
    private String imageName = DEFAULT_HALO_IMAGE_NAME;
    private String containerName = "halo-for-plugin-development";
}
