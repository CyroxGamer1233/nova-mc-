package com.novamc.ui;

import com.novamc.settings.GameSettings;
import com.novamc.settings.LauncherSettings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.function.BiConsumer;

public class SettingsTab extends TabPane {
    // Launcher settings controls
    private TextField gameDirField, javaPathField, msClientIdField;
    private Slider minRamSlider, maxRamSlider;
    private Label minRamLabel, maxRamLabel;
    private CheckBox closeLauncherCheck, showConsoleCheck;
    private ComboBox<String> themeCombo;

    // Game settings controls
    private Slider renderDistSlider, maxFpsSlider, fovSlider, gammaSlider;
    private Slider musicVolSlider, soundVolSlider;
    private Label renderDistLabel, maxFpsLabel, fovLabel, gammaLabel;
    private Label musicVolLabel, soundVolLabel;
    private ComboBox<String> graphicsCombo;
    private CheckBox fullscreenCheck, autoJumpCheck, vsyncCheck;
    private TextField langField;

    private BiConsumer<LauncherSettings, GameSettings> onSave;
    private Stage ownerStage;

    public SettingsTab(Stage ownerStage) {
        this.ownerStage = ownerStage;
        setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);

        Tab launcherTab = new Tab("🚀 Launcher", buildLauncherPane());
        Tab gameTab = new Tab("🎮 Game", buildGamePane());
        getTabs().addAll(launcherTab, gameTab);
    }

    private ScrollPane buildLauncherPane() {
        gameDirField = new TextField();
        Button browseDirBtn = new Button("Browse");
        browseDirBtn.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Game Directory");
            File f = dc.showDialog(ownerStage);
            if (f != null) gameDirField.setText(f.getAbsolutePath());
        });

        javaPathField = new TextField();
        Button browseJavaBtn = new Button("Browse");
        browseJavaBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Java Executable");
            File f = fc.showOpenDialog(ownerStage);
            if (f != null) javaPathField.setText(f.getAbsolutePath());
        });
        Button detectJavaBtn = new Button("Auto-Detect");
        detectJavaBtn.setOnAction(e -> javaPathField.setText(com.novamc.core.JavaManager.detectJava()));

        minRamSlider = new Slider(256, 4096, 512); minRamSlider.setMajorTickUnit(512); minRamSlider.setShowTickMarks(true);
        minRamLabel = new Label("512 MB");
        minRamSlider.valueProperty().addListener((obs, ov, nv) -> minRamLabel.setText((int)minRamSlider.getValue() + " MB"));

        maxRamSlider = new Slider(512, 16384, 2048); maxRamSlider.setMajorTickUnit(2048); maxRamSlider.setShowTickMarks(true);
        maxRamLabel = new Label("2048 MB");
        maxRamSlider.valueProperty().addListener((obs, ov, nv) -> maxRamLabel.setText((int)maxRamSlider.getValue() + " MB"));

        closeLauncherCheck = new CheckBox("Close launcher when game starts");
        showConsoleCheck = new CheckBox("Show console tab");

        themeCombo = new ComboBox<>();
        themeCombo.getItems().addAll("dark", "light");
        themeCombo.getSelectionModel().selectFirst();

        msClientIdField = new TextField();
        msClientIdField.setPromptText("Your Azure App Client ID (for Microsoft login)");

        Button saveBtn = buildSaveButton();

        VBox pane = new VBox(12,
                makeSection("Game Directory"),
                makeRow("Directory:", gameDirField, browseDirBtn),
                makeSection("Java"),
                makeRow("Java Path:", javaPathField, browseJavaBtn, detectJavaBtn),
                makeSection("Memory"),
                makeLabeledSlider("Min RAM:", minRamSlider, minRamLabel),
                makeLabeledSlider("Max RAM:", maxRamSlider, maxRamLabel),
                makeSection("Options"),
                closeLauncherCheck, showConsoleCheck,
                makeSection("Theme"),
                themeCombo,
                makeSection("Microsoft Auth"),
                new Label("Enter your Azure App Client ID to enable Microsoft account login:"),
                msClientIdField,
                saveBtn
        );
        pane.setPadding(new Insets(20));
        pane.setStyle("-fx-background-color: #1a1a2e;");
        ScrollPane sp = new ScrollPane(pane);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: #1a1a2e;");
        return sp;
    }

    private ScrollPane buildGamePane() {
        renderDistSlider = new Slider(2, 32, 12); renderDistLabel = new Label("12");
        renderDistSlider.valueProperty().addListener((obs, ov, nv) -> renderDistLabel.setText(String.valueOf((int)renderDistSlider.getValue())));

        maxFpsSlider = new Slider(30, 260, 60); maxFpsLabel = new Label("60");
        maxFpsSlider.valueProperty().addListener((obs, ov, nv) -> {
            int v = (int) maxFpsSlider.getValue();
            maxFpsLabel.setText(v >= 260 ? "Unlimited" : String.valueOf(v));
        });

        fovSlider = new Slider(30, 110, 70); fovLabel = new Label("70");
        fovSlider.valueProperty().addListener((obs, ov, nv) -> fovLabel.setText(String.valueOf((int)fovSlider.getValue())));

        gammaSlider = new Slider(0, 1, 0); gammaLabel = new Label("0%");
        gammaSlider.valueProperty().addListener((obs, ov, nv) -> gammaLabel.setText((int)(gammaSlider.getValue() * 100) + "%"));

        musicVolSlider = new Slider(0, 100, 100); musicVolLabel = new Label("100%");
        musicVolSlider.valueProperty().addListener((obs, ov, nv) -> musicVolLabel.setText((int)musicVolSlider.getValue() + "%"));

        soundVolSlider = new Slider(0, 100, 100); soundVolLabel = new Label("100%");
        soundVolSlider.valueProperty().addListener((obs, ov, nv) -> soundVolLabel.setText((int)soundVolSlider.getValue() + "%"));

        graphicsCombo = new ComboBox<>();
        graphicsCombo.getItems().addAll("fast", "fancy", "fabulous");
        graphicsCombo.getSelectionModel().selectFirst();

        fullscreenCheck = new CheckBox("Fullscreen");
        autoJumpCheck = new CheckBox("Auto-Jump");
        vsyncCheck = new CheckBox("VSync");

        langField = new TextField("en_us");

        Button saveBtn = buildSaveButton();

        VBox pane = new VBox(12,
                makeSection("Performance"),
                makeLabeledSlider("Render Distance:", renderDistSlider, renderDistLabel),
                makeLabeledSlider("Max FPS:", maxFpsSlider, maxFpsLabel),
                makeLabeledSlider("FOV:", fovSlider, fovLabel),
                makeSection("Graphics"),
                makeRow("Graphics Mode:", graphicsCombo),
                fullscreenCheck, vsyncCheck,
                makeSection("Brightness"),
                makeLabeledSlider("Gamma:", gammaSlider, gammaLabel),
                makeSection("Audio"),
                makeLabeledSlider("Music Volume:", musicVolSlider, musicVolLabel),
                makeLabeledSlider("Sound Volume:", soundVolSlider, soundVolLabel),
                makeSection("Gameplay"),
                autoJumpCheck,
                makeSection("Language"),
                langField,
                saveBtn
        );
        pane.setPadding(new Insets(20));
        pane.setStyle("-fx-background-color: #1a1a2e;");
        ScrollPane sp = new ScrollPane(pane);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: #1a1a2e;");
        return sp;
    }

    private Button buildSaveButton() {
        Button btn = new Button("💾 Save Settings");
        btn.setStyle("-fx-background-color: #00d4aa; -fx-text-fill: #0a0a1a; -fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 6;");
        btn.setOnAction(e -> {
            if (onSave != null) onSave.accept(collectLauncherSettings(), collectGameSettings());
        });
        return btn;
    }

    public void loadSettings(LauncherSettings ls, GameSettings gs) {
        gameDirField.setText(ls.gameDirectory);
        javaPathField.setText(ls.javaPath);
        minRamSlider.setValue(ls.minRamMb);
        maxRamSlider.setValue(ls.maxRamMb);
        closeLauncherCheck.setSelected(ls.closeLauncherOnStart);
        showConsoleCheck.setSelected(ls.showConsole);
        themeCombo.setValue(ls.theme);
        msClientIdField.setText(ls.msClientId != null ? ls.msClientId : "");

        renderDistSlider.setValue(gs.renderDistance);
        maxFpsSlider.setValue(gs.maxFps);
        fovSlider.setValue(gs.fov);
        gammaSlider.setValue(Math.min(gs.gamma, 1.0));
        musicVolSlider.setValue(gs.musicVolume * 100);
        soundVolSlider.setValue(gs.soundVolume * 100);
        graphicsCombo.setValue(gs.graphicsMode);
        fullscreenCheck.setSelected(gs.fullscreen);
        autoJumpCheck.setSelected(gs.autoJump);
        vsyncCheck.setSelected(gs.vsync);
        langField.setText(gs.lang);
    }

    private LauncherSettings collectLauncherSettings() {
        LauncherSettings ls = new LauncherSettings();
        ls.gameDirectory = gameDirField.getText();
        ls.javaPath = javaPathField.getText();
        ls.minRamMb = (int) minRamSlider.getValue();
        ls.maxRamMb = (int) maxRamSlider.getValue();
        ls.closeLauncherOnStart = closeLauncherCheck.isSelected();
        ls.showConsole = showConsoleCheck.isSelected();
        ls.theme = themeCombo.getValue();
        ls.msClientId = msClientIdField.getText();
        return ls;
    }

    private GameSettings collectGameSettings() {
        GameSettings gs = new GameSettings();
        gs.renderDistance = (int) renderDistSlider.getValue();
        gs.maxFps = (int) maxFpsSlider.getValue();
        gs.fov = (int) fovSlider.getValue();
        gs.gamma = gammaSlider.getValue();
        gs.musicVolume = musicVolSlider.getValue() / 100.0;
        gs.soundVolume = soundVolSlider.getValue() / 100.0;
        gs.graphicsMode = graphicsCombo.getValue();
        gs.fullscreen = fullscreenCheck.isSelected();
        gs.autoJump = autoJumpCheck.isSelected();
        gs.vsync = vsyncCheck.isSelected();
        gs.lang = langField.getText();
        return gs;
    }

    public void setOnSave(BiConsumer<LauncherSettings, GameSettings> handler) { this.onSave = handler; }

    // Helper layout builders
    private Label makeSection(String text) {
        Label l = new Label(text.toUpperCase());
        l.setStyle("-fx-text-fill: #606090; -fx-font-size: 11px; -fx-font-weight: bold;");
        VBox.setMargin(l, new Insets(6, 0, 0, 0));
        return l;
    }

    private HBox makeRow(String label, javafx.scene.Node... nodes) {
        Label l = new Label(label); l.setPrefWidth(130); l.setStyle("-fx-text-fill: #a0a0c0;");
        HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(l); row.getChildren().addAll(nodes);
        return row;
    }

    private HBox makeLabeledSlider(String label, Slider slider, Label valueLabel) {
        Label l = new Label(label); l.setPrefWidth(130); l.setStyle("-fx-text-fill: #a0a0c0;");
        slider.setPrefWidth(250);
        valueLabel.setPrefWidth(80); valueLabel.setStyle("-fx-text-fill: #00d4aa;");
        HBox row = new HBox(10, l, slider, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
