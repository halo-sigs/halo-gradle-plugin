package run.halo.gradle.docker;

import com.github.dockerjava.api.command.CopyArchiveToContainerCmd;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import run.halo.gradle.utils.Assert;

/**
 * @author guqing
 * @since 2.0.0
 */
@Slf4j
public class DockerCopyFileToContainer extends DockerExistingContainer {
    /**
     * Path of file inside container
     */
    @Input
    @Optional
    final Property<String> remotePath = getProject().getObjects().property(String.class);

    /**
     * File path on host to copy into container
     */
    @Input
    @Optional
    final Property<String> hostPath = getProject().getObjects().property(String.class);

    /**
     * Tar file we will copy into container
     */
    @InputFile
    @Optional
    final RegularFileProperty tarFile = getProject().getObjects().fileProperty();

    @Input
    @Optional
    final List<CopyFileToContainer> copyFiles = new ArrayList<>();

    private final FileOperations fileOperations =
        ((ProjectInternal) getProject()).getFileOperations();

    @Override
    public void runRemoteCommand() {
        if (remotePath.getOrNull() != null) {
            if (hostPath.getOrNull() != null && tarFile.getOrNull() != null) {
                throw new GradleException("Can specify either hostPath or tarFile not both");
            } else {
                if (hostPath.getOrNull() != null) {
                    withFile(hostPath.get(), remotePath.get());
                } else if (tarFile.getOrNull() != null) {
                    withTarFile(tarFile.get(), remotePath.get());
                }
            }
        }

        for (CopyFileToContainer fileToCopy : copyFiles) {
            log.debug("Copying file to container with ID [{}] at [{}].", containerId.get(),
                fileToCopy.getRemotePath());
            CopyArchiveToContainerCmd containerCommand =
                getDockerClient().copyArchiveToContainerCmd(containerId.get());
            setContainerCommandConfig(containerCommand, fileToCopy);
            containerCommand.exec();
        }
    }

    private void setContainerCommandConfig(CopyArchiveToContainerCmd containerCommand,
        CopyFileToContainer copyFileToContainer) {
        File localHostPath = fileOperations.file(copyFileToContainer.hostPath);

        containerCommand.withRemotePath(copyFileToContainer.remotePath.toString());

        if (copyFileToContainer.isTar()) {
            try {
                containerCommand.withTarInputStream(new FileInputStream(localHostPath));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            containerCommand.withHostResource(localHostPath.getPath());
        }
    }

    /**
     * Add a file to be copied into container
     *
     * @param hostPath can be either String, GString, File or Closure which returns any of the
     * previous.
     * @param remotePath can be either String, GString, File or Closure which returns any of the
     * previous.
     */
    void withFile(String hostPath, String remotePath) {
        Assert.notNull(hostPath, "hostPath cannot be null");
        Assert.notNull(remotePath, "remotePath cannot be null");
        CopyFileToContainer copyFileToContainer = new CopyFileToContainer();
        copyFileToContainer.setHostPath(hostPath);
        copyFileToContainer.setRemotePath(remotePath);
        copyFiles.add(copyFileToContainer);
    }

    void withTarFile(RegularFile hostPath, String remotePath) {
        Assert.notNull(hostPath, "hostPath cannot be null");
        Assert.notNull(remotePath, "remotePath cannot be null");
        CopyFileToContainer copyFileToContainer = new CopyFileToContainer();
        copyFileToContainer.setHostPath(hostPath);
        copyFileToContainer.setRemotePath(remotePath);
        copyFileToContainer.setTar(true);
        copyFiles.add(copyFileToContainer);
    }

    /**
     * Class holding metadata for an arbitrary copy-file-to-container invocation.
     */
    @Data
    public static class CopyFileToContainer {
        /**
         * The host path.
         * <p>
         * Can take the form of {@code String}, {@code GString}, {@code File}, or {@code Closure}
         * which returns any of the previous.
         */
        @Input
        private Object hostPath;

        /**
         * The remote path.
         * <p>
         * Can take the form of {@code String}, {@code GString}, {@code File}, or {@code Closure}
         * which returns any of the previous.
         */
        @Input
        private Object remotePath;

        /**
         * Indicates if copied file is a TAR file.
         */
        @Internal
        private boolean isTar = false;
    }

    public Property<String> getRemotePath() {
        return remotePath;
    }

    public Property<String> getHostPath() {
        return hostPath;
    }

    public List<CopyFileToContainer> getCopyFiles() {
        return copyFiles;
    }

    public RegularFileProperty getTarFile() {
        return tarFile;
    }
}
