package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxmlFiles/mainScreen.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        Image icon = new Image(getClass().getResourceAsStream("/uiIcons/appIcon.png"));
        stage.getIcons().add(icon);
        stage.setResizable(false);
        stage.show();
    }
}