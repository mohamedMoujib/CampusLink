package org.example.campusLink.utils;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class AlertHelper {

    public enum AlertType {
        SUCCESS, ERROR, WARNING, INFO
    }

    // ── TOAST ALERT ───────────────────────────────────────────────────────────

    public static void showAlert(StackPane root, String message, AlertType type) {
        HBox alertBox = new HBox(12);
        alertBox.setAlignment(Pos.CENTER);
        alertBox.setMinHeight(50);
        alertBox.setMaxHeight(50);
        alertBox.setMinWidth(280);
        alertBox.setMaxWidth(350);
        alertBox.setStyle(getStyleForType(type));

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.12));
        shadow.setRadius(12);
        shadow.setOffsetY(3);
        alertBox.setEffect(shadow);

        StackPane iconContainer = new StackPane();
        iconContainer.setStyle(getIconContainerStyle(type));
        iconContainer.setPrefSize(32, 32);
        iconContainer.setMaxSize(32, 32);
        iconContainer.setMinSize(32, 32);

        Label icon = new Label(getIconForType(type));
        icon.setStyle("-fx-font-size:16px; -fx-text-fill:white; -fx-font-weight:bold;");
        iconContainer.getChildren().add(icon);

        Label messageLabel = new Label(message);
        messageLabel.setStyle(
                "-fx-text-fill:#1F2937; -fx-font-size:13px; -fx-font-weight:600;"
        );
        messageLabel.setMaxWidth(250);

        alertBox.getChildren().addAll(iconContainer, messageLabel);
        StackPane.setAlignment(alertBox, Pos.CENTER);
        root.getChildren().add(alertBox);

        // Entrance
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), alertBox);
        scaleIn.setFromX(0.8); scaleIn.setFromY(0.8);
        scaleIn.setToX(1.0);   scaleIn.setToY(1.0);
        scaleIn.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), alertBox);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);

        ParallelTransition entrance = new ParallelTransition(scaleIn, fadeIn);

        // Exit
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(250), alertBox);
        scaleOut.setFromX(1.0); scaleOut.setFromY(1.0);
        scaleOut.setToX(0.9);   scaleOut.setToY(0.9);
        scaleOut.setDelay(Duration.seconds(2.5));
        scaleOut.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), alertBox);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(2.5));

        ParallelTransition exit = new ParallelTransition(scaleOut, fadeOut);
        exit.setOnFinished(e -> root.getChildren().remove(alertBox));

        entrance.play();
        entrance.setOnFinished(e -> exit.play());
    }

    // ── CONFIRM DIALOG ────────────────────────────────────────────────────────

    public static void showConfirm(StackPane root, String title,
                                   String message, Runnable onConfirm) {
        // Dim overlay
        VBox overlay = new VBox();
        overlay.setStyle("-fx-background-color:rgba(0,0,0,0.45);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Card
        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(360);
        card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:20;" +
                        "-fx-padding:28 32;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),20,0,0,6);"
        );

        Label icon = new Label("❓");
        icon.setStyle("-fx-font-size:36px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#111827;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(300);
        titleLabel.setAlignment(Pos.CENTER);

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#6b7280;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(300);
        msgLabel.setAlignment(Pos.CENTER);

        Button btnConfirm = new Button("Confirmer");
        btnConfirm.setStyle(
                "-fx-background-color:#4f46e5; -fx-text-fill:white;" +
                        "-fx-font-weight:bold; -fx-background-radius:20;" +
                        "-fx-padding:9 28; -fx-cursor:hand; -fx-font-size:13px;"
        );

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle(
                "-fx-background-color:#f3f4f6; -fx-text-fill:#374151;" +
                        "-fx-font-weight:bold; -fx-background-radius:20;" +
                        "-fx-padding:9 28; -fx-cursor:hand; -fx-font-size:13px;"
        );

        HBox buttons = new HBox(12, btnCancel, btnConfirm);
        buttons.setAlignment(Pos.CENTER);

        card.getChildren().addAll(icon, titleLabel, msgLabel, buttons);
        overlay.getChildren().add(card);
        root.getChildren().add(overlay);

        // Entrance animation
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(250), card);
        scaleIn.setFromX(0.85); scaleIn.setFromY(0.85);
        scaleIn.setToX(1.0);    scaleIn.setToY(1.0);
        scaleIn.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), overlay);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        new ParallelTransition(scaleIn, fadeIn).play();

        // Close helper
        Runnable close = () -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(180), overlay);
            fadeOut.setFromValue(1); fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> root.getChildren().remove(overlay));
            fadeOut.play();
        };

        btnCancel.setOnAction(e -> close.run());
        btnConfirm.setOnAction(e -> {
            close.run();
            onConfirm.run();
        });
    }

    // ── STYLE HELPERS ─────────────────────────────────────────────────────────

    private static String getStyleForType(AlertType type) {
        String base =
                "-fx-background-radius:25; -fx-padding:12 18;" +
                        "-fx-border-radius:25; -fx-border-width:1.5; ";
        return switch (type) {
            case SUCCESS -> base +
                    "-fx-background-color:#F0FDF4; -fx-border-color:#86EFAC;";
            case ERROR   -> base +
                    "-fx-background-color:#FEF2F2; -fx-border-color:#FECACA;";
            case WARNING -> base +
                    "-fx-background-color:#FFFBEB; -fx-border-color:#FDE68A;";
            default      -> base +
                    "-fx-background-color:#EEF2FF; -fx-border-color:#C7D2FE;";
        };
    }

    private static String getIconContainerStyle(AlertType type) {
        String base = "-fx-background-radius:16; -fx-alignment:center; ";
        return switch (type) {
            case SUCCESS -> base + "-fx-background-color:#22C55E;";
            case ERROR   -> base + "-fx-background-color:#EF4444;";
            case WARNING -> base + "-fx-background-color:#F59E0B;";
            default      -> base + "-fx-background-color:#5D5FEF;";
        };
    }

    private static String getIconForType(AlertType type) {
        return switch (type) {
            case SUCCESS -> "✓";
            case ERROR   -> "✕";
            case WARNING -> "!";
            default      -> "i";
        };
    }
}