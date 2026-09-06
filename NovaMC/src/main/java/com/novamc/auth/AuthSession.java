package com.novamc.auth;

import com.novamc.util.JsonUtils;

public class AuthSession {
    public String username;
    public String uuid;
    public String accessToken;
    public String refreshToken;
    public boolean online;
    public long expiresAt;

    public AuthSession() {}

    public AuthSession(String username, String uuid, String accessToken,
                       String refreshToken, boolean online, long expiresAt) {
        this.username = username;
        this.uuid = uuid;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.online = online;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public String toJson() {
        return JsonUtils.toJson(this);
    }

    public static AuthSession fromJson(String json) {
        return JsonUtils.fromJson(json, AuthSession.class);
    }
}
