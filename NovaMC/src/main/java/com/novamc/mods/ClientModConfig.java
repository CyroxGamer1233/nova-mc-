package com.novamc.mods;

import com.novamc.util.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientModConfig {
    public boolean fullbright = false;
    public boolean fpsBoost = false;
    public boolean autoSprint = false;
    public boolean showFps = false;
    public boolean antiAfk = false;
    public boolean customCrosshair = false;
    public int crosshairStyle = 0;
    public boolean zoomKey = true;
    public double zoomFov = 15.0;
    public boolean freelook = false;

    public static ClientModConfig load(Path file) {
        if (!Files.exists(file)) return new ClientModConfig();
        try { return FileUtils.readJson(file, ClientModConfig.class); }
        catch (IOException e) { return new ClientModConfig(); }
    }

    public void save(Path file) throws IOException {
        FileUtils.writeJson(file, this);
    }
}
