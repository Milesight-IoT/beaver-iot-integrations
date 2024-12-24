package com.milesight.beaveriot.parser.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
public class ClassResourceTestUtil {

    private ClassResourceTestUtil() {
    }

    @SneakyThrows
    public static String getResourceFileContentAsString(String path) {
        try (val inputStream = getResourceFileContentAsInputStream(path)) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to load resource file: {}", path, e);
            return null;
        }
    }

    @SneakyThrows
    public static InputStream getResourceFileContentAsInputStream(String path) {
        return ClassResourceTestUtil.class.getClassLoader().getResourceAsStream(path);
    }

    @SneakyThrows
    public static byte[] getResourceFileContentAsByteArray(String path) {
        try (val inputStream = getResourceFileContentAsInputStream(path)) {
            return StreamUtils.copyToByteArray(inputStream);
        } catch (Exception e) {
            log.error("Failed to load resource file: {}", path, e);
            return new byte[0];
        }
    }

}
