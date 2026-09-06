package com.novamc.ui;

import com.novamc.auth.AuthSession;
import com.novamc.core.*;
import com.novamc.mods.ClientModManager;
import com.novamc.settings.*;
import com.novamc.util.FileUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class MainWindow {
    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);

    private Stage stage;
    private AuthSession currentSession;
    private LauncherSettings launcherSettings;
    private GameSettings gameSettings;
    private ClientModManager modManager;
    private final VersionManager versionManager = new VersionManager();
    private final AssetManager assetManager = new AssetManager();
    private final LibraryManager libManager = new LibraryManager();
    private final GameLauncher gameLauncher = new GameLauncher();

    private HomeTab homeTab;
    private VersionsTab versionsTab;
    private ModsTab modsTab;
    private SettingsTab settingsTab;
    private ConsoleTab consoleTab;

    private StackPane contentArea;
    private Process gameProcess;
    private List<VersionManager.VersionEntry> allVersions = new ArrayList<>();

    public void initialize(Stage stage, AuthSession session) {
        this.stage = stage;
        this.currentSession = session;

        Path homeDir = Path.of(System.getProperty("user.home"), ".novamc");
        try { FileUtils.ensureDir(homeDir); } catch (IOException e) { log.error("Cannot create game dir", e); }

        launcherSettings = LauncherSettings.load(homeDir.resolve("launcher.json"));
        Path gameDir = Path.of(launcherSettings.gameDirectory);
        try { FileUtils.ensureDir(gameDir); } catch (IOException e) { log.error("Cannot create game dir", e); }

        gameSettings = GameSettings.load(gameDir.resolve("options.txt"));
        modManager = new ClientModManager(homeDir.resolve("clientmods.json"));

        buildUI(stage, homeDir, gameDir);
        loadVersionsAsync();
    }

    private void buildUI(Stage stage, Path homeDir, Path gameDir) {
        // Tabs
        homeTab = new HomeTab();
        versionsTab = new VersionsTab();
        modsTab = new ModsTab();
        settingsTab = new SettingsTab(stage);
        consoleTab = new ConsoleTab();

        homeTab.setSession(currentSession);
        homeTab.setGameDir(launcherSettings.gameDirectory);
        modsTab.loadConfig(modManager.getConfig());
        settingsTab.loadSettings(launcherSettings, gameSettings);

        // Sidebar
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color: #0f0f1f;");

        Label logoLabel = new Label("⚡ NovaMC");
        logoLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #00d4aa; -fx-padding: 20 15 15 15;");
        sidebar.getChildren().add(logoLabel);

        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #1a1a2e;");

        String[][] navItems = {
                {"🏠", "Home"},
                {"📦", "Versions"},
                {"🔧", "Mods"},
                {"⚙", "Settings"},
                {"📺", "Console"}
        };
        javafx.scene.Node[] tabContents = {homeTab, versionsTab, modsTab, settingsTab, consoleTab};
        Button[] navBtns = new Button[navItems.length];

        for (int i = 0; i < navItems.length; i++) {
            final int idx = i;
            Button btn = new Button(navItems[i][0] + "  " + navItems[i][1]);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setPrefHeight(50);
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.setPadding(new Insets(0, 0, 0, 20));
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0a0c0; -fx-font-size: 13px; -fx-cursor: hand;");
            btn.setOnMouseEntered(e -> {
                if (!btn.getStyle().contains("#16213e"))
                    btn.setStyle("-fx-background-color: #252540; -fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-cursor: hand;");
            });
            btn.setOnMouseExited(e -> {
                if (!btn.getStyle().contains("#16213e"))
                    btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0a0c0; -fx-font-size: 13px; -fx-cursor: hand;");
            });
            btn.setOnAction(e -> {
                for (Button b : navBtns)
                    b.setStyle("-fx-background-color: transparent; -fx-text-fill: #a0a0c0; -fx-font-size: 13px; -fx-cursor: hand;");
                btn.setStyle("-fx-background-color: #16213e; -fx-text-fill: #00d4aa; -fx-font-size: 13px; -fx-cursor: hand; -fx-border-color: #00d4aa; -fx-border-width: 0 0 0 3;");
                contentArea.getChildren().setAll(tabContents[idx]);
            });
            navBtns[i] = btn;
            sidebar.getChildren().add(btn);
        }

        // Wire play button
        homeTab.setOnPlay(() -> handlePlay(gameDir));

        // Wire versions tab
        versionsTab.setOnInstall(entry -> handleInstall(entry, gameDir));
        versionsTab.setOnDelete(entry -> {
            // Delete version jar
            Path jar = gameDir.resolve("versions").resolve(entry.id()).resolve(entry.id() + ".jar");
            try { Files.deleteIfExists(jar); } catch (IOException e) { log.warn("Delete failed", e); }
            versionsTab.setVersions(allVersions, getInstalledVersions(gameDir));
        });

        // Wire mods tab
        modsTab.setOnChange(() -> {
            modsTab.saveToConfig(modManager.getConfig());
            modManager.save();
        });

        // Wire settings tab
        settingsTab.setOnSave((ls, gs) -> {
            // Merge profiles and other non-settings-tab fields
            ls.profiles = launcherSettings.profiles;
            ls.selectedVersion = launcherSettings.selectedVersion;
            ls.selectedProfile = launcherSettings.selectedProfile;
            launcherSettings = ls;
            gameSettings = gs;
            try {
                launcherSettings.save(homeDir.resolve("launcher.json"));
                gs.save(gameDir.resolve("options.txt"));
            } catch (IOException e) { log.error("Save failed", e); }
            Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "Settings saved!", "Your settings have been saved."));
        });

        // Select Home tab by default
        contentArea.getChildren().setAll(homeTab);
        navBtns[0].setStyle("-fx-background-color: #16213e; -fx-text-fill: #00d4aa; -fx-font-size: 13px; -fx-cursor: hand; -fx-border-color: #00d4aa; -fx-border-width: 0 0 0 3;");

        BorderPane root = new BorderPane();
        root.setLeft(sidebar);
        root.setCenter(contentArea);
        root.setStyle("-fx-background-color: #1a1a2e;");

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/css/style.css") != null
                ? getClass().getResource("/css/style.css").toExternalForm() : "");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    private void handlePlay(Path gameDir) {
        String selectedVersion = homeTab.getSelectedVersion();
        if (selectedVersion == null || selectedVersion.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "No Version Selected", "Please select a Minecraft version first.");
            return;
        }

        if (gameProcess != null && gameProcess.isAlive()) {
            showAlert(Alert.AlertType.WARNING, "Game Running", "Minecraft is already running!");
            return;
        }

        homeTab.setPlayEnabled(false);
        homeTab.setStatus("Starting...", 0.0);

        Thread t = new Thread(() -> {
            try {
                // Find the version entry
                VersionManager.VersionEntry entry = allVersions.stream()
                        .filter(v -> v.id().equals(selectedVersion)).findFirst().orElse(null);
                if (entry == null) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Version Not Found", "Could not find version: " + selectedVersion));
                    return;
                }

                homeTab.setStatus("Fetching version details...", 0.05);
                VersionManager.VersionDetails details = versionManager.fetchVersionDetails(entry);

                // Download if not installed
                if (!versionManager.isVersionInstalled(launcherSettings.gameDirectory, selectedVersion)) {
                    homeTab.setStatus("Downloading game...", 0.1);
                    versionManager.downloadVersion(entry, details, gameDir, p -> homeTab.setStatus("Downloading game... " + (int)(p*100) + "%", 0.1 + p * 0.25));
                }

                homeTab.setStatus("Downloading libraries...", 0.35);
                libManager.downloadLibraries(details.libraries, gameDir, p -> homeTab.setStatus("Downloading libraries... " + (int)(p*100) + "%", 0.35 + p * 0.25));

                homeTab.setStatus("Extracting natives...", 0.6);
                libManager.extractNatives(details.libraries, gameDir, selectedVersion);

                if (details.assetIndex != null) {
                    homeTab.setStatus("Downloading assets...", 0.65);
                    assetManager.downloadAssets(details.assetIndex, gameDir, p -> homeTab.setStatus("Downloading assets... " + (int)(p*100) + "%", 0.65 + p * 0.30));
                }

                homeTab.setStatus("Applying settings...", 0.97);
                GameLauncher.applyGameSettings(gameSettings, modManager.getConfig(), gameDir);

                homeTab.setStatus("Launching Minecraft...", 1.0);
                gameProcess = gameLauncher.launch(currentSession, details, launcherSettings, modManager, gameDir, line -> {
                    consoleTab.appendLine(line);
                });

                // Switch to console
                Platform.runLater(() -> {
                    // Navigate to console tab
                    contentArea.getChildren().setAll(consoleTab);
                    homeTab.setStatus("Game Running ✓", -1);
                });

                int exitCode = gameProcess.waitFor();
                Platform.runLater(() -> {
                    homeTab.setStatus("Game closed (exit: " + exitCode + ")", -1);
                    homeTab.setPlayEnabled(true);
                    consoleTab.appendLine("--- Game exited with code " + exitCode + " ---");
                });

            } catch (Exception e) {
                log.error("Launch failed", e);
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Launch Failed", e.getMessage());
                    homeTab.setStatus("Launch failed: " + e.getMessage(), -1);
                    homeTab.setPlayEnabled(true);
                });
            }
        }, "game-launcher");
        t.setDaemon(true);
        t.start();
    }

    private void handleInstall(VersionManager.VersionEntry entry, Path gameDir) {
        com.novamc.ui.components.ProgressDialog dialog = new com.novamc.ui.components.ProgressDialog(stage);
        dialog.setTitle("Installing " + entry.id());
        dialog.show();

        Thread t = new Thread(() -> {
            try {
                dialog.setStatus("Fetching version details...");
                VersionManager.VersionDetails details = versionManager.fetchVersionDetails(entry);

                dialog.setStatus("Downloading game JAR...");
                versionManager.downloadVersion(entry, details, gameDir, p -> dialog.setProgress(p * 0.4));

                dialog.setStatus("Downloading libraries...");
                libManager.downloadLibraries(details.libraries, gameDir, p -> dialog.setProgress(0.4 + p * 0.3));

                dialog.setStatus("Extracting natives...");
                libManager.extractNatives(details.libraries, gameDir, entry.id());

                if (details.assetIndex != null) {
                    dialog.setStatus("Downloading assets...");
                    assetManager.downloadAssets(details.assetIndex, gameDir, p -> dialog.setProgress(0.7 + p * 0.3));
                }

                dialog.setProgress(1.0);
                dialog.setStatus("Done!");
                Thread.sleep(500);
                dialog.close();
                Platform.runLater(() -> {
                    versionsTab.markInstalled(entry.id());
                    showAlert(Alert.AlertType.INFORMATION, "Installed!", entry.id() + " has been installed.");
                });
            } catch (Exception e) {
                log.error("Install failed", e);
                dialog.close();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Install Failed", e.getMessage()));
            }
        }, "installer");
        t.setDaemon(true);
        t.start();
    }

    private void loadVersionsAsync() {
        Thread t = new Thread(() -> {
            try {
                allVersions = versionManager.fetchVersionList();
                Path gameDir = Path.of(launcherSettings.gameDirectory);
                Set<String> installed = getInstalledVersions(gameDir);
                List<String> releaseNames = allVersions.stream()
                        .filter(v -> v.type().equals("release")).map(VersionManager.VersionEntry::id)
                        .collect(Collectors.toList());
                Platform.runLater(() -> {
                    homeTab.setVersions(releaseNames);
                    versionsTab.setVersions(allVersions, installed);
                    if (!launcherSettings.selectedVersion.isBlank()) {
                        // homeTab.versionCombo won't expose directly, rely on setVersions
                    }
                    homeTab.setStatus("Ready", -1);
                });
            } catch (Exception e) {
                log.warn("Failed to load versions: {}", e.getMessage());
                Platform.runLater(() -> homeTab.setStatus("No internet — versions unavailable", -1));
            }
        }, "version-loader");
        t.setDaemon(true);
        t.start();
    }

    private Set<String> getInstalledVersions(Path gameDir) {
        Set<String> installed = new HashSet<>();
        Path versionsDir = gameDir.resolve("versions");
        if (!Files.exists(versionsDir)) return installed;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(versionsDir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    String name = entry.getFileName().toString();
                    if (Files.exists(entry.resolve(name + ".jar"))) installed.add(name);
                }
            }
        } catch (IOException e) { log.warn("Error listing installed versions", e); }
        return installed;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.initOwner(stage);
        alert.showAndWait();
    }
}
