package com.novamc.core;

import com.google.gson.JsonObject;
import com.novamc.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class AssetManager {
    private static final Logger log = LoggerFactory.getLogger(AssetManager.class);
    private static final String RESOURCES_URL = "https://resources.download.minecraft.net/";

    public void downloadAssets(VersionManager.AssetInfo assetIndex, Path gameDir, Consumer<Double> progress) throws IOException, InterruptedException {
        Path indexDir = gameDir.resolve("assets").resolve("indexes");
        Path indexFile = indexDir.resolve(assetIndex.id() + ".json");
        FileUtils.ensureDir(indexDir);

        // Download asset index
        if (!HashUtils.verifySha1(indexFile, assetIndex.sha1())) {
            log.info("Downloading asset index: {}", assetIndex.id());
            HttpClient.downloadFile(assetIndex.url(), indexFile, null);
        }

        String indexJson = FileUtils.readString(indexFile);
        JsonObject root = JsonUtils.parseObject(indexJson);
        JsonObject objects = root.getAsJsonObject("objects");
        Set<Map.Entry<String, com.google.gson.JsonElement>> entries = objects.entrySet();
        int total = entries.size();
        int[] done = {0};

        for (Map.Entry<String, com.google.gson.JsonElement> entry : entries) {
            String hash = entry.getValue().getAsJsonObject().get("hash").getAsString();
            String prefix = hash.substring(0, 2);
            Path objectPath = gameDir.resolve("assets").resolve("objects").resolve(prefix).resolve(hash);

            if (!Files.exists(objectPath)) {
                FileUtils.ensureDir(objectPath.getParent());
                String url = RESOURCES_URL + prefix + "/" + hash;
                try {
                    HttpClient.downloadFile(url, objectPath, null);
                } catch (Exception e) {
                    log.warn("Failed to download asset {}: {}", hash, e.getMessage());
                }
            }
            done[0]++;
            if (progress != null) progress.accept((double) done[0] / total);
        }
        log.info("Assets downloaded: {}/{}", done[0], total);
    }
}
