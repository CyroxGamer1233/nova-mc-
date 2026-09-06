package com.novamc;

import com.novamc.auth.AuthSession;
import com.novamc.settings.LauncherSettings;
import com.novamc.ui.LoginScreen;
import com.novamc.ui.MainWindow;
import com.novamc.util.FileUtils;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main extends Application {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("NovaMC Launcher");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        Path homeDir = Path.of(System.getProperty("user.home"), ".novamc");
        try { FileUtils.ensureDir(homeDir); } catch (Exception e) { log.error("Cannot create home dir", e); }

        LauncherSettings settings = LauncherSettings.load(homeDir.resolve("launcher.json"));
        AuthSession savedSession = tryLoadSession(homeDir);

        if (savedSession != null && !savedSession.isExpired()) {
            log.info("Restoring session for {}", savedSession.username);
            openMainWindow(primaryStage, savedSession);
        } else {
            showLoginScreen(primaryStage, settings, homeDir);
        }
    }

    private void showLoginScreen(Stage stage, LauncherSettings settings, Path homeDir) {
        LoginScreen login = new LoginScreen(stage);
        login.setOnLogin(session -> {
            // Save session
            try { FileUtils.writeString(homeDir.resolve("session.json"), session.toJson()); }
            catch (Exception e) { log.warn("Could not save session", e); }
            openMainWindow(stage, session);
        });
        stage.setScene(login.buildScene(settings.msClientId));
        stage.setWidth(520);
        stage.setHeight(600);
        stage.show();
    }

    private void openMainWindow(Stage stage, AuthSession session) {
        stage.setWidth(1100);
        stage.setHeight(700);
        stage.centerOnScreen();
        MainWindow mainWindow = new MainWindow();
        mainWindow.initialize(stage, session);
    }

    private AuthSession tryLoadSession(Path homeDir) {
        Path sessionFile = homeDir.resolve("session.json");
        if (!Files.exists(sessionFile)) return null;
        try {
            String json = com.novamc.util.FileUtils.readString(sessionFile);
            AuthSession s = AuthSession.fromJson(json);
            return s;
        } catch (Exception e) {
            log.warn("Could not load session: {}", e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        try { launch(args); }
        catch (Exception e) { LoggerFactory.getLogger(Main.class).error("Fatal error", e); }
    }
}
