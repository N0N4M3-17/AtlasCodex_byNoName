package com.atlaslibrary.ui;

import com.atlaslibrary.codex.MarkdownService;
import com.atlaslibrary.domain.AnchorType;
import com.atlaslibrary.domain.AppVersion;
import com.atlaslibrary.domain.Article;
import com.atlaslibrary.domain.AtlasNode;
import com.atlaslibrary.domain.Layer;
import com.atlaslibrary.domain.NodeType;
import com.atlaslibrary.domain.ProjectState;
import com.atlaslibrary.storage.AtlasBundleService;
import com.atlaslibrary.storage.SqliteProjectStore;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class MainWindow {
    private final ProjectState state = bootstrap();
    private final SqliteProjectStore store = new SqliteProjectStore();
    private final AtlasBundleService bundles = new AtlasBundleService();
    private final MarkdownService markdown = new MarkdownService();

    private final Canvas canvas = new Canvas(1200, 800);
    private final ListView<String> layersList = new ListView<>();
    private final ListView<String> objectList = new ListView<>();
    private final TextArea markdownRaw = new TextArea();
    private final TextArea markdownPreview = new TextArea();
    private double zoom = 1.0;
    private double panX = 0;
    private double panY = 0;
    private double lastX;
    private double lastY;

    public Parent buildRoot(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(buildTopBar(stage));
        root.setCenter(buildWorkspace());
        root.setBottom(statusBar());
        refreshLists();
        render();
        return root;
    }

    private HBox buildTopBar(Stage stage) {
        Button addText = new Button("Add Text");
        addText.setOnAction(e -> { AtlasNode n = new AtlasNode(); n.type = NodeType.TEXT; n.layerId = state.layers.getFirst().id; n.content = "New text"; n.x = 100; n.y = 100; state.nodes.add(n); refreshLists(); render();});
        Button addImage = new Button("Add Image");
        addImage.setOnAction(e -> { AtlasNode n = new AtlasNode(); n.type = NodeType.IMAGE; n.layerId = state.layers.getFirst().id; n.content = "image-placeholder"; n.x = 220; n.y = 180; state.nodes.add(n); refreshLists(); render();});
        Button save = new Button("Save .atlaslib");
        save.setOnAction(e -> saveProject(stage));
        Button open = new Button("Open .atlaslib");
        open.setOnAction(e -> openProject(stage));
        Button full = new Button("Fullscreen");
        full.setOnAction(e -> stage.setFullScreen(!stage.isFullScreen()));
        HBox box = new HBox(10, addText, addImage, save, open, full, new Label("Ctrl+Wheel Zoom | Drag Pan | Left Click Select"));
        box.setPadding(new Insets(10)); box.getStyleClass().add("top-bar");
        return box;
    }

    private Parent buildWorkspace() {
        SplitPane split = new SplitPane();
        VBox left = new VBox(8, new Label("Layers"), layersList, new Label("Objects"), objectList, new Button("Add INTERNAL anchor") {{ setOnAction(e -> createAnchor()); }});
        left.setPadding(new Insets(10)); left.getStyleClass().add("side-panel");

        StackPane center = new StackPane(canvas);
        center.getStyleClass().add("viewport");
        setupCanvasInteraction();

        markdownPreview.setEditable(false);
        markdownRaw.setText("# Article\nStart writing codex notes.");
        markdownPreview.setText(markdown.toPreviewText(markdownRaw.getText()));
        markdownRaw.textProperty().addListener((obs, o, n) -> markdownPreview.setText(markdown.toPreviewText(n)));
        VBox right = new VBox(8, new Label("Codex Raw"), markdownRaw, new Label("Codex Preview"), markdownPreview);
        right.setPadding(new Insets(10)); right.getStyleClass().add("side-panel");

        split.getItems().addAll(left, center, right);
        split.setDividerPositions(0.2, 0.78);
        return split;
    }

    private void setupCanvasInteraction() {
        canvas.setOnScroll(e -> { zoom = Math.max(0.2, Math.min(4.0, zoom + (e.getDeltaY() > 0 ? 0.1 : -0.1))); render(); });
        canvas.setOnMousePressed(e -> { lastX = e.getX(); lastY = e.getY(); if (e.getButton() == MouseButton.PRIMARY) selectAt(e.getX(), e.getY()); });
        canvas.setOnMouseDragged(e -> { panX += e.getX() - lastX; panY += e.getY() - lastY; lastX = e.getX(); lastY = e.getY(); render(); });
        canvas.widthProperty().addListener((o,a,b)->render());
        canvas.heightProperty().addListener((o,a,b)->render());
    }

    private void render() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.web("#0b1320")); g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
        g.save(); g.translate(panX, panY); g.scale(zoom, zoom);
        for (AtlasNode n : state.nodes) {
            if (n.type == NodeType.IMAGE) { g.setFill(Color.web("#2f7bdb")); g.fillRect(n.x, n.y, 140, 90); g.setFill(Color.WHITE); g.fillText("Image", n.x + 10, n.y + 20); }
            if (n.type == NodeType.TEXT) { g.setFill(Color.web("#ffd166")); g.fillText(n.content, n.x, n.y); }
        }
        g.restore();
    }

    private void selectAt(double sx, double sy) {
        objectList.getSelectionModel().clearSelection();
        for (int i=0;i<state.nodes.size();i++) {
            AtlasNode n = state.nodes.get(i);
            double nx = n.x * zoom + panX; double ny = n.y * zoom + panY;
            if (Math.abs(sx - nx) < 80 && Math.abs(sy - ny) < 40) { objectList.getSelectionModel().select(i); break; }
        }
    }

    private void saveProject(Stage stage) {
        try {
            FileChooser fc = new FileChooser(); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Atlas Library Bundle", "*.atlaslib"));
            File file = fc.showSaveDialog(stage); if (file == null) return;
            Path temp = Files.createTempDirectory("atlas-save"); Path db = temp.resolve("project.sqlite");
            store.save(db, state); bundles.writeManifestAndDb(file.toPath(), db);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void openProject(Stage stage) {
        try {
            FileChooser fc = new FileChooser(); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Atlas Library Bundle", "*.atlaslib"));
            File file = fc.showOpenDialog(stage); if (file == null) return;
            Path temp = Files.createTempDirectory("atlas-open"); Path db = bundles.extractDb(file.toPath(), temp); ProjectState loaded = store.load(db);
            state.layers.clear(); state.layers.addAll(loaded.layers); state.nodes.clear(); state.nodes.addAll(loaded.nodes); state.articles.clear(); state.articles.addAll(loaded.articles); state.anchors.clear(); state.anchors.addAll(loaded.anchors);
            refreshLists(); render();
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void createAnchor() {
        if (state.nodes.isEmpty()) return;
        var a = new com.atlaslibrary.domain.Anchor(); a.sourceNodeId = state.nodes.getFirst().id; a.type = AnchorType.INTERNAL_ARTICLE;
        if (state.articles.isEmpty()) { Article art = new Article(); art.slug = "entry"; art.title = "Entry"; art.markdownBody = markdownRaw.getText(); state.articles.add(art); }
        a.target = state.articles.getFirst().id; state.anchors.add(a);
    }

    private HBox statusBar() {
        HBox bar = new HBox(15, new Label("Atlas " + AppVersion.VALUE), new Label("Nodes: " + state.nodes.size()));
        bar.setPadding(new Insets(8)); bar.getStyleClass().add("status-bar");
        return bar;
    }

    private void refreshLists() {
        layersList.getItems().setAll(state.layers.stream().map(l -> l.name).toList());
        objectList.getItems().setAll(state.nodes.stream().map(n -> n.type + " @(" + (int)n.x + "," + (int)n.y + ")").toList());
    }

    private ProjectState bootstrap() {
        ProjectState s = new ProjectState();
        s.layers.add(new Layer("Base", 0));
        return s;
    }
}
