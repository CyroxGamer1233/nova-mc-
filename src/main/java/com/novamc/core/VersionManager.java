package com.novamc.core;

import com.google.gson.*;
import com.novamc.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public class VersionManager {
    private static final Logger log = LoggerFactory.getLogger(VersionManager.class);
    public static final String VERSION_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    public record VersionEntry(String id, String type, String url, String sha1) {}

    public record AssetInfo(String id, String url, String sha1, long totalSize) {}

    public static class LibraryEntry {
        public String name;
        public String url;
        public String sha1;
        public long size;
        public String nativesKey; // e.g. "natives-windows"
        public LibraryEntry(String name, String url, String sha1, long size, String nativesKey) {
            this.name = name; this.url = url; this.sha1 = sha1; this.size = size; this.nativesKey = nativesKey;
        }
    }

    public static class VersionDetails {
        public String id;
        public String mainClass;
        public String type;
        public JsonElement arguments;
        public List<LibraryEntry> libraries = new ArrayList<>();
        public AssetInfo assetIndex;
        public String clientJarUrl;
        public String clientJarSha1;
        public long clientJarSize;
    }

    public List<VersionEntry> fetchVersionList() throws IOException, InterruptedException {
        log.info("Fetching version manifest...");
        String json = HttpClient.get(VERSION_MANIFEST_URL);
        JsonObject root = JsonUtils.parseObject(json);
        JsonArray versions = root.getAsJsonArray("versions");
        List<VersionEntry> list = new ArrayList<>();
        for (JsonElement el : versions) {
            JsonObject v = el.getAsJsonObject();
            list.add(new VersionEntry(
                    v.get("id").getAsString(),
                    v.get("type").getAsString(),
                    v.get("url").getAsString(),
                    v.has("sha1") ? v.get("sha1").getAsString() : ""
            ));
        }
        return list;
    }

    public VersionDetails fetchVersionDetails(VersionEntry entry) throws IOException, InterruptedException {
        log.info("Fetching version details for {}", entry.id());
        String json = HttpClient.get(entry.url());
        JsonObject root = JsonUtils.parseObject(json);
        VersionDetails details = new VersionDetails();
        details.id = root.get("id").getAsString();
        details.mainClass = root.get("mainClass").getAsString();
        details.type = root.get("type").getAsString();
        if (root.has("arguments")) details.arguments = root.get("arguments");
        else if (root.has("minecraftArguments"))
            details.arguments = root.get("minecraftArguments");

        // Client JAR
        JsonObject downloads = root.getAsJsonObject("downloads");
        if (downloads != null && downloads.has("client")) {
            JsonObject client = downloads.getAsJsonObject("client");
            details.clientJarUrl = client.get("url").getAsString();
            details.clientJarSha1 = client.get("sha1").getAsString();
            details.clientJarSize = client.get("size").getAsLong();
        }

        // Asset index
        if (root.has("assetIndex")) {
            JsonObject ai = root.getAsJsonObject("assetIndex");
            details.assetIndex = new AssetInfo(
                    ai.get("id").getAsString(),
                    ai.get("url").getAsString(),
                    ai.get("sha1").getAsString(),
                    ai.has("totalSize") ? ai.get("totalSize").getAsLong() : 0L
            );
        }

        // Libraries
        if (root.has("libraries")) {
            for (JsonElement libEl : root.getAsJsonArray("libraries")) {
                JsonObject lib = libEl.getAsJsonObject();
                if (!isLibraryApplicable(lib)) continue;
                String name = lib.get("name").getAsString();
                if (lib.has("downloads")) {
                    JsonObject libDownloads = lib.getAsJsonObject("downloads");
                    if (libDownloads.has("artifact")) {
                        JsonObject artifact = libDownloads.getAsJsonObject("artifact");
                        details.libraries.add(new LibraryEntry(
                                name,
                                artifact.get("url").getAsString(),
                                artifact.has("sha1") ? artifact.get("sha1").getAsString() : "",
                                artifact.has("size") ? artifact.get("size").getAsLong() : 0L,
                                null
                        ));
                    }
                    // Natives
                    if (lib.has("natives") && libDownloads.has("classifiers")) {
                        JsonObject natives = lib.getAsJsonObject("natives");
                        String os = getOsName();
                        if (natives.has(os)) {
                            String classifier = natives.get(os).getAsString();
                            JsonObject classifiers = libDownloads.getAsJsonObject("classifiers");
                            if (classifiers.has(classifier)) {
                                JsonObject nat = classifiers.getAsJsonObject(classifier);
                                details.libraries.add(new LibraryEntry(
                                        name + ":" + classifier,
                                        nat.get("url").getAsString(),
                                        nat.has("sha1") ? nat.get("sha1").getAsString() : "",
                                        nat.has("size") ? nat.get("size").getAsLong() : 0L,
                                        classifier
                                ));
                            }
                        }
                    }
                }
            }
        }
        return details;
    }

    private boolean isLibraryApplicable(JsonObject lib) {
        if (!lib.has("rules")) return true;
        JsonArray rules = lib.getAsJsonArray("rules");
        boolean allow = false;
        for (JsonElement ruleEl : rules) {
            JsonObject rule = ruleEl.getAsJsonObject();
            String action = rule.get("action").getAsString();
            if (rule.has("os")) {
                JsonObject os = rule.getAsJsonObject("os");
                if (os.has("name") && os.get("name").getAsString().equals(getOsName())) {
                    allow = action.equals("allow");
                }
            } else {
                allow = action.equals("allow");
            }
        }
        return allow;
    }

    private String getOsName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "osx";
        return "linux";
    }

    public Path getVersionJar(String gameDir, String versionId) {
        return Path.of(gameDir, "versions", versionId, versionId + ".jar");
    }

    public boolean isVersionInstalled(String gameDir, String versionId) {
        return FileUtils.exists(getVersionJar(gameDir, versionId));
    }

    public void downloadVersion(VersionEntry entry, VersionDetails details, Path gameDir, Consumer<Double> progress) throws IOException, InterruptedException {
        Path jarPath = gameDir.resolve("versions").resolve(entry.id()).resolve(entry.id() + ".jar");
        if (HashUtils.verifySha1(jarPath, details.clientJarSha1)) {
            log.info("Version JAR already downloaded and verified: {}", entry.id());
            if (progress != null) progress.accept(1.0);
            return;
        }
        log.info("Downloading version JAR: {}", entry.id());
        long[] downloaded = {0};
        long total = details.clientJarSize > 0 ? details.clientJarSize : 1;
        HttpClient.downloadFile(details.clientJarUrl, jarPath, bytes -> {
            downloaded[0] = bytes;
            if (progress != null) progress.accept((double) bytes / total);
        });
        // Also save version JSON
        String versionJson = HttpClient.get(entry.url());
        Path jsonPath = gameDir.resolve("versions").resolve(entry.id()).resolve(entry.id() + ".json");
        FileUtils.writeString(jsonPath, versionJson);
        log.info("Version {} downloaded", entry.id());
    }
}
