package io.github.guqing.plugin.watch;

import com.github.dockerjava.api.command.KillContainerCmd;
import io.github.guqing.plugin.HaloPluginExtension;
import io.github.guqing.plugin.WatchExecutionParameters;
import io.github.guqing.plugin.docker.DockerStartContainer;
import io.github.guqing.plugin.steps.CreateHttpClientStep;
import io.github.guqing.plugin.steps.ReloadPluginStep;
import org.gradle.StartParameter;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.internal.classpath.ClassPath;
import org.gradle.internal.classpath.DefaultClassPath;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author guqing
 * @since 2.0.0
 */
public class WatchTask extends DockerStartContainer {

    @Input
    private final ListProperty<WatchTarget> targets =
            getProject().getObjects().listProperty(WatchTarget.class);

    private final HaloPluginExtension pluginExtension = getProject().getExtensions().getByType(HaloPluginExtension.class);

    final HttpClient httpClient = createHttpClient();

    Thread shutdownHook;

    public void registerShutdownHook() {
        if (this.shutdownHook == null) {
            // No shutdown hook registered yet.
            this.shutdownHook = new Thread(() -> {
                try (KillContainerCmd killContainerCmd = getDockerClient()
                        .killContainerCmd(getContainerId().get())) {
                    killContainerCmd.exec();
                }
            });
            Runtime.getRuntime().addShutdownHook(this.shutdownHook);
        }
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
        registerShutdownHook();
        //Amount of time to wait between polling for classpath changes.
        Duration pollInterval = Duration.ofSeconds(5);
        //Amount of quiet time required without any classpath changes before a restart is triggered.
        Duration quietPeriod = Duration.ofMillis(800);
        FileSystemWatcher watcher = new FileSystemWatcher(false, pollInterval,
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
        String host = pluginExtension.getHost();
        ReloadPluginStep reloadPluginStep = new ReloadPluginStep(host, httpClient);
        WatchExecutionParameters parameters = getParameters(List.of("build"));

        System.out.println("运行........");
        try (WatchTaskRunner runner = new WatchTaskRunner(getProject());) {
            watcher.addListener(changeSet -> {
                System.out.println("File changed......" + changeSet);
                runner.run(parameters);
                reloadPluginStep.execute(getPluginName(), getPluginBuildFile());
            });
            watcher.start();
            // start docker container and waiting
            super.runRemoteCommand();
        }
    }

    private File getPluginBuildFile() {
        Path buildLibPath = getProject().getBuildDir().toPath().resolve("lib");
        try (Stream<Path> pathStream = Files.find(buildLibPath, 1, (path, basicFileAttributes) -> {
            String fileName = path.getFileName().toString();
            return fileName.startsWith(getPluginName()) && fileName.endsWith(".jar");
        })) {

            return pathStream.findFirst()
                    .orElseThrow(() -> new IllegalStateException("未找到插件jar包"))
                    .toFile();
        } catch (IOException e) {
            throw new IllegalStateException("未找到插件jar包", e);
        }
    }

    private HttpClient createHttpClient() {
        String username = pluginExtension.getSecurity().getSuperAdminUsername();
        String password = pluginExtension.getSecurity().getSuperAdminPassword();
        return new CreateHttpClientStep(username, password).create();
    }

    private String getPluginName() {
        return pluginExtension.getPluginName();
    }
}
