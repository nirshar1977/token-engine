package com.tokenengine.gateway.internal;

import com.tokenengine.usage.api.RequestUsage;
import com.tokenengine.usage.api.UsageRecorder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Records usage off the request thread, so a slow database never delays a developer's response.
 * Deliberately not an {@code Executor} bean: that would switch off Spring Boot's own task executor.
 */
@Slf4j
@Component
class AsyncUsageRecorder implements AutoCloseable {

    private final UsageRecorder recorder;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    AsyncUsageRecorder(UsageRecorder recorder) {
        this.recorder = recorder;
    }

    void recordLater(RequestUsage usage) {
        executor.execute(() -> {
            try {
                recorder.record(usage);
            } catch (RuntimeException e) {
                log.warn("Could not record usage for {}: {}", usage.requestId(), e.toString());
            }
        });
    }

    /** Waits for in-flight recordings on shutdown. */
    @Override
    public void close() {
        executor.close();
    }
}
