package run.halo.gradle.docker;

import groovy.transform.CompileStatic;
import java.util.concurrent.Callable;
import lombok.Getter;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;

@Getter
@CompileStatic
public abstract class DockerExistingContainer extends AbstractDockerRemoteApiTask {
    /**
     * The ID or name of container used to perform operation. The container for the provided ID has to be created first.
     */
    @Input
    final Property<String> containerId = getProject().getObjects().property(String.class);

    /**
     * Sets the target container ID or name.
     *
     * @param containerId Container ID or name
     * @see #targetContainerId(Callable)
     * @see #targetContainerId(Provider)
     */
    void targetContainerId(String containerId) {
        this.containerId.set(containerId);
    }

    /**
     * Sets the target container ID or name.
     *
     * @param containerId Container ID or name as Callable
     * @see #targetContainerId(String)
     * @see #targetContainerId(Provider)
     */
    void targetContainerId(Callable<String> containerId) {
        targetContainerId(getProject().provider(containerId));
    }

    /**
     * Sets the target container ID or name.
     *
     * @param containerId Container ID or name as Provider
     * @see #targetContainerId(String)
     * @see #targetContainerId(Callable)
     */
    void targetContainerId(Provider<String> containerId) {
        this.containerId.set(containerId);
    }
}
