package run.halo.gradle.docker;

import java.util.concurrent.Callable;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;

public abstract class DockerExistingImage extends AbstractDockerRemoteApiTask {
    /**
     * The ID or name of image used to perform operation. The image for the provided ID has to be
     * created first.
     */
    @Input
    public final Property<String> getImageId() {
        return imageId;
    }

    private final Property<String> imageId = getProject().getObjects().property(String.class);

    private final ProviderFactory providers = getProject().getProviders();

    /**
     * Sets the target image ID or name.
     *
     * @param imageId Image ID or name
     * @see #targetImageId(Callable)
     * @see #targetImageId(Provider)
     */
    public void targetImageId(String imageId) {
        this.imageId.set(imageId);
    }

    /**
     * Sets the target image ID or name.
     *
     * @param imageId Image ID or name as Callable
     * @see #targetImageId(String)
     * @see #targetImageId(Provider)
     */
    public void targetImageId(Callable<String> imageId) {
        targetImageId(providers.provider(imageId));
    }

    /**
     * Sets the target image ID or name.
     *
     * @param imageId Image ID or name as Provider
     * @see #targetImageId(String)
     * @see #targetImageId(Callable)
     */
    public void targetImageId(Provider<String> imageId) {
        this.imageId.set(imageId);
    }
}
