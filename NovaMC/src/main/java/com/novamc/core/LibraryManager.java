package com.novamc.core;

import com.novamc.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.function.Consumer;

public class LibraryManager {
    private static final Logger log = LoggerFactory.getLogger(LibraryManager.class);

    public void downloadLibraries(List<VersionManager.LibraryEntry> libs, Path gameDir, Consumer<Double> progress) throws IOException, InterruptedException {
        int total = libs.size();
        for (int i = 0; i < total; i++) {
            VersionManager.LibraryEntry lib = libs.get(i);
            if (lib.url == null || lib.url.isBlank()) continue;
            Path libPath = resolveLibPath(lib, gameDir);
            if (!HashUtils.verifySha1(libPath, lib.sha1)) {
                log.debug("Downloading library: {}", lib.name);
                HttpClient.downloadFile(lib.url, libPath, null);
            }
            if (progress != null) progress.accept((double)(i + 1) / total);
        }
    }

    public String buildClasspath(List<VersionManager.LibraryEntry> libs, Path gameDir, String versionId) {
        String sep = System.getProperty("os.name").toLowerCase().contains("win") ? ";" : ":";
        StringBuilder cp = new StringBuilder();
        for (VersionManager.LibraryEntry lib : libs) {
            if (lib.nativesKey != null) continue; // natives are extracted, not on classpath
            Path libPath = resolveLibPath(lib, gameDir);
            if (Files.exists(libPath)) {
                if (cp.length() > 0) cp.append(sep);
                cp.append(libPath.toAbsolutePath());
            }
        }
        // Add the version JAR itself
        Path versionJar = gameDir.resolve("versions").resolve(versionId).resolve(versionId + ".jar");
        if (Files.exists(versionJar)) {
            if (cp.length() > 0) cp.append(sep);
            cp.append(versionJar.toAbsolutePath());
        }
        return cp.toString();
    }

    public void extractNatives(List<VersionManager.LibraryEntry> libs, Path gameDir, String versionId) throws IOException {
        Path nativesDir = gameDir.resolve("versions").resolve(versionId).resolve("natives");
        FileUtils.ensureDir(nativesDir);
        for (VersionManager.LibraryEntry lib : libs) {
            if (lib.nativesKey == null) continue;
            Path libPath = resolveLibPath(lib, gameDir);
            if (Files.exists(libPath)) {
                log.debug("Extracting native: {}", lib.name);
                FileUtils.extractZip(libPath, nativesDir);
            }
        }
    }

    private Path resolveLibPath(VersionManager.LibraryEntry lib, Path gameDir) {
        // Convert Maven coordinate to path: com.example:thing:1.0 -> com/example/thing/1.0/thing-1.0.jar
        String[] parts = lib.name.split(":");
        if (parts.length < 3) return gameDir.resolve("libraries").resolve(lib.name.replace(':', '/'));
        String group = parts[0].replace('.', '/');
        String artifact = parts[1];
        String version = parts[2];
        String classifier = parts.length > 3 ? "-" + parts[3] : "";
        String filename = artifact + "-" + version + classifier + ".jar";
        return gameDir.resolve("libraries").resolve(group).resolve(artifact).resolve(version).resolve(filename);
    }
}
