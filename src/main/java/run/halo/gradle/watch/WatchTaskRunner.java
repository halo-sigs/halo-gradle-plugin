package run.halo.gradle.watch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.gradle.StartParameter;
import org.gradle.api.Project;
import org.gradle.internal.classpath.ClassPath;
import org.gradle.tooling.BuildException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.internal.consumer.DefaultBuildLauncher;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.gradle.wrapper.GradleUserHomeLookup;
import run.halo.gradle.WatchExecutionParameters;
import run.halo.gradle.utils.Assert;

/**
 * @author guqing
 * @since 2.0.0
 */
public class WatchTaskRunner {

    private final GradleConnector gradleConnector;

    public WatchTaskRunner(Project project) {
        // StartParameter parameter = project.getGradle().getStartParameter();
        DefaultGradleConnector gradleConnector =
            (DefaultGradleConnector) GradleConnector.newConnector();
        gradleConnector.useGradleUserHomeDir(project.getGradle().getGradleUserHomeDir());
        gradleConnector.useDistributionBaseDir(GradleUserHomeLookup.gradleUserHome());
        this.gradleConnector = gradleConnector.forProjectDirectory(project.getProjectDir());
    }

    public void run(WatchExecutionParameters parameters) {
        Assert.notNull(parameters, "WatchExecutionParameters must not be null");
        try (var connection = gradleConnector.connect()) {
            var launcher = createBuildLauncher(parameters, connection);
            launcher.run();
        } catch (BuildException e) {
            System.err.println(e.getMessage());
        }
    }

    @Nonnull
    private static DefaultBuildLauncher createBuildLauncher(
        WatchExecutionParameters parameters, ProjectConnection connection) {
        DefaultBuildLauncher launcher = (DefaultBuildLauncher) connection
            .newBuild()
            .setStandardOutput(new NoCloseOutputStream(parameters.getStandardOutput()))
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
        return launcher;
    }

    static String[] getArguments(StartParameter parameter) {
        List<String> args = new ArrayList<>();
        for (Map.Entry<String, String> e : parameter.getProjectProperties().entrySet()) {
            args.add("-P" + e.getKey() + "=" + e.getValue());
        }
        return args.toArray(new String[0]);
    }

}
