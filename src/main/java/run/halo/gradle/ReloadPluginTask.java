package run.halo.gradle;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import org.gradle.api.GradleException;
import org.gradle.work.DisableCachingByDefault;
import run.halo.gradle.docker.AbstractDockerRemoteApiTask;
import run.halo.gradle.docker.DockerCreateContainer;
import run.halo.gradle.extension.HaloExtension;
import run.halo.gradle.extension.HaloPluginExtension;
import run.halo.gradle.steps.HaloSiteOption;
import run.halo.gradle.steps.PluginClient;
import run.halo.gradle.utils.JsonUtils;

/**
 * A task to reload plugin by name.
 *
 * @author guqing
 * @see HaloExtension
 * @see HaloPluginExtension
 * @since 1.0.0
 */
@DisableCachingByDefault(because = "Not worth caching")
public class ReloadPluginTask extends AbstractDockerRemoteApiTask {
    public static final String TASK_NAME = "reloadPlugin";

    private final PluginClient pluginClient;
    private final File containerIdFile;
    private final boolean explicitPort;

    public ReloadPluginTask() {
        this.pluginClient = new PluginClient(getProject());
        this.containerIdFile = getProject().getRootProject().getLayout().getProjectDirectory()
            .file(DockerCreateContainer.containerIdFilePath(getProject().getPath())).getAsFile();
        this.explicitPort = getProject().getProviders().gradleProperty("port").isPresent();
        var halo = getProject().getExtensions().getByType(HaloExtension.class);
        getUrl().convention(halo.getDocker().getUrl());
    }

    @Override
    public void runRemoteCommand() {
        reloadPlugin();
    }

    public void reloadPlugin() {
        var siteOption = resolveSiteOption();
        new PluginClient(pluginClient.getPluginName(), siteOption).reloadPlugin();
    }

    private HaloSiteOption resolveSiteOption() {
        var configured = pluginClient.getSiteOption();
        if (explicitPort || !containerIdFile.exists()) {
            return configured;
        }

        String containerId;
        try {
            containerId = Files.readString(containerIdFile.toPath()).trim();
        } catch (IOException e) {
            throw new GradleException("Cannot read the recorded Halo container ID.", e);
        }
        if (containerId.isEmpty()) {
            throw new GradleException("Recorded Halo container ID is empty. Restart haloServer.");
        }

        InspectContainerResponse container;
        try (var command = getDockerClient().inspectContainerCmd(containerId)) {
            container = command.exec();
        } catch (NotFoundException e) {
            throw new GradleException("Recorded Halo container no longer exists. "
                + "Restart haloServer or specify -Pport to select a server.");
        }
        if (container.getState() == null
            || !Boolean.TRUE.equals(container.getState().getRunning())) {
            throw new GradleException("Recorded Halo container is not running. "
                + "Restart haloServer or specify -Pport to select a server.");
        }
        return new HaloSiteOption(configured.username(), configured.password(),
            containerUrl(container));
    }

    private URI containerUrl(InspectContainerResponse container) {
        String[] environment = container.getConfig() == null
            ? null : container.getConfig().getEnv();
        if (environment != null) {
            for (String entry : environment) {
                String prefix = "SPRING_APPLICATION_JSON=";
                if (entry.startsWith(prefix)) {
                    try {
                        var configuration = JsonUtils.mapper()
                            .readTree(entry.substring(prefix.length()));
                        if (configuration == null) {
                            break;
                        }
                        var url = URI.create(configuration.path("halo")
                            .path("external-url").asText());
                        if (("http".equalsIgnoreCase(url.getScheme())
                            || "https".equalsIgnoreCase(url.getScheme()))
                            && url.getHost() != null) {
                            return url;
                        }
                    } catch (IOException | IllegalArgumentException e) {
                        // Do not expose container configuration, which can contain credentials.
                    }
                    break;
                }
            }
        }
        throw new GradleException("Cannot determine the recorded Halo container URL. "
            + "Restart haloServer or specify -Pport to select a server.");
    }
}
