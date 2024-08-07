package run.halo.gradle.watch;

import java.io.File;
import run.halo.gradle.utils.Assert;

/**
 * A snapshot of a File at a given point in time.
 *
 * @author guqing
 * @since 2.0.0
 */
public class FileSnapshot {
    private final File file;

    private final boolean exists;

    private final long length;

    private final long lastModified;

    FileSnapshot(File file) {
        Assert.notNull(file, "File must not be null");
        Assert.isTrue(file.isFile() || !file.exists(), "File must not be a directory");
        this.file = file;
        this.exists = file.exists();
        this.length = file.length();
        this.lastModified = file.lastModified();
    }

    File getFile() {
        return this.file;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof FileSnapshot other) {
            boolean equals = this.file.equals(other.file);
            equals = equals && this.exists == other.exists;
            equals = equals && this.length == other.length;
            equals = equals && this.lastModified == other.lastModified;
            return equals;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        int hashCode = this.file.hashCode();
        hashCode = 31 * hashCode + Boolean.hashCode(this.exists);
        hashCode = 31 * hashCode + Long.hashCode(this.length);
        hashCode = 31 * hashCode + Long.hashCode(this.lastModified);
        return hashCode;
    }

    @Override
    public String toString() {
        return this.file.toString();
    }

}
