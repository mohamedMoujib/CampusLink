package org.example.campusLink.controllers.reviews;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.campusLink.Services.reviews.ReviewsService;
import org.example.campusLink.Services.TranslationService;
import org.example.campusLink.entities.Reviews;
import org.example.campusLink.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
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

    private int tutorId;
    private List<Reviews> allReviews;
    private List<Reviews> filteredReviews;
    private String currentLanguage = "fr";

    // =========================================================
    // INIT
    // =========================================================

    @FXML
    public void initialize() {
        reviewsService = new ReviewsService();
        translationService = new TranslationService();
        setupLanguageSelector();
    }

    public void setTutorId(int id) {
        this.tutorId = id;
        loadUserInfo();
        loadReviews();
        updateStatistics();
    }

    // =========================================================
    // LANGUAGE
    // =========================================================

    private void setupLanguageSelector() {
        languageSelector.setItems(FXCollections.observableArrayList(
                "🇫🇷 Français", "🇹🇳 العربية", "🇬🇧 English"
        ));
        languageSelector.setValue("🇫🇷 Français");

        languageSelector.setOnAction(e -> {
            String selected = languageSelector.getValue();
            if (selected.contains("Français")) currentLanguage = "fr";
            else if (selected.contains("العربية")) currentLanguage = "ar";
            else currentLanguage = "en";

            displayReviews(filteredReviews);
        });
    }

    // =========================================================
    // LOAD
    // =========================================================

    private void loadUserInfo() {
        String sql = "SELECT name, email FROM users WHERE id = ?";
        try {
            Connection conn = MyDatabase.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, tutorId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        tutorName.setText(rs.getString("name"));
                        tutorEmail.setText(rs.getString("email"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadReviews() {
        allReviews = reviewsService.getReviewsByTutor(tutorId);
        filteredReviews = allReviews;
        displayReviews(filteredReviews);
    }

    private void displayReviews(List<Reviews> reviews) {
        reviewsContainer.getChildren().clear();

        if (reviews == null || reviews.isEmpty()) {
            Label empty = new Label("Aucun avis pour le moment");
            empty.setStyle("-fx-font-size:14px;-fx-text-fill:#666;");
            reviewsContainer.getChildren().add(empty);
            return;
        }

        for (Reviews r : reviews) {
            reviewsContainer.getChildren().add(createReviewCard(r));
        }
    }

    // =========================================================
    // CARD (🔥 MERGED AVEC REPORT)
    // =========================================================

    private VBox createReviewCard(Reviews review) {
        VBox card = new VBox(12);
        card.getStyleClass().add("review-card");

        // ===== HEADER =====
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

        Label date = new Label(LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        date.getStyleClass().add("review-date");

        header.getChildren().addAll(titleSection, spacer, date);

        // ===== STARS =====
        HBox starsDisplay = createStarsDisplay(review.getRating());

        // ===== COMMENT =====
        Label comment = new Label(review.getComment());
        comment.setWrapText(true);
        comment.getStyleClass().add("review-comment");

        // ===== FOOTER =====
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-padding:10 0 0 0;-fx-border-color:#e5e7eb;-fx-border-width:1 0 0 0;");

        // 🔥 BADGE SI SIGNALÉ
        if (review.isReported()) {
            Label reportedBadge = new Label("⚠️ Signalé à l'admin");
            reportedBadge.setStyle(
                    "-fx-background-color:#fef3c7;" +
                            "-fx-text-fill:#92400e;" +
                            "-fx-padding:5 10;" +
                            "-fx-background-radius:4;" +
                            "-fx-font-size:11px;" +
                            "-fx-font-weight:bold;"
            );
            footer.getChildren().add(reportedBadge);
        }

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        // 🔥 BOUTON SIGNALER
        Button btnReport = new Button(
                review.isReported() ? "✓ Déjà signalé" : "🚩 Signaler"
        );
        btnReport.setDisable(review.isReported());

        if (review.isReported()) {
            btnReport.setStyle(
                    "-fx-background-color:#e5e7eb;" +
                            "-fx-text-fill:#6b7280;" +
                            "-fx-font-size:12px;" +
                            "-fx-padding:6 12;" +
                            "-fx-background-radius:6;"
            );
        } else {
            btnReport.getStyleClass().add("link-button-danger");
            btnReport.setStyle("-fx-font-size:12px;-fx-padding:6 12;");
        }

        btnReport.setOnAction(e -> reportReview(review));

        footer.getChildren().addAll(footerSpacer, btnReport);

        card.getChildren().addAll(header, starsDisplay, comment, footer);
        return card;
    }

    // =========================================================
    // REPORT REVIEW
    // =========================================================

    private void reportReview(Reviews review) {

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Signaler cet avis");
        dialog.setHeaderText("Pourquoi souhaitez-vous signaler cet avis ?");

        ButtonType reportButtonType =
                new ButtonType("Signaler", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(reportButtonType, ButtonType.CANCEL);

        // champ raison
        TextArea reasonField = new TextArea();
        reasonField.setPromptText("Décrivez la raison du signalement...");
        reasonField.setPrefRowCount(4);
        reasonField.setWrapText(true);

        dialog.getDialogPane().setContent(reasonField);

        // 🔥 bouton OK
        Button okButton = (Button) dialog.getDialogPane().lookupButton(reportButtonType);
        okButton.setDisable(true); // désactivé au début

        // 🔥 validation temps réel
        reasonField.textProperty().addListener((obs, oldText, newText) -> {
            okButton.setDisable(newText == null || newText.trim().isEmpty());
        });

        dialog.setResultConverter(button -> {
            if (button == reportButtonType) {
                String reason = reasonField.getText().trim();
                return reason.isEmpty() ? null : reason;
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(reason -> {
            try {
                reviewsService.reportReview(review.getId(), reason);
                loadReviews();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // =========================================================
    // STARS
    // =========================================================

    private HBox createStarsDisplay(int rating) {
        HBox container = new HBox(8);
        container.setAlignment(Pos.CENTER_LEFT);

        int abs = Math.abs(rating);
        abs = Math.min(abs, 5);

        String stars = "★".repeat(abs) + "☆".repeat(5 - abs);
        String color = rating < 0 ? "#ef4444" : "#fbbf24";

        Label starsLabel = new Label(stars);
        starsLabel.setStyle("-fx-font-size:22px;-fx-text-fill:" + color + ";");

        container.getChildren().add(starsLabel);
        return container;
    }

    // =========================================================
    // STATS
    // =========================================================

    private void updateStatistics() {
        if (allReviews == null || allReviews.isEmpty()) {
            averageRatingLabel.setText("0.0");
            totalReviewsLabel.setText("0");
            monthReviewsLabel.setText("0");
        } else {
            double avg = allReviews.stream()
                    .mapToInt(Reviews::getRating)
                    .average()
                    .orElse(0.0);

            averageRatingLabel.setText(String.format("%.1f", avg));
            totalReviewsLabel.setText(String.valueOf(allReviews.size()));
            monthReviewsLabel.setText("5");
        }

        trustPointsLabel.setText(
                String.valueOf(reviewsService.getTrustPoints(tutorId))
        );
    }

    // =========================================================
    // FILTERS
    // =========================================================

    @FXML private void filterAll() { filteredReviews = allReviews; displayReviews(filteredReviews); }
    @FXML private void filter5Stars() { filter(r -> r.getRating() == 5); }
    @FXML private void filter4Stars() { filter(r -> r.getRating() == 4); }
    @FXML private void filter3Stars() { filter(r -> r.getRating() == 3); }
    @FXML private void filterLow() { filter(r -> r.getRating() <= 2); }

    private void filter(java.util.function.Predicate<Reviews> p) {
        filteredReviews = allReviews.stream().filter(p).collect(Collectors.toList());
        displayReviews(filteredReviews);
    }

    // =========================================================
    // NAV
    // =========================================================

    @FXML
    private void goToDashboard() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/Views/TutorDashboardView.fxml")
            );
            Scene scene = reviewsContainer.getScene();
            scene.setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}