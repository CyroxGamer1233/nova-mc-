package com.novamc.auth;

import com.google.gson.JsonObject;
import com.novamc.util.HttpClient;
import com.novamc.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MicrosoftAuthManager {
    private static final Logger log = LoggerFactory.getLogger(MicrosoftAuthManager.class);
    private static final String MS_TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String XBL_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    private static final String MC_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    private final String clientId;

    public MicrosoftAuthManager(String clientId) {
        if (clientId == null || clientId.isBlank())
            throw new IllegalArgumentException("Microsoft Client ID is required for online authentication");
        this.clientId = clientId;
    }

    public String buildAuthUrl() {
        String scope = URLEncoder.encode("XboxLive.signin offline_access", StandardCharsets.UTF_8);
        String redirectUri = URLEncoder.encode("https://login.microsoftonline.com/common/oauth2/nativeclient", StandardCharsets.UTF_8);
        return "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize"
                + "?client_id=" + clientId
                + "&response_type=code"
                + "&redirect_uri=" + redirectUri
                + "&scope=" + scope
                + "&prompt=select_account";
    }

    public AuthSession authenticateWithCode(String authCode) throws IOException, InterruptedException {
        log.info("Starting Microsoft auth flow...");
        // Step 1: Exchange auth code for MS access token
        String tokenBody = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&code=" + URLEncoder.encode(authCode, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code"
                + "&redirect_uri=" + URLEncoder.encode("https://login.microsoftonline.com/common/oauth2/nativeclient", StandardCharsets.UTF_8)
                + "&scope=" + URLEncoder.encode("XboxLive.signin offline_access", StandardCharsets.UTF_8);

        String msTokenResponse = postForm(MS_TOKEN_URL, tokenBody);
        JsonObject msTokenJson = JsonUtils.parseObject(msTokenResponse);
        String msAccessToken = msTokenJson.get("access_token").getAsString();
        String msRefreshToken = msTokenJson.has("refresh_token") ? msTokenJson.get("refresh_token").getAsString() : null;
        long expiresIn = msTokenJson.has("expires_in") ? msTokenJson.get("expires_in").getAsLong() : 3600L;
        long expiresAt = System.currentTimeMillis() + (expiresIn * 1000L);

        // Step 2: Xbox Live
        String xblBody = JsonUtils.toJson(new XblRequest(msAccessToken));
        String xblResponse = HttpClient.postJson(XBL_URL, xblBody);
        JsonObject xblJson = JsonUtils.parseObject(xblResponse);
        String xblToken = xblJson.get("Token").getAsString();
        String userHash = xblJson.getAsJsonObject("DisplayClaims")
                .getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();

        // Step 3: XSTS
        String xstsBody = JsonUtils.toJson(new XstsRequest(xblToken));
        String xstsResponse = HttpClient.postJson(XSTS_URL, xstsBody);
        JsonObject xstsJson = JsonUtils.parseObject(xstsResponse);
        String xstsToken = xstsJson.get("Token").getAsString();

        // Step 4: Minecraft login
        String mcBody = "{\"identityToken\":\"XBL3.0 x=" + userHash + ";" + xstsToken + "\"}";
        String mcResponse = HttpClient.postJson(MC_LOGIN_URL, mcBody);
        JsonObject mcJson = JsonUtils.parseObject(mcResponse);
        String mcAccessToken = mcJson.get("access_token").getAsString();

        // Step 5: Get Minecraft profile
        String profileResponse = getWithBearer(MC_PROFILE_URL, mcAccessToken);
        JsonObject profileJson = JsonUtils.parseObject(profileResponse);
        String uuid = profileJson.get("id").getAsString();
        String username = profileJson.get("name").getAsString();

        log.info("Auth successful for user: {}", username);
        return new AuthSession(username, uuid, mcAccessToken, msRefreshToken, true, expiresAt);
    }

    public AuthSession refreshSession(AuthSession session) throws IOException, InterruptedException {
        if (session.refreshToken == null) throw new IllegalStateException("No refresh token available");
        String body = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&refresh_token=" + URLEncoder.encode(session.refreshToken, StandardCharsets.UTF_8)
                + "&grant_type=refresh_token"
                + "&scope=" + URLEncoder.encode("XboxLive.signin offline_access", StandardCharsets.UTF_8);
        String response = postForm(MS_TOKEN_URL, body);
        JsonObject json = JsonUtils.parseObject(response);
        String newMsToken = json.get("access_token").getAsString();
        String newRefreshToken = json.has("refresh_token") ? json.get("refresh_token").getAsString() : session.refreshToken;
        long expiresIn = json.has("expires_in") ? json.get("expires_in").getAsLong() : 3600L;
        long expiresAt = System.currentTimeMillis() + (expiresIn * 1000L);
        return authenticateWithCode(newMsToken); // Re-chain with new MS token
    }

    private String postForm(String url, String body) throws IOException, InterruptedException {
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();
        java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300)
            throw new IOException("HTTP " + resp.statusCode() + ": " + resp.body());
        return resp.body();
    }

    private String getWithBearer(String url, String token) throws IOException, InterruptedException {
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                .header("Authorization", "Bearer " + token)
                .GET().build();
        java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300)
            throw new IOException("HTTP " + resp.statusCode() + ": " + resp.body());
        return resp.body();
    }

    // Helper classes for JSON serialization
    private static class XblRequest {
        Properties Properties;
        String RelyingParty = "http://auth.xboxlive.com";
        String TokenType = "JWT";
        XblRequest(String msToken) {
            Properties = new Properties(msToken);
        }
        static class Properties {
            String AuthMethod = "RPS";
            String SiteName = "user.auth.xboxlive.com";
            String RpsTicket;
            Properties(String token) { this.RpsTicket = "d=" + token; }
        }
    }

    private static class XstsRequest {
        Properties Properties;
        String RelyingParty = "rp://api.minecraftservices.com/";
        String TokenType = "JWT";
        XstsRequest(String xblToken) {
            Properties = new Properties(xblToken);
        }
        static class Properties {
            String SandboxId = "RETAIL";
            String[] UserTokens;
            Properties(String token) { this.UserTokens = new String[]{token}; }
        }
    }
}
