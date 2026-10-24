package com.tokenengine.gateway.internal;

import java.io.ByteArrayOutputStream;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Reads usage from a non-streamed JSON response (or a JSON error body). */
final class JsonUsageTap implements UsageTap {

    private static final int MAX_BODY_BYTES = 16 << 20;

    private final ObjectMapper mapper;
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();
    private boolean tooLarge;

    JsonUsageTap(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void accept(byte[] buffer, int offset, int length) {
        if (tooLarge) {
            return;
        }
        if (body.size() + length > MAX_BODY_BYTES) {
            tooLarge = true;
            body.reset();
            return;
        }
        body.write(buffer, offset, length);
    }

    @Override
    public ObservedUsage result() {
        if (tooLarge || body.size() == 0) {
            return ObservedUsage.NONE;
        }
        JsonNode root;
        try {
            root = mapper.readTree(body.toByteArray());
        } catch (Exception e) {
            return ObservedUsage.NONE;
        }
        UsageAccumulator usage = new UsageAccumulator();
        String type = root.path("type").isString() ? root.path("type").asString() : "";
        if ("error".equals(type)) {
            usage.error(root.path("error").path("type"));
            return usage.snapshot();
        }
        usage.model(root.path("model"));
        usage.applyUsage(root.path("usage"));
        if ("message".equals(type)) {
            usage.markComplete();
        }
        return usage.snapshot();
    }
}
