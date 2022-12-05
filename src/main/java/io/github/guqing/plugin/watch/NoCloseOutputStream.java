package io.github.guqing.plugin.watch;

import lombok.NonNull;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author guqing
 * @since 2.0.0
 */
public class NoCloseOutputStream extends OutputStream {
    private final OutputStream delegate;

    public NoCloseOutputStream(OutputStream delegate) {
        this.delegate = delegate;
    }

    public synchronized void write(int b) throws IOException {
        this.delegate.write(b);
    }

    public synchronized void flush() throws IOException {
        this.delegate.flush();
    }

    public synchronized void write(byte @NonNull [] b, int off, int len) throws IOException {
        this.delegate.write(b, off, len);
    }

    public synchronized void write(byte @NonNull [] b) throws IOException {
        this.delegate.write(b);
    }

    public synchronized void close() throws IOException {
    }
}
