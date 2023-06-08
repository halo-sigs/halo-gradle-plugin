package run.halo.gradle;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.internal.tasks.execution.ExecuteTaskBuildOperationDetails;
import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.internal.operations.BuildOperationDescriptor;
import org.gradle.internal.operations.BuildOperationListener;
import org.gradle.internal.operations.OperationFinishEvent;
import org.gradle.internal.operations.OperationIdentifier;
import org.gradle.internal.operations.OperationProgressEvent;
import org.gradle.internal.operations.OperationStartEvent;
import run.halo.gradle.docker.DockerClientConfiguration;
import run.halo.gradle.docker.DockerClientService;

public abstract class HaloServerBuildOperationListener
    implements BuildService<HaloServerBuildOperationListener.Params>, BuildOperationListener {

    private final static Set<String> SUPPORTED_TASK_NAME =
        Set.of(HaloServerTask.TASK_NAME, "watch");

    public interface Params extends BuildServiceParameters {

        /**
         * Docker client service to operate docker.
         *
         * @return docker client service
         */
        Property<DockerClientService> getDockerClientService();

        /**
         * The existing container of halo server.
         *
         * @return The container id
         */
        Property<File> getContainerId();
    }

    @Override
    public void started(@Nonnull BuildOperationDescriptor buildOperation,
        @Nonnull OperationStartEvent startEvent) {

    }

    @Override
    public void progress(@Nonnull OperationIdentifier operationIdentifier,
        @Nonnull OperationProgressEvent progressEvent) {
    }

    @Override
    public void finished(BuildOperationDescriptor buildOperation,
        @Nonnull OperationFinishEvent finishEvent) {
        Object details = buildOperation.getDetails();
        if (details instanceof ExecuteTaskBuildOperationDetails executeTaskDetails) {
            String name = executeTaskDetails.getTask().getName();
            if (!SUPPORTED_TASK_NAME.contains(name)) {
                return;
            }
            DockerClientService dockerClientService =
                getParameters().getDockerClientService().getOrNull();
            if (dockerClientService == null) {
                return;
            }
            File containerIdFile = getParameters().getContainerId().get();
            if (!containerIdFile.exists()) {
                return;
            }
            String containerId = null;
            try {
                containerId = readFirstLine(containerIdFile);
            } catch (IOException e) {
                //ignore this exception
            }
            if (StringUtils.isBlank(containerId)) {
                return;
            }
            try (DockerClient dockerClient = getDockerClient(dockerClientService);
                 RemoveContainerCmd removeContainerCmd = dockerClient.removeContainerCmd(
                     containerId)) {
                removeContainerCmd.withForce(true).exec();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    String readFirstLine(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        return lines.isEmpty() ? null : lines.get(0);
    }

    DockerClient getDockerClient(DockerClientService dockerClientService) {
        return dockerClientService.getDockerClient(createDockerClientConfig(dockerClientService));
    }

    private DockerClientConfiguration createDockerClientConfig(
        DockerClientService dockerClientService) {
        DockerClientService.Params parameters = dockerClientService.getParameters();
        DockerClientConfiguration dockerClientConfig = new DockerClientConfiguration();
        dockerClientConfig.setUrl(parameters.getUrl().getOrNull());
        dockerClientConfig.setCertPath(parameters.getCertPath().getOrNull());
        dockerClientConfig.setApiVersion(parameters.getApiVersion().getOrNull());
        return dockerClientConfig;
    }
}
