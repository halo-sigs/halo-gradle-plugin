package run.halo.gradle.watch;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * @author guqing
 * @since 2.0.0
 */
public class DevNullPrintStream extends PrintStream {

    public DevNullPrintStream() {
        super(new OutputStream() {
            public void write(int b) { }
            public void write(byte[] b) { }
            public void write(byte[] b, int off, int len) { }
        });
    }
}
