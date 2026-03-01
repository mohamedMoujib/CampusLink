package org.example.campusLink.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.campusLink.Services.TutorDashboardService;

import java.util.List;
import java.util.Map;

public class TutorDashboardController {

    @FXML
    private Label totalServicesLabel;

    @FXML
    private Label totalReservationsLabel;

    @FXML
    private Label totalRevenueLabel;

    @FXML
    private Label averageRatingLabel;

    @FXML
    private Label trustPointsLabel;

    @FXML
    private Label totalReviewsLabel;

    @FXML
    private LineChart<String, Number> monthlyChart;

    @FXML
    private BarChart<String, Number> topServicesChart;

    @FXML
    private PieChart ratingDistributionChart;

    @FXML
    private VBox recentReviewsContainer;

    private TutorDashboardService dashboardService;
    private final int tutorId = 2;

    @FXML
    public void initialize() {
        dashboardService = new TutorDashboardService();
        loadDashboard();
    }

    private void loadDashboard() {
        Map<String, Object> stats = dashboardService.getDashboardStats(tutorId);

        // Mettre à jour les statistiques principales
        totalServicesLabel.setText(stats.get("totalServices").toString());
        totalReservationsLabel.setText(stats.get("totalReservations").toString());
        totalRevenueLabel.setText(String.format("%.2f €", stats.get("totalRevenue")));
        averageRatingLabel.setText(String.format("%.1f", stats.get("averageRating")));
        trustPointsLabel.setText(stats.get("trustPoints").toString());
        totalReviewsLabel.setText(stats.get("totalReviews").toString());

        // Graphiques
        loadMonthlyChart((List<Map<String, Object>>) stats.get("monthlyStats"));
        loadTopServicesChart((List<Map<String, Object>>) stats.get("topServices"));
        loadRatingDistribution((Map<Integer, Integer>) stats.get("ratingDistribution"));
        loadRecentReviews((List<Map<String, Object>>) stats.get("recentReviews"));
    }

    private void loadMonthlyChart(List<Map<String, Object>> monthlyStats) {
        XYChart.Series<String, Number> reservationsSeries = new XYChart.Series<>();
        reservationsSeries.setName("Réservations");

        XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
        revenueSeries.setName("Revenus (€)");

        for (Map<String, Object> stat : monthlyStats) {
            String month = stat.get("month").toString();

            // 🔥 CORRECTION : Conversion sécurisée
            Number reservations = (Number) stat.get("reservations");
            Number revenue = (Number) stat.get("revenue");

            reservationsSeries.getData().add(new XYChart.Data<>(month, reservations));
            revenueSeries.getData().add(new XYChart.Data<>(month, revenue));
        }

        monthlyChart.getData().addAll(reservationsSeries, revenueSeries);
    }

    private void loadTopServicesChart(List<Map<String, Object>> topServices) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Réservations");

        for (Map<String, Object> service : topServices) {
            String name = service.get("name").toString();
            int bookings = (int) service.get("bookings");
            series.getData().add(new XYChart.Data<>(name, bookings));
        }

        topServicesChart.getData().add(series);
    }

    private void loadRatingDistribution(Map<Integer, Integer> distribution) {
        if (distribution.isEmpty()) {
            Label emptyLabel = new Label("Aucune donnée");
            emptyLabel.setStyle("-fx-text-fill: #6b7280;");
            return;
        }

        for (Map.Entry<Integer, Integer> entry : distribution.entrySet()) {
            if (entry.getValue() > 0) {
                String label = entry.getKey() > 0 ? "+" + entry.getKey() : String.valueOf(entry.getKey());
                ratingDistributionChart.getData().add(
                        new PieChart.Data(label + " étoiles (" + entry.getValue() + ")", entry.getValue())
                );
            }
        }
    }

    private void loadRecentReviews(List<Map<String, Object>> recentReviews) {
        recentReviewsContainer.getChildren().clear();

        if (recentReviews.isEmpty()) {
            Label emptyLabel = new Label("Aucun avis récent");
            emptyLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px; -fx-padding: 20;");
            recentReviewsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Map<String, Object> review : recentReviews) {
            HBox reviewCard = new HBox(15);
            reviewCard.setStyle("-fx-padding: 15; -fx-background-color: #f9fafb; -fx-background-radius: 8; -fx-border-color: #e5e7eb; -fx-border-width: 1; -fx-border-radius: 8;");
            reviewCard.setAlignment(Pos.CENTER_LEFT);

            // Rating
            int rating = (int) review.get("rating");
            String stars = "★".repeat(Math.abs(rating)) + "☆".repeat(5 - Math.abs(rating));
            String color = rating > 0 ? "#fbbf24" : "#ef4444";

            Label starsLabel = new Label(stars);
            starsLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: " + color + "; -fx-min-width: 100;");

            // Info
            VBox infoBox = new VBox(5);
            Label studentLabel = new Label("Par " + review.get("studentName"));
            studentLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

            Label serviceLabel = new Label("Service : " + review.get("serviceTitle"));
            serviceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

            Label commentLabel = new Label(truncateComment(review.get("comment").toString()));
            commentLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
            commentLabel.setWrapText(true);
            commentLabel.setMaxWidth(600);

            infoBox.getChildren().addAll(studentLabel, serviceLabel, commentLabel);

            reviewCard.getChildren().addAll(starsLabel, infoBox);
            recentReviewsContainer.getChildren().add(reviewCard);
        }
    }

    private String truncateComment(String comment) {
        if (comment == null) return "";
        return comment.length() > 150 ? comment.substring(0, 150) + "..." : comment;
    }

    @FXML
    private void goToReviews() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Views/Reviews/TutorReviews.fxml"));
            Scene scene = monthlyChart.getScene();
            scene.setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la navigation vers les avis: " + e.getMessage());
        }
    }
}