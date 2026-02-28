package org.example.campusLink.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainFX extends Application {

    public static void main(String[] args) {
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("com.sun.webkit.useHTTP2Loader", "false");
        launch(args);
    }

    @Override

    public void start(Stage primaryStage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/View/Payment.fxml"));
            Parent root = fxmlLoader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/Style/PaymentForm.css").toExternalForm());

            primaryStage.setScene(scene);
            primaryStage.setTitle("Campus Link");
            primaryStage.setMaximized(true);
            System.setProperty("prism.order", "sw");
            System.out.println(getClass().getResource("/map/map.html"));

            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}