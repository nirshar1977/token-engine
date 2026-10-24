package com.tokenengine.analyser.api;

/**
 * What the analyser found in one Messages API request. Read-only: producing a report never changes the request.
 *
 * @param blocksScanned          content blocks large enough to be checked
 * @param duplicateBlocks        blocks whose exact text already appeared earlier in the conversation
 * @param duplicateBytes         total size of those repeated blocks
 * @param latestTurnDuplicateBytes repeated bytes in the newest message only: content that is not in the
 *                               prompt cache yet and is billed at the full input price on this request
 *                               (typically an agent re-reading a file it already read)
 */
public record WasteReport(int blocksScanned, int duplicateBlocks, long duplicateBytes, long latestTurnDuplicateBytes) {

    public static WasteReport none() {
        return new WasteReport(0, 0, 0, 0);
    }

    /** Rough token equivalent of the latest-turn waste (about 4 bytes per token for code and English). */
    public long estimatedLatestTurnWasteTokens() {
        return latestTurnDuplicateBytes / 4;
    }
}
