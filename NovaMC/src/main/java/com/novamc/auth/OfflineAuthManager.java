package com.novamc.auth;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class OfflineAuthManager {
    public static AuthSession createSession(String username) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username cannot be empty");
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
        return new AuthSession(
                username,
                uuid.toString().replace("-", ""),
                "0",
                null,
                false,
                Long.MAX_VALUE
        );
    }
}
