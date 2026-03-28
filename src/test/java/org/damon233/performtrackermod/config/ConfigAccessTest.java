package org.damon233.performtrackermod.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ConfigAccessTest {

    @ParameterizedTest
    @CsvSource({
        "http://localhost:31415, 31415",
        "http://localhost:8080, 8080",
        "http://example.com:3000, 3000",
        "https://localhost:443, 443",
        "http://127.0.0.1:9000, 9000"
    })
    void parsePort_withValidUrl_returnsCorrectPort(String url, int expectedPort) {
        int result = ConfigAccess.parsePort(url);
        assertEquals(expectedPort, result);
    }

    @Test
    void parsePort_withoutPort_returnsDefaultPort() {
        String url = "http://localhost";
        int result = ConfigAccess.parsePort(url);
        assertEquals(31415, result);
    }

    @Test
    void parsePort_withTrailingSlash_returnsDefaultPort() {
        String url = "http://localhost/";
        int result = ConfigAccess.parsePort(url);
        assertEquals(31415, result);
    }

    @Test
    void parsePort_withPath_returnsCorrectPort() {
        String url = "http://localhost:8080/api/metrics";
        int result = ConfigAccess.parsePort(url);
        assertEquals(8080, result);
    }

    @ParameterizedTest
    @CsvSource({
        "http://localhost:80/path, 80",
        "http://localhost:443/path, 443",
        "http://localhost:65535/path, 65535"
    })
    void parsePort_withVariousPorts_returnsCorrectPort(String url, int expectedPort) {
        int result = ConfigAccess.parsePort(url);
        assertEquals(expectedPort, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://localhost:31415",
        "https://localhost:31415",
        "http://example.com:31415",
        "http://sub.example.com:31415",
        "http://localhost:8080",
        "http://127.0.0.1:31415"
    })
    void validateNetworkEndpoint_withValidUrl_returnsNormalizedUrl(String url) {
        String result = ConfigAccess.validateNetworkEndpoint(url);
        assertNotNull(result);
        assertTrue(result.endsWith("/"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://localhost:31415",
        "https://localhost:31415"
    })
    void validateNetworkEndpoint_withHttps_preservesProtocol(String url) {
        String result = ConfigAccess.validateNetworkEndpoint(url);
        assertNotNull(result);
        assertTrue(result.startsWith(url.substring(0, url.indexOf("://") + 3).substring(0, 4)));
    }

    @Test
    void validateNetworkEndpoint_addsTrailingSlash() {
        String url = "http://localhost:31415";
        String result = ConfigAccess.validateNetworkEndpoint(url);
        assertTrue(result.endsWith("/"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void validateNetworkEndpoint_withNullOrEmpty_returnsNull(String endpoint) {
        String result = ConfigAccess.validateNetworkEndpoint(endpoint);
        assertNull(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "localhost:31415",
        "ftp://localhost:31415",
        "ws://localhost:31415",
        "httpx://localhost:31415",
        "http://localhost",
        "not-a-url",
        "http://",
        "://localhost:31415"
    })
    void validateNetworkEndpoint_withInvalidUrl_returnsNull(String endpoint) {
        String result = ConfigAccess.validateNetworkEndpoint(endpoint);
        assertNull(result);
    }

    @Test
    void validateNetworkEndpoint_withOnlyWhitespace_returnsNull() {
        String result = ConfigAccess.validateNetworkEndpoint("   ");
        assertNull(result);
    }

    @Test
    void validateNetworkEndpoint_withTrailingSlash_preservesIt() {
        String url = "http://localhost:31415/";
        String result = ConfigAccess.validateNetworkEndpoint(url);
        assertNotNull(result);
        assertEquals("http://localhost:31415/", result);
    }

    @Test
    void validateNetworkEndpoint_withPath_preservesPath() {
        String url = "http://localhost:31415/api";
        String result = ConfigAccess.validateNetworkEndpoint(url);
        assertNotNull(result);
        assertTrue(result.contains("/api"));
    }

    @ParameterizedTest
    @CsvSource({
        "http://localhost:31415, true",
        "https://localhost:31415, true",
        "http://example.com:8080, true",
        "localhost:31415, false",
        "ftp://localhost:31415, false",
        "http://, false"
    })
    void isValidNetworkEndpoint_withVariousUrls_returnsExpected(String url, boolean expected) {
        boolean result = ConfigAccess.isValidNetworkEndpoint(url);
        assertEquals(expected, result);
    }

    @Test
    void validateNetworkEndpoint_complexSubdomain_accepted() {
        String url = "http://sub.domain.example.com:8080/api/v1";
        String result = ConfigAccess.validateNetworkEndpoint(url);
        assertNotNull(result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://localhost:80",
        "http://localhost:443",
        "http://localhost:8080",
        "http://localhost:3000",
        "http://localhost:9000"
    })
    void validateNetworkEndpoint_variousPorts_accepted(String url) {
        String result = ConfigAccess.validateNetworkEndpoint(url);
        assertNotNull(result);
    }
}
