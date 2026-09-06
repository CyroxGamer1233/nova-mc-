package com.novamc.core;

import com.google.gson.*;
import com.novamc.auth.AuthSession;
import com.novamc.mods.ClientModManager;
import com.novamc.settings.*;
import com.novamc.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

public class GameLauncher {
    private static final Logger log = LoggerFactory.getLogger(GameLauncher.class);

    public Process launch(
            AuthSession session,
            VersionManager.VersionDetails version,
            LauncherSettings settings,
            ClientModManager modManager,
            Path gameDir,
            Consumer<String> logCallback
    ) throws IOException {
        String javaPath = settings.javaPath.equals("auto") ? JavaManager.detectJava() : settings.javaPath;
        Path nativesDir = gameDir.resolve("versions").resolve(version.id).resolve("natives");
        LibraryManager libManager = new LibraryManager();
        String classpath = libManager.buildClasspath(version.libraries, gameDir, version.id);

        List<String> cmd = new ArrayList<>();
        cmd.add(javaPath);
        cmd.add("-Xms" + settings.minRamMb + "m");
        cmd.add("-Xmx" + settings.maxRamMb + "m");
        cmd.add("-Djava.library.path=" + nativesDir.toAbsolutePath());
        cmd.add("-Dminecraft.launcher.brand=NovaMC");
        cmd.add("-Dminecraft.launcher.version=1.0.0");
        cmd.addAll(modManager.getExtraJvmArgs());
        cmd.add("-cp");
        cmd.add(classpath);
        cmd.add(version.mainClass);

        // Game arguments
        cmd.addAll(buildGameArgs(session, version, settings, gameDir));

        log.info("Launching: {}", String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(gameDir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Stream output
        Thread t = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String l = line;
                    if (logCallback != null) logCallback.accept(l);
                    log.info("[Game] {}", l);
                }
            } catch (IOException ignored) {}
        }, "game-output-reader");
        t.setDaemon(true);
        t.start();

        return process;
    }

    private List<String> buildGameArgs(AuthSession session, VersionManager.VersionDetails version,
                                       LauncherSettings settings, Path gameDir) {
        List<String> args = new ArrayList<>();
        String assetsDir = gameDir.resolve("assets").toAbsolutePath().toString();
        String nativesDir = gameDir.resolve("versions").resolve(version.id).resolve("natives").toAbsolutePath().toString();
        Map<String, String> vars = new HashMap<>();
        vars.put("${auth_player_name}", session.username);
        vars.put("${auth_uuid}", session.uuid);
        vars.put("${auth_access_token}", session.accessToken);
        vars.put("${user_type}", session.online ? "msa" : "legacy");
        vars.put("${version_name}", version.id);
        vars.put("${game_directory}", gameDir.toAbsolutePath().toString());
        vars.put("${assets_root}", assetsDir);
        vars.put("${assets_index_name}", version.assetIndex != null ? version.assetIndex.id() : version.id);
        vars.put("${version_type}", version.type != null ? version.type : "release");
        vars.put("${natives_directory}", nativesDir);
        vars.put("${launcher_name}", "NovaMC");
        vars.put("${launcher_version}", "1.0.0");
        vars.put("${classpath}", ""); // not needed here
        vars.put("${resolution_width}", "854");
        vars.put("${resolution_height}", "480");

        if (version.arguments instanceof JsonObject argsObj) {
            if (argsObj.has("game")) {
                for (JsonElement el : argsObj.getAsJsonArray("game")) {
                    if (el.isJsonPrimitive()) {
                        String val = el.getAsString();
                        args.add(substitute(val, vars));
                    }
                    // Skip complex rule-based args for simplicity
                }
            }
        } else if (version.arguments instanceof JsonPrimitive) {
            // Old format: single string
            String[] parts = version.arguments.getAsString().split(" ");
            for (String p : parts) args.add(substitute(p, vars));
        } else {
            // Fallback minimal args
            args.add("--username"); args.add(session.username);
            args.add("--version"); args.add(version.id);
            args.add("--gameDir"); args.add(gameDir.toAbsolutePath().toString());
            args.add("--assetsDir"); args.add(assetsDir);
            args.add("--assetIndex"); args.add(version.assetIndex != null ? version.assetIndex.id() : version.id);
            args.add("--uuid"); args.add(session.uuid);
            args.add("--accessToken"); args.add(session.accessToken);
            args.add("--userType"); args.add(session.online ? "msa" : "legacy");
        }
        return args;
    }

    private String substitute(String template, Map<String, String> vars) {
        for (Map.Entry<String, String> e : vars.entrySet()) {
            template = template.replace(e.getKey(), e.getValue());
        }
        return template;
    }

    public static void applyGameSettings(GameSettings gameSettings, com.novamc.mods.ClientModConfig mods, Path gameDir) throws IOException {
        gameSettings.applyTweaks(mods);
        gameSettings.save(gameDir.resolve("options.txt"));
    }
}
