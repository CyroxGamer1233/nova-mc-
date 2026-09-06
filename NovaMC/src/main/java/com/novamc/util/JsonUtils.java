package com.novamc.util;

import com.google.gson.*;
import java.io.Reader;

public class JsonUtils {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static <T> T fromJson(Reader reader, Class<T> clazz) {
        return GSON.fromJson(reader, clazz);
    }

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    public static JsonObject parseObject(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    public static JsonArray parseArray(String json) {
        return JsonParser.parseString(json).getAsJsonArray();
    }
}
