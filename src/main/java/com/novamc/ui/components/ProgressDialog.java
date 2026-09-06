package com.novamc.ui.components;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

public class ProgressDialog {
    private final Stage stage;
    private final Label titleLabel;
    private final Label statusLabel;
    private final ProgressBar progressBar;
    private final Label percentLabel;

    public ProgressDialog(Stage owner) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setResizable(false);

        titleLabel = new Label("Downloading...");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e0e0e0;");

        statusLabel = new Label("Please wait...");
        statusLabel.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 12px;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setStyle("-fx-accent: #00d4aa;");

        percentLabel = new Label("0%");
        percentLabel.setStyle("-fx-text-fill: #00d4aa; -fx-font-size: 14px;");

        VBox box = new VBox(15, titleLabel, statusLabel, progressBar, percentLabel);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-background-color: #1a1a2e; -fx-border-color: #404060; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        Scene scene = new Scene(box);
        scene.setFill(null);
        stage.setScene(scene);
    }

    public void setTitle(String title) { Platform.runLater(() -> titleLabel.setText(title)); }
    public void setStatus(String status) { Platform.runLater(() -> statusLabel.setText(status)); }

    public void setProgress(double progress) {
        Platform.runLater(() -> {
            progressBar.setProgress(progress);
            percentLabel.setText((int)(progress * 100) + "%");
        });
    }

    public void show() { Platform.runLater(stage::show); }
    public void close() { Platform.runLater(stage::close); }
}
