package com.novamc.ui;

import com.novamc.mods.ClientModConfig;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ModsTab extends ScrollPane {
    private ToggleButton fullbrightBtn, fpsBoostBtn, autoSprintBtn, showFpsBtn;
    private ToggleButton antiAfkBtn, zoomKeyBtn, freelookBtn, customCrosshairBtn;
    private ComboBox<String> crosshairStyle;
    private Runnable onChange;

    public ModsTab() {
        setFitToWidth(true);
        setStyle("-fx-background-color: #1a1a2e;");

        VBox content = new VBox(4);
        content.setPadding(new Insets(15));
        content.setStyle("-fx-background-color: #1a1a2e;");

        Label header = new Label("Built-in Client Mods");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e0e0e0;");
        Label sub = new Label("These tweaks apply to your next game launch.");
        sub.setStyle("-fx-text-fill: #606080; -fx-font-size: 12px;");

        fullbrightBtn = makeToggle();
        fpsBoostBtn = makeToggle();
        autoSprintBtn = makeToggle();
        showFpsBtn = makeToggle();
        antiAfkBtn = makeToggle();
        zoomKeyBtn = makeToggle();
        freelookBtn = makeToggle();
        customCrosshairBtn = makeToggle();

        crosshairStyle = new ComboBox<>();
        crosshairStyle.getItems().addAll("Default", "Dot", "Plus", "Circle");
        crosshairStyle.getSelectionModel().selectFirst();
        crosshairStyle.setStyle("-fx-background-color: #252540; -fx-text-fill: #e0e0e0;");
        crosshairStyle.setOnAction(e -> { if (onChange != null) onChange.run(); });

        content.getChildren().addAll(
                header, sub, makeSep(),
                makeRow("☀ Fullbright", "Removes all darkness from caves and the night sky", fullbrightBtn),
                makeRow("⚡ FPS Boost", "Uncaps FPS and disables VSync for maximum performance", fpsBoostBtn),
                makeRow("🏃 Auto-Sprint", "Always sprinting without holding the sprint key", autoSprintBtn),
                makeRow("📊 Show FPS", "Displays FPS counter in the corner of the screen", showFpsBtn),
                makeRow("⏱ Anti-AFK", "Periodically moves to prevent AFK kicks on servers", antiAfkBtn),
                makeRow("🔍 Zoom Key (C)", "Press C to zoom in like OptiFine zoom", zoomKeyBtn),
                makeRow("👁 Freelook (V)", "Hold V to look around without moving your character body", freelookBtn),
                makeSep(),
                makeRowWithExtra("✚ Custom Crosshair", "Change the crosshair appearance style", customCrosshairBtn, crosshairStyle),
                makeSep(),
                makeWarning()
        );

        setContent(content);
    }

    private ToggleButton makeToggle() {
        ToggleButton tb = new ToggleButton("OFF");
        tb.setPrefWidth(60);
        tb.setStyle("-fx-background-color: #252540; -fx-text-fill: #a0a0c0; -fx-background-radius: 4;");
        tb.selectedProperty().addListener((obs, ov, nv) -> {
            tb.setText(nv ? "ON" : "OFF");
            tb.setStyle(nv
                    ? "-fx-background-color: #00d4aa; -fx-text-fill: #0a0a1a; -fx-font-weight: bold; -fx-background-radius: 4;"
                    : "-fx-background-color: #252540; -fx-text-fill: #a0a0c0; -fx-background-radius: 4;");
            if (onChange != null) onChange.run();
        });
        return tb;
    }

    private HBox makeRow(String name, String desc, ToggleButton toggle) {
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e0e0e0;");
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #808090;");
        VBox info = new VBox(2, nameLabel, descLabel);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(spacer.getMaxWidth(), info, spacer, toggle);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 15, 10, 15));
        row.setStyle("-fx-background-color: #1e1e34; -fx-background-radius: 6;");
        row.setSpacing(10);
        return row;
    }

    private HBox makeRowWithExtra(String name, String desc, ToggleButton toggle, Control extra) {
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e0e0e0;");
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #808090;");
        VBox info = new VBox(2, nameLabel, descLabel);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, info, spacer, extra, toggle);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 15, 10, 15));
        row.setStyle("-fx-background-color: #1e1e34; -fx-background-radius: 6;");
        return row;
    }

    private Separator makeSep() {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #303050;");
        VBox.setMargin(sep, new Insets(8, 0, 8, 0));
        return sep;
    }

    private Label makeWarning() {
        Label w = new Label("⚠️ These tweaks are for personal/educational use. Some may violate server rules on competitive servers.");
        w.setStyle("-fx-text-fill: #806040; -fx-font-size: 11px; -fx-wrap-text: true;");
        return w;
    }

    public void loadConfig(ClientModConfig config) {
        fullbrightBtn.setSelected(config.fullbright);
        fpsBoostBtn.setSelected(config.fpsBoost);
        autoSprintBtn.setSelected(config.autoSprint);
        showFpsBtn.setSelected(config.showFps);
        antiAfkBtn.setSelected(config.antiAfk);
        zoomKeyBtn.setSelected(config.zoomKey);
        freelookBtn.setSelected(config.freelook);
        customCrosshairBtn.setSelected(config.customCrosshair);
        crosshairStyle.getSelectionModel().select(config.crosshairStyle);
    }

    public void saveToConfig(ClientModConfig config) {
        config.fullbright = fullbrightBtn.isSelected();
        config.fpsBoost = fpsBoostBtn.isSelected();
        config.autoSprint = autoSprintBtn.isSelected();
        config.showFps = showFpsBtn.isSelected();
        config.antiAfk = antiAfkBtn.isSelected();
        config.zoomKey = zoomKeyBtn.isSelected();
        config.freelook = freelookBtn.isSelected();
        config.customCrosshair = customCrosshairBtn.isSelected();
        config.crosshairStyle = crosshairStyle.getSelectionModel().getSelectedIndex();
    }

    public void setOnChange(Runnable r) { this.onChange = r; }
}
