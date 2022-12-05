package io.github.guqing.plugin.watch;

import static java.util.Collections.addAll;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;
import org.gradle.api.Named;
import org.gradle.api.file.DirectoryTree;
import org.gradle.api.file.FileCollection;

/**
 * @author guqing
 * @since 2.0.0
 */
public class WatchTarget implements Named {

    private final String name;

    public WatchTarget(String name) {
        this.name = name;
    }

    private final List<FileCollection> files = new ArrayList<>();
    private final List<String> tasks = new ArrayList<>();


    public List<String> getTasks() {
        return tasks;
    }

    public void files(FileCollection files) {
        this.files.add(files);
    }

    public void tasks(String... tasks) {
        addAll(this.tasks, tasks);
    }

    void register(Watcher watcher) throws IOException {
        for (FileCollection files : this.files) {
            if (files instanceof DirectoryTree dirTree) {
                watcher.register(dirTree.getDir().toPath());
            } else {
                for (File file : files) {
                    watcher.register(file.toPath());
                }
            }
        }
    }

    public List<FileCollection> getFiles() {
        return files;
    }

    @Override
    public @NonNull String getName() {
        return this.name;
    }
}
