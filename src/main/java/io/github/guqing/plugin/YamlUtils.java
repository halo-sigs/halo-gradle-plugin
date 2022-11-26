package io.github.guqing.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.util.function.Function;

/**
 * @author guqing
 * @since 2.0.0
 */
public class YamlUtils {
    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper(new YAMLFactory());
    }

    public static <T> T read(File yamlSource, Class<T> clazz) {
        try {
            return mapper.readValue(yamlSource, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void write(File yamlSource, Function<JsonNode, JsonNode> transform) {
        try {
            JsonNode jsonNode = mapper.readTree(yamlSource);
            JsonNode root = transform.apply(jsonNode);
            mapper.writer().writeValue(yamlSource, root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
