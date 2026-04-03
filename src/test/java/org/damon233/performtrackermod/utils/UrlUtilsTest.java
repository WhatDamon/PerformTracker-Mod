package org.damon233.performtrackermod.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class UrlUtilsTest {

    @ParameterizedTest
    @CsvSource({
        "http://localhost:31415, 31415",
        "http://localhost:8080, 8080",
        "http://example.com:3000, 3000",
        "https://localhost:443, 443",
        "http://127.0.0.1:9000, 9000",
        "http://localhost:8080/api/metrics, 8080",
        "http://sub.domain.example.com:65535, 65535"
    })
    void parsePort_withPort_returnsCorrectPort(String url, int expectedPort) {
        int result = UrlUtils.parsePort(url);
        assertEquals(expectedPort, result);
    }

    @Test
    void parsePort_withoutPort_returnsDefaultPort() {
        int result = UrlUtils.parsePort("http://localhost");
        assertEquals(31415, result);
    }

    @Test
    void parsePort_withTrailingSlash_returnsDefaultPort() {
        int result = UrlUtils.parsePort("http://localhost/");
        assertEquals(31415, result);
    }

    @Test
    void parsePort_withPath_returnsDefaultPort() {
        int result = UrlUtils.parsePort("http://localhost/api/v1");
        assertEquals(31415, result);
    }

    @ParameterizedTest
    @CsvSource({
        "http://localhost:80/path, 80",
        "http://localhost:443/path, 443",
        "http://localhost:65535/path, 65535"
    })
    void parsePort_withVariousPorts_returnsCorrectPort(String url, int expectedPort) {
        int result = UrlUtils.parsePort(url);
        assertEquals(expectedPort, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://localhost:1",
        "http://localhost:65535"
    })
    void parsePort_edgePortValues_accepted(String url) {
        int result = UrlUtils.parsePort(url);
        assertTrue(result >= 1 && result <= 65535);
    }

    @ParameterizedTest
    @CsvSource({
        "http://localhost, localhost",
        "http://127.0.0.1, 127.0.0.1",
        "http://example.com, example.com",
        "https://localhost:443, localhost",
        "http://sub.domain.example.com:8080, sub.domain.example.com"
    })
    void parseHost_withValidUrl_returnsCorrectHost(String url, String expectedHost) {
        String result = UrlUtils.parseHost(url);
        assertEquals(expectedHost, result);
    }

    @Test
    void parseHost_withIpv6_returnsCorrectHost() {
        String result = UrlUtils.parseHost("http://[::1]:8080/");
        assertEquals("[::1]", result);
    }

    @Test
    void parseHost_withPath_returnsCorrectHost() {
        String result = UrlUtils.parseHost("http://example.com/api/metrics");
        assertEquals("example.com", result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void parseHost_withNullOrEmpty_returnsDefault(String url) {
        String result = UrlUtils.parseHost(url);
        assertEquals("localhost", result);
    }

    @Test
    void parseHost_withInvalidUrl_returnsDefault() {
        String result = UrlUtils.parseHost("not-a-url");
        assertEquals("localhost", result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://localhost:31415",
        "https://localhost:31415",
        "http://example.com:8080",
        "http://sub.domain.example.com:3000",
        "http://127.0.0.1:9000"
    })
    void parseHost_variousUrls_returnsNonEmpty(String url) {
        String result = UrlUtils.parseHost(url);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}
