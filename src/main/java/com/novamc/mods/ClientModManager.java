package com.novamc.mods;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ClientModManager {
    private static final Logger log = LoggerFactory.getLogger(ClientModManager.class);
    private ClientModConfig config;
    private final Path configFile;

    public ClientModManager(Path configFile) {
        this.configFile = configFile;
        this.config = ClientModConfig.load(configFile);
    }

    public ClientModConfig getConfig() { return config; }

    public void save() {
        try { config.save(configFile); }
        catch (IOException e) { log.error("Failed to save mod config", e); }
    }

    public List<String> getExtraJvmArgs() {
        List<String> args = new ArrayList<>();
        if (config.zoomKey) {
            args.add("-Dnovamc.zoom=true");
            args.add("-Dnovamc.zoom.fov=" + config.zoomFov);
        }
        if (config.showFps) args.add("-Dnovamc.showfps=true");
        if (config.autoSprint) args.add("-Dnovamc.autosprint=true");
        if (config.freelook) args.add("-Dnovamc.freelook=true");
        if (config.antiAfk) args.add("-Dnovamc.antiafk=true");
        if (config.customCrosshair) args.add("-Dnovamc.crosshair=" + config.crosshairStyle);
        return args;
    }
}
