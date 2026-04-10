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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CharsetDetector {
    
    public static String decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        
        Charset systemCharset = detect();
        if (systemCharset != null) {
            try {
                String result = new String(bytes, systemCharset);
                if (!result.contains("\ufffd")) {
                    return result;
                }
            } catch (Exception ignored) {
            }
        }
        
        String[] candidates = {"UTF-8", "GBK", "BIG5", "GB2312", "EUC-KR", "Shift_JIS", "ISO-8859-1"};
        for (String name : candidates) {
            try {
                String result = new String(bytes, name);
                if (!result.contains("\ufffd")) {
                    return result;
                }
            } catch (Exception ignored) {
            }
        }
        
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    public static Charset detect() {
        String osName = System.getProperty("os.name").toLowerCase();
        
        if (osName.contains("windows")) {
            return detectWindows();
        }
        if (osName.contains("linux")) {
            return detectLinux();
        }
        if (osName.contains("mac") || osName.contains("darwin")) {
            return StandardCharsets.UTF_8;
        }
        
        return null;
    }
    
    private static Charset detectWindows() {
        String codepage = runCommand("chcp.com");
        if (codepage == null) {
            return null;
        }
        
        try {
            String num = codepage.replaceAll("[^0-9]", "");
            int code = Integer.parseInt(num);
            return codepageToCharset(code);
        } catch (Exception ignored) {
        }
        return null;
    }
    
    private static Charset detectLinux() {
        String locale = System.getenv("LANG");
        Charset charset = parseLocaleToCharset(locale);
        if (charset != null) {
            return charset;
        }
        
        locale = runCommand("locale charmap");
        return parseLocaleToCharset(locale);
    }
    
    static Charset parseLocaleToCharset(String locale) {
        if (locale == null) {
            return null;
        }
        
        locale = locale.trim().toUpperCase();
        if (locale.contains("UTF-8") || locale.contains("UTF8")) {
            return StandardCharsets.UTF_8;
        }
        if (locale.contains("GBK") || locale.contains("GB2312") || locale.contains("GB18030")) {
            return Charset.forName("GBK");
        }
        if (locale.contains("BIG5")) {
            return Charset.forName("BIG5");
        }
        if (locale.contains("EUC-KR") || locale.contains("KOREAN")) {
            return Charset.forName("EUC-KR");
        }
        if (locale.contains("SHIFT-JIS") || locale.contains("SHIFT_JIS")) {
            return Charset.forName("Shift_JIS");
        }
        
        return null;
    }
    
    static Charset codepageToCharset(int codepage) {
        return switch (codepage) {
            case 65001 -> StandardCharsets.UTF_8;
            case 936 -> Charset.forName("GBK");
            case 950 -> Charset.forName("BIG5");
            case 932 -> Charset.forName("Shift_JIS");
            case 949 -> Charset.forName("EUC-KR");
            case 1252 -> StandardCharsets.ISO_8859_1;
            default -> Charset.forName("Cp" + codepage);
        };
    }
    
    private static String runCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command.split("\\s+"));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream is = process.getInputStream()) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
            }
            process.waitFor();
            
            return baos.toString().trim();
        } catch (Exception ignored) {
        }
        return null;
    }
}
