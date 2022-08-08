package io.github.guqing.plugin;

import groovy.transform.CompileStatic;
import groovy.transform.Internal;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.options.Option;
import org.gradle.process.JavaExecSpec;

@CompileStatic
public class HaloServerTask extends DefaultTask {
    public static final String TASK_NAME = "server";
    private final List<Action<JavaExecSpec>> execSpecActions = new ArrayList<>();
    @Classpath
    final Property<Configuration> haloServerRuntime =
        getProject().getObjects().property(Configuration.class);

    @Input
    final Property<File> haloHome = getProject().getObjects().property(File.class);

    @Input
    @Option(option = "port", description = "Port to start Halo on (default: 8090)")
    final Property<String> port = getProject().getObjects().property(String.class)
        .convention("8090");

    @Internal
    final Provider<String> extractedMainClass = haloServerRuntime.map(it -> {
        it.getResolvedConfiguration().getFirstLevelModuleDependencies()
            .stream()
            .filter(resolvedDependency -> {
                return "halo".equals(resolvedDependency.getModuleName());
            }).findFirst().get();
        return "";
    });


    @Inject
    public HaloServerTask() {
        doFirst(action -> {
            System.out.println("--->" + haloHome.get());
            getProject().javaexec(s -> {
                s.classpath(haloServerRuntime.get());
                s.args("-Dserver.port=" + port.get());
                s.systemProperty("HALO_HOME", haloHome.get());
                execSpecActions.forEach(spec -> spec.execute(s));
            });
        });
    }

    void execSpec(Action<JavaExecSpec> action) {
        execSpecActions.add(action);
    }

    public Property<File> getHaloHome() {
        return haloHome;
    }

    public Property<Configuration> getHaloServerRuntime() {
        return haloServerRuntime;
    }

    public Property<String> getPort() {
        return port;
    }
}
