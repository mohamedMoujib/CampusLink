package org.example.campusLink.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.campusLink.Services.ReviewsService;
import org.example.campusLink.entities.Reviews;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.geometry.Pos;

public class TutorReviewsController {

    @FXML
    private VBox reviewsContainer;

    @FXML
    private Label averageRatingLabel;

    @FXML
    private Label totalReviewsLabel;

    @FXML
    private Label trustPointsLabel;

    @FXML
    private Label monthReviewsLabel;

    @FXML
    private Label tutorName;

    @FXML
    private Label tutorEmail;

    private ReviewsService reviewsService;

    private final int tutorId = 2;

    private List<Reviews> allReviews;
    private List<Reviews> filteredReviews;

    @FXML
    public void initialize() {
        reviewsService = new ReviewsService();

        tutorName.setText("Jean Martin");
        tutorEmail.setText("jean.martin@service.fr");

        loadReviews();
        updateStatistics();
    }

    private void loadReviews() {
        allReviews = reviewsService.getReviewsByTutor(tutorId);
        filteredReviews = allReviews;
        displayReviews(filteredReviews);
    }

    private void displayReviews(List<Reviews> reviews) {
        reviewsContainer.getChildren().clear();

        if (reviews.isEmpty()) {
            Label emptyLabel = new Label("Aucun avis pour le moment");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
            reviewsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Reviews r : reviews) {
            VBox card = createReviewCard(r);
            reviewsContainer.getChildren().add(card);
        }
    }

    private VBox createReviewCard(Reviews review) {
        VBox card = new VBox(12);
        card.getStyleClass().add("review-card");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleSection = new VBox(3);
        Label title = new Label(review.getServiceTitle());
        title.getStyleClass().add("review-title");

        Label subtitle = new Label("par " + review.getStudentName());
        subtitle.getStyleClass().add("review-subtitle");

        titleSection.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label date = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        date.getStyleClass().add("review-date");

        header.getChildren().addAll(titleSection, spacer, date);

        HBox starsDisplay = createStarsDisplay(review.getRating());

        Label comment = new Label(review.getComment());
        comment.setWrapText(true);
        comment.getStyleClass().add("review-comment");

        // 🔥 FOOTER avec bouton de signalement
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-padding: 10 0 0 0; -fx-border-color: #e5e7eb; -fx-border-width: 1 0 0 0;");

        // Badge si déjà signalé
        if (review.isReported()) {
            Label reportedBadge = new Label("⚠️ Signalé à l'admin");
            reportedBadge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; " +
                    "-fx-padding: 5 10; -fx-background-radius: 4; -fx-font-size: 11px; " +
                    "-fx-font-weight: bold;");
            footer.getChildren().add(reportedBadge);
        }

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        // Bouton signaler (désactivé si déjà signalé)
        Button btnReport = new Button(review.isReported() ? "✓ Déjà signalé" : "🚩 Signaler");
        btnReport.setDisable(review.isReported());

        if (review.isReported()) {
            btnReport.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #6b7280; " +
                    "-fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 6;");
        } else {
            btnReport.getStyleClass().add("link-button-danger");
            btnReport.setStyle("-fx-font-size: 12px; -fx-padding: 6 12;");
        }

        btnReport.setOnAction(e -> reportReview(review));

        footer.getChildren().addAll(footerSpacer, btnReport);

        card.getChildren().addAll(header, starsDisplay, comment, footer);
        return card;
    }

    private HBox createStarsDisplay(int rating) {
        HBox container = new HBox(8);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setStyle("-fx-padding: 5 0 5 0;");

        String starsText;
        String color;
        String badge;

        if (rating < 0) {
            int absRating = Math.abs(rating);
            starsText = "★".repeat(absRating) + "☆".repeat(5 - absRating);
            color = "#ef4444";
            badge = "(" + rating + ")";
        } else if (rating > 0) {
            starsText = "★".repeat(rating) + "☆".repeat(5 - rating);
            color = "#fbbf24";
            badge = "(+" + rating + ")";
        } else {
            starsText = "☆☆☆☆☆";
            color = "#d1d5db";
            badge = "(Neutre)";
        }

        Label stars = new Label(starsText);
        stars.setStyle("-fx-font-size: 22px; -fx-text-fill: " + color + ";");

        Label badgeLabel = new Label(badge);
        badgeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + "; -fx-font-weight: bold;");

        container.getChildren().addAll(stars, badgeLabel);
        return container;
    }

    // 🔥 SIGNALER UN AVIS AVEC CONTRÔLE DE SAISIE
    private void reportReview(Reviews review) {
        // Dialog pour choisir la raison du signalement
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Signaler cet avis");
        dialog.setHeaderText("Pourquoi souhaitez-vous signaler cet avis ?");

        ButtonType reportButtonType = new ButtonType("Signaler", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(reportButtonType, ButtonType.CANCEL);

        VBox content = new VBox(10);

        Label instruction = new Label("Sélectionnez une raison :");
        instruction.setStyle("-fx-font-weight: bold;");

        ToggleGroup reasonGroup = new ToggleGroup();

        RadioButton reason1 = new RadioButton("Contenu inapproprié ou offensant");
        reason1.setToggleGroup(reasonGroup);
        reason1.setSelected(true);

        RadioButton reason2 = new RadioButton("Faux avis / spam");
        reason2.setToggleGroup(reasonGroup);

        RadioButton reason3 = new RadioButton("Harcèlement ou menaces");
        reason3.setToggleGroup(reasonGroup);

        RadioButton reason4 = new RadioButton("Informations fausses ou trompeuses");
        reason4.setToggleGroup(reasonGroup);

        RadioButton reason5 = new RadioButton("Autre");
        reason5.setToggleGroup(reasonGroup);

        TextArea otherReason = new TextArea();
        otherReason.setPromptText("Précisez la raison...");
        otherReason.setPrefRowCount(3);
        otherReason.setDisable(true);

        // 🔥 Activer/désactiver le champ texte selon la sélection
        reason5.setOnAction(e -> otherReason.setDisable(!reason5.isSelected()));

        content.getChildren().addAll(
                instruction,
                reason1, reason2, reason3, reason4, reason5,
                otherReason
        );

        dialog.getDialogPane().setContent(content);

        // 🔥 VALIDATION : Désactiver le bouton "Signaler" si "Autre" est vide
        Button signalButton = (Button) dialog.getDialogPane().lookupButton(reportButtonType);

        // Validation initiale
        signalButton.setDisable(false);

        // Écouter les changements de sélection
        reasonGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == reason5) {
                // Si "Autre" est sélectionné, vérifier que le champ n'est pas vide
                signalButton.setDisable(otherReason.getText().trim().isEmpty());
            } else {
                // Si une autre option est sélectionnée, activer le bouton
                signalButton.setDisable(false);
            }
        });

        // 🔥 Écouter les changements dans le TextArea si "Autre" est sélectionné
        otherReason.textProperty().addListener((obs, oldText, newText) -> {
            if (reason5.isSelected()) {
                signalButton.setDisable(newText.trim().isEmpty());
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == reportButtonType) {
                RadioButton selected = (RadioButton) reasonGroup.getSelectedToggle();
                if (selected == reason5) {
                    String customReason = otherReason.getText().trim();
                    // 🔥 Double vérification avant de retourner
                    if (customReason.isEmpty()) {
                        return null; // Annuler si vide (normalement le bouton devrait être désactivé)
                    }
                    return customReason;
                }
                return selected.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(reason -> {
            // 🔥 Vérification finale avant d'envoyer
            if (reason == null || reason.trim().isEmpty()) {
                Alert warningAlert = new Alert(Alert.AlertType.WARNING);
                warningAlert.setTitle("Raison manquante");
                warningAlert.setHeaderText("Veuillez préciser la raison du signalement");
                warningAlert.setContentText("Vous devez indiquer une raison pour signaler cet avis.");
                warningAlert.showAndWait();
                return;
            }

            try {
                reviewsService.reportReview(review.getId(), reason);

                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Signalement envoyé");
                successAlert.setHeaderText("✓ Avis signalé avec succès");
                successAlert.setContentText(
                        "Votre signalement a été transmis à l'équipe d'administration.\n\n" +
                                "Ils examineront cet avis dans les plus brefs délais."
                );
                successAlert.showAndWait();

                loadReviews();

            } catch (Exception e) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Erreur");
                errorAlert.setHeaderText("Erreur lors du signalement");
                errorAlert.setContentText("Une erreur est survenue : " + e.getMessage());
                errorAlert.showAndWait();
            }
        });
    }

    private void updateStatistics() {
        if (allReviews.isEmpty()) {
            averageRatingLabel.setText("0.0");
            totalReviewsLabel.setText("0");
            monthReviewsLabel.setText("0");
        } else {
            double average = allReviews.stream()
                    .mapToInt(Reviews::getRating)
                    .average()
                    .orElse(0.0);
            averageRatingLabel.setText(String.format("%.1f", average));

            totalReviewsLabel.setText(String.valueOf(allReviews.size()));
            monthReviewsLabel.setText("5");
        }

        int trustPoints = reviewsService.getTrustPoints(tutorId);
        trustPointsLabel.setText(String.valueOf(trustPoints));
    }

    @FXML
    private void filterAll() {
        filteredReviews = allReviews;
        displayReviews(filteredReviews);
    }

    @FXML
    private void filter5Stars() {
        filteredReviews = allReviews.stream()
                .filter(r -> r.getRating() == 5)
                .collect(Collectors.toList());
        displayReviews(filteredReviews);
    }

    @FXML
    private void filter4Stars() {
        filteredReviews = allReviews.stream()
                .filter(r -> r.getRating() == 4)
                .collect(Collectors.toList());
        displayReviews(filteredReviews);
    }

    @FXML
    private void filter3Stars() {
        filteredReviews = allReviews.stream()
                .filter(r -> r.getRating() == 3)
                .collect(Collectors.toList());
        displayReviews(filteredReviews);
    }

    @FXML
    private void filterLow() {
        filteredReviews = allReviews.stream()
                .filter(r -> r.getRating() <= 2)
                .collect(Collectors.toList());
        displayReviews(filteredReviews);
    }
    @FXML
    private void goToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/TutorDashboardView.fxml"));
            Scene scene = reviewsContainer.getScene();
            scene.setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur de navigation");
            alert.setContentText("Impossible d'ouvrir le tableau de bord.");
            alert.showAndWait();
        }
    }
}