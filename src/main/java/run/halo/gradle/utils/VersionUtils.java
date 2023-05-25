package run.halo.gradle.utils;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class VersionUtils {

    public static String latestVersionBySemverRange(String range, String[] versions) {
        if (StringUtils.isBlank(range)) {
            return "latest";
        }
        return Stream.of(versions)
            .filter(StringUtils::isNotBlank)
            .map(tag -> {
                try {
                    return Version.valueOf(tag);
                } catch (ParseException e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .filter(semver -> semver.satisfies(range))
            .max(Comparator.naturalOrder())
            .map(Version::getNormalVersion)
            .orElse("latest");
    }
}
