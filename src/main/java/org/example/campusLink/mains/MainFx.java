package org.example.campusLink.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFx extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        Parent root = FXMLLoader.load(
                getClass().getResource("/Views/Login.fxml")
        );

        Scene scene = new Scene(root);

        primaryStage.setTitle("CampusLink - Plateforme de Services Étudiants");
        primaryStage.setScene(scene);

        primaryStage.setMaximized(true); // Full window
        primaryStage.setResizable(true);

        primaryStage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}