package com.atlaslibrary.app;

import com.atlaslibrary.ui.MainWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AtlasLibraryApp extends Application {
    @Override
    public void start(Stage stage) {
        MainWindow mainWindow = new MainWindow();
        Scene scene = new Scene(mainWindow.buildRoot(), 1500, 900);
        scene.getStylesheets().add(getClass().getResource("/atlas-theme.css").toExternalForm());

        stage.setTitle("Atlas Library - v0.0.1aa");
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(720);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
