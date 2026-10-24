package com.tokenengine.usage.api;

public interface UsageRecorder {

    /** Stores the usage, prices it and publishes {@link UsageRecorded}. */
    void record(RequestUsage usage);
}
