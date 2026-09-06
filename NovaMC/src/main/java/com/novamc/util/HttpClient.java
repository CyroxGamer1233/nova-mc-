package com.novamc.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Consumer;

public class HttpClient {
    private static final Logger log = LoggerFactory.getLogger(HttpClient.class);
    private static final java.net.http.HttpClient CLIENT = java.net.http.HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS)
            .build();

    public static String get(String url) throws IOException, InterruptedException {
        log.debug("GET {}", url);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .GET().timeout(Duration.ofSeconds(60)).build();
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300)
            throw new IOException("HTTP " + resp.statusCode() + " for " + url);
        return resp.body();
    }

    public static byte[] getBytes(String url) throws IOException, InterruptedException {
        log.debug("GET bytes {}", url);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .GET().timeout(Duration.ofSeconds(60)).build();
        HttpResponse<byte[]> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300)
            throw new IOException("HTTP " + resp.statusCode() + " for " + url);
        return resp.body();
    }

    public static String postJson(String url, String jsonBody) throws IOException, InterruptedException {
        log.debug("POST {}", url);
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(30)).build();
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300)
            throw new IOException("HTTP " + resp.statusCode() + " for " + url + ": " + resp.body());
        return resp.body();
    }

    public static void downloadFile(String url, Path dest, Consumer<Long> progressCallback) throws IOException, InterruptedException {
        log.debug("Download {} -> {}", url, dest);
        FileUtils.ensureDir(dest.getParent());
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .GET().timeout(Duration.ofSeconds(120)).build();
        HttpResponse<InputStream> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300)
            throw new IOException("HTTP " + resp.statusCode() + " for " + url);
        try (InputStream is = resp.body()) {
            byte[] buf = new byte[8192];
            long downloaded = 0;
            java.io.OutputStream out = Files.newOutputStream(dest);
            try {
                int n;
                while ((n = is.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    downloaded += n;
                    if (progressCallback != null) progressCallback.accept(downloaded);
                }
            } finally {
                out.close();
            }
        }
    }
}
