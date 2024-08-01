package run.halo.gradle.watch;

import java.io.File;
import java.io.FileFilter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import run.halo.gradle.utils.Assert;

/**
 * From
 * <a href="https://github.com/spring-projects/spring-boot/blob/main/spring-boot-project/spring-boot-devtools/src/main/java/org/springframework/boot/devtools/filewatch/FileSystemWatcher.java">Springboot devtools file system watcher</a>
 *
 * @author guqing
 * @since 1.0.0
 */
public class FileSystemWatcher {
    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(1000);

    private static final Duration DEFAULT_QUIET_PERIOD = Duration.ofMillis(400);

    private final List<FileChangeListener> listeners = new ArrayList<>();

    private final boolean daemon;

    private final long pollInterval;

    private final long quietPeriod;

    private final SnapshotStateRepository snapshotStateRepository;

    private final AtomicInteger remainingScans = new AtomicInteger(-1);

    private final Map<File, DirectorySnapshot> directories = new HashMap<>();

    private Thread watchThread;

    private FileFilter triggerFilter;

    private FileFilter excludeFileFilter;

    private final Object monitor = new Object();

    /**
     * Create a new {@link FileSystemWatcher} instance.
     */
    public FileSystemWatcher() {
        this(true, DEFAULT_POLL_INTERVAL, DEFAULT_QUIET_PERIOD);
    }

    /**
     * Create a new {@link FileSystemWatcher} instance.
     *
     * @param daemon if a daemon thread used to monitor changes
     * @param pollInterval the amount of time to wait between checking for changes
     * @param quietPeriod the amount of time required after a change has been detected to
     * ensure that updates have completed
     */
    public FileSystemWatcher(boolean daemon, Duration pollInterval, Duration quietPeriod) {
        this(daemon, pollInterval, quietPeriod, null);
    }

    /**
     * Create a new {@link FileSystemWatcher} instance.
     *
     * @param daemon if a daemon thread used to monitor changes
     * @param pollInterval the amount of time to wait between checking for changes
     * @param quietPeriod the amount of time required after a change has been detected to
     * ensure that updates have completed
     * @param snapshotStateRepository the snapshot state repository
     * @since 2.4.0
     */
    public FileSystemWatcher(boolean daemon, Duration pollInterval, Duration quietPeriod,
        SnapshotStateRepository snapshotStateRepository) {
        Assert.notNull(pollInterval, "PollInterval must not be null");
        Assert.notNull(quietPeriod, "QuietPeriod must not be null");
        Assert.isTrue(pollInterval.toMillis() > 0, "PollInterval must be positive");
        Assert.isTrue(quietPeriod.toMillis() > 0, "QuietPeriod must be positive");
        Assert.isTrue(pollInterval.toMillis() > quietPeriod.toMillis(),
            "PollInterval must be greater than QuietPeriod");
        this.daemon = daemon;
        this.pollInterval = pollInterval.toMillis();
        this.quietPeriod = quietPeriod.toMillis();
        this.snapshotStateRepository = (snapshotStateRepository != null) ? snapshotStateRepository
            : SnapshotStateRepository.NONE;
    }

    /**
     * Add listener for file change events. Cannot be called after the watcher has been
     * {@link #start() started}.
     *
     * @param fileChangeListener the listener to add
     */
    public void addListener(FileChangeListener fileChangeListener) {
        Assert.notNull(fileChangeListener, "FileChangeListener must not be null");
        synchronized (this.monitor) {
            checkNotStarted();
            this.listeners.add(fileChangeListener);
        }
    }

    /**
     * Add source directories to monitor. Cannot be called after the watcher has been
     * {@link #start() started}.
     *
     * @param directories the directories to monitor
     */
    public void addSourceDirectories(Iterable<File> directories) {
        Assert.notNull(directories, "Directories must not be null");
        synchronized (this.monitor) {
            directories.forEach(this::addSourceDirectory);
        }
    }

    public void setExcludeFileFilter(FileFilter excludeFileFilter) {
        this.excludeFileFilter = excludeFileFilter;
    }

    /**
     * Add a source directory to monitor. Cannot be called after the watcher has been
     * {@link #start() started}.
     *
     * @param directory the directory to monitor
     */
    public void addSourceDirectory(File directory) {
        Assert.notNull(directory, "Directory must not be null");
        Assert.isTrue(!directory.isFile(),
            () -> "Directory '" + directory + "' must not be a file");
        synchronized (this.monitor) {
            checkNotStarted();
            this.directories.put(directory, null);
        }
    }

    /**
     * Set an optional {@link FileFilter} used to limit the files that trigger a change.
     *
     * @param triggerFilter a trigger filter or null
     */
    public void setTriggerFilter(FileFilter triggerFilter) {
        synchronized (this.monitor) {
            this.triggerFilter = triggerFilter;
        }
    }

    private void checkNotStarted() {
        synchronized (this.monitor) {
            Assert.state(this.watchThread == null, "FileSystemWatcher already started");
        }
    }

    /**
     * Start monitoring the source directory for changes.
     */
    public void start() {
        synchronized (this.monitor) {
            createOrRestoreInitialSnapshots();
            if (this.watchThread == null) {
                Map<File, DirectorySnapshot> localDirectories = new HashMap<>(this.directories);
                Watcher watcher = new Watcher(this.remainingScans, new ArrayList<>(this.listeners),
                    this.triggerFilter,
                    this.excludeFileFilter, this.pollInterval, this.quietPeriod, localDirectories,
                    this.snapshotStateRepository);
                this.watchThread = new Thread(watcher);
                this.watchThread.setName("File Watcher");
                this.watchThread.setDaemon(this.daemon);
                this.watchThread.start();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void createOrRestoreInitialSnapshots() {
        Map<File, DirectorySnapshot> restored =
            (Map<File, DirectorySnapshot>) this.snapshotStateRepository.restore();
        this.directories.replaceAll((f, v) -> {
            DirectorySnapshot restoredSnapshot = (restored != null) ? restored.get(f) : null;
            return (restoredSnapshot != null) ? restoredSnapshot
                : new DirectorySnapshot(f, excludeFileFilter);
        });
    }

    /**
     * Stop monitoring the source directories.
     */
    public void stop() {
        stopAfter(0);
    }

    /**
     * Stop monitoring the source directories.
     *
     * @param remainingScans the number of remaining scans
     */
    void stopAfter(int remainingScans) {
        Thread thread;
        synchronized (this.monitor) {
            thread = this.watchThread;
            if (thread != null) {
                this.remainingScans.set(remainingScans);
                if (remainingScans <= 0) {
                    thread.interrupt();
                }
            }
            this.watchThread = null;
        }
        if (thread != null && Thread.currentThread() != thread) {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class Watcher implements Runnable {

        private final AtomicInteger remainingScans;

        private final List<FileChangeListener> listeners;

        private final FileFilter triggerFilter;

        private final FileFilter excludeFileFilter;

        private final long pollInterval;

        private final long quietPeriod;

        private Map<File, DirectorySnapshot> directories;

        private final SnapshotStateRepository snapshotStateRepository;

        private Watcher(AtomicInteger remainingScans, List<FileChangeListener> listeners,
            FileFilter triggerFilter,
            FileFilter excludeFileFilter, long pollInterval, long quietPeriod,
            Map<File, DirectorySnapshot> directories,
            SnapshotStateRepository snapshotStateRepository) {
            this.remainingScans = remainingScans;
            this.listeners = listeners;
            this.triggerFilter = triggerFilter;
            this.excludeFileFilter = excludeFileFilter;
            this.pollInterval = pollInterval;
            this.quietPeriod = quietPeriod;
            this.directories = directories;
            this.snapshotStateRepository = snapshotStateRepository;

        }

        @Override
        public void run() {
            int remainingScans = this.remainingScans.get();
            while (remainingScans > 0 || remainingScans == -1) {
                try {
                    if (remainingScans > 0) {
                        this.remainingScans.decrementAndGet();
                    }
                    scan();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                remainingScans = this.remainingScans.get();
            }
        }

        private void scan() throws InterruptedException {
            Thread.sleep(this.pollInterval - this.quietPeriod);
            Map<File, DirectorySnapshot> previous;
            Map<File, DirectorySnapshot> current = this.directories;
            do {
                previous = current;
                current = getCurrentSnapshots();
                Thread.sleep(this.quietPeriod);
            }
            while (isDifferent(previous, current));
            if (isDifferent(this.directories, current)) {
                updateSnapshots(current.values());
            }
        }

        private boolean isDifferent(Map<File, DirectorySnapshot> previous,
            Map<File, DirectorySnapshot> current) {
            if (!previous.keySet().equals(current.keySet())) {
                return true;
            }
            for (Map.Entry<File, DirectorySnapshot> entry : previous.entrySet()) {
                DirectorySnapshot previousDirectory = entry.getValue();
                DirectorySnapshot currentDirectory = current.get(entry.getKey());
                if (!previousDirectory.equals(currentDirectory, this.triggerFilter)) {
                    return true;
                }
            }
            return false;
        }

        private Map<File, DirectorySnapshot> getCurrentSnapshots() {
            Map<File, DirectorySnapshot> snapshots = new LinkedHashMap<>();
            for (File directory : this.directories.keySet()) {
                DirectorySnapshot directorySnapshot =
                    new DirectorySnapshot(directory, this.excludeFileFilter);
                snapshots.put(directory, directorySnapshot);
            }
            return snapshots;
        }

        private void updateSnapshots(Collection<DirectorySnapshot> snapshots) {
            Map<File, DirectorySnapshot> updated = new LinkedHashMap<>();
            Set<ChangedFiles> changeSet = new LinkedHashSet<>();
            for (DirectorySnapshot snapshot : snapshots) {
                DirectorySnapshot previous = this.directories.get(snapshot.getDirectory());
                updated.put(snapshot.getDirectory(), snapshot);
                ChangedFiles changedFiles = previous.getChangedFiles(snapshot, this.triggerFilter);
                if (!changedFiles.getFiles().isEmpty()) {
                    changeSet.add(changedFiles);
                }
            }
            this.directories = updated;
            this.snapshotStateRepository.save(updated);
            if (!changeSet.isEmpty()) {
                fireListeners(Collections.unmodifiableSet(changeSet));
            }
        }

        private void fireListeners(Set<ChangedFiles> changeSet) {
            for (FileChangeListener listener : this.listeners) {
                listener.onChange(changeSet);
            }
        }

    }
}
