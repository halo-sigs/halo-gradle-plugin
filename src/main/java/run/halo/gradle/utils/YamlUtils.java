package run.halo.gradle.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.util.function.Function;

/**
 * @author guqing
 * @since 2.0.0
 */
public class YamlUtils {
    public static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new JavaTimeModule());
    }

    public static <T> T read(File yamlSource, Class<T> clazz) {
        try {
            return mapper.readValue(yamlSource, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T read(String yamlSource, Class<T> clazz) {
        try {
            return mapper.readValue(yamlSource, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String writeAsString(JsonNode jsonNode) {
        try {
            return mapper.writeValueAsString(jsonNode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void write(File yamlSource, Function<JsonNode, JsonNode> transform, File target) {
        try {
            JsonNode jsonNode = mapper.readTree(yamlSource);
            JsonNode root = transform.apply(jsonNode);
            mapper.writer().writeValue(target, root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
