package com.atlaslibrary.app;

import com.atlaslibrary.ui.MainWindow;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class AtlasLibraryApp extends Application {
    @Override
    public void start(Stage stage) {
        MainWindow mainWindow = new MainWindow();
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = bounds.getWidth() * 0.70;
        double height = bounds.getHeight() * 0.70;

        Scene scene = new Scene(mainWindow.buildRoot(stage), width, height);
        scene.getStylesheets().add(getClass().getResource("/atlas-theme.css").toExternalForm());

        stage.setTitle("Atlas Library - v0.0.1ab");
        stage.setScene(scene);
        stage.setX(bounds.getMinX() + (bounds.getWidth() - width) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - height) / 2);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();
    }
}
