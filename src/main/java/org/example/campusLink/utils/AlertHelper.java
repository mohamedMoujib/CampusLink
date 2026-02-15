package org.example.campusLink.utils;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class AlertHelper {

    public enum AlertType {
        SUCCESS, ERROR, WARNING, INFO
    }

    /**
     * Affiche une petite alerte mignonne et centrée
     */
    public static void showAlert(StackPane root, String message, AlertType type) {
        // Créer le conteneur de l'alerte (PETIT et CUTE!)
        HBox alertBox = new HBox(12);
        alertBox.setAlignment(Pos.CENTER);

        // Taille compacte
        alertBox.setMinHeight(50);
        alertBox.setMaxHeight(50);
        alertBox.setMinWidth(280);
        alertBox.setMaxWidth(350);

        alertBox.setStyle(getStyleForType(type));

        // Ajouter l'effet d'ombre douce
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.12));
        shadow.setRadius(12);
        shadow.setOffsetY(3);
        alertBox.setEffect(shadow);

        // Icône petite et mignonne
        StackPane iconContainer = new StackPane();
        iconContainer.setStyle(getIconContainerStyle(type));
        iconContainer.setPrefSize(32, 32);
        iconContainer.setMaxSize(32, 32);
        iconContainer.setMinSize(32, 32);

        Label icon = new Label(getIconForType(type));
        icon.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: bold;");
        iconContainer.getChildren().add(icon);

        // Message compact
        Label messageLabel = new Label(message);
        messageLabel.setStyle(
                "-fx-text-fill: #1F2937; " +
                        "-fx-font-size: 13px; " +
                        "-fx-font-weight: 600;"
        );
        messageLabel.setMaxWidth(250);

        alertBox.getChildren().addAll(iconContainer, messageLabel);

        // CENTRER l'alerte au milieu de l'écran!
        StackPane.setAlignment(alertBox, Pos.CENTER);

        // Ajouter à la scène
        root.getChildren().add(alertBox);

        // Animation d'entrée (scale + fade in) - Effet "pop"
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), alertBox);
        scaleIn.setFromX(0.8);
        scaleIn.setFromY(0.8);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        scaleIn.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), alertBox);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ParallelTransition entrance = new ParallelTransition(scaleIn, fadeIn);

        // Animation de sortie (scale + fade out)
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(250), alertBox);
        scaleOut.setFromX(1.0);
        scaleOut.setFromY(1.0);
        scaleOut.setToX(0.9);
        scaleOut.setToY(0.9);
        scaleOut.setDelay(Duration.seconds(2.5)); // Reste visible 2.5 secondes
        scaleOut.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), alertBox);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(2.5));

        ParallelTransition exit = new ParallelTransition(scaleOut, fadeOut);

        // Supprimer après l'animation
        exit.setOnFinished(e -> root.getChildren().remove(alertBox));

        // Lancer les animations
        entrance.play();
        entrance.setOnFinished(e -> exit.play());
    }

    private static String getStyleForType(AlertType type) {
        String baseStyle =
                "-fx-background-radius: 25; " +      // Bordures très arrondies (cute!)
                        "-fx-padding: 12 18; " +
                        "-fx-border-radius: 25; " +
                        "-fx-border-width: 1.5; ";

        switch (type) {
            case SUCCESS:
                return baseStyle +
                        "-fx-background-color: #F0FDF4; " +
                        "-fx-border-color: #86EFAC;";
            case ERROR:
                return baseStyle +
                        "-fx-background-color: #FEF2F2; " +
                        "-fx-border-color: #FECACA;";
            case WARNING:
                return baseStyle +
                        "-fx-background-color: #FFFBEB; " +
                        "-fx-border-color: #FDE68A;";
            case INFO:
            default:
                return baseStyle +
                        "-fx-background-color: #EEF2FF; " +
                        "-fx-border-color: #C7D2FE;";
        }
    }

    private static String getIconContainerStyle(AlertType type) {
        String baseStyle =
                "-fx-background-radius: 16; " +      // Petit cercle mignon
                        "-fx-alignment: center; ";

        switch (type) {
            case SUCCESS:
                return baseStyle + "-fx-background-color: #22C55E;";
            case ERROR:
                return baseStyle + "-fx-background-color: #EF4444;";
            case WARNING:
                return baseStyle + "-fx-background-color: #F59E0B;";
            case INFO:
            default:
                return baseStyle + "-fx-background-color: #5D5FEF;";
        }
    }

    private static String getIconForType(AlertType type) {
        switch (type) {
            case SUCCESS:
                return "✓";
            case ERROR:
                return "✕";
            case WARNING:
                return "!";
            case INFO:
            default:
                return "i";
        }
    }
}