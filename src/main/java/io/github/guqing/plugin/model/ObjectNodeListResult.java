package io.github.guqing.plugin.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author guqing
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ObjectNodeListResult extends ListResult<ObjectNode> {
}
