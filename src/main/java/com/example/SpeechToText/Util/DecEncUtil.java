package com.example.SpeechToText.Util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class DecEncUtil {
    public static String encode(final String clearText) throws NoSuchAlgorithmException {
        return new String(
                Base64.getEncoder().encode(MessageDigest.getInstance("SHA-256").digest(clearText.getBytes(StandardCharsets.UTF_8))));
    }
}
