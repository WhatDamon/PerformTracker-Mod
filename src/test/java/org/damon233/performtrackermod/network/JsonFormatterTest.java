package org.damon233.performtrackermod.network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.*;

class JsonFormatterTest {

    @Test
    void formatMetrics_withAllMetricsEnabled_containsAllFields() {
        String result = JsonFormatter.formatMetrics(
            1234567890L, "session-123", 1,
            true, 60.5,
            true, 20.0,
            true, 45.2,
            true, 512.0, 1024.0,
            true, 45.0
        );

        assertTrue(result.contains("\"timestamp\":1234567890"));
        assertTrue(result.contains("\"sessionId\":\"session-123\""));
        assertTrue(result.contains("\"sampleNumber\":1"));
        assertTrue(result.contains("\"fps\":60.5"));
        assertTrue(result.contains("\"tps\":20.0"));
        assertTrue(result.contains("\"mspt\":45.2"));
        assertTrue(result.contains("\"heapUsed\":512.0"));
        assertTrue(result.contains("\"heapMax\":1024.0"));
        assertTrue(result.contains("\"cpu\":45.0"));
    }

    @Test
    void formatMetrics_withOnlyFps_containsOnlyFpsField() {
        String result = JsonFormatter.formatMetrics(
            1234567890L, "session-123", 1,
            true, 60.5,
            false, 0,
            false, 0,
            false, 0, 0,
            false, 0
        );

        assertTrue(result.contains("\"fps\":60.5"));
        assertFalse(result.contains("\"tps\""));
        assertFalse(result.contains("\"mspt\""));
        assertFalse(result.contains("\"heapUsed\""));
        assertFalse(result.contains("\"cpu\""));
    }

    @Test
    void formatMetrics_withOnlyTps_containsOnlyTpsField() {
        String result = JsonFormatter.formatMetrics(
            1234567890L, "session-123", 1,
            false, 0,
            true, 20.0,
            false, 0,
            false, 0, 0,
            false, 0
        );

        assertTrue(result.contains("\"tps\":20.0"));
        assertFalse(result.contains("\"fps\""));
        assertFalse(result.contains("\"mspt\""));
        assertFalse(result.contains("\"cpu\""));
    }

    @Test
    void formatMetrics_withOnlyHeap_containsHeapFields() {
        String result = JsonFormatter.formatMetrics(
            1234567890L, "session-123", 1,
            false, 0,
            false, 0,
            false, 0,
            true, 256.0, 512.0,
            false, 0
        );

        assertTrue(result.contains("\"heapUsed\":256.0"));
        assertTrue(result.contains("\"heapMax\":512.0"));
        assertFalse(result.contains("\"fps\""));
        assertFalse(result.contains("\"tps\""));
        assertFalse(result.contains("\"mspt\""));
        assertFalse(result.contains("\"cpu\""));
    }

    @Test
    void formatMetrics_withOnlyCpu_containsCpuField() {
        String result = JsonFormatter.formatMetrics(
            1234567890L, "session-123", 1,
            false, 0,
            false, 0,
            false, 0,
            false, 0, 0,
            true, 75.5
        );

        assertTrue(result.contains("\"cpu\":75.5"));
        assertFalse(result.contains("\"fps\""));
        assertFalse(result.contains("\"tps\""));
        assertFalse(result.contains("\"mspt\""));
        assertFalse(result.contains("\"heapUsed\""));
    }

    @Test
    void formatMetrics_withNoMetricsEnabled_containsOnlyMetadata() {
        String result = JsonFormatter.formatMetrics(
            1234567890L, "session-123", 1,
            false, 0,
            false, 0,
            false, 0,
            false, 0, 0,
            false, 0
        );

        assertTrue(result.contains("\"timestamp\":1234567890"));
        assertTrue(result.contains("\"sessionId\":\"session-123\""));
        assertTrue(result.contains("\"data\":{}"));
        assertFalse(result.contains("\"fps\""));
        assertFalse(result.contains("\"tps\""));
        assertFalse(result.contains("\"mspt\""));
        assertFalse(result.contains("\"heapUsed\""));
        assertFalse(result.contains("\"cpu\""));
    }

    @Test
    void formatMetrics_createsValidJson() {
        String result = JsonFormatter.formatMetrics(
            1234567890L, "session-123", 42,
            true, 120.0,
            true, 20.0,
            true, 50.5,
            true, 1024.0, 2048.0,
            true, 60.0
        );

        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
        assertTrue(result.contains("\"data\":{"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void escapeJson_withNullOrEmpty_returnsEmptyString(String input) {
        String result = JsonFormatter.escapeJson(input);
        assertEquals("", result);
    }

    @Test
    void escapeJson_withNormalString_returnsUnchanged() {
        String input = "Hello World";
        String result = JsonFormatter.escapeJson(input);
        assertEquals("Hello World", result);
    }

    @Test
    void escapeJson_withBackslash_escapesCorrectly() {
        String input = "path\\to\\file";
        String result = JsonFormatter.escapeJson(input);
        assertEquals("path\\\\to\\\\file", result);
    }

    @Test
    void escapeJson_withDoubleQuote_escapesCorrectly() {
        String input = "say \"hello\"";
        String result = JsonFormatter.escapeJson(input);
        assertEquals("say \\\"hello\\\"", result);
    }

    @Test
    void escapeJson_withNewline_escapesCorrectly() {
        String input = "line1\nline2";
        String result = JsonFormatter.escapeJson(input);
        assertEquals("line1\\nline2", result);
    }

    @Test
    void escapeJson_withCarriageReturn_escapesCorrectly() {
        String input = "line1\rline2";
        String result = JsonFormatter.escapeJson(input);
        assertEquals("line1\\rline2", result);
    }

    @Test
    void escapeJson_withTab_escapesCorrectly() {
        String input = "col1\tcol2";
        String result = JsonFormatter.escapeJson(input);
        assertEquals("col1\\tcol2", result);
    }

    @Test
    void escapeJson_withMultipleSpecialChars_escapesAllCorrectly() {
        String input = "path\\to\"file\nwith\ttabs";
        String result = JsonFormatter.escapeJson(input);
        assertEquals("path\\\\to\\\"file\\nwith\\ttabs", result);
    }

    @ParameterizedTest
    @CsvSource({
        "hello, hello",
        "test value, test value"
    })
    void escapeJson_withSimpleStrings_unchanged(String input, String expected) {
        String result = JsonFormatter.escapeJson(input);
        assertEquals(expected, result);
    }
}
