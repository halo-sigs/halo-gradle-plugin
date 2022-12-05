package io.github.guqing.plugin.watch;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;

/**
 * @author guqing
 * @since 2.0.0
 */
public interface Watcher extends Closeable {

    void register(Path path) throws IOException;
    void unregister(Path path) throws IOException;
    boolean isWatching(Path path);
    WatchKey take() throws InterruptedException;
}
