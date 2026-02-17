package org.example.campusLink.controllers;

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

    // 🔥 Note sélectionnée (maintenant de -5 à +5)
    private int selectedRating = 0;

    // 🔥 Labels des étoiles cliquables (10 étoiles : 5 négatives + 5 positives)
    private Label[] starLabels = new Label[10];

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

    // 🔥 CRÉER LES ÉTOILES CLIQUABLES (-5 à +5)
    private void createStarRating() {
        starsRatingBox.getChildren().clear();
        starsRatingBox.setSpacing(5);
        starsRatingBox.setAlignment(Pos.CENTER_LEFT);

        // 5 étoiles négatives (rouges) - de -5 à -1
        for (int i = 0; i < 5; i++) {
            final int starIndex = -(5 - i); // -5, -4, -3, -2, -1
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

        // Séparateur visuel
        Label separator = new Label("|");
        separator.setStyle("-fx-font-size: 32px; -fx-text-fill: #9ca3af; -fx-padding: 0 10 0 10;");
        starsRatingBox.getChildren().add(separator);

        // 5 étoiles positives (jaunes/dorées) - de 1 à 5
        for (int i = 0; i < 5; i++) {
            final int starIndex = i + 1; // 1, 2, 3, 4, 5
            Label star = new Label("★");
            star.setStyle("-fx-font-size: 32px; -fx-cursor: hand; -fx-text-fill: #d1d5db;");

            // Survol
            star.setOnMouseEntered(e -> updateStarPreview(starIndex));

            // Clic
            star.setOnMouseClicked(e -> {
                selectedRating = starIndex;
                updateStarDisplay(starIndex);
            });

            starLabels[i + 5] = star;
            starsRatingBox.getChildren().add(star);
        }

        // Quand la souris sort de la zone, revenir à la sélection
        starsRatingBox.setOnMouseExited(e -> updateStarDisplay(selectedRating));

        // Initialiser à 0 étoiles (neutre)
        updateStarDisplay(0);
    }

    // 🔥 METTRE À JOUR L'APERÇU AU SURVOL
    private void updateStarPreview(int hoverRating) {
        // Étoiles négatives (-5 à -1)
        for (int i = 0; i < 5; i++) {
            int starValue = -(5 - i); // -5, -4, -3, -2, -1
            if (hoverRating < 0 && starValue >= hoverRating) {
                starLabels[i].setStyle("-fx-font-size: 32px; -fx-cursor: hand; -fx-text-fill: #ef4444;");
            } else {
                starLabels[i].setStyle("-fx-font-size: 32px; -fx-cursor: hand; -fx-text-fill: #d1d5db;");
            }
        }

        // Étoiles positives (1 à 5)
        for (int i = 5; i < 10; i++) {
            int starValue = (i - 5) + 1; // 1, 2, 3, 4, 5
            if (hoverRating > 0 && starValue <= hoverRating) {
                starLabels[i].setStyle("-fx-font-size: 32px; -fx-cursor: hand; -fx-text-fill: #fbbf24;");
            } else {
                starLabels[i].setStyle("-fx-font-size: 32px; -fx-cursor: hand; -fx-text-fill: #d1d5db;");
            }
        }
    }

    // 🔥 METTRE À JOUR L'AFFICHAGE DES ÉTOILES SÉLECTIONNÉES
    private void updateStarDisplay(int rating) {
        // Étoiles négatives (-5 à -1)
        for (int i = 0; i < 5; i++) {
            int starValue = -(5 - i); // -5, -4, -3, -2, -1
            if (rating < 0 && starValue >= rating) {
                starLabels[i].setStyle("-fx-font-size: 32px; -fx-cursor: hand; -fx-text-fill: #ef4444;");
            } else {
                starLabels[i].setStyle("-fx-font-size: 32px; -fx-cursor: hand; -fx-text-fill: #d1d5db;");
            }
        }

        // Étoiles positives (1 à 5)
        for (int i = 5; i < 10; i++) {
            int starValue = (i - 5) + 1; // 1, 2, 3, 4, 5
            if (rating > 0 && starValue <= rating) {
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
        selectedRating = 0;
        updateStarDisplay(0);
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
        HBox starsDisplay = createStarsDisplay(review.getRating());

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

        card.getChildren().addAll(header, starsDisplay, comment, footer);
        return card;
    }

    // 🔥 CRÉER L'AFFICHAGE DES ÉTOILES (retourne un HBox)
    private HBox createStarsDisplay(int rating) {
        HBox container = new HBox(8);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setStyle("-fx-padding: 5 0 5 0;");

        String starsText;
        String color;
        String badge;

        if (rating < 0) {
            // Étoiles rouges pour notes négatives
            int absRating = Math.abs(rating);
            starsText = "★".repeat(absRating) + "☆".repeat(5 - absRating);
            color = "#ef4444"; // Rouge
            badge = "(" + rating + ")";
        } else if (rating > 0) {
            // Étoiles jaunes pour notes positives
            starsText = "★".repeat(rating) + "☆".repeat(5 - rating);
            color = "#fbbf24"; // Jaune/doré
            badge = "(+" + rating + ")";
        } else {
            // Aucune étoile pour note neutre (0)
            starsText = "☆☆☆☆☆";
            color = "#d1d5db"; // Gris
            badge = "(Neutre)";
        }

        Label stars = new Label(starsText);
        stars.setStyle("-fx-font-size: 22px; -fx-text-fill: " + color + ";");

        Label badgeLabel = new Label(badge);
        badgeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + "; -fx-font-weight: bold;");

        container.getChildren().addAll(stars, badgeLabel);
        return container;
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

        // Vérifier qu'une note a été sélectionnée (ne peut pas être 0)
        if (selectedRating == 0) {
            showAlert("Note requise", "Veuillez sélectionner une note (positive ou négative).");
            return;
        }

        if (selectedRating < -5 || selectedRating > 5) {
            showAlert("Note invalide", "Veuillez sélectionner une note entre -5 et +5.");
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