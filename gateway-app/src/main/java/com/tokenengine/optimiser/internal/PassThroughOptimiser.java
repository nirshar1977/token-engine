package com.tokenengine.optimiser.internal;

import com.tokenengine.analyser.api.WasteReport;
import com.tokenengine.optimiser.api.OptimisationResult;
import com.tokenengine.optimiser.api.Optimiser;
import org.springframework.stereotype.Component;

/**
 * Milestone 1: change nothing. Run the gateway like this first to collect a clean baseline of tokens,
 * cost and cache hits; the analyser's waste reports show where the first real strategy should aim.
 */
@Component
class PassThroughOptimiser implements Optimiser {

    @Override
    public OptimisationResult optimise(byte[] messagesRequestBody, WasteReport report) {
        return OptimisationResult.unchanged(messagesRequestBody, "pass-through");
    }
}
