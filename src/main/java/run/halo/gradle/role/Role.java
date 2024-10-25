package run.halo.gradle.role;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class Role {
    private final String kind = "Role";

    private final String apiVersion = "v1alpha1";

    private final Metadata metadata = new Metadata();

    private final List<PolicyRule> rules = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class PolicyRule {
        private String[] apiGroups;

        private String[] resources;

        private String[] resourceNames;

        private String[] nonResourceURLs;

        private String[] verbs;

        @Builder
        public PolicyRule(String[] apiGroups, String[] resources, String[] resourceNames,
            String[] nonResourceURLs, String[] verbs) {
            this.apiGroups = apiGroups;
            this.resources = resources;
            this.resourceNames = resourceNames;
            this.nonResourceURLs = nonResourceURLs;
            this.verbs = verbs;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metadata {
        private String name;
        private Map<String, String> labels = new HashMap<>();
        private Map<String, String> annotations = new HashMap<>();
    }
}
