package com.tokenengine.optimiser.api;

/**
 * @param body     the request body to send upstream (the original array when nothing changed)
 * @param applied  whether the body was changed
 * @param strategy name of the strategy that ran, for reporting
 */
public record OptimisationResult(byte[] body, boolean applied, String strategy) {

    public static OptimisationResult unchanged(byte[] body, String strategy) {
        return new OptimisationResult(body, false, strategy);
    }
}
