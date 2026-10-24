package com.tokenengine.analyser.internal;

import com.tokenengine.analyser.api.WasteReport;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Finds text and tool_result blocks whose exact content already appeared earlier in the conversation.
 * Plain Java (no Spring) so it is easy to unit test and to reuse.
 */
final class DuplicateContentDetector {

    private final ObjectMapper mapper;
    private final int minBlockBytes;

    DuplicateContentDetector(ObjectMapper mapper, int minBlockBytes) {
        this.mapper = mapper;
        this.minBlockBytes = minBlockBytes;
    }

    WasteReport analyse(byte[] body) {
        JsonNode messages;
        try {
            messages = mapper.readTree(body).path("messages");
        } catch (Exception e) {
            return WasteReport.none();
        }
        if (!messages.isArray() || messages.isEmpty()) {
            return WasteReport.none();
        }

        Set<String> seen = new HashSet<>();
        int scanned = 0;
        int duplicates = 0;
        long duplicateBytes = 0;
        long latestTurnDuplicateBytes = 0;
        int lastIndex = messages.size() - 1;

        for (int i = 0; i <= lastIndex; i++) {
            for (String text : textsOf(messages.get(i).path("content"))) {
                byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                if (bytes.length < minBlockBytes) {
                    continue;
                }
                scanned++;
                if (!seen.add(sha256(bytes))) {
                    duplicates++;
                    duplicateBytes += bytes.length;
                    if (i == lastIndex) {
                        latestTurnDuplicateBytes += bytes.length;
                    }
                }
            }
        }
        return new WasteReport(scanned, duplicates, duplicateBytes, latestTurnDuplicateBytes);
    }

    /** Text of a message's content: a plain string, text blocks, and the text inside tool_result blocks. */
    private static List<String> textsOf(JsonNode content) {
        List<String> texts = new ArrayList<>();
        if (content.isString()) {
            texts.add(content.asString());
            return texts;
        }
        if (!content.isArray()) {
            return texts;
        }
        for (JsonNode block : content) {
            String type = block.path("type").asString();
            if ("text".equals(type)) {
                texts.add(block.path("text").asString());
            } else if ("tool_result".equals(type)) {
                JsonNode inner = block.path("content");
                if (inner.isString()) {
                    texts.add(inner.asString());
                } else if (inner.isArray()) {
                    for (JsonNode part : inner) {
                        if ("text".equals(part.path("type").asString())) {
                            texts.add(part.path("text").asString());
                        }
                    }
                }
            }
        }
        return texts;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
