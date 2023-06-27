package run.halo.gradle.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutionException;
import lombok.experimental.UtilityClass;

/**
 * @author guqing
 * @since 2.0.0
 */
@UtilityClass
public class DebugUtils {

    public static String findAvailableDebugAddress() throws ExecutionException {
        final int freePort;
        try {
            freePort = findAvailableSocketPort();
        } catch (IOException e) {
            throw new ExecutionException(e);
        }
        return Integer.toString(freePort);
    }

    private static int findAvailableSocketPort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            // workaround for linux : calling close() immediately after opening socket
            // may result that socket is not closed
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (serverSocket) {
                try {
                    //noinspection WaitNotInLoop
                    serverSocket.wait(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return port;
        }
    }
}
