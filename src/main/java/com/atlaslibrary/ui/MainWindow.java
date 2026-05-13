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
    private final ProjectState state = bootstrap(); private final SqliteProjectStore store = new SqliteProjectStore(); private final AtlasBundleService bundles = new AtlasBundleService(); private final MarkdownService markdown = new MarkdownService();
    private final Map<String, Image> imageCache = new HashMap<>(); private final Canvas canvas = new Canvas(1600, 950);
    private final ListView<Layer> layersList = new ListView<>(); private final ListView<AtlasNode> objectList = new ListView<>(); private final ListView<Article> articleList = new ListView<>();
    private final TextArea markdownRaw = new TextArea(); private final TextArea markdownPreview = new TextArea(); private final TextField nodeText = new TextField(); private final Spinner<Integer> fontSize = new Spinner<>(8,72,18);
    private AtlasNode selectedNode; private double zoom=1,panX=0,panY=0,lastX,lastY;

    public Parent buildRoot(Stage stage){ BorderPane root=new BorderPane(); root.getStyleClass().add("root-pane"); root.setTop(top(stage)); root.setCenter(centerViews(stage)); root.setBottom(status()); bind(); refresh(); render(); return root; }

    private Parent top(Stage stage){ Label brand = new Label("Atlas Library"); brand.getStyleClass().add("brand");
        ToggleButton codex = new ToggleButton("Codex"); ToggleButton map = new ToggleButton("Map"); codex.getStyleClass().add("tab-toggle"); map.getStyleClass().add("tab-toggle"); ToggleGroup tg = new ToggleGroup(); codex.setToggleGroup(tg); map.setToggleGroup(tg); codex.setSelected(true);
        StackPane codexView = buildCodexView(); StackPane mapView = buildMapView(stage); codex.setOnAction(e->{codexView.setVisible(true); mapView.setVisible(false);}); map.setOnAction(e->{codexView.setVisible(false); mapView.setVisible(true);});
        HBox right = new HBox(8,codex,map); HBox bar = new HBox(20,brand,new Region(),right); HBox.setHgrow(bar.getChildren().get(1), Priority.ALWAYS); bar.setPadding(new Insets(10,16,10,16)); bar.getStyleClass().add("top-bar");
        rootCenter = new StackPane(codexView,mapView); mapView.setVisible(false); container= rootCenter; return bar; }

    private StackPane rootCenter; private Parent container;
    private Parent centerViews(Stage s){ return container; }

    private StackPane buildCodexView(){
        VBox left = new VBox(8,new Label("Entries")); left.getChildren().getFirst().getStyleClass().add("section-title"); left.getChildren().add(articleList); left.setPadding(new Insets(10)); left.setPrefWidth(250); left.getStyleClass().add("side-panel");
        markdownPreview.setEditable(false); markdownRaw.setText("# New Article\nWrite world lore..."); markdownPreview.setText(markdown.toPreviewText(markdownRaw.getText())); markdownRaw.textProperty().addListener((o,a,b)->markdownPreview.setText(markdown.toPreviewText(b)));
        VBox main = new VBox(10,new Label("Article Editor"),markdownRaw,new Label("Preview"),markdownPreview); VBox.setVgrow(markdownRaw,Priority.ALWAYS); VBox.setVgrow(markdownPreview,Priority.ALWAYS); main.setPadding(new Insets(14));
        HBox wrap = new HBox(left,main); HBox.setHgrow(main,Priority.ALWAYS); return new StackPane(wrap);
    }

    private StackPane buildMapView(Stage stage){
        VBox left = new VBox(8,new Label("Layers"),layersList,new Label("Objects"),objectList); ((Label)left.getChildren().getFirst()).getStyleClass().add("section-title"); left.setPadding(new Insets(10)); left.setPrefWidth(260); left.getStyleClass().add("side-panel");
        Button addLayer = new Button("Add Layer"); addLayer.setOnAction(e->{state.layers.add(new Layer("Layer "+(state.layers.size()+1),state.layers.size())); refresh();});
        Button addText = new Button("Add Text"); addText.setOnAction(e->{AtlasNode n=new AtlasNode(); n.type=NodeType.TEXT; n.layerId=activeLayer().id; n.content="Editable text"; n.x=140; n.y=120; state.nodes.add(n); refresh(); render();});
        Button addImage = new Button("Add Image"); addImage.getStyleClass().add("primary-btn"); addImage.setOnAction(e->addImageNode(stage));
        Button save = new Button("Save"); save.setOnAction(e->save(stage)); Button open = new Button("Open"); open.setOnAction(e->open(stage));
        left.getChildren().addAll(addLayer,addText,addImage,save,open);

        StackPane center = new StackPane(canvas); center.getStyleClass().add("viewport"); setupCanvas();

        VBox right = new VBox(8,new Label("Inspector"),nodeText,new Label("Font Size"),fontSize); right.setPadding(new Insets(10)); right.setPrefWidth(250); right.getStyleClass().add("side-panel");
        HBox map = new HBox(left,center,right); HBox.setHgrow(center,Priority.ALWAYS); return new StackPane(map);
    }

    private void setupCanvas(){ canvas.setOnScroll(e->{zoom=Math.max(0.2,Math.min(6,zoom+(e.getDeltaY()>0?0.1:-0.1)));render();}); canvas.setOnMousePressed(e->{lastX=e.getX();lastY=e.getY(); if(e.getButton()==MouseButton.PRIMARY){selectedNode=hit(e.getX(),e.getY()); objectList.getSelectionModel().select(selectedNode); sync(); render();}}); canvas.setOnMouseDragged(e->{ if(e.isMiddleButtonDown()||e.isSecondaryButtonDown()){panX+=e.getX()-lastX; panY+=e.getY()-lastY;} else if(selectedNode!=null && e.isPrimaryButtonDown()){selectedNode.x+=(e.getX()-lastX)/zoom; selectedNode.y+=(e.getY()-lastY)/zoom;} lastX=e.getX();lastY=e.getY(); render(); refresh(); });}
    private void render(){ GraphicsContext g=canvas.getGraphicsContext2D(); g.setFill(Color.WHITE); g.fillRect(0,0,canvas.getWidth(),canvas.getHeight()); g.setStroke(Color.web("#f0f0f0")); for(int x=0;x<canvas.getWidth();x+=64) g.strokeLine(x,0,x,canvas.getHeight()); for(int y=0;y<canvas.getHeight();y+=64) g.strokeLine(0,y,canvas.getWidth(),y); g.save(); g.translate(panX,panY); g.scale(zoom,zoom); for(AtlasNode n: state.nodes){ if(!layerById(n.layerId).visible) continue; if(n.type==NodeType.IMAGE){Image img=imageCache.get(n.content); if(img!=null) g.drawImage(img,n.x,n.y,n.width,n.height);} else {g.setFill(Color.web("#222")); g.setFont(Font.font(n.fontSize)); g.fillText(n.content,n.x,n.y);} if(n==selectedNode){g.setStroke(Color.web("#d4a44a")); g.strokeRect(n.x-4,n.y-20,n.width+8,n.height+24);} } g.restore(); }

    private AtlasNode hit(double sx,double sy){ double wx=(sx-panX)/zoom, wy=(sy-panY)/zoom; for(int i=state.nodes.size()-1;i>=0;i--){AtlasNode n=state.nodes.get(i); if(wx>=n.x&&wx<=n.x+n.width&&wy>=n.y-20&&wy<=n.y+n.height) return n;} return null; }
    private void addImageNode(Stage s){ FileChooser fc=new FileChooser(); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images","*.png","*.jpg","*.jpeg","*.webp")); File f=fc.showOpenDialog(s); if(f==null)return; Image im=new Image(f.toURI().toString()); imageCache.put(f.getAbsolutePath(),im); AtlasNode n=new AtlasNode(); n.type=NodeType.IMAGE; n.layerId=activeLayer().id; n.content=f.getAbsolutePath(); n.width=Math.max(140,Math.min(700,im.getWidth())); n.height=n.width*(im.getHeight()/Math.max(1,im.getWidth())); n.x=120; n.y=120; state.nodes.add(n); refresh(); render(); }

    private void bind(){ layersList.setCellFactory(v->new ListCell<>(){ @Override protected void updateItem(Layer i, boolean e){super.updateItem(i,e); setText(e||i==null?"":i.name);}}); objectList.setCellFactory(v->new ListCell<>(){ @Override protected void updateItem(AtlasNode i, boolean e){super.updateItem(i,e); setText(e||i==null?"":i.type+"  " + (int)i.x+","+(int)i.y);}}); articleList.setCellFactory(v->new ListCell<>(){ @Override protected void updateItem(Article i, boolean e){super.updateItem(i,e); setText(e||i==null?"":i.title);}});
        objectList.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{selectedNode=b; sync(); render();}); nodeText.textProperty().addListener((o,a,b)->{ if(selectedNode!=null && selectedNode.type==NodeType.TEXT){selectedNode.content=b; render(); refresh();}}); fontSize.valueProperty().addListener((o,a,b)->{ if(selectedNode!=null){selectedNode.fontSize=b; render();}});
    }
    private void sync(){ if(selectedNode==null)return; nodeText.setText(selectedNode.content==null?"":selectedNode.content); fontSize.getValueFactory().setValue((int)selectedNode.fontSize); }
    private void refresh(){ layersList.getItems().setAll(state.layers); objectList.getItems().setAll(state.nodes); if(state.articles.isEmpty()){Article a=new Article(); a.title="The Ashveil Dominion"; a.slug="ashveil-dominion"; a.markdownBody=markdownRaw.getText(); state.articles.add(a);} articleList.getItems().setAll(state.articles); }

    private void save(Stage s){ try{FileChooser fc=new FileChooser(); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Atlas","*.atlaslib")); File f=fc.showSaveDialog(s); if(f==null)return; Path t=Files.createTempDirectory("atlas-save"); Path db=t.resolve("project.sqlite"); store.save(db,state); bundles.writeManifestAndDb(f.toPath(),db);}catch(Exception ex){ex.printStackTrace();}}
    private void open(Stage s){ try{FileChooser fc=new FileChooser(); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Atlas","*.atlaslib")); File f=fc.showOpenDialog(s); if(f==null)return; Path t=Files.createTempDirectory("atlas-open"); ProjectState p=store.load(bundles.extractDb(f.toPath(),t)); state.layers.clear();state.layers.addAll(p.layers);state.nodes.clear();state.nodes.addAll(p.nodes);state.articles.clear();state.articles.addAll(p.articles); refresh(); render(); }catch(Exception ex){ex.printStackTrace();}}

    private HBox status(){ HBox h=new HBox(16,new Label("Atlas "+AppVersion.VALUE),new Label("Nodes: "+state.nodes.size())); h.setPadding(new Insets(8)); h.getStyleClass().add("status-bar"); return h; }
    private Layer activeLayer(){ Layer l=layersList.getSelectionModel().getSelectedItem(); return l!=null?l:state.layers.getFirst(); } private Layer layerById(String id){ return state.layers.stream().filter(x->x.id.equals(id)).findFirst().orElse(state.layers.getFirst()); }
    private ProjectState bootstrap(){ ProjectState p=new ProjectState(); p.layers.add(new Layer("Base",0)); return p; }
}
