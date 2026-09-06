package com.novamc.settings;

import com.novamc.mods.ClientModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class GameSettings {
    private static final Logger log = LoggerFactory.getLogger(GameSettings.class);
    public int renderDistance = 12;
    public int maxFps = 60;
    public int fov = 70;
    public boolean fullscreen = false;
    public String graphicsMode = "fast";
    public double gamma = 0.0;
    public double musicVolume = 1.0;
    public double soundVolume = 1.0;
    public String lang = "en_us";
    public boolean autoJump = false;
    public boolean vsync = true;

    public static GameSettings load(Path optionsFile) {
        GameSettings gs = new GameSettings();
        if (!Files.exists(optionsFile)) return gs;
        try {
            List<String> lines = Files.readAllLines(optionsFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                int idx = line.indexOf(':');
                if (idx < 0) continue;
                String key = line.substring(0, idx).trim();
                String val = line.substring(idx + 1).trim();
                switch (key) {
                    case "renderDistance" -> gs.renderDistance = Integer.parseInt(val);
                    case "maxFps" -> gs.maxFps = Integer.parseInt(val);
                    case "fov" -> gs.fov = (int) Double.parseDouble(val);
                    case "fullscreen" -> gs.fullscreen = Boolean.parseBoolean(val);
                    case "graphicsMode" -> gs.graphicsMode = val;
                    case "gamma" -> gs.gamma = Double.parseDouble(val);
                    case "soundCategory_music" -> gs.musicVolume = Double.parseDouble(val);
                    case "soundCategory_master" -> gs.soundVolume = Double.parseDouble(val);
                    case "lang" -> gs.lang = val;
                    case "autoJump" -> gs.autoJump = Boolean.parseBoolean(val);
                    case "enableVsync" -> gs.vsync = Boolean.parseBoolean(val);
                }
            }
        } catch (IOException | NumberFormatException e) {
            log.warn("Error loading options.txt: {}", e.getMessage());
        }
        return gs;
    }

    public void save(Path optionsFile) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("renderDistance:" + renderDistance);
        lines.add("maxFps:" + maxFps);
        lines.add("fov:" + fov);
        lines.add("fullscreen:" + fullscreen);
        lines.add("graphicsMode:" + graphicsMode);
        lines.add("gamma:" + gamma);
        lines.add("soundCategory_music:" + musicVolume);
        lines.add("soundCategory_master:" + soundVolume);
        lines.add("lang:" + lang);
        lines.add("autoJump:" + autoJump);
        lines.add("enableVsync:" + vsync);
        Files.createDirectories(optionsFile.getParent());
        Files.write(optionsFile, lines, StandardCharsets.UTF_8);
    }

    public void applyTweaks(ClientModConfig mods) {
        if (mods.fullbright) gamma = 1000.0;
        if (mods.fpsBoost) { maxFps = 260; vsync = false; }
    }
}
