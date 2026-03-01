package org.example.campusLink.controllers.reviews;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.example.campusLink.services.reviews.ReviewsService;
import org.example.campusLink.entities.Reviews;
import org.example.campusLink.entities.User;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StudentReviewController {

    @FXML private VBox reviewContainer;
    @FXML private TextArea commentField;
    @FXML private Button addButton;
    @FXML private Label formTitle;
    @FXML private VBox formContainer;
    @FXML private HBox starsRatingBox;

    private ReviewsService reviewsService;
    private User currentUser;

    // ✅ IMPORTANT : uniquement la réservation
    private int currentReservationId = 4; // ⚠️ à injecter dynamiquement

    private Integer editingReviewId = null;
    private int selectedRating = 0;
    private Label[] starLabels = new Label[10];

    @FXML
    public void initialize() {
        reviewsService = new ReviewsService();
        createStarRating();

        formContainer.setVisible(false);
        formContainer.setManaged(false);

        commentField.textProperty().addListener((obs, o, n) -> {
            if (n.trim().isEmpty()) {
                commentField.setStyle("-fx-border-color:#ef4444;-fx-border-width:2px;");
            } else {
                commentField.setStyle("");
            }
        });
    }

    // ================= USER =================

    public void setUser(User user) {
        this.currentUser = user;
        loadReviews();
    }

    private int getStudentId() {
        return currentUser != null ? currentUser.getId() : 0;
    }

    // ================= STARS =================

    private void createStarRating() {
        starsRatingBox.getChildren().clear();
        starsRatingBox.setSpacing(5);
        starsRatingBox.setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < 5; i++) {
            final int starIndex = -(5 - i);
            Label star = new Label("★");
            star.setStyle("-fx-font-size:32px;-fx-cursor:hand;-fx-text-fill:#d1d5db;");
            star.setOnMouseEntered(e -> updateStarPreview(starIndex));
            star.setOnMouseClicked(e -> {
                selectedRating = starIndex;
                updateStarDisplay(starIndex);
            });
            starLabels[i] = star;
            starsRatingBox.getChildren().add(star);
        }

        Label sep = new Label("|");
        sep.setStyle("-fx-font-size:32px;-fx-text-fill:#9ca3af;-fx-padding:0 10 0 10;");
        starsRatingBox.getChildren().add(sep);

        for (int i = 0; i < 5; i++) {
            final int starIndex = i + 1;
            Label star = new Label("★");
            star.setStyle("-fx-font-size:32px;-fx-cursor:hand;-fx-text-fill:#d1d5db;");
            star.setOnMouseEntered(e -> updateStarPreview(starIndex));
            star.setOnMouseClicked(e -> {
                selectedRating = starIndex;
                updateStarDisplay(starIndex);
            });
            starLabels[i + 5] = star;
            starsRatingBox.getChildren().add(star);
        }

        starsRatingBox.setOnMouseExited(e -> updateStarDisplay(selectedRating));
        updateStarDisplay(0);
    }

    private void updateStarPreview(int hoverRating) {
        for (int i = 0; i < 5; i++) {
            int v = -(5 - i);
            starLabels[i].setStyle("-fx-font-size:32px;-fx-cursor:hand;-fx-text-fill:" +
                    (hoverRating < 0 && v >= hoverRating ? "#ef4444" : "#d1d5db") + ";");
        }
        for (int i = 5; i < 10; i++) {
            int v = (i - 5) + 1;
            starLabels[i].setStyle("-fx-font-size:32px;-fx-cursor:hand;-fx-text-fill:" +
                    (hoverRating > 0 && v <= hoverRating ? "#fbbf24" : "#d1d5db") + ";");
        }
    }

    private void updateStarDisplay(int rating) {
        updateStarPreview(rating);
    }

    // ================= FORM =================

    @FXML
    private void showAddForm() {
        formContainer.setVisible(true);
        formContainer.setManaged(true);
        editingReviewId = null;
        commentField.clear();
        selectedRating = 0;
        updateStarDisplay(0);
        formTitle.setText("Laisser un avis");
        addButton.setText("Publier");
    }

    @FXML
    private void hideForm() {
        formContainer.setVisible(false);
        formContainer.setManaged(false);
    }

    // ================= LOAD =================

    private void loadReviews() {
        reviewContainer.getChildren().clear();
        if (currentUser == null) return;

        List<Reviews> reviews =
                reviewsService.getReviewsByStudentWithDetails(getStudentId());

        if (reviews.isEmpty()) {
            Label empty = new Label("Aucun avis pour le moment");
            empty.setStyle("-fx-font-size:14px;-fx-text-fill:#666;");
            reviewContainer.getChildren().add(empty);
            return;
        }

        for (Reviews r : reviews) {
            reviewContainer.getChildren().add(createReviewCard(r));
        }
    }

    // ================= CARD =================

    private VBox createReviewCard(Reviews review) {
        VBox card = new VBox(12);
        card.getStyleClass().add("review-card");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(3);
        Label title = new Label(review.getServiceTitle());
        title.getStyleClass().add("review-title");

        Label sub = new Label("avec " + review.getPrestataireName());
        sub.getStyleClass().add("review-subtitle");
        titleBox.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label date = new Label(
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        date.getStyleClass().add("review-date");

        header.getChildren().addAll(titleBox, spacer, date);

        HBox stars = createStarsDisplay(review.getRating());

        Label comment = new Label(review.getComment());
        comment.setWrapText(true);
        comment.getStyleClass().add("review-comment");

        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);

        Region fs = new Region();
        HBox.setHgrow(fs, Priority.ALWAYS);

        Button edit = new Button("Modifier");
        edit.getStyleClass().add("link-button-primary");
        edit.setOnAction(e -> startEditing(review));

        Button del = new Button("Supprimer");
        del.getStyleClass().add("link-button-danger");
        del.setOnAction(e -> {
            reviewsService.deleteReview(review.getId());
            loadReviews();
        });

        footer.getChildren().addAll(fs, edit, del);

        card.getChildren().addAll(header, stars, comment, footer);
        return card;
    }

    private HBox createStarsDisplay(int rating) {
        HBox c = new HBox(8);
        c.setAlignment(Pos.CENTER_LEFT);

        String starsText, color, badge;

        if (rating < 0) {
            int abs = Math.abs(rating);
            starsText = "★".repeat(abs) + "☆".repeat(5 - abs);
            color = "#ef4444";
            badge = "(" + rating + ")";
        } else {
            starsText = "★".repeat(rating) + "☆".repeat(5 - rating);
            color = rating == 0 ? "#d1d5db" : "#fbbf24";
            badge = rating == 0 ? "(Neutre)" : "(+" + rating + ")";
        }

        Label stars = new Label(starsText);
        stars.setStyle("-fx-font-size:22px;-fx-text-fill:" + color + ";");

        Label badgeL = new Label(badge);
        badgeL.setStyle("-fx-font-size:12px;-fx-text-fill:" + color + ";-fx-font-weight:bold;");

        c.getChildren().addAll(stars, badgeL);
        return c;
    }

    private void startEditing(Reviews review) {
        formContainer.setVisible(true);
        formContainer.setManaged(true);
        editingReviewId = review.getId();
        selectedRating = review.getRating();
        updateStarDisplay(selectedRating);
        commentField.setText(review.getComment());
        formTitle.setText("Modifier l'avis");
        addButton.setText("Mettre à jour");
    }

    // ================= ADD =================

    @FXML
    private void handleAddReview() {
        String comment = commentField.getText().trim();

        if (comment.isEmpty()) {
            showAlert("Commentaire requis", "Veuillez saisir un commentaire.");
            return;
        }

        if (selectedRating == 0) {
            showAlert("Note requise", "Veuillez sélectionner une note.");
            return;
        }

        if (editingReviewId != null) {
            reviewsService.updateReview(editingReviewId, selectedRating, comment);
        } else {

            // ✅ 🔥 ICI LE FIX — on récupère le prestataire depuis la réservation
            Integer prestataireId =
                    reviewsService.getPrestataireIdFromReservation(currentReservationId);

            if (prestataireId == null) {
                showAlert("Erreur", "Prestataire introuvable pour cette réservation.");
                return;
            }

            Reviews review = new Reviews(
                    getStudentId(),
                    prestataireId,          // ✅ toujours valide
                    currentReservationId,   // ✅ FK OK
                    selectedRating,
                    comment
            );

            reviewsService.addReview(review);
        }

        hideForm();
        loadReviews();
    }

    private void showAlert(String t, String m) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(t);
        a.setHeaderText(null);
        a.setContentText(m);
        a.showAndWait();
    }
}