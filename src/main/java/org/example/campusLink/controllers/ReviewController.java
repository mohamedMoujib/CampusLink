package org.example.campusLink.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;
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
    private Button addButton;

    @FXML
    private Label formTitle;

    @FXML
    private VBox formContainer;

    @FXML
    private HBox starsRatingBox;

    private ReviewsService reviewsService;

    // ⚠️ Simulation utilisateur connecté
    private final int studentId = 1;
    private final int prestataireId = 2;

    // 🔥 Pour savoir si on est en mode édition
    private Integer editingReviewId = null;

    // 🔥 Note sélectionnée
    private int selectedRating = 5;

    // 🔥 Labels des étoiles cliquables
    private Label[] starLabels = new Label[5];

    @FXML
    public void initialize() {
        reviewsService = new ReviewsService();

        // 🔥 Créer les étoiles cliquables
        createStarRating();

        // Cacher le formulaire au départ
        formContainer.setVisible(false);
        formContainer.setManaged(false);

        // 🔥 VALIDATION EN TEMPS RÉEL (optionnel)
        commentField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                commentField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px;");
            } else {
                commentField.setStyle("");
            }
        });

        loadReviews();
    }

    // 🔥 CRÉER LES ÉTOILES CLIQUABLES
    private void createStarRating() {
        starsRatingBox.getChildren().clear();
        starsRatingBox.setSpacing(5);
        starsRatingBox.setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < 5; i++) {
            final int starIndex = i + 1;
            Label star = new Label("★");
            star.setStyle("-fx-font-size: 32px; -fx-cursor: hand; -fx-text-fill: #d1d5db;");

            // Survol
            star.setOnMouseEntered(e -> updateStarPreview(starIndex));

            // Clic
            star.setOnMouseClicked(e -> {
                selectedRating = starIndex;
                updateStarDisplay(starIndex);
            });

            starLabels[i] = star;
            starsRatingBox.getChildren().add(star);
        }

        // Quand la souris sort de la zone, revenir à la sélection
        starsRatingBox.setOnMouseExited(e -> updateStarDisplay(selectedRating));

        // Initialiser à 5 étoiles
        updateStarDisplay(5);
    }

    // 🔥 METTRE À JOUR L'APERÇU AU SURVOL
    private void updateStarPreview(int hoverRating) {
        for (int i = 0; i < 5; i++) {
            if (i < hoverRating) {
                starLabels[i].setStyle("-fx-font-size: 32px; -fx-cursor: hand; -fx-text-fill: #fbbf24;");
            } else {
                starLabels[i].setStyle("-fx-font-size: 32px; -fx-cursor: hand; -fx-text-fill: #d1d5db;");
            }
        }
    }

    // 🔥 METTRE À JOUR L'AFFICHAGE DES ÉTOILES SÉLECTIONNÉES
    private void updateStarDisplay(int rating) {
        for (int i = 0; i < 5; i++) {
            if (i < rating) {
                starLabels[i].setStyle("-fx-font-size: 32px; -fx-cursor: hand; -fx-text-fill: #fbbf24;");
            } else {
                starLabels[i].setStyle("-fx-font-size: 32px; -fx-cursor: hand; -fx-text-fill: #d1d5db;");
            }
        }
    }

    // ===================== SHOW FORM =====================

    @FXML
    private void showAddForm() {
        formContainer.setVisible(true);
        formContainer.setManaged(true);
        editingReviewId = null;
        commentField.clear();
        commentField.setStyle(""); // Réinitialiser le style
        selectedRating = 5;
        updateStarDisplay(5);
        formTitle.setText("Laisser un avis");
        addButton.setText("Publier");
    }

    @FXML
    private void hideForm() {
        formContainer.setVisible(false);
        formContainer.setManaged(false);
        commentField.setStyle(""); // Réinitialiser le style
    }

    // ===================== LOAD =====================

    private void loadReviews() {
        reviewContainer.getChildren().clear();

        List<Reviews> reviews = reviewsService.getReviewsByStudentWithDetails(studentId);

        if (reviews.isEmpty()) {
            Label emptyLabel = new Label("Aucun avis pour le moment");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
            reviewContainer.getChildren().add(emptyLabel);
            return;
        }

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
        Label title = new Label(review.getServiceTitle());
        title.getStyleClass().add("review-title");

        Label subtitle = new Label("avec " + review.getPrestataireName());
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

        card.getChildren().addAll(header, starsLabel, comment, footer);
        return card;
    }

    // ===================== START EDITING =====================

    private void startEditing(Reviews review) {
        formContainer.setVisible(true);
        formContainer.setManaged(true);

        editingReviewId = review.getId();
        selectedRating = review.getRating();
        updateStarDisplay(review.getRating());
        commentField.setText(review.getComment());
        commentField.setStyle(""); // Réinitialiser le style

        formTitle.setText("Modifier l'avis");
        addButton.setText("Mettre à jour");
    }

    // ===================== ADD / UPDATE =====================

    @FXML
    private void handleAddReview() {
        // 🔥 CONTRÔLE DE SAISIE

        // Vérifier que le commentaire n'est pas vide
        String comment = commentField.getText().trim();
        if (comment.isEmpty()) {
            showAlert("Commentaire requis", "Veuillez saisir un commentaire avant de publier votre avis.");
            commentField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px;");
            return;
        }

        // Vérifier qu'une note a été sélectionnée (optionnel car déjà initialisé à 5)
        if (selectedRating < 1 || selectedRating > 5) {
            showAlert("Note invalide", "Veuillez sélectionner une note entre 1 et 5 étoiles.");
            return;
        }

        // Si tout est valide, procéder à l'ajout/modification
        if (editingReviewId != null) {
            reviewsService.updateReview(
                    editingReviewId,
                    selectedRating,
                    comment
            );
        } else {
            Reviews review = new Reviews(
                    studentId,
                    prestataireId,
                    10,
                    selectedRating,
                    comment
            );
            reviewsService.addReview(review);
        }

        hideForm();
        loadReviews();
    }

    // 🔥 MÉTHODE POUR AFFICHER LES ALERTES
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}