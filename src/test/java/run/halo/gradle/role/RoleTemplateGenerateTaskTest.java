package run.halo.gradle.role;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RoleTemplateGenerateTask}.
 *
 * @author guqing
 * @since 0.3.0
 */
class RoleTemplateGenerateTaskTest {

    @Test
    void writeListAsStringTest() {
        var role1 = new Role();
        role1.getRules().add(Role.PolicyRule.builder()
            .apiGroups(new String[] {"api.console.doc.halo.run"})
            .resources(new String[] {"docs"})
            .verbs(new String[] {"create"})
            .build());
        var role2 = new Role();
        role2.getRules().add(Role.PolicyRule.builder()
            .apiGroups(new String[] {"api.console.content.halo.run"})
            .resources(new String[] {"posts"})
            .verbs(new String[] {"get", "list"})
            .build());
        var result = RoleTemplateGenerateTask.writeListAsString(List.of(role1, role2));
        assertThat(result).isEqualToIgnoringNewLines("""
            ---
            kind: Role
            apiVersion: v1alpha1
            metadata:
              labels: {}
              annotations: {}
            rules:
              - apiGroups:
                  - api.console.doc.halo.run
                resources:
                  - docs
                verbs:
                  - create
            ---
            kind: Role
            apiVersion: v1alpha1
            metadata:
              labels: {}
              annotations: {}
            rules:
              - apiGroups:
                  - api.console.content.halo.run
                resources:
                  - posts
                verbs:
                  - get
                  - list
            """);
    }
}