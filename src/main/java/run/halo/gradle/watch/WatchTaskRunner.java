package run.halo.gradle.watch;

import run.halo.gradle.Assert;
import run.halo.gradle.WatchExecutionParameters;
import org.gradle.StartParameter;
import org.gradle.api.Project;
import org.gradle.api.logging.Logging;
import org.gradle.internal.classpath.ClassPath;
import org.gradle.tooling.BuildException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.internal.consumer.DefaultBuildLauncher;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.gradle.wrapper.GradleUserHomeLookup;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author guqing
 * @since 2.0.0
 */
public class WatchTaskRunner implements AutoCloseable {

    private static final Logger LOG = Logging.getLogger(WatchTaskRunner.class);
    private final ProjectConnection connection;

    public WatchTaskRunner(Project project) {
        // StartParameter parameter = project.getGradle().getStartParameter();
        DefaultGradleConnector gradleConnector =
            (DefaultGradleConnector) GradleConnector.newConnector();
        gradleConnector.useGradleUserHomeDir(project.getGradle().getGradleUserHomeDir());
        gradleConnector.useDistributionBaseDir(GradleUserHomeLookup.gradleUserHome());
        connection = gradleConnector.forProjectDirectory(project.getProjectDir())
            .connect();
    }

    public void run(WatchExecutionParameters parameters) {
        Assert.notNull(parameters, "WatchExecutionParameters must not be null");
        DefaultBuildLauncher launcher = (DefaultBuildLauncher) connection
            .newBuild()
            .setStandardOutput(new NoCloseOutputStream(parameters.getStandardError()))
            .setStandardError(new NoCloseOutputStream(parameters.getStandardError()));

        if (parameters.getStandardInput() != null) {
            launcher.setStandardInput(parameters.getStandardInput());
        }
        ClassPath injectedClassPath = parameters.getInjectedClassPath();
        if (injectedClassPath != null && !injectedClassPath.isEmpty()) {
            launcher.withInjectedClassPath(injectedClassPath);
        }

        List<String> buildArgs = parameters.getBuildArgs();
        if (buildArgs != null && !buildArgs.isEmpty()) {
            launcher.withArguments(buildArgs);
        }

        launcher.setJvmArguments(parameters.getJvmArgs().toArray(new String[0]));
        launcher.setEnvironmentVariables(parameters.getEnvironment());

        final int[] taskNum = new int[1];
        launcher.addProgressListener((ProgressListener) event -> {
            if ("Execute tasks".equals(event.getDescription())) {
                taskNum[0]++;
            }
        });
        System.out.println("Executed " + taskNum[0] + " tasks.");

        try {

            launcher.run();
        } catch (BuildException e) {
            // ignore...
            e.printStackTrace();
        }
    }

    public void close() {
        connection.close();
    }

    private String[] getArguments(StartParameter parameter) {
        List<String> args = new ArrayList<>();
        for (Map.Entry<String, String> e : parameter.getProjectProperties().entrySet()) {
            args.add("-P" + e.getKey() + "=" + e.getValue());
        }
        return args.toArray(new String[0]);
    }

}
