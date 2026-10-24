package com.tokenengine.analyser.internal;

import com.tokenengine.analyser.api.ContextAnalyser;
import com.tokenengine.analyser.api.WasteReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
class JsonContextAnalyser implements ContextAnalyser {

    private final DuplicateContentDetector detector;

    JsonContextAnalyser(ObjectMapper mapper, AnalyserProperties properties) {
        this.detector = new DuplicateContentDetector(mapper, properties.minBlockBytes());
    }

    @Override
    public WasteReport analyse(byte[] messagesRequestBody) {
        try {
            return detector.analyse(messagesRequestBody);
        } catch (RuntimeException e) {
            log.warn("Context analysis failed, request continues untouched: {}", e.toString());
            return WasteReport.none();
        }
    }
}
