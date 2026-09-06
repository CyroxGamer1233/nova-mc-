package com.novamc.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {
    public static String sha1(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            try (InputStream is = Files.newInputStream(file)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) md.update(buf, 0, n);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 not available", e);
        }
    }

    public static boolean verifySha1(Path file, String expectedSha1) {
        if (!Files.exists(file)) return false;
        try {
            return sha1(file).equalsIgnoreCase(expectedSha1);
        } catch (IOException e) {
            return false;
        }
    }
}
