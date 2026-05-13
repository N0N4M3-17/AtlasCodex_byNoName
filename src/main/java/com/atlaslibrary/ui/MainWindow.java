package com.atlaslibrary.ui;

import com.atlaslibrary.codex.MarkdownService;
import com.atlaslibrary.domain.*;
import com.atlaslibrary.storage.AtlasBundleService;
import com.atlaslibrary.storage.SqliteProjectStore;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MainWindow {
    private final ProjectState state = bootstrap();
    private final SqliteProjectStore store = new SqliteProjectStore();
    private final AtlasBundleService bundles = new AtlasBundleService();
    private final MarkdownService markdown = new MarkdownService();
    private final Map<String, Image> imageCache = new HashMap<>();

    private final Canvas canvas = new Canvas(1500, 900);
    private final ListView<Layer> layersList = new ListView<>();
    private final ListView<AtlasNode> objectList = new ListView<>();
    private final TextField nodeText = new TextField();
    private final Slider nodeOpacity = new Slider(0.1, 1.0, 1.0);
    private final Spinner<Integer> fontSize = new Spinner<>(8, 72, 18);
    private final TextArea markdownRaw = new TextArea();
    private final TextArea markdownPreview = new TextArea();
    private AtlasNode selectedNode;
    private double zoom = 1.0, panX = 0, panY = 0, lastX, lastY;

    public Parent buildRoot(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(toolbar(stage));
        root.setCenter(workspace());
        root.setBottom(statusBar());
        bindLists();
        render();
        return root;
    }

    private Parent toolbar(Stage stage) {
        Button addText = new Button("Add Text");
        addText.setOnAction(e -> { AtlasNode n = new AtlasNode(); n.type = NodeType.TEXT; n.layerId = activeLayer().id; n.content = "Editable text"; n.x = 150; n.y = 140; state.nodes.add(n); refresh(); });
        Button addImage = new Button("Add Image");
        addImage.setOnAction(e -> addImageNode(stage));
        Button addLayer = new Button("Add Layer");
        addLayer.setOnAction(e -> { state.layers.add(new Layer("Layer " + (state.layers.size()+1), state.layers.size())); refresh(); });
        Button save = new Button("Save"); save.setOnAction(e -> saveProject(stage));
        Button open = new Button("Open"); save.setDefaultButton(false); open.setOnAction(e -> openProject(stage));
        Button full = new Button("Fullscreen"); full.setOnAction(e -> stage.setFullScreen(!stage.isFullScreen()));
        HBox box = new HBox(10, addText, addImage, addLayer, save, open, full);
        box.setPadding(new Insets(10)); box.getStyleClass().add("top-bar"); return box;
    }

    private Parent workspace() {
        SplitPane split = new SplitPane();
        VBox left = new VBox(8, new Label("Layers"), layersList, new Label("Objects"), objectList);
        left.setPadding(new Insets(10)); left.getStyleClass().add("side-panel");

        StackPane center = new StackPane(canvas); center.getStyleClass().add("viewport"); setupCanvasInteraction();

        markdownPreview.setEditable(false);
        markdownRaw.setText("# Article\nWrite and link your lore."); markdownPreview.setText(markdown.toPreviewText(markdownRaw.getText()));
        markdownRaw.textProperty().addListener((o,a,b)-> markdownPreview.setText(markdown.toPreviewText(b)));
        nodeText.setPromptText("Selected text node content");
        nodeText.textProperty().addListener((o,a,b)-> { if(selectedNode!=null && selectedNode.type==NodeType.TEXT){selectedNode.content=b; render(); refresh();}});
        nodeOpacity.valueProperty().addListener((o,a,b)-> { if(selectedNode!=null){selectedNode.opacity=b.doubleValue(); render(); }});
        fontSize.valueProperty().addListener((o,a,b)-> { if(selectedNode!=null && selectedNode.type==NodeType.TEXT){selectedNode.fontSize=b; render();}});
        VBox right = new VBox(8, new Label("Inspector"), nodeText, new Label("Opacity"), nodeOpacity, new Label("Font Size"), fontSize, new Separator(), new Label("Codex"), markdownRaw, markdownPreview);
        right.setPadding(new Insets(10)); right.getStyleClass().add("side-panel");

        split.getItems().addAll(left, center, right); split.setDividerPositions(0.18,0.75);
        return split;
    }

    private void setupCanvasInteraction() {
        canvas.setOnScroll(e -> { zoom = Math.max(0.15, Math.min(6.0, zoom + (e.getDeltaY()>0?0.1:-0.1))); render(); });
        canvas.setOnMousePressed(e -> { lastX=e.getX(); lastY=e.getY(); if(e.getButton()==MouseButton.PRIMARY){selectedNode = hitTest(e.getX(),e.getY()); objectList.getSelectionModel().select(selectedNode); syncInspector(); render();} });
        canvas.setOnMouseDragged(e -> {
            if (e.getButton()==MouseButton.MIDDLE || e.isSecondaryButtonDown()) { panX += e.getX()-lastX; panY += e.getY()-lastY; }
            else if (selectedNode!=null && e.isPrimaryButtonDown()) { selectedNode.x += (e.getX()-lastX)/zoom; selectedNode.y += (e.getY()-lastY)/zoom; }
            lastX=e.getX(); lastY=e.getY(); render(); refresh();
        });
    }

    private void render() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.WHITE); g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
        g.setStroke(Color.web("#e8e8e8"));
        for(int x=0;x<canvas.getWidth();x+=50) g.strokeLine(x,0,x,canvas.getHeight());
        for(int y=0;y<canvas.getHeight();y+=50) g.strokeLine(0,y,canvas.getWidth(),y);
        g.save(); g.translate(panX,panY); g.scale(zoom,zoom);
        state.nodes.stream().filter(n -> layerById(n.layerId).visible && n.visible).forEach(n -> {
            g.save(); g.setGlobalAlpha(n.opacity);
            if(n.type==NodeType.IMAGE){
                Image img = imageCache.get(n.content);
                if(img!=null) g.drawImage(img,n.x,n.y,n.width,n.height);
                else { g.setFill(Color.LIGHTBLUE); g.fillRect(n.x,n.y,n.width,n.height); g.setFill(Color.BLACK); g.fillText("Missing image",n.x+8,n.y+18); }
            } else if(n.type==NodeType.TEXT){
                g.setFill(Color.web("#222")); g.setFont(Font.font(n.fontSize)); g.fillText(n.content==null?"":n.content,n.x,n.y);
            }
            if(n==selectedNode){ g.setStroke(Color.RED); g.strokeRect(n.x-4,n.y-20,n.width+8,n.height+24); }
            g.restore();
        });
        g.restore();
    }

    private AtlasNode hitTest(double sx,double sy){
        double wx=(sx-panX)/zoom, wy=(sy-panY)/zoom;
        for(int i=state.nodes.size()-1;i>=0;i--){ AtlasNode n=state.nodes.get(i); if(wx>=n.x && wx<=n.x+n.width && wy>=n.y-20 && wy<=n.y+n.height) return n; }
        return null;
    }

    private void addImageNode(Stage stage){
        FileChooser fc = new FileChooser(); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images","*.png","*.jpg","*.jpeg","*.webp"));
        File file = fc.showOpenDialog(stage); if(file==null) return;
        Image image = new Image(file.toURI().toString()); imageCache.put(file.getAbsolutePath(), image);
        AtlasNode n = new AtlasNode(); n.type=NodeType.IMAGE; n.layerId = activeLayer().id; n.content=file.getAbsolutePath(); n.width=Math.max(120,Math.min(600,image.getWidth())); n.height=n.width*(image.getHeight()/Math.max(1,image.getWidth())); n.x=120; n.y=120;
        state.nodes.add(n); refresh(); render();
    }

    private void saveProject(Stage stage){ try{ FileChooser fc=new FileChooser(); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Atlas","*.atlaslib")); File f=fc.showSaveDialog(stage); if(f==null)return; Path t=Files.createTempDirectory("atlas-save"); Path db=t.resolve("project.sqlite"); store.save(db,state); bundles.writeManifestAndDb(f.toPath(),db);}catch(Exception ex){ex.printStackTrace();}}
    private void openProject(Stage stage){ try{ FileChooser fc=new FileChooser(); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Atlas","*.atlaslib")); File f=fc.showOpenDialog(stage); if(f==null)return; Path t=Files.createTempDirectory("atlas-open"); ProjectState p=store.load(bundles.extractDb(f.toPath(),t)); state.layers.clear();state.layers.addAll(p.layers);state.nodes.clear();state.nodes.addAll(p.nodes);state.articles.clear();state.articles.addAll(p.articles);state.anchors.clear();state.anchors.addAll(p.anchors); refresh(); render(); }catch(Exception ex){ex.printStackTrace();}}

    private HBox statusBar(){ HBox b = new HBox(16,new Label("Atlas "+ AppVersion.VALUE),new Label("Nodes: "+state.nodes.size()),new Label("Zoom: "+(int)(zoom*100)+"%")); b.setPadding(new Insets(8)); b.getStyleClass().add("status-bar"); return b; }
    private void bindLists(){ layersList.setCellFactory(v->new ListCell<>(){ @Override protected void updateItem(Layer item, boolean empty){super.updateItem(item, empty); setText(empty||item==null?"":item.name+(item.visible?"":" (hidden)"));}}); objectList.setCellFactory(v->new ListCell<>(){ @Override protected void updateItem(AtlasNode item, boolean empty){super.updateItem(item, empty); setText(empty||item==null?"":item.type+" @ "+(int)item.x+","+(int)item.y);}}); objectList.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{selectedNode=b; syncInspector(); render();}); }
    private void syncInspector(){ if(selectedNode==null){nodeText.setText(""); return;} nodeOpacity.setValue(selectedNode.opacity); nodeText.setText(selectedNode.content==null?"":selectedNode.content); fontSize.getValueFactory().setValue((int)selectedNode.fontSize); }
    private void refresh(){ layersList.getItems().setAll(state.layers); objectList.getItems().setAll(state.nodes); }
    private Layer activeLayer(){ Layer s=layersList.getSelectionModel().getSelectedItem(); return s!=null?s:state.layers.getFirst(); }
    private Layer layerById(String id){ return state.layers.stream().filter(l->l.id.equals(id)).findFirst().orElse(state.layers.getFirst()); }
    private ProjectState bootstrap(){ ProjectState s=new ProjectState(); s.layers.add(new Layer("Base",0)); return s; }
}
