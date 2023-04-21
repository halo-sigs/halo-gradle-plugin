package run.halo.gradle;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.gradle.internal.classpath.ClassPath;

/**
 * @author guqing
 * @since 2.0.0
 */
@Value
@Builder
public class WatchExecutionParameters {

    @NonNull
    File projectDir;
    @Builder.Default
    List<String> buildArgs = List.of("-q");

    @Builder.Default
    List<String> jvmArgs = List.of();

    ClassPath injectedClassPath;

    boolean embedded;
    @Builder.Default
    OutputStream standardOutput = System.out;

    @Builder.Default
    OutputStream standardError = System.err;

    InputStream standardInput;

    @Builder.Default
    Map<String, String> environment = new LinkedHashMap<>();
}
