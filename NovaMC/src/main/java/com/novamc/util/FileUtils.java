package com.novamc.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {
    public static void ensureDir(Path dir) throws IOException {
        if (dir != null && !Files.exists(dir)) Files.createDirectories(dir);
    }

    public static void writeString(Path file, String content) throws IOException {
        ensureDir(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    public static String readString(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    public static void writeJson(Path file, Object obj) throws IOException {
        writeString(file, JsonUtils.toJson(obj));
    }

    public static <T> T readJson(Path file, Class<T> clazz) throws IOException {
        return JsonUtils.fromJson(readString(file), clazz);
    }

    public static void extractZip(Path zipFile, Path destDir) throws IOException {
        ensureDir(destDir);
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path target = destDir.resolve(entry.getName()).normalize();
                if (!target.startsWith(destDir)) continue; // security check
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    ensureDir(target.getParent());
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    public static void copyStream(InputStream in, Path dest) throws IOException {
        ensureDir(dest.getParent());
        Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    public static boolean exists(Path p) {
        return Files.exists(p);
    }
}
