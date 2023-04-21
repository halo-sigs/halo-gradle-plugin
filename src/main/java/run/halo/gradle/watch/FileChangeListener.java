package run.halo.gradle.watch;

import java.util.Set;

/**
 * @author guqing
 * @since 2.0.0
 */
@FunctionalInterface
public interface FileChangeListener {

    /**
     * Called when files have been changed.
     *
     * @param changeSet a set of the {@link ChangedFiles}
     */
    void onChange(Set<ChangedFiles> changeSet);

}
