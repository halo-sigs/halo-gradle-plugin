package io.github.guqing.plugin.watch;

import com.github.dockerjava.api.command.RemoveContainerCmd;
import io.github.guqing.plugin.WatchExecutionParameters;
import io.github.guqing.plugin.docker.DockerStartContainer;
import org.gradle.StartParameter;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.internal.classpath.ClassPath;
import org.gradle.internal.classpath.DefaultClassPath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author guqing
 * @since 2.0.0
 */
public class WatchTask extends DockerStartContainer {

    private final FileSystemWatcher watcher;

    @Input
    private final ListProperty<WatchTarget> targets =
            getProject().getObjects().listProperty(WatchTarget.class);

    private final WatchExecutionParameters parameters;

    Thread shutdownHook;

    public void registerShutdownHook() {
        if (this.shutdownHook == null) {
            // No shutdown hook registered yet.
            this.shutdownHook = new Thread(() -> {
                try (RemoveContainerCmd removeContainerCmd = getDockerClient()
                        .removeContainerCmd(getContainerId().get())) {
                    removeContainerCmd.withForce(true)
                            .exec();
                    Files.createFile(Paths.get("/Users/guqing/watch.txt"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
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
        parameters = getParameters(List.of("build"));
    }

    WatchExecutionParameters getParameters(List<String> buildArgs) {
        return WatchExecutionParameters.builder()
                .projectDir(getProject().getProjectDir())
                .injectedClassPath(getInjectedClassPath())
                .buildArgs(buildArgs)
                .build();
    }

    private ClassPath getInjectedClassPath() {
        StartParameter parameter = getProject().getGradle().getStartParameter();
        String classpath = parameter.getProjectProperties().get("classpath");
        if (classpath != null) {
            List<File> files = Arrays.stream(classpath.split(", "))
                    .map(File::new)
                    .toList();
            return DefaultClassPath.of(files);
        }
        return null;
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

    @Override
    public void runRemoteCommand() {
        System.out.println("运行........");
        try (WatchTaskRunner runner = new WatchTaskRunner(getProject());) {
            watcher.addListener(changeSet -> {
                System.out.println("File changed......" + changeSet);
                runner.run(parameters);
            });
            watcher.start();
            // start docker container and waiting
            super.runRemoteCommand();
        }
    }
}
