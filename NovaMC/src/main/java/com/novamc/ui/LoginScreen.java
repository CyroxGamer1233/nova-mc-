package com.novamc.ui;

import com.novamc.auth.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.util.function.Consumer;

public class LoginScreen {
    private final Stage stage;
    private Consumer<AuthSession> onLogin;

    public LoginScreen(Stage stage) {
        this.stage = stage;
    }

    public void setOnLogin(Consumer<AuthSession> callback) {
        this.onLogin = callback;
    }

    public Scene buildScene(String msClientId) {
        // Logo
        Label logo = new Label("⚡ NovaMC");
        logo.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #00d4aa;");
        Label subtitle = new Label("Minecraft Launcher");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #a0a0c0;");

        // Offline tab
        VBox offlineBox = buildOfflineTab();
        // Microsoft tab
        VBox msBox = buildMicrosoftTab(msClientId);

        TabPane tabPane = new TabPane();
        Tab offlineTab = new Tab("🎮 Offline / Cracked", offlineBox);
        Tab msTab = new Tab("🔑 Microsoft Account", msBox);
        offlineTab.setClosable(false);
        msTab.setClosable(false);
        tabPane.getTabs().addAll(offlineTab, msTab);
        tabPane.setStyle("-fx-background-color: #1a1a2e;");
        tabPane.setMaxWidth(420);

        Label disclaimer = new Label("⚠️ For personal/educational use only. Requires a valid Minecraft license for online servers.");
        disclaimer.setStyle("-fx-text-fill: #606060; -fx-font-size: 10px; -fx-wrap-text: true;");
        disclaimer.setMaxWidth(420);

        VBox root = new VBox(20, logo, subtitle, tabPane, disclaimer);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #1a1a2e;");

        Scene scene = new Scene(root, 500, 580);
        scene.setFill(Color.web("#1a1a2e"));
        return scene;
    }

    private VBox buildOfflineTab() {
        Label info = new Label("Play without a Microsoft account. Works for singleplayer and offline servers.");
        info.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 12px; -fx-wrap-text: true;");
        info.setMaxWidth(380);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username...");
        usernameField.setMaxWidth(380);
        usernameField.setStyle("-fx-background-color: #252540; -fx-text-fill: #e0e0e0; -fx-border-color: #404060; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #ff6060; -fx-font-size: 11px;");

        Button playBtn = new Button("▶ Play Offline");
        playBtn.setStyle("-fx-background-color: #00d4aa; -fx-text-fill: #0a0a1a; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 30; -fx-background-radius: 6; -fx-cursor: hand;");
        playBtn.setMaxWidth(380);
        playBtn.setOnAction(e -> {
            String name = usernameField.getText().trim();
            if (name.isEmpty() || name.length() < 3) {
                errorLabel.setText("Username must be at least 3 characters.");
                return;
            }
            AuthSession session = OfflineAuthManager.createSession(name);
            if (onLogin != null) onLogin.accept(session);
        });

        VBox box = new VBox(12, info, usernameField, errorLabel, playBtn);
        box.setPadding(new Insets(16));
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private VBox buildMicrosoftTab(String savedClientId) {
        Label info = new Label("Login with your Microsoft account (requires an Azure App Client ID).");
        info.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 12px; -fx-wrap-text: true;");
        info.setMaxWidth(380);

        TextField clientIdField = new TextField(savedClientId != null ? savedClientId : "");
        clientIdField.setPromptText("Azure App Client ID...");
        clientIdField.setMaxWidth(380);
        clientIdField.setStyle("-fx-background-color: #252540; -fx-text-fill: #e0e0e0; -fx-border-color: #404060; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");

        Button openBrowserBtn = new Button("🌐 Open Microsoft Login");
        openBrowserBtn.setStyle("-fx-background-color: #0078d4; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        openBrowserBtn.setMaxWidth(380);

        TextField codeField = new TextField();
        codeField.setPromptText("Paste the auth code from the browser URL here...");
        codeField.setMaxWidth(380);
        codeField.setStyle("-fx-background-color: #252540; -fx-text-fill: #e0e0e0; -fx-border-color: #404060; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8;");
        codeField.setVisible(false);

        Button completeBtn = new Button("✓ Complete Login");
        completeBtn.setStyle("-fx-background-color: #00d4aa; -fx-text-fill: #0a0a1a; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 20; -fx-background-radius: 6; -fx-cursor: hand;");
        completeBtn.setMaxWidth(380);
        completeBtn.setVisible(false);

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: #a0a0c0; -fx-font-size: 11px; -fx-wrap-text: true;");
        statusLabel.setMaxWidth(380);

        final MicrosoftAuthManager[] mgr = {null};

        openBrowserBtn.setOnAction(e -> {
            String clientId = clientIdField.getText().trim();
            if (clientId.isEmpty()) {
                statusLabel.setText("Please enter your Azure Client ID first.");
                return;
            }
            try {
                mgr[0] = new MicrosoftAuthManager(clientId);
                String authUrl = mgr[0].buildAuthUrl();
                Desktop.getDesktop().browse(URI.create(authUrl));
                codeField.setVisible(true);
                completeBtn.setVisible(true);
                statusLabel.setText("Browser opened. After login, copy the 'code' parameter from the URL and paste it below.");
            } catch (Exception ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        completeBtn.setOnAction(e -> {
            if (mgr[0] == null) return;
            String code = codeField.getText().trim();
            if (code.isEmpty()) { statusLabel.setText("Please paste the auth code."); return; }
            completeBtn.setDisable(true);
            statusLabel.setText("Authenticating...");
            Thread t = new Thread(() -> {
                try {
                    AuthSession session = mgr[0].authenticateWithCode(code);
                    if (onLogin != null) Platform.runLater(() -> onLogin.accept(session));
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Auth failed: " + ex.getMessage());
                        completeBtn.setDisable(false);
                    });
                }
            }, "ms-auth");
            t.setDaemon(true);
            t.start();
        });

        VBox box = new VBox(10, info, clientIdField, openBrowserBtn, codeField, completeBtn, statusLabel);
        box.setPadding(new Insets(16));
        box.setAlignment(Pos.CENTER);
        return box;
    }
}
