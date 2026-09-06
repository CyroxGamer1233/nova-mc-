package com.novamc.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;

public class ConsoleTab extends VBox {
    private final TextArea textArea;

    public ConsoleTab() {
        setSpacing(5);
        setPadding(new Insets(10));

        textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setStyle("-fx-control-inner-background: #0a0a0a; -fx-text-fill: #00ff88; -fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");
        VBox.setVgrow(textArea, Priority.ALWAYS);

        Button clearBtn = new Button("🗑 Clear");
        clearBtn.setOnAction(e -> textArea.clear());

        Button copyBtn = new Button("📋 Copy All");
        copyBtn.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(textArea.getText());
            Clipboard.getSystemClipboard().setContent(content);
        });

        HBox toolbar = new HBox(10, clearBtn, copyBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label headerLabel = new Label("Game Console Output");
        headerLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #a0a0c0;");

        getChildren().addAll(headerLabel, toolbar, textArea);
    }

    public void appendLine(String line) {
        Platform.runLater(() -> {
            textArea.appendText(line + "\n");
            textArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    public void clear() {
        Platform.runLater(textArea::clear);
    }
}
