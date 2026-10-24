package com.tokenengine.optimiser.api;

import com.tokenengine.analyser.api.WasteReport;

/**
 * Rewrites a Messages API request to use fewer tokens.
 *
 * <p>Rules every implementation must follow:
 * <ul>
 *   <li>Never change the cached prefix: the {@code system} array, {@code tools}, and every message that was
 *       already sent on an earlier turn. Changing it breaks Anthropic's prompt cache (cache reads cost a fraction
 *       of normal input) and can trigger "bound to a different conversation" thinking-signature errors.</li>
 *   <li>Keep {@code cache_control} markers, block-form content and the field/header pairs Claude Code relies on.</li>
 *   <li>Fail open: on any doubt, return the body unchanged.</li>
 * </ul>
 */
public interface Optimiser {

    OptimisationResult optimise(byte[] messagesRequestBody, WasteReport report);
}
