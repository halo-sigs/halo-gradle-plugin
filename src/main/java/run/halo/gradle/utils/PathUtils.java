package run.halo.gradle.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PathUtils {

    public static String combinePath(String... pathSegments) {
        StringBuilder sb = new StringBuilder();
        for (String path : pathSegments) {
            if (path == null) {
                continue;
            }
            String s = path.startsWith("/") ? path : "/" + path;
            String segment = s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
            sb.append(segment);
        }
        return sb.toString();
    }
}
