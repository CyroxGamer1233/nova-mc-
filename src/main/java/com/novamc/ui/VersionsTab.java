package com.novamc.ui;

import com.novamc.core.VersionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class VersionsTab extends VBox {
    private final TableView<VersionManager.VersionEntry> table;
    private List<VersionManager.VersionEntry> allVersions = new ArrayList<>();
    private Set<String> installedVersions = new HashSet<>();
    private Consumer<VersionManager.VersionEntry> onInstall;
    private Consumer<VersionManager.VersionEntry> onDelete;
    private final CheckBox showReleases;
    private final CheckBox showSnapshots;
    private final CheckBox showOld;

    public VersionsTab() {
        setSpacing(10);
        setPadding(new Insets(15));

        Label header = new Label("Version Manager");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e0e0e0;");

        showReleases = new CheckBox("Releases"); showReleases.setSelected(true);
        showSnapshots = new CheckBox("Snapshots");
        showOld = new CheckBox("Old / Beta");

        showReleases.setOnAction(e -> refreshFilter());
        showSnapshots.setOnAction(e -> refreshFilter());
        showOld.setOnAction(e -> refreshFilter());

        HBox filterBar = new HBox(20, new Label("Show:"), showReleases, showSnapshots, showOld);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        table = new TableView<>();
        table.setPlaceholder(new Label("No versions loaded. Check your internet connection."));
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<VersionManager.VersionEntry, String> idCol = new TableColumn<>("Version");
        idCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().id()));
        idCol.setPrefWidth(180);

        TableColumn<VersionManager.VersionEntry, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(formatType(d.getValue().type())));
        typeCol.setPrefWidth(120);

        TableColumn<VersionManager.VersionEntry, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(
                installedVersions.contains(d.getValue().id()) ? "✓ Installed" : "Not Installed"));
        statusCol.setPrefWidth(120);

        table.getColumns().addAll(idCol, typeCol, statusCol);

        Button installBtn = new Button("⬇ Install Selected");
        installBtn.setStyle("-fx-background-color: #00d4aa; -fx-text-fill: #0a0a1a; -fx-font-weight: bold;");
        installBtn.setOnAction(e -> {
            VersionManager.VersionEntry sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && onInstall != null) onInstall.accept(sel);
        });

        Button deleteBtn = new Button("🗑 Delete Selected");
        deleteBtn.setOnAction(e -> {
            VersionManager.VersionEntry sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && onDelete != null) onDelete.accept(sel);
        });

        HBox btnBar = new HBox(10, installBtn, deleteBtn);
        btnBar.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(header, filterBar, table, btnBar);
    }

    private String formatType(String type) {
        return switch (type) {
            case "release" -> "🟢 Release";
            case "snapshot" -> "🔶 Snapshot";
            case "old_beta" -> "🔷 Old Beta";
            case "old_alpha" -> "🔵 Old Alpha";
            default -> type;
        };
    }

    private void refreshFilter() {
        List<VersionManager.VersionEntry> filtered = allVersions.stream().filter(v -> {
            return switch (v.type()) {
                case "release" -> showReleases.isSelected();
                case "snapshot" -> showSnapshots.isSelected();
                case "old_beta", "old_alpha" -> showOld.isSelected();
                default -> true;
            };
        }).collect(Collectors.toList());
        table.getItems().setAll(filtered);
    }

    public void setVersions(List<VersionManager.VersionEntry> list, Set<String> installed) {
        this.allVersions = list;
        this.installedVersions = installed;
        refreshFilter();
    }

    public void setOnInstall(Consumer<VersionManager.VersionEntry> handler) { this.onInstall = handler; }
    public void setOnDelete(Consumer<VersionManager.VersionEntry> handler) { this.onDelete = handler; }
    public void markInstalled(String versionId) {
        installedVersions.add(versionId);
        table.refresh();
    }
}
