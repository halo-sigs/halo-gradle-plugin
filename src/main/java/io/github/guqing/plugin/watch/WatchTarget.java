package io.github.guqing.plugin.watch;

import lombok.NonNull;
import lombok.ToString;
import org.gradle.api.Named;
import org.gradle.api.file.FileCollection;

import java.util.*;

/**
 * @author guqing
 * @since 2.0.0
 */
@ToString
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
