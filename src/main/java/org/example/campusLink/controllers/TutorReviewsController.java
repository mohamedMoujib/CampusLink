package org.example.campusLink.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.campusLink.Services.ReviewsService;
import org.example.campusLink.Services.TranslationService;
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

    @FXML private VBox reviewsContainer;
    @FXML private Label tutorName;
    @FXML private Label tutorEmail;
    @FXML private Label averageRatingLabel;
    @FXML private Label totalReviewsLabel;
    @FXML private Label trustPointsLabel;
    @FXML private Label monthReviewsLabel;
    @FXML private ComboBox<String> languageSelector;

    private ReviewsService reviewsService;
    private TranslationService translationService;

    private final int tutorId = 2;
    private List<Reviews> allReviews;
    private List<Reviews> filteredReviews;
    private String currentLanguage = "fr";

    @FXML
    public void initialize() {
        reviewsService = new ReviewsService();
        translationService = new TranslationService();
        setupLanguageSelector();
        loadUserInfo();
        loadReviews();
        updateStatistics();
    }

    private void setupLanguageSelector() {
        languageSelector.setItems(FXCollections.observableArrayList(
                "🇫🇷 Français", "🇹🇳 العربية", "🇬🇧 English"
        ));
        languageSelector.setValue("🇫🇷 Français");
        languageSelector.setOnAction(e -> {
            String selected = languageSelector.getValue();
            if (selected.contains("Français"))   currentLanguage = "fr";
            else if (selected.contains("العربية")) currentLanguage = "ar";
            else if (selected.contains("English")) currentLanguage = "en";
            displayReviews(filteredReviews);
        });
    }

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
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadReviews() {
        allReviews = reviewsService.getReviewsByTutor(tutorId);
        filteredReviews = allReviews;
        displayReviews(filteredReviews);
    }

    private void displayReviews(List<Reviews> reviews) {
        reviewsContainer.getChildren().clear();
        if (reviews.isEmpty()) {
            Label empty = new Label("Aucun avis pour le moment");
            empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
            reviewsContainer.getChildren().add(empty);
            return;
        }
        for (Reviews r : reviews)
            reviewsContainer.getChildren().add(createReviewCard(r));
    }

    private VBox createReviewCard(Reviews review) {
        VBox card = new VBox(12);
        card.getStyleClass().add("review-card");

        // === HEADER ===
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
        int absRating   = Math.abs(review.getRating());
        int clamped     = Math.min(absRating, 5);
        String stars    = "★".repeat(clamped) + "☆".repeat(5 - clamped);
        String starColor = review.getRating() < 0 ? "#ef4444" : "#fbbf24";
        Label starsLabel = new Label(stars);
        starsLabel.setStyle("-fx-font-size: 22px; -fx-text-fill: " + starColor + "; -fx-padding: 5 0 5 0;");

        // === COMMENTAIRE + BOUTON TRADUIRE ===
        VBox commentSection = new VBox(8);

        // Commentaire original
        Label originalComment = new Label(review.getComment());
        originalComment.setWrapText(true);
        originalComment.getStyleClass().add("review-comment");

        // Zone traduction (cachée par défaut)
        VBox translationBox = new VBox(4);
        translationBox.setVisible(false);
        translationBox.setManaged(false);
        translationBox.setStyle("-fx-background-color: #f5f3ff; -fx-padding: 10; -fx-background-radius: 8;");

        Label sepLabel = new Label(TranslationService.getLanguageFlag(currentLanguage)
                + "  Traduction en " + TranslationService.getLanguageName(currentLanguage));
        sepLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7c3aed; -fx-font-weight: bold;");

        Label translatedLabel = new Label();
        translatedLabel.setWrapText(true);
        translatedLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #1f2937;");

        translationBox.getChildren().addAll(sepLabel, translatedLabel);

        // Bouton traduire — inline à droite du commentaire
        HBox commentRow = new HBox(10);
        commentRow.setAlignment(Pos.TOP_LEFT);

        originalComment.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(originalComment, Priority.ALWAYS);

        Button translateBtn = new Button();
        translateBtn.setStyle(styleBtnTranslate());
        translateBtn.setText(TranslationService.getLanguageFlag(currentLanguage) + " Traduire");

        // Cacher le bouton si langue = fr
        boolean isFr = currentLanguage.equals("fr");
        translateBtn.setVisible(!isFr);
        translateBtn.setManaged(!isFr);

        commentRow.getChildren().addAll(originalComment, translateBtn);

        // État local du bouton
        final boolean[] translated = {false};
        final boolean[] loading    = {false};

        translateBtn.setOnAction(e -> {
            if (loading[0]) return;

            if (translated[0]) {
                // Masquer
                translationBox.setVisible(false);
                translationBox.setManaged(false);
                translated[0] = false;
                translateBtn.setText(TranslationService.getLanguageFlag(currentLanguage) + " Traduire");
                translateBtn.setStyle(styleBtnTranslate());
            } else {
                // Traduire
                loading[0] = true;
                translateBtn.setText("⏳");
                translateBtn.setDisable(true);

                new Thread(() -> {
                    String result = translationService.translate(review.getComment(), currentLanguage);
                    javafx.application.Platform.runLater(() -> {
                        translatedLabel.setText(result);

                        // ✅ RTL pour l'arabe
                        if (currentLanguage.equals("ar")) {
                            translatedLabel.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                            sepLabel.setText("🇹🇳  ترجمة إلى العربية");
                        } else {
                            translatedLabel.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
                            sepLabel.setText(TranslationService.getLanguageFlag(currentLanguage)
                                    + "  Traduction en " + TranslationService.getLanguageName(currentLanguage));
                        }

                        translationBox.setVisible(true);
                        translationBox.setManaged(true);
                        translated[0] = true;
                        loading[0]    = false;
                        translateBtn.setDisable(false);
                        translateBtn.setText("✕ Masquer");
                        translateBtn.setStyle(styleBtnHide());
                    });
                }).start();
            }
        });

        commentSection.getChildren().addAll(commentRow, translationBox);

        // === BADGE ===
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getChildren().add(buildBadge(review.getRating()));

        card.getChildren().addAll(header, starsLabel, commentSection, footer);
        return card;
    }

    private Label buildBadge(int rating) {
        Label badge = new Label();
        if      (rating == 5) { badge.setText("⭐ Excellent");    badge.setStyle("-fx-background-color:#dcfce7;-fx-text-fill:#16a34a;-fx-padding:4 12;-fx-background-radius:12;-fx-font-size:12px;-fx-font-weight:bold;"); }
        else if (rating == 4) { badge.setText("👍 Très bien");    badge.setStyle("-fx-background-color:#dbeafe;-fx-text-fill:#2563eb;-fx-padding:4 12;-fx-background-radius:12;-fx-font-size:12px;-fx-font-weight:bold;"); }
        else if (rating == 3) { badge.setText("😊 Bien");         badge.setStyle("-fx-background-color:#fef3c7;-fx-text-fill:#d97706;-fx-padding:4 12;-fx-background-radius:12;-fx-font-size:12px;-fx-font-weight:bold;"); }
        else if (rating <  0) { badge.setText("👎 Avis négatif"); badge.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;-fx-padding:4 12;-fx-background-radius:12;-fx-font-size:12px;-fx-font-weight:bold;"); }
        else                  { badge.setText("⚠️ À améliorer");  badge.setStyle("-fx-background-color:#fee2e2;-fx-text-fill:#dc2626;-fx-padding:4 12;-fx-background-radius:12;-fx-font-size:12px;-fx-font-weight:bold;"); }
        return badge;
    }

    private String styleBtnTranslate() {
        return "-fx-background-color:#ede9fe;-fx-text-fill:#7c3aed;" +
                "-fx-font-size:11px;-fx-padding:4 10;-fx-background-radius:6;-fx-cursor:hand;";
    }

    private String styleBtnHide() {
        return "-fx-background-color:#f3f4f6;-fx-text-fill:#6b7280;" +
                "-fx-font-size:11px;-fx-padding:4 10;-fx-background-radius:6;-fx-cursor:hand;";
    }

    private void updateStatistics() {
        if (allReviews.isEmpty()) {
            averageRatingLabel.setText("0.0");
            totalReviewsLabel.setText("0");
            monthReviewsLabel.setText("0");
        } else {
            double avg = allReviews.stream().mapToInt(Reviews::getRating).average().orElse(0.0);
            averageRatingLabel.setText(String.format("%.1f", avg));
            totalReviewsLabel.setText(String.valueOf(allReviews.size()));
            monthReviewsLabel.setText("5");
        }
        trustPointsLabel.setText(String.valueOf(reviewsService.getTrustPoints(tutorId)));
    }

    @FXML private void filterAll()     { filteredReviews = allReviews; displayReviews(filteredReviews); }
    @FXML private void filter5Stars()  { filter(r -> r.getRating() == 5); }
    @FXML private void filter4Stars()  { filter(r -> r.getRating() == 4); }
    @FXML private void filter3Stars()  { filter(r -> r.getRating() == 3); }
    @FXML private void filterLow()     { filter(r -> r.getRating() <= 2); }

    private void filter(java.util.function.Predicate<Reviews> pred) {
        filteredReviews = allReviews.stream().filter(pred).collect(Collectors.toList());
        displayReviews(filteredReviews);
    }

    @FXML
    private void goToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/TutorDashboardView.fxml"));
            reviewsContainer.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}