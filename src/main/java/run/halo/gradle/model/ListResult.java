package run.halo.gradle.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

/**
 * @author guqing
 * @since 2.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListResult<T> {
    private int page;

    private int size;

    private long total;

    private List<T> items;
}
