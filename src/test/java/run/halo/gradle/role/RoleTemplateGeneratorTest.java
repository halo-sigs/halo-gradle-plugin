package run.halo.gradle.role;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.skyscreamer.jsonassert.JSONAssert;
import run.halo.gradle.utils.JsonUtils;

/**
 * Tests for {@link RoleTemplateGenerator}.
 *
 * @author guqing
 * @since 0.3.0
 */
class RoleTemplateGeneratorTest {

    @Test
    void createRoles(@TempDir Path tempDir) throws JsonProcessingException, JSONException {
        var schemaJsonFile = tempDir.resolve("schema.json");
        var schemaJson = fakeSchemaJson();
        writeToFile(schemaJsonFile, schemaJson);

        var roleTemplateGenerator = new RoleTemplateGenerator(List.of(schemaJsonFile.toFile()));
        var roles = roleTemplateGenerator.createRoles()
            .stream()
            .peek(role -> role.getMetadata().setName("a-name"))
            .toList();

        JSONAssert.assertEquals("""
            [
                {
                    "kind": "Role",
                    "apiVersion": "v1alpha1",
                    "metadata": {
                        "name": "a-name",
                        "labels": {
                            "halo.run/role-template": "true"
                        },
                        "annotations": {
                            "rbac.authorization.halo.run/ui-permissions": "['{定义 UI 权限}']",
                            "rbac.authorization.halo.run/display-name": "{角色显示名称}",
                            "rbac.authorization.halo.run/module": "{所属模块}"
                        }
                    },
                    "rules": [
                        {
                            "apiGroups": ["api.console.halo.run"],
                            "resources": ["attachments"],
                            "verbs": ["list"]
                        },
                        {
                            "apiGroups": ["api.console.halo.run"],
                            "resources": ["attachments", "attachments/download"],
                            "resourceNames": ["{name}"],
                            "verbs": ["get"]
                        }
                    ]
                },
                {
                    "kind": "Role",
                    "apiVersion": "v1alpha1",
                    "metadata": {
                        "name": "a-name",
                        "labels": {
                            "halo.run/role-template": "true"
                        },
                        "annotations": {
                            "rbac.authorization.halo.run/ui-permissions": "['{定义 UI 权限}']",
                            "rbac.authorization.halo.run/display-name": "{角色显示名称}",
                            "rbac.authorization.halo.run/module": "{所属模块}"
                        }
                    },
                    "rules": [
                        {
                            "apiGroups": ["api.content.halo.run"],
                            "resources": ["categories", "posts"],
                            "verbs": ["create", "list"]
                        },
                        {
                            "apiGroups": ["api.content.halo.run"],
                            "resources": ["categories", "categories/posts"],
                            "resourceNames": ["{name}"],
                            "verbs": ["get"]
                        }
                    ]
                },
                {
                    "kind": "Role",
                    "apiVersion": "v1alpha1",
                    "metadata": {
                        "name": "a-name",
                        "labels": {
                            "halo.run/role-template": "true"
                        },
                        "annotations": {
                            "rbac.authorization.halo.run/ui-permissions": "['{定义 UI 权限}']",
                            "rbac.authorization.halo.run/display-name": "{角色显示名称}",
                            "rbac.authorization.halo.run/module": "{所属模块}"
                        }
                    },
                    "rules": [{
                        "nonResourceURLs": ["/actuator/info"],
                        "verbs": ["get"]
                    }]
                },
                {
                    "kind": "Role",
                    "apiVersion": "v1alpha1",
                    "metadata": {
                        "name": "a-name",
                        "labels": {
                            "halo.run/role-template": "true"
                        },
                        "annotations": {
                            "rbac.authorization.halo.run/ui-permissions": "['{定义 UI 权限}']",
                            "rbac.authorization.halo.run/display-name": "{角色显示名称}",
                            "rbac.authorization.halo.run/module": "{所属模块}"
                        }
                    },
                    "rules": [{
                        "nonResourceURLs": ["/health"],
                        "verbs": ["get"]
                    }]
                }
            ]
            """, JsonUtils.mapper().writeValueAsString(roles), true);
    }

    private void writeToFile(Path path, String content) {
        try {
            Files.writeString(path, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    String fakeSchemaJson() {
        return """
            {
              "paths": {
                "/apis/api.console.halo.run/v1alpha1/attachments": {
                  "get": {}
                },
                "/apis/api.console.halo.run/v1alpha1/attachments/{name}": {
                    "get": {}
                },
                "/apis/api.console.halo.run/v1alpha1/attachments/{name}/download": {
                    "get": {}
                },
                "/apis/api.content.halo.run/v1alpha1/categories": {
                    "get": {}
                },
                "/apis/api.content.halo.run/v1alpha1/categories/{name}": {
                    "get": {}
                },
                "/apis/api.content.halo.run/v1alpha1/categories/{name}/posts": {
                    "get": {}
                },
                "/apis/api.content.halo.run/v1alpha1/posts": {
                    "post": {}
                },
                "/health": {
                    "get": {}
                },
                "/actuator/info": {
                    "get": {}
                }
              }
            }
            """;
    }
}
