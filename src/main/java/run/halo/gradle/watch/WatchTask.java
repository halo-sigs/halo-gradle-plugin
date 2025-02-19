package run.halo.gradle.watch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.gradle.StartParameter;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.pattern.PatternMatcher;
import org.gradle.api.internal.file.pattern.PatternMatcherFactory;
import org.gradle.internal.classpath.ClassPath;
import org.gradle.internal.classpath.DefaultClassPath;
import run.halo.gradle.WatchExecutionParameters;
import run.halo.gradle.docker.DockerStartContainer;
import run.halo.gradle.extension.HaloPluginExtension;
import run.halo.gradle.steps.PluginClient;
import run.halo.gradle.steps.SetupHaloStep;

/**
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
public class WatchTask extends DockerStartContainer {

    private final PluginClient pluginClient;
    private final HaloPluginExtension pluginExtension;

    public WatchTask() {
        this.pluginClient = new PluginClient(getProject());
        this.pluginExtension = getProject().getExtensions()
            .getByType(HaloPluginExtension.class);
    }

    WatchExecutionParameters getParameters(List<String> buildArgs) {
        return WatchExecutionParameters.builder()
            .projectDir(getProject().getProjectDir())
            .buildArgs(buildArgs)
            .environment(System.getenv())
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

    @Override
    public void runRemoteCommand() {
        //Amount of time to wait between polling for classpath changes.
        Duration pollInterval = Duration.ofSeconds(2);
        //Amount of quiet time required without any classpath changes before a restart is triggered.
        Duration quietPeriod = Duration.ofMillis(500);

        FileSystemWatcher watcher = new FileSystemWatcher(false, pollInterval,
            quietPeriod, SnapshotStateRepository.STATIC);
        configWatchFiles(watcher);

        CompletableFuture<Void> initializeFuture = CompletableFuture.runAsync(() -> {
            new SetupHaloStep(pluginClient.getSiteOption()).execute();
            pluginClient.checkPluginState();
        });
        initializeFuture.exceptionally(e -> {
            log.error(e.getMessage(), e);
            return null;
        });

        WatchExecutionParameters parameters = getParameters(List.of("build"));
        WatchTaskRunner runner = new WatchTaskRunner(getProject());
        watcher.addListener(changeSet -> {
            System.out.println("File changed......" + changeSet);
            runner.run(parameters);
            pluginClient.reloadPlugin();
        });
        watcher.start();
        // start docker container and waiting
        super.runRemoteCommand();
    }

    private void configWatchFiles(FileSystemWatcher watcher) {
        List<WatchTarget> watchTargets = new ArrayList<>(pluginExtension.getWatchDomains());
        Set<File> watchFiles = new HashSet<>();
        Set<String> excludes = new HashSet<>();

        WatchTarget javaSourceTarget = new WatchTarget("javaSource");
        javaSourceTarget.files(getProject().files("src/main/"));
        watchTargets.add(javaSourceTarget);

        for (WatchTarget watchTarget : watchTargets) {
            Set<File> files = watchTarget.getFiles().stream()
                .map(FileCollection::getFiles)
                .flatMap(Collection::stream)
                .filter(File::isDirectory)
                .collect(Collectors.toSet());
            watchFiles.addAll(files);
            excludes.addAll(watchTarget.getExcludes());
        }
        populateDefaultExcludeRules(excludes);
        log.info("Excludes files to watching: {}", excludes);

        // set exclude file filter with pattern matcher
        PatternMatcher patternsMatcher =
            PatternMatcherFactory.getPatternsMatcher(false, true, excludes);
        watcher.setExcludeFileFilter(new FileMatchingFilter(patternsMatcher));

        log.info("Watching files: {}", watchFiles);
        watcher.addSourceDirectories(watchFiles);
    }

    private void populateDefaultExcludeRules(Set<String> excludes) {
        if (excludes == null) {
            return;
        }
        excludes.add("**/node_modules/**");
        excludes.add("**/.idea/**");
        excludes.add("**/.git/**");
        excludes.add("**/.gradle/**");
        excludes.add(excludePattern("src/main/resources/console/**"));
        excludes.add(excludePattern("build/**"));
        excludes.add(excludePattern("gradle/**"));
        excludes.add(excludePattern("dist/**"));
        excludes.add(excludePattern("test/java/**"));
        excludes.add(excludePattern("test/resources/**"));
    }

    String excludePattern(String pattern) {
        Path projectPath = getProject().getProjectDir().toPath();
        return projectPath + "/" + pattern;
    }

    private File getPluginBuildFile() {
        Path buildLibPath = getProject().getLayout().getBuildDirectory()
            .dir("libs").get().getAsFile().toPath();
        try (Stream<Path> pathStream = Files.find(buildLibPath, 1, (path, basicFileAttributes) -> {
            String fileName = path.getFileName().toString();
            return fileName.endsWith(".jar");
        })) {
            return pathStream.findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到插件jar包"))
                .toFile();
        } catch (IOException e) {
            throw new IllegalStateException("未找到插件jar包", e);
        }
    }
}
