package com.tokenengine.analyser.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.tokenengine.analyser.api.WasteReport;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class DuplicateContentDetectorTest {

    private final DuplicateContentDetector detector = new DuplicateContentDetector(JsonMapper.builder().build(), 100);
    private final String file = "x".repeat(500); // stands in for a file the agent read

    @Test
    void findsAFileReadTwiceAndFlagsTheUncachedRepeat() {
        String body = """
                {"model":"claude-sonnet-4-6","messages":[
                  {"role":"user","content":"fix the bug"},
                  {"role":"assistant","content":[{"type":"tool_use","id":"t1","name":"Read","input":{}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"t1","content":"%s"}]},
                  {"role":"assistant","content":[{"type":"tool_use","id":"t2","name":"Read","input":{}}]},
                  {"role":"user","content":[{"type":"tool_result","tool_use_id":"t2","content":[{"type":"text","text":"%s"}]}]}
                ]}""".formatted(file, file);

        WasteReport report = detector.analyse(body.getBytes(StandardCharsets.UTF_8));

        assertThat(report.blocksScanned()).isEqualTo(2);
        assertThat(report.duplicateBlocks()).isEqualTo(1);
        assertThat(report.duplicateBytes()).isEqualTo(500);
        assertThat(report.latestTurnDuplicateBytes()).isEqualTo(500);
        assertThat(report.estimatedLatestTurnWasteTokens()).isEqualTo(125);
    }

    @Test
    void ignoresSmallBlocksAndBadJson() {
        String body = "{\"messages\":[{\"role\":\"user\",\"content\":\"hi\"},{\"role\":\"user\",\"content\":\"hi\"}]}";

        assertThat(detector.analyse(body.getBytes(StandardCharsets.UTF_8)).duplicateBlocks()).isZero();
        assertThat(detector.analyse("not json".getBytes(StandardCharsets.UTF_8))).isEqualTo(WasteReport.none());
    }
}
