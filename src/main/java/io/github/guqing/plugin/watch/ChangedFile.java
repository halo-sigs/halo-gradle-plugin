package io.github.guqing.plugin.watch;

import io.github.guqing.plugin.Assert;
import org.apache.commons.io.file.PathUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author guqing
 * @since 2.0.0
 */
public class ChangedFile {
    private final File sourceDirectory;

    private final File file;

    private final Type type;

    /**
     * Create a new {@link ChangedFile} instance.
     *
     * @param sourceDirectory the source directory
     * @param file            the file
     * @param type            the type of change
     */
    public ChangedFile(File sourceDirectory, File file, Type type) {
        Assert.notNull(sourceDirectory, "SourceDirectory must not be null");
        Assert.notNull(file, "File must not be null");
        Assert.notNull(type, "Type must not be null");
        this.sourceDirectory = sourceDirectory;
        this.file = file;
        this.type = type;
    }

    /**
     * Return the file that was changed.
     *
     * @return the file
     */
    public File getFile() {
        return this.file;
    }

    /**
     * Return the type of change.
     *
     * @return the type of change
     */
    public Type getType() {
        return this.type;
    }

    /**
     * Return the name of the file relative to the source directory.
     *
     * @return the relative name
     */
    public String getRelativeName() {
        File directory = this.sourceDirectory.getAbsoluteFile();
        File file = this.file.getAbsoluteFile();
        try {
            String directoryName = PathUtils.cleanDirectory(directory.toPath()).toString();
            String fileName = PathUtils.cleanDirectory(file.toPath()).toString();
            Assert.state(fileName.startsWith(directoryName),
                    () -> "The file " + fileName + " is not contained in the source directory " + directoryName);
            return fileName.substring(directoryName.length() + 1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof ChangedFile other) {
            return this.file.equals(other.file) && this.type.equals(other.type);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.file.hashCode() * 31 + this.type.hashCode();
    }

    @Override
    public String toString() {
        return this.file + " (" + this.type + ")";
    }

    /**
     * Change types.
     */
    public enum Type {

        /**
         * A new file has been added.
         */
        ADD,

        /**
         * An existing file has been modified.
         */
        MODIFY,

        /**
         * An existing file has been deleted.
         */
        DELETE

    }
}
