package com.atlaslibrary.ui;

import com.atlaslibrary.domain.AppVersion;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class MainWindow {
    public Parent buildRoot() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        root.setTop(buildTopBar());
        root.setCenter(buildWorkspace());
        root.setBottom(buildStatusBar());

        return root;
    }

    private HBox buildTopBar() {
        TextField commandPalette = new TextField();
        commandPalette.setPromptText("Command palette (Ctrl+K) …");
        HBox.setHgrow(commandPalette, Priority.ALWAYS);

        Button newProject = new Button("New");
        Button openProject = new Button("Open");
        Button saveProject = new Button("Save");
        Button fullscreen = new Button("Fullscreen");

        HBox bar = new HBox(10, newProject, openProject, saveProject, new Separator(Orientation.VERTICAL), commandPalette, fullscreen);
        bar.setPadding(new Insets(10));
        bar.getStyleClass().add("top-bar");
        return bar;
    }

    private SplitPane buildWorkspace() {
        VBox leftPanel = new VBox(8,
                sectionTitle("Layers"),
                new ListView<>() {{
                    getItems().addAll("Base Map", "Terrain", "Settlements", "Routes", "Labels");
                }},
                sectionTitle("Objects"),
                new ListView<>() {{
                    getItems().addAll("ImageNode: world_map.png", "TextNode: Northern Reach", "Anchor: Capital City → Article");
                }}
        );
        leftPanel.setPadding(new Insets(12));
        leftPanel.getStyleClass().add("side-panel");

        StackPane viewport = new StackPane();
        viewport.getStyleClass().add("viewport");

        Rectangle mapShape = new Rectangle(900, 560);
        mapShape.setArcHeight(16);
        mapShape.setArcWidth(16);
        mapShape.setFill(Color.web("#17212d"));
        mapShape.setStroke(Color.web("#2f7bdb"));
        mapShape.setStrokeWidth(1.5);

        Label viewportGuide = new Label("Infinite Canvas\nPan: Middle Mouse | Zoom: Ctrl + Wheel | Select: Click/Drag\nImage/Text/Anchor tooling is scaffolded in v0.0.1aa");
        viewportGuide.setAlignment(Pos.CENTER);
        viewportGuide.setTextFill(Color.web("#d4e3ff"));

        viewport.getChildren().addAll(mapShape, viewportGuide);

        VBox rightPanel = new VBox(8,
                sectionTitle("Inspector"),
                new Label("Mode: Map Edit"),
                new Label("Transform: x=120, y=240, scale=1.00, rot=0°"),
                new Label("Opacity: 100%"),
                new Separator(),
                sectionTitle("Codex"),
                new Label("Raw Markdown"),
                new TextArea("# Northern Reach\nA cold frontier marked by old ruins and contested roads."),
                new Label("Rendered Preview"),
                new TextArea("Northern Reach\nA cold frontier marked by old ruins and contested roads.")
        );
        rightPanel.setPadding(new Insets(12));
        rightPanel.getStyleClass().add("side-panel");

        SplitPane splitPane = new SplitPane(leftPanel, viewport, rightPanel);
        splitPane.setDividerPositions(0.2, 0.77);
        return splitPane;
    }

    private HBox buildStatusBar() {
        Label version = new Label("Atlas Library " + AppVersion.VALUE);
        Label coords = new Label("Coordinates: x=0 y=0");
        Label zoom = new Label("Zoom: 100%");
        Label queue = new Label("Task Queue: idle");

        HBox bar = new HBox(18, version, coords, zoom, queue);
        bar.setPadding(new Insets(8, 12, 8, 12));
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }
}
