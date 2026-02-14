package org.example.campusLink.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.campusLink.Services.ReviewsService;
import org.example.campusLink.entities.Reviews;
import org.example.campusLink.units.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class TutorReviewsController {

    @FXML
    private VBox reviewsContainer;

    @FXML
    private Label tutorName;

    @FXML
    private Label tutorEmail;

    @FXML
    private Label averageRatingLabel;

    @FXML
    private Label totalReviewsLabel;

    @FXML
    private Label trustPointsLabel;

    @FXML
    private Label monthReviewsLabel;

    private ReviewsService reviewsService;

    // ⚠️ Simulation tuteur connecté
    private final int tutorId = 2;

    private List<Reviews> allReviews;
    private List<Reviews> filteredReviews;

    @FXML
    public void initialize() {
        reviewsService = new ReviewsService();

        // 🔥 CHARGER LES INFOS DU TUTEUR
        loadUserInfo();

        loadReviews();
        updateStatistics();
    }

    // ===================== LOAD USER INFO =====================

    private void loadUserInfo() {
        String sql = "SELECT name, email FROM users WHERE id = ?";

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tutorId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    tutorName.setText(rs.getString("name"));
                    tutorEmail.setText(rs.getString("email"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===================== LOAD REVIEWS =====================

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

        // === HEADER (Service + Date) ===
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

        // === STARS ===
        String starsText = "★".repeat(review.getRating()) + "☆".repeat(5 - review.getRating());
        Label starsLabel = new Label(starsText);
        starsLabel.setStyle("-fx-font-size: 22px; -fx-text-fill: #fbbf24; -fx-padding: 5 0 5 0;");

        // === COMMENT ===
        Label comment = new Label(review.getComment());
        comment.setWrapText(true);
        comment.getStyleClass().add("review-comment");

        // === FOOTER ===
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);

        // Badge selon la note
        Label badge = new Label();
        if (review.getRating() == 5) {
            badge.setText("⭐ Excellent");
            badge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: bold;");
        } else if (review.getRating() == 4) {
            badge.setText("👍 Très bien");
            badge.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #2563eb; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: bold;");
        } else if (review.getRating() == 3) {
            badge.setText("😊 Bien");
            badge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #d97706; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: bold;");
        } else {
            badge.setText("⚠️ À améliorer");
            badge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: bold;");
        }

        footer.getChildren().add(badge);

        card.getChildren().addAll(header, starsLabel, comment, footer);
        return card;
    }

    // ===================== STATISTICS =====================

    private void updateStatistics() {
        if (allReviews.isEmpty()) {
            averageRatingLabel.setText("0.0");
            totalReviewsLabel.setText("0");
            monthReviewsLabel.setText("0");
        } else {
            // Note moyenne
            double average = allReviews.stream()
                    .mapToInt(Reviews::getRating)
                    .average()
                    .orElse(0.0);
            averageRatingLabel.setText(String.format("%.1f", average));

            // Total
            totalReviewsLabel.setText(String.valueOf(allReviews.size()));

            // Ce mois (simulation)
            monthReviewsLabel.setText("5");
        }

        // 🔥 RÉCUPÉRER LES POINTS DE CONFIANCE
        int trustPoints = reviewsService.getTrustPoints(tutorId);
        trustPointsLabel.setText(String.valueOf(trustPoints));
    }

    // ===================== FILTERS =====================

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
}