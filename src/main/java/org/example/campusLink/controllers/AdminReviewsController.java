package org.example.campusLink.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.campusLink.services.ReviewsService;
import org.example.campusLink.entities.Reviews;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminReviewsController {

    @FXML
    private VBox reviewsContainer;

    @FXML
    private ComboBox<String> prestataireFilter;

    @FXML
    private ComboBox<String> ratingFilter;

    @FXML
    private TextField searchField;

    @FXML
    private Label totalReviewsLabel;

    @FXML
    private Label positiveReviewsLabel;

    @FXML
    private Label negativeReviewsLabel;

    @FXML
    private Label reportedReviewsLabel;

    private ReviewsService reviewsService;

    private List<Reviews> allReviews;
    private List<Reviews> filteredReviews;

    @FXML
    public void initialize() {
        reviewsService = new ReviewsService();

        setupFilters();
        loadAllReviews();
        updateStatistics();
    }

    private void setupFilters() {
        prestataireFilter.setPromptText("Tous les prestataires");

        ratingFilter.setItems(FXCollections.observableArrayList(
                "Toutes les notes",
                "Positives (+1 à +5)",
                "Négatives (-5 à -1)",
                "Très positives (+4 et +5)",
                "Très négatives (-5 et -4)",
                "Neutres (0)",
                "🚩 Signalés uniquement" // 🔥 Nouveau filtre
        ));
        ratingFilter.setValue("Toutes les notes");

        prestataireFilter.setOnAction(e -> applyFilters());
        ratingFilter.setOnAction(e -> applyFilters());
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
    }

    private void loadAllReviews() {
        allReviews = reviewsService.getAllReviewsWithDetails();
        filteredReviews = allReviews;

        List<String> prestataires = allReviews.stream()
                .map(Reviews::getPrestataireName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        prestataires.add(0, "Tous les prestataires");
        prestataireFilter.setItems(FXCollections.observableArrayList(prestataires));
        prestataireFilter.setValue("Tous les prestataires");

        displayReviews(filteredReviews);
    }

    private void applyFilters() {
        filteredReviews = allReviews;

        String selectedPrestataire = prestataireFilter.getValue();
        if (selectedPrestataire != null && !selectedPrestataire.equals("Tous les prestataires")) {
            filteredReviews = filteredReviews.stream()
                    .filter(r -> r.getPrestataireName().equals(selectedPrestataire))
                    .collect(Collectors.toList());
        }

        String selectedRating = ratingFilter.getValue();
        if (selectedRating != null) {
            switch (selectedRating) {
                case "Positives (+1 à +5)":
                    filteredReviews = filteredReviews.stream()
                            .filter(r -> r.getRating() > 0)
                            .collect(Collectors.toList());
                    break;
                case "Négatives (-5 à -1)":
                    filteredReviews = filteredReviews.stream()
                            .filter(r -> r.getRating() < 0)
                            .collect(Collectors.toList());
                    break;
                case "Très positives (+4 et +5)":
                    filteredReviews = filteredReviews.stream()
                            .filter(r -> r.getRating() >= 4)
                            .collect(Collectors.toList());
                    break;
                case "Très négatives (-5 et -4)":
                    filteredReviews = filteredReviews.stream()
                            .filter(r -> r.getRating() <= -4)
                            .collect(Collectors.toList());
                    break;
                case "Neutres (0)":
                    filteredReviews = filteredReviews.stream()
                            .filter(r -> r.getRating() == 0)
                            .collect(Collectors.toList());
                    break;
                case "🚩 Signalés uniquement": // 🔥 Nouveau filtre
                    filteredReviews = filteredReviews.stream()
                            .filter(Reviews::isReported)
                            .collect(Collectors.toList());
                    break;
            }
        }

        String searchText = searchField.getText().trim().toLowerCase();
        if (!searchText.isEmpty()) {
            filteredReviews = filteredReviews.stream()
                    .filter(r -> r.getComment().toLowerCase().contains(searchText) ||
                            r.getServiceTitle().toLowerCase().contains(searchText) ||
                            r.getStudentName().toLowerCase().contains(searchText) ||
                            r.getPrestataireName().toLowerCase().contains(searchText))
                    .collect(Collectors.toList());
        }

        displayReviews(filteredReviews);
        updateStatistics();
    }

    private void displayReviews(List<Reviews> reviews) {
        reviewsContainer.getChildren().clear();

        if (reviews.isEmpty()) {
            Label emptyLabel = new Label("Aucun avis trouvé");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666; -fx-padding: 40;");
            reviewsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Reviews r : reviews) {
            VBox card = createReviewCard(r);
            reviewsContainer.getChildren().add(card);
        }
    }

    private VBox createReviewCard(Reviews review) {
        VBox card = new VBox(15);
        card.getStyleClass().add("review-card");

        // 🔥 Bordure rouge pour les avis signalés
        if (review.isReported()) {
            card.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2; -fx-background-color: #fef2f2;");
        }

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox infoSection = new VBox(5);

        Label studentLabel = new Label("👤 Étudiant : " + review.getStudentName());
        studentLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #374151;");

        Label serviceLabel = new Label("📚 Service : " + review.getServiceTitle());
        serviceLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        Label prestataireLabel = new Label("🎓 Prestataire : " + review.getPrestataireName());
        prestataireLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        infoSection.getChildren().addAll(studentLabel, serviceLabel, prestataireLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox metaSection = new VBox(3);
        metaSection.setAlignment(Pos.TOP_RIGHT);

        Label idLabel = new Label("ID: " + review.getId());
        idLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");

        Label date = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        date.setStyle("-fx-font-size: 12px; -fx-text-fill: #9ca3af;");

        metaSection.getChildren().addAll(idLabel, date);

        header.getChildren().addAll(infoSection, spacer, metaSection);

        HBox starsDisplay = createStarsDisplay(review.getRating());

        // 🔥 AFFICHER LA RAISON DU SIGNALEMENT
        VBox commentSection = new VBox(5);

        if (review.isReported()) {
            HBox reportHeader = new HBox(10);
            reportHeader.setAlignment(Pos.CENTER_LEFT);
            reportHeader.setStyle("-fx-background-color: #fef3c7; -fx-padding: 10; -fx-background-radius: 6;");

            Label warningIcon = new Label("⚠️");
            warningIcon.setStyle("-fx-font-size: 18px;");

            VBox reportInfo = new VBox(3);
            Label reportTitle = new Label("SIGNALÉ PAR LE PRESTATAIRE");
            reportTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #92400e;");

            Label reportReason = new Label("Raison : " + review.getReportReason());
            reportReason.setStyle("-fx-font-size: 11px; -fx-text-fill: #92400e;");
            reportReason.setWrapText(true);

            if (review.getReportedAt() != null) {
                Label reportDate = new Label("Signalé le : " +
                        review.getReportedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
                reportDate.setStyle("-fx-font-size: 10px; -fx-text-fill: #92400e;");
                reportInfo.getChildren().addAll(reportTitle, reportReason, reportDate);
            } else {
                reportInfo.getChildren().addAll(reportTitle, reportReason);
            }

            reportHeader.getChildren().addAll(warningIcon, reportInfo);
            commentSection.getChildren().add(reportHeader);
        }

        Label commentTitle = new Label("Commentaire :");
        commentTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6b7280;");

        Label comment = new Label(review.getComment());
        comment.setWrapText(true);
        comment.getStyleClass().add("review-comment");

        commentSection.getChildren().addAll(commentTitle, comment);

        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-padding: 10 0 0 0; -fx-border-color: #e5e7eb; -fx-border-width: 1 0 0 0;");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        // 🔥 Bouton pour annuler le signalement
        if (review.isReported()) {
            Button btnUnreport = new Button("✓ Marquer comme traité");
            btnUnreport.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; " +
                    "-fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 6; " +
                    "-fx-cursor: hand;");
            btnUnreport.setOnAction(e -> unreportReview(review));
            footer.getChildren().add(btnUnreport);
        }

        Button btnView = new Button("👁 Voir détails");
        btnView.getStyleClass().add("secondary-button");
        btnView.setStyle("-fx-font-size: 12px; -fx-padding: 6 12;");
        btnView.setOnAction(e -> viewDetails(review));

        Button btnDelete = new Button("🗑 Supprimer");
        btnDelete.getStyleClass().add("link-button-danger");
        btnDelete.setStyle("-fx-font-size: 12px; -fx-padding: 6 12;");
        btnDelete.setOnAction(e -> deleteReview(review));

        footer.getChildren().addAll(footerSpacer, btnView, btnDelete);

        card.getChildren().addAll(header, starsDisplay, commentSection, footer);
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
        stars.setStyle("-fx-font-size: 20px; -fx-text-fill: " + color + ";");

        Label badgeLabel = new Label(badge);
        badgeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + "; -fx-font-weight: bold;");

        container.getChildren().addAll(stars, badgeLabel);
        return container;
    }

    private void updateStatistics() {
        totalReviewsLabel.setText(String.valueOf(filteredReviews.size()));

        long positiveCount = filteredReviews.stream()
                .filter(r -> r.getRating() > 0)
                .count();
        positiveReviewsLabel.setText(String.valueOf(positiveCount));

        long negativeCount = filteredReviews.stream()
                .filter(r -> r.getRating() < 0)
                .count();
        negativeReviewsLabel.setText(String.valueOf(negativeCount));

        // 🔥 Compte réel des avis signalés
        long reportedCount = allReviews.stream()
                .filter(Reviews::isReported)
                .count();
        reportedReviewsLabel.setText(String.valueOf(reportedCount));
    }

    // 🔥 ANNULER LE SIGNALEMENT
    private void unreportReview(Reviews review) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Marquer comme traité");
        confirmAlert.setHeaderText("Marquer ce signalement comme traité ?");
        confirmAlert.setContentText(
                "Cette action va retirer le signalement de cet avis.\n\n" +
                        "L'avis restera visible mais ne sera plus marqué comme signalé."
        );

        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                reviewsService.unreportReview(review.getId());

                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Succès");
                successAlert.setHeaderText(null);
                successAlert.setContentText("Le signalement a été marqué comme traité.");
                successAlert.showAndWait();

                loadAllReviews();
                applyFilters();

            } catch (Exception e) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Erreur");
                errorAlert.setHeaderText("Erreur lors du traitement");
                errorAlert.setContentText("Une erreur est survenue : " + e.getMessage());
                errorAlert.showAndWait();
            }
        }
    }

    private void viewDetails(Reviews review) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de l'avis");
        alert.setHeaderText("Avis #" + review.getId());

        String details = String.format("""
                Étudiant : %s
                Prestataire : %s
                Service : %s
                Note : %d
                
                Commentaire :
                %s
                
                Réservation ID : %d
                
                %s
                """,
                review.getStudentName(),
                review.getPrestataireName(),
                review.getServiceTitle(),
                review.getRating(),
                review.getComment(),
                review.getReservationId(),
                review.isReported() ?
                        "⚠️ SIGNALÉ\nRaison : " + review.getReportReason() +
                                "\nDate : " + (review.getReportedAt() != null ?
                                review.getReportedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) : "N/A")
                        : ""
        );

        alert.setContentText(details);
        alert.showAndWait();
    }

    private void deleteReview(Reviews review) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmation de suppression");
        confirmAlert.setHeaderText("Supprimer l'avis #" + review.getId() + " ?");
        confirmAlert.setContentText(
                "Êtes-vous sûr de vouloir supprimer cet avis ?\n\n" +
                        "⚠️ Cette action est irréversible !\n\n" +
                        "Étudiant : " + review.getStudentName() + "\n" +
                        "Prestataire : " + review.getPrestataireName() + "\n" +
                        "Note : " + review.getRating() +
                        (review.isReported() ? "\n\n🚩 CET AVIS EST SIGNALÉ" : "")
        );

        ButtonType buttonTypeConfirm = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonTypeCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(buttonTypeConfirm, buttonTypeCancel);

        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == buttonTypeConfirm) {
            try {
                reviewsService.deleteReview(review.getId());

                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Suppression réussie");
                successAlert.setHeaderText(null);
                successAlert.setContentText("L'avis a été supprimé avec succès.");
                successAlert.showAndWait();

                loadAllReviews();
                applyFilters();

            } catch (Exception e) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Erreur");
                errorAlert.setHeaderText("Erreur lors de la suppression");
                errorAlert.setContentText("Une erreur est survenue : " + e.getMessage());
                errorAlert.showAndWait();
            }
        }
    }

    @FXML
    private void resetFilters() {
        prestataireFilter.setValue("Tous les prestataires");
        ratingFilter.setValue("Toutes les notes");
        searchField.clear();
        applyFilters();
    }

    @FXML
    private void exportReviews() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export");
        alert.setHeaderText("Fonctionnalité à venir");
        alert.setContentText("L'export des avis sera disponible prochainement.");
        alert.showAndWait();
    }
}