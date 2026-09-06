package com.novamc.settings;

import com.novamc.util.FileUtils;
import com.novamc.util.JsonUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LauncherSettings {
    public String gameDirectory = System.getProperty("user.home").replace("\\", "/") + "/.novamc";
    public String javaPath = "auto";
    public int minRamMb = 512;
    public int maxRamMb = 2048;
    public String selectedVersion = "";
    public String selectedProfile = "Default";
    public boolean closeLauncherOnStart = true;
    public boolean showConsole = false;
    public String theme = "dark";
    public String msClientId = "";
    public List<ProfileEntry> profiles = new ArrayList<>();

    public LauncherSettings() {
        profiles.add(new ProfileEntry());
    }

    public static class ProfileEntry {
        public String name = "Default";
        public String version = "";
        public String gameDir = "";
        public String jvmArgs = "";
        public int minRam = 512;
        public int maxRam = 2048;
    }

    public static LauncherSettings load(Path file) {
        if (!Files.exists(file)) return new LauncherSettings();
        try {
            return FileUtils.readJson(file, LauncherSettings.class);
        } catch (IOException e) {
            return new LauncherSettings();
        }
    }

    public void save(Path file) throws IOException {
        FileUtils.writeJson(file, this);
    }
}
