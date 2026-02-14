package org.example.campusLink.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


import java.io.IOException;

public class MainFX extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/jarreb.fxml"));
        try{
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/PaymentForm.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("Campus Link");
            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
