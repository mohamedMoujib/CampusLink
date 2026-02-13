package org.example.campusLink.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
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

public class ReviewController {

    @FXML
    private VBox reviewContainer;

    @FXML
    private TextArea commentField;

    @FXML
    private Spinner<Integer> ratingSpinner;

    @FXML
    private Button addButton;

    @FXML
    private Label formTitle;

    @FXML
    private VBox formContainer;

    private ReviewsService reviewsService;

    // ⚠️ Simulation utilisateur connecté
    private final int studentId = 1;
    private final int prestataireId = 2;

    // 🔥 Pour savoir si on est en mode édition
    private Integer editingReviewId = null;

    @FXML
    public void initialize() {
        reviewsService = new ReviewsService();

        ratingSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 5)
        );

        // Cacher le formulaire au départ
        formContainer.setVisible(false);
        formContainer.setManaged(false);

        loadReviews();
    }

    // ===================== SHOW FORM =====================

    @FXML
    private void showAddForm() {
        formContainer.setVisible(true);
        formContainer.setManaged(true);
        editingReviewId = null;
        commentField.clear();
        ratingSpinner.getValueFactory().setValue(5);
        formTitle.setText("Laisser un avis");
        addButton.setText("Publier");
    }

    @FXML
    private void hideForm() {
        formContainer.setVisible(false);
        formContainer.setManaged(false);
    }

    // ===================== LOAD =====================

    private void loadReviews() {
        reviewContainer.getChildren().clear();

        List<Reviews> reviews = reviewsService.getReviewsByStudent(studentId);

        for (Reviews r : reviews) {
            VBox card = createReviewCard(r);
            reviewContainer.getChildren().add(card);
        }
    }

    private VBox createReviewCard(Reviews review) {
        VBox card = new VBox(12);
        card.getStyleClass().add("review-card");

        // === HEADER (Titre + Date) ===
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleSection = new VBox(3);
        Label title = new Label("Nom du service"); // À adapter selon tes données
        title.getStyleClass().add("review-title");

        Label subtitle = new Label("avec Nom Prestataire"); // À adapter
        subtitle.getStyleClass().add("review-subtitle");

        titleSection.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label date = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        date.getStyleClass().add("review-date");

        header.getChildren().addAll(titleSection, spacer, date);

        // === STARS ===
        HBox starsBox = new HBox(2);
        for (int i = 0; i < review.getRating(); i++) {
            Label star = new Label("⭐");
            star.setStyle("-fx-font-size: 16px;");
            starsBox.getChildren().add(star);
        }

        // === COMMENT ===
        Label comment = new Label(review.getComment());
        comment.setWrapText(true);
        comment.getStyleClass().add("review-comment");

        // === FOOTER (Likes + Actions) ===
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label likes = new Label("👍 12 personnes ont trouvé cet avis utile");
        likes.getStyleClass().add("review-meta");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Button btnModifier = new Button("Modifier");
        btnModifier.getStyleClass().add("link-button-primary");
        btnModifier.setOnAction(e -> startEditing(review));

        Button btnSupprimer = new Button("Supprimer");
        btnSupprimer.getStyleClass().add("link-button-danger");
        btnSupprimer.setOnAction(e -> {
            reviewsService.deleteReview(review.getId());
            loadReviews();
        });

        footer.getChildren().addAll(likes, footerSpacer, btnModifier, btnSupprimer);

        card.getChildren().addAll(header, starsBox, comment, footer);
        return card;
    }

    // ===================== START EDITING =====================

    private void startEditing(Reviews review) {
        formContainer.setVisible(true);
        formContainer.setManaged(true);

        editingReviewId = review.getId();
        ratingSpinner.getValueFactory().setValue(review.getRating());
        commentField.setText(review.getComment());

        formTitle.setText("Modifier l'avis");
        addButton.setText("Mettre à jour");
    }

    // ===================== ADD / UPDATE =====================

    @FXML
    private void handleAddReview() {
        if (editingReviewId != null) {
            reviewsService.updateReview(
                    editingReviewId,
                    ratingSpinner.getValue(),
                    commentField.getText()
            );
        } else {
            Reviews review = new Reviews(
                    studentId,
                    prestataireId,
                    10,
                    ratingSpinner.getValue(),
                    commentField.getText()
            );
            reviewsService.addReview(review);
        }

        hideForm();
        loadReviews();
    }
}