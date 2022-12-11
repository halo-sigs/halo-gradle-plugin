package io.github.guqing.plugin;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RetryUtils {
    public interface CallToRetry {
        boolean process() throws Exception;
    }

    /**
     * Retry the given call.
     * We use a function <code>baseSleepMillis * Math.pow(retryTimes, 2)</code> to calculate the sleep time
     */
    public static void withRetry(int maxTimes, long baseSleepMillis, CallToRetry call) {
        if (maxTimes <= 0) {
            throw new IllegalArgumentException("Must run at least one time");
        }
        if (baseSleepMillis <= 0) {
            throw new IllegalArgumentException("Initial wait must be at least 1");
        }
        Exception thrown = null;
        for (int i = 0; i < maxTimes; i++) {
            try {
                boolean result = call.process();
                if (result) {
                    break;
                }
            } catch (Exception e) {
                thrown = e;
                log.debug("Encountered failure on {} due to {}, attempt retry {} of {}",
                    call.getClass().getName(), e.getMessage(), (i + 1), maxTimes, e);
            }

            try {
                long sleepTime = baseSleepMillis * (long) Math.pow(i + 1, 2);
                Thread.sleep(sleepTime);
            } catch (InterruptedException wakeAndAbort) {
                break;
            }
        }

        if (thrown != null) {
            throw new IllegalStateException("Execution timeout:" + thrown);
        }
    }
}
