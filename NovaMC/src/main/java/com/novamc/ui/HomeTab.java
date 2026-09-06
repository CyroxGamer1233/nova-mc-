package com.novamc.ui;

import com.novamc.auth.AuthSession;
import com.novamc.ui.components.ProfileCard;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class HomeTab extends BorderPane {
    private final ProfileCard profileCard;
    private final ComboBox<String> versionCombo;
    private final Button playButton;
    private final Label statusLabel;
    private final ProgressBar progressBar;
    private final Label gameDirLabel;
    private Runnable onPlay;

    public HomeTab() {
        // Left panel
        profileCard = new ProfileCard();

        Label novaMCLabel = new Label("⚡ NovaMC");
        novaMCLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #606080; -fx-font-weight: bold;");

        Label versionLabel = new Label("v1.0.0");
        versionLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #404060;");

        VBox leftPanel = new VBox(20, profileCard, novaMCLabel, versionLabel);
        leftPanel.setPadding(new Insets(20));
        leftPanel.setPrefWidth(220);
        leftPanel.setStyle("-fx-background-color: #0f0f1f;");
        setLeft(leftPanel);

        // Center panel
        Label title = new Label("⚡ NovaMC Launcher");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #00d4aa;");

        Label selectVersionLabel = new Label("Select Version:");
        selectVersionLabel.setStyle("-fx-text-fill: #a0a0c0;");

        versionCombo = new ComboBox<>();
        versionCombo.setPromptText("No version selected");
        versionCombo.setPrefWidth(300);
        versionCombo.setStyle("-fx-background-color: #252540; -fx-text-fill: #e0e0e0;");

        playButton = new Button("▶  PLAY");
        playButton.setStyle("-fx-background-color: #00d4aa; -fx-text-fill: #0a0a1a; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 15 60; -fx-background-radius: 8; -fx-cursor: hand;");
        playButton.setOnAction(e -> { if (onPlay != null) onPlay.run(); });

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 13px;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        progressBar.setVisible(false);
        progressBar.setStyle("-fx-accent: #00d4aa;");

        gameDirLabel = new Label("");
        gameDirLabel.setStyle("-fx-text-fill: #505070; -fx-font-size: 11px;");

        VBox centerBox = new VBox(24, title, selectVersionLabel, versionCombo, playButton, statusLabel, progressBar, gameDirLabel);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(40));
        setCenter(centerBox);
    }

    public void setSession(AuthSession session) { profileCard.update(session); }
    public void setVersions(List<String> versions) {
        Platform.runLater(() -> {
            versionCombo.getItems().setAll(versions);
            if (!versions.isEmpty()) versionCombo.getSelectionModel().selectFirst();
        });
    }
    public String getSelectedVersion() { return versionCombo.getValue(); }
    public void setStatus(String status, double progress) {
        Platform.runLater(() -> {
            statusLabel.setText(status);
            if (progress < 0) { progressBar.setVisible(false); }
            else { progressBar.setVisible(true); progressBar.setProgress(progress); }
        });
    }
    public void setOnPlay(Runnable r) { this.onPlay = r; }
    public void setGameDir(String dir) { Platform.runLater(() -> gameDirLabel.setText("Game directory: " + dir)); }
    public void setPlayEnabled(boolean enabled) { Platform.runLater(() -> playButton.setDisable(!enabled)); }
}
