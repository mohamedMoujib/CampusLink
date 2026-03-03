package org.example.campusLink.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.campusLink.utils.MyDatabase;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/reservations.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 760);
        stage.setTitle("CampusLink - Réservations");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        MyDatabase.getInstance().close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
