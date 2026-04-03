/*
 * Copyright 2026 Damon Lu and open-source contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.damon233.performtrackermod.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CharsetDetectorTest {

    @Test
    void decode_withNullBytes_returnsEmptyString() {
        String result = CharsetDetector.decode(null);
        assertEquals("", result);
    }

    @Test
    void decode_withEmptyBytes_returnsEmptyString() {
        String result = CharsetDetector.decode(new byte[0]);
        assertEquals("", result);
    }

    @Test
    void decode_withValidUtf8Bytes_returnsDecodedString() {
        byte[] bytes = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        String result = CharsetDetector.decode(bytes);
        assertEquals("Hello, World!", result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "en_US.UTF-8",
        "en_US.utf8",
        "zh_CN.UTF-8",
        "C.UTF-8"
    })
    void detect_withUtf8Locale_returnsUtf8(String locale) {
        Charset result = CharsetDetector.parseLocaleToCharset(locale);
        assertEquals(StandardCharsets.UTF_8, result);
    }

    @ParameterizedTest
    @CsvSource({
        "zh_CN.GBK, GBK",
        "zh_CN.GB2312, GBK",
        "zh_CN.GB18030, GBK"
    })
    void detect_withChineseLocale_returnsGbk(String locale, String expected) {
        Charset result = CharsetDetector.parseLocaleToCharset(locale);
        assertEquals(Charset.forName(expected), result);
    }

    @ParameterizedTest
    @CsvSource({
        "zh_TW.BIG5, BIG5",
        "zh_HK.BIG5HKSCS, BIG5"
    })
    void detect_withTraditionalChineseLocale_returnsBig5(String locale, String expected) {
        Charset result = CharsetDetector.parseLocaleToCharset(locale);
        assertEquals(Charset.forName(expected), result);
    }

    @ParameterizedTest
    @CsvSource({
        "ko_KR.EUC-KR, EUC-KR",
        "ko_KR.UTF-8, UTF-8"
    })
    void detect_withKoreanLocale_returnsKoreanCharset(String locale, String expected) {
        Charset result = CharsetDetector.parseLocaleToCharset(locale);
        assertEquals(Charset.forName(expected), result);
    }

    @ParameterizedTest
    @CsvSource({
        "ja_JP.Shift_JIS, Shift_JIS",
        "ja_JP.UTF-8, UTF-8"
    })
    void detect_withJapaneseLocale_returnsJapaneseCharset(String locale, String expected) {
        Charset result = CharsetDetector.parseLocaleToCharset(locale);
        assertEquals(Charset.forName(expected), result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void parseLocaleToCharset_withNullOrEmpty_returnsNull(String locale) {
        Charset result = CharsetDetector.parseLocaleToCharset(locale);
        assertNull(result);
    }

    @Test
    void parseLocaleToCharset_withUnknownLocale_returnsNull() {
        Charset result = CharsetDetector.parseLocaleToCharset("xx_XX.UNKNOWN");
        assertNull(result);
    }

    @ParameterizedTest
    @CsvSource({
        "65001, UTF-8",
        "936, GBK",
        "950, BIG5",
        "932, Shift_JIS",
        "949, EUC-KR",
        "1252, ISO-8859-1"
    })
    void codepageToCharset_withKnownCodepage_returnsCorrectCharset(int codepage, String expected) {
        Charset result = CharsetDetector.codepageToCharset(codepage);
        assertEquals(Charset.forName(expected), result);
    }

    @ParameterizedTest
    @CsvSource({
        "437, Cp437",
        "852, Cp852",
        "1250, Cp1250"
    })
    void codepageToCharset_withUnknownCodepage_returnsCodepageCharset(int codepage, String expected) {
        Charset result = CharsetDetector.codepageToCharset(codepage);
        assertEquals(Charset.forName(expected), result);
    }
}
