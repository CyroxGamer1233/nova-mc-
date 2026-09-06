package com.novamc.ui.components;

import com.novamc.auth.AuthSession;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ProfileCard extends HBox {
    private final ImageView avatarView;
    private final Label usernameLabel;
    private final Label statusLabel;

    public ProfileCard() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);
        setPadding(new Insets(12));
        setStyle("-fx-background-color: #0f0f1f; -fx-background-radius: 8;");

        // Placeholder avatar
        Rectangle placeholder = new Rectangle(40, 40, Color.web("#252540"));
        avatarView = new ImageView();
        avatarView.setFitWidth(40);
        avatarView.setFitHeight(40);
        avatarView.setPreserveRatio(true);

        StackPane avatarPane = new StackPane(placeholder, avatarView);

        usernameLabel = new Label("Player");
        usernameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e0e0e0;");
        statusLabel = new Label("Offline");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #a0a0c0;");

        VBox info = new VBox(3, usernameLabel, statusLabel);
        getChildren().addAll(avatarPane, info);
    }

    public void update(AuthSession session) {
        usernameLabel.setText(session.username);
        statusLabel.setText(session.online ? "● Online" : "● Offline");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (session.online ? "#00d4aa" : "#a0a0c0") + ";");

        // Load avatar async
        String uuid = session.uuid;
        Thread t = new Thread(() -> {
            try {
                String url = "https://api.minetools.eu/avatar/" + uuid + "/40";
                Image img = new Image(url, 40, 40, true, true, false);
                Platform.runLater(() -> avatarView.setImage(img));
            } catch (Exception ignored) {}
        }, "avatar-loader");
        t.setDaemon(true);
        t.start();
    }
}
