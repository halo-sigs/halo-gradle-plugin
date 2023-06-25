package run.halo.gradle.watch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.gradle.api.Named;
import org.gradle.api.file.FileCollection;

/**
 * If two objects have the same name, they are considered the same object.
 *
 * @author guqing
 * @since 2.0.0
 */
@ToString
@EqualsAndHashCode(of = "name")
public class WatchTarget implements Named {

    private final String name;
    List<FileCollection> files = new ArrayList<>();
    Set<String> excludes = new HashSet<>();

    public WatchTarget(String name) {
        this.name = name;
    }

    public List<FileCollection> getFiles() {
        return files;
    }

    public WatchTarget files(FileCollection files) {
        this.files.add(files);
        return this;
    }

    public WatchTarget excludes(String... excludes) {
        this.excludes.addAll(Arrays.asList(excludes));
        return this;
    }

    public WatchTarget exclude(String exclude) {
        this.excludes.add(exclude);
        return this;
    }

    public Set<String> getExcludes() {
        return excludes;
    }

    @Override
    public @NonNull String getName() {
        return this.name;
    }
}
