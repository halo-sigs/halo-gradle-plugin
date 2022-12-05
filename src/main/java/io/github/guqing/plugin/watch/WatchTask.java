package io.github.guqing.plugin.watch;

import io.github.guqing.plugin.WatchExecutionParameters;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.gradle.StartParameter;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

/**
 * @author guqing
 * @since 2.0.0
 */
public class WatchTask extends DefaultTask {

    private final FileSystemWatcher watcher;

    @Input
    private final ListProperty<WatchTarget> targets =
        getProject().getObjects().listProperty(WatchTarget.class);

    private final WatchExecutionParameters parameters;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    Thread shutdownHook;

    public void registerShutdownHook() {
        if (this.shutdownHook == null) {
            // No shutdown hook registered yet.
            this.shutdownHook = new Thread() {
                @Override
                public void run() {
                    try {
                        Files.createFile(Paths.get("/Users/guqing/watch.txt"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            Runtime.getRuntime().addShutdownHook(this.shutdownHook);
        }
    }

    public WatchTask() {
        registerShutdownHook();
        //Amount of time to wait between polling for classpath changes.
        Duration pollInterval = Duration.ofSeconds(1);
        //Amount of quiet time required without any classpath changes before a restart is triggered.
        Duration quietPeriod = Duration.ofMillis(400);
        watcher = new FileSystemWatcher(false, pollInterval,
            quietPeriod, SnapshotStateRepository.STATIC);

        Path projectDir = getProject().getProjectDir().toPath();
        Path sourcePath = projectDir.resolve("src/main");
        Path resourcePath = projectDir.resolve("src/main/resources");
        if (Files.exists(sourcePath)) {
            watcher.addSourceDirectory(sourcePath.toFile());
        }
        if (Files.exists(resourcePath)) {
            watcher.addSourceDirectory(resourcePath.toFile());
        }

//        FileFilter fileFilter = pathname -> {
//            if (pathname.toPath().startsWith(sourcePath)) {
//                return pathname.getName().endsWith(".java");
//            }
//            return true;
//        };

        parameters = WatchExecutionParameters.builder()
            .projectDir(getProject().getProjectDir())
            .buildArgs(List.of("build"))
            .build();
    }

    @TaskAction
    public void watch() throws IOException, InterruptedException {
        System.out.println("运行........");
        WatchTaskRunner runner = new WatchTaskRunner(getProject());
        watcher.addListener(changeSet -> {
            System.out.println("File changed......" + changeSet);
            runner.run(parameters);
        });

        watcher.start();
        countDownLatch.await();
    }

    private List<String> getBuildArgs() {
        StartParameter parameter = getProject().getGradle().getStartParameter();
        return getArguments(parameter);
    }

    private List<String> getArguments(StartParameter parameter) {
        List<String> args = new ArrayList<>();
        for (Map.Entry<String, String> e : parameter.getProjectProperties().entrySet()) {
            args.add("-P" + e.getKey() + "=" + e.getValue());
        }
        return args;
    }

    public ListProperty<WatchTarget> getTargets() {
        return targets;
    }
}
