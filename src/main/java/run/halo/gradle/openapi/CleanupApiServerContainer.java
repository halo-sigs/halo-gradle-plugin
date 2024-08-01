package run.halo.gradle.openapi;

import com.github.dockerjava.api.DockerClient;
import javax.annotation.Nonnull;
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

public abstract class CleanupApiServerContainer
    implements BuildService<CleanupApiServerContainer.Params>, BuildOperationListener {

    public interface Params extends BuildServiceParameters {

        /**
         * Docker client service to operate docker.
         *
         * @return docker client service
         */
        Property<DockerClientService> getDockerClientService();
    }

    @Override
    public void started(@Nonnull BuildOperationDescriptor buildOperationDescriptor,
        @Nonnull OperationStartEvent operationStartEvent) {

    }

    @Override
    public void progress(@Nonnull OperationIdentifier operationIdentifier,
        @Nonnull OperationProgressEvent operationProgressEvent) {

    }

    @Override
    public void finished(@Nonnull BuildOperationDescriptor buildOperation,
        @Nonnull OperationFinishEvent finishEvent) {
        Object details = buildOperation.getDetails();
        if (details instanceof ExecuteTaskBuildOperationDetails executeTaskDetails) {
            String name = executeTaskDetails.getTask().getName();
            if (!OpenApiDocsGeneratorTask.TASK_NAME.contains(name)) {
                return;
            }
            DockerClientService dockerClientService =
                getParameters().getDockerClientService().getOrNull();
            if (dockerClientService == null) {
                return;
            }
            try (var dockerClient = getDockerClient(dockerClientService)) {
                dockerClient.removeContainerCmd(OpenApiDocsGeneratorTask.CONTAINER_NAME)
                    .withForce(true).exec();
            } catch (Exception e) {
                // ignore this exception
            }
        }
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
