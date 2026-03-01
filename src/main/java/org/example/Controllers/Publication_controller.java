package org.example.Controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import org.example.campusLink.Services.Gestion_publication;
import org.example.campusLink.entities.Publications;
import org.example.campusLink.entities.Publications.TypePublication;
import org.example.campusLink.entities.Publications.StatusPublication;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Publication_controller {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private FlowPane publicationsContainer;

    @FXML private Button allPublicationsTab;
    @FXML private Button myPublicationsTab;

    @FXML private Label statsTotal;
    @FXML private Label statsVentes;
    @FXML private Label statsDemandes;
    @FXML private Label statsActives;

    private Gestion_publication gestionPublication;
    private int currentStudentId = 1;
    private boolean showingMyPublications = false;
    private List<Publications> basePublications = new ArrayList<>();

    public void setCurrentStudentId(int id) {
        this.currentStudentId = id;
    }

    @FXML
    public void initialize() {
        System.out.println("Initializing Publication_controller...");
        try {
            gestionPublication = new Gestion_publication();
            setupFilters();
            loadStatistics();
            reloadBasePublications();
            System.out.println("Publication_controller initialized successfully");
        } catch (Exception e) {
            System.err.println("Error initializing Publication_controller: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur d'initialisation: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // IMAGE RESOLUTION  ← THE FIX
    // ═══════════════════════════════════════════════════════════════

    /**
     * Resolves an image URL stored in the DB to an actual File on disk.
     *
     * Handles three formats that may exist in the database:
     *
     *   1. Relative path  → "uploads/publications/1234_photo.png"
     *      (written by the new CreatePublication_controller)
     *      Resolved against the app working directory.
     *
     *   2. Absolute file URI → "file:/C:/Users/ALI/OneDrive/Images/photo.jpg"
     *      (written by the OLD upload code — still in the DB for legacy records)
     *      Parsed with URI so Windows paths are handled correctly.
     *
     *   3. Absolute native path → "C:/Users/ALI/..."  (edge case)
     *      Wrapped directly in new File().
     *
     * Returns null if the string is blank or the file does not exist.
     */
    private File resolveImageFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;

        try {
            File file;

            if (imageUrl.startsWith("file:")) {
                // Case 2: absolute file URI — use URI parser (handles %20 spaces etc.)
                file = new File(new URI(imageUrl));
            } else if (imageUrl.contains(":\\") || imageUrl.startsWith("/")) {
                // Case 3: absolute native path (Windows C:\ or Unix /)
                file = new File(imageUrl);
            } else {
                // Case 1: relative path — resolve from working directory
                file = new File(imageUrl);
            }

            System.out.println("Looking for publication image at: " + file.getAbsolutePath());

            if (file.exists()) {
                System.out.println("✅ Publication image loaded: " + imageUrl);
                return file;
            } else {
                System.err.println("❌ Publication image not found: " + file.getAbsolutePath());
                return null;
            }

        } catch (Exception e) {
            System.err.println("❌ Error resolving image path '" + imageUrl + "': " + e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // FILTERS & LOADING
    // ═══════════════════════════════════════════════════════════════

    private void setupFilters() {
        if (typeFilter != null) {
            typeFilter.setItems(FXCollections.observableArrayList(
                    "Tous", "🏷️ Ventes", "🔍 Demandes de services"
            ));
            typeFilter.setValue("Tous");
            typeFilter.setOnAction(e -> updateDisplayedPublications());
        }
        if (statusFilter != null) {
            statusFilter.setItems(FXCollections.observableArrayList(
                    "Tous", "Actif", "En cours", "Terminé", "Annulé"
            ));
            statusFilter.setValue("Tous");
            statusFilter.setOnAction(e -> updateDisplayedPublications());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                updateDisplayedPublications();
            });
        }
    }

    private void loadStatistics() {
        try {
            List<Publications> all = gestionPublication.afficherPublications();
            int total    = all.size();
            int ventes   = (int) all.stream().filter(p -> p.getTypePublication() == TypePublication.VENTE_OBJET).count();
            int demandes = (int) all.stream().filter(p -> p.getTypePublication() == TypePublication.DEMANDE_SERVICE).count();
            int actives  = (int) all.stream().filter(p -> p.getStatus() == StatusPublication.ACTIVE).count();

            if (statsTotal    != null) statsTotal.setText(String.valueOf(total));
            if (statsVentes   != null) statsVentes.setText(String.valueOf(ventes));
            if (statsDemandes != null) statsDemandes.setText(String.valueOf(demandes));
            if (statsActives  != null) statsActives.setText(String.valueOf(actives));
        } catch (Exception e) {
            System.err.println("Error loading statistics: " + e.getMessage());
        }
    }

    private void reloadBasePublications() {
        try {
            if (showingMyPublications) {
                basePublications = gestionPublication.afficherPublicationsParEtudiant(currentStudentId);
            } else {
                basePublications = gestionPublication.afficherPublications();
            }
            updateDisplayedPublications();
        } catch (Exception e) {
            System.err.println("Error reloading publications: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les publications: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateDisplayedPublications() {
        if (publicationsContainer == null) return;

        publicationsContainer.getChildren().clear();

        if (basePublications == null || basePublications.isEmpty()) {
            Label emptyLabel = new Label(
                    showingMyPublications
                            ? "Vous n'avez pas encore de publications.\nCliquez sur '+ Créer une publication' pour commencer."
                            : "Aucune publication disponible pour le moment."
            );
            emptyLabel.getStyleClass().add("empty-state");
            emptyLabel.setWrapText(true);
            publicationsContainer.getChildren().add(emptyLabel);
            return;
        }

        List<Publications> publications = applyFilters(basePublications);
        publications = applyKeywordFilter(publications, searchField != null ? searchField.getText() : null);

        if (publications.isEmpty()) {
            String keyword = searchField != null ? searchField.getText() : null;
            String msg = (keyword != null && !keyword.isBlank())
                    ? "Aucun résultat pour \"" + keyword.trim() + "\""
                    : "Aucune publication ne correspond aux filtres sélectionnés.";
            Label emptyLabel = new Label(msg);
            emptyLabel.getStyleClass().add("empty-state");
            emptyLabel.setWrapText(true);
            publicationsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Publications pub : publications) {
            publicationsContainer.getChildren().add(createPublicationCard(pub));
        }
    }

    private List<Publications> applyKeywordFilter(List<Publications> publications, String keyword) {
        if (keyword == null) return publications;
        String k = keyword.trim();
        if (k.isEmpty()) return publications;
        String needle = k.toLowerCase(Locale.ROOT);

        return publications.stream().filter(p -> {
            String titre = p.getTitre();
            String msg = p.getMessage();
            String loc = p.getLocalisation();
            String serviceName = p.getServiceName();
            String categoryName = p.getCategoryName();
            String prestataireName = p.getPrestataireName();

            return (titre != null && titre.toLowerCase(Locale.ROOT).contains(needle)) ||
                    (msg != null && msg.toLowerCase(Locale.ROOT).contains(needle)) ||
                    (loc != null && loc.toLowerCase(Locale.ROOT).contains(needle)) ||
                    (serviceName != null && serviceName.toLowerCase(Locale.ROOT).contains(needle)) ||
                    (categoryName != null && categoryName.toLowerCase(Locale.ROOT).contains(needle)) ||
                    (prestataireName != null && prestataireName.toLowerCase(Locale.ROOT).contains(needle));
        }).toList();
    }

    // ═══════════════════════════════════════════════════════════════
    // CARD BUILDER
    // ═══════════════════════════════════════════════════════════════

    private VBox createPublicationCard(Publications pub) {
        VBox card = new VBox(12);
        card.getStyleClass().add("publication-card");

        // ===== 1. HEADER (status masqué pour l'utilisateur) =====
        HBox header = new HBox(10);
        header.getStyleClass().add("card-header");
        header.setAlignment(Pos.CENTER_LEFT);

        // ===== 2. TITLE =====
        Label title = new Label(pub.getTitre());
        title.getStyleClass().add("card-title");
        title.setMaxWidth(360);
        title.setWrapText(true);

        // ===== 3. IMAGE — uses resolveImageFile() for robust path handling =====
        ImageView imageView = null;
        if (pub.hasImage()) {
            File imageFile = resolveImageFile(pub.getImageUrl());
            if (imageFile != null) {
                try {
                    Image image = new Image(imageFile.toURI().toString());
                    imageView = new ImageView(image);
                    imageView.getStyleClass().add("card-image");
                    imageView.setFitWidth(360);
                    imageView.setFitHeight(200);
                    imageView.setPreserveRatio(true);
                } catch (Exception e) {
                    System.err.println("Error creating ImageView: " + e.getMessage());
                }
            }
        }

        // ===== 4. INFO ROWS =====
        VBox infoBox = new VBox(6);

        HBox studentRow = new HBox(8);
        studentRow.getStyleClass().add("card-info-row");
        Label studentIcon = new Label("👤");
        studentIcon.getStyleClass().add("card-info-icon");
        String displayStudentName = (pub.getStudentName() != null && !pub.getStudentName().isBlank())
                ? pub.getStudentName()
                : "Étudiant #" + pub.getStudentId();
        Label studentText = new Label(displayStudentName);
        studentText.getStyleClass().add("card-info-text");
        studentRow.getChildren().addAll(studentIcon, studentText);
        infoBox.getChildren().add(studentRow);

        if (pub.getLocalisation() != null && !pub.getLocalisation().isEmpty()) {
            HBox locationRow = new HBox(8);
            locationRow.getStyleClass().add("card-info-row");
            Label locationIcon = new Label("📍");
            locationIcon.getStyleClass().add("card-info-icon");
            Label locationText = new Label(pub.getLocalisation());
            locationText.getStyleClass().add("card-info-text");
            locationRow.getChildren().addAll(locationIcon, locationText);
            infoBox.getChildren().add(locationRow);
        }

        HBox dateRow = new HBox(8);
        dateRow.getStyleClass().add("card-info-row");
        Label dateIcon = new Label("🕐");
        dateIcon.getStyleClass().add("card-info-icon");
        Label dateText = new Label(pub.getRelativeTime());
        dateText.getStyleClass().add("card-info-text");
        dateRow.getChildren().addAll(dateIcon, dateText);
        infoBox.getChildren().add(dateRow);

        // ===== 5. DESCRIPTION =====
        Label description = new Label(pub.getMessagePreview(150));
        description.getStyleClass().add("card-description");
        description.setStyle("-fx-text-fill: #4a5568; -fx-font-size: 14px;");
        description.setWrapText(true);
        description.setMaxWidth(360);
        description.setMaxHeight(60);

        // ===== 6. FOOTER =====
        HBox footer = new HBox(10);
        footer.getStyleClass().add("card-footer");
        footer.setAlignment(Pos.CENTER_LEFT);

        VBox priceBox = new VBox(2);
        priceBox.getStyleClass().add("card-price-container");
        // Show formatted price — fallback if entity returns null/empty
        String priceText = pub.getFormattedPrice();
        if (priceText == null || priceText.isBlank() || priceText.contains("non spécifié")) {
            java.math.BigDecimal pv = pub.getPrixVente();
            priceText = (pv != null && pv.compareTo(java.math.BigDecimal.ZERO) > 0)
                    ? String.format("%.2f €", pv).replace(".", ",")
                    : "Prix non spécifié";
        }
        Label price = new Label(priceText);
        price.getStyleClass().add("card-price");
        Label views = new Label("👁 " + pub.getVues() + " vues");
        views.getStyleClass().add("card-views");
        priceBox.getChildren().addAll(price, views);

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Button detailsBtn = new Button("Voir détails");
        detailsBtn.getStyleClass().add("btn-contact");
        detailsBtn.setOnAction(e -> viewPublicationDetails(pub));

        footer.getChildren().addAll(priceBox, footerSpacer, detailsBtn);

        if (showingMyPublications && pub.getStudentId() == currentStudentId) {
            Button deleteBtn = new Button("🗑");
            deleteBtn.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-font-size: 16px;" +
                            "-fx-cursor: hand;" +
                            "-fx-text-fill: #9ca3af;" +
                            "-fx-padding: 6 10;" +
                            "-fx-background-radius: 8;"
            );
            deleteBtn.setOnAction(e -> deletePublication(pub, card));
            deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(
                    "-fx-background-color: #fee2e2;" +
                            "-fx-font-size: 16px;" +
                            "-fx-cursor: hand;" +
                            "-fx-text-fill: #dc2626;" +
                            "-fx-padding: 6 10;" +
                            "-fx-background-radius: 8;"
            ));
            deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-font-size: 16px;" +
                            "-fx-cursor: hand;" +
                            "-fx-text-fill: #9ca3af;" +
                            "-fx-padding: 6 10;" +
                            "-fx-background-radius: 8;"
            ));
            footer.getChildren().add(deleteBtn);
        }

        // ===== ASSEMBLE =====
        card.getChildren().add(header);
        card.getChildren().add(title);
        if (imageView != null) card.getChildren().add(imageView);
        card.getChildren().addAll(infoBox, description, footer);

        return card;
    }

    // ═══════════════════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════════════════

    private void deletePublication(Publications pub, VBox card) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Supprimer la publication");
        confirmAlert.setHeaderText("Supprimer \"" + pub.getTitre() + "\" ?");
        confirmAlert.setContentText("Cette action est irréversible.");

        ButtonType btnDelete = new ButtonType("🗑 Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Annuler",      ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(btnDelete, btnCancel);
        confirmAlert.getDialogPane().lookupButton(btnDelete).setStyle(
                "-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold;"
        );

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == btnDelete) {
                try {
                    gestionPublication.supprimerPublication(pub.getId());
                    publicationsContainer.getChildren().remove(card);
                    if (publicationsContainer.getChildren().isEmpty()) {
                        Label emptyLabel = new Label("Aucune publication disponible pour le moment.");
                        emptyLabel.getStyleClass().add("empty-state");
                        publicationsContainer.getChildren().add(emptyLabel);
                    }
                    loadStatistics();
                    System.out.println("✅ Publication deleted: ID=" + pub.getId());
                } catch (Exception e) {
                    System.err.println("Error deleting publication: " + e.getMessage());
                    e.printStackTrace();
                    showAlert("Erreur", "Impossible de supprimer: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void viewPublicationDetails(Publications pub) {
        try {
            gestionPublication.incrementerVues(pub.getId());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Détails de la Publication");
            alert.setHeaderText(pub.getTypeIcon() + " " + pub.getTitre());

            StringBuilder content = new StringBuilder();
            content.append("Type: ").append(pub.getTypePublication().getLabel()).append("\n");
            content.append("Prix: ").append(pub.getFormattedPrice()).append("\n\n");
            content.append("Description:\n").append(pub.getMessage()).append("\n\n");
            if (pub.getLocalisation() != null)
                content.append("Localisation: ").append(pub.getLocalisation()).append("\n");
            content.append("Publié: ").append(pub.getFormattedDate()).append("\n");
            content.append("Vues: ").append(pub.getVues() + 1);

            alert.setContentText(content.toString());
            alert.showAndWait();

            reloadBasePublications();
            loadStatistics();
        } catch (Exception e) {
            System.err.println("Error viewing publication: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void goToCreatePublication() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Create_Publication.fxml"));
            Scene scene = new Scene(loader.load());

            CreatePublication_controller controller = loader.getController();
            controller.setCurrentStudentId(currentStudentId);

            Stage stage = getStage();
            if (stage != null) {
                // Pass the current scene so goBack() can restore it exactly
                controller.setPreviousScene(stage.getScene());
                stage.setScene(scene);
                stage.setTitle("Nouvelle Publication");
            }
        } catch (Exception e) {
            System.err.println("Error navigating to create publication: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void goToMesPublications(MouseEvent event) {
        try {
            if (publicationsContainer == null) return;
            List<Publications> mesPublications = gestionPublication.afficherPublicationsParEtudiant(currentStudentId);
            publicationsContainer.getChildren().clear();

            if (mesPublications.isEmpty()) {
                Label emptyLabel = new Label("Vous n'avez pas encore de publications.");
                emptyLabel.getStyleClass().add("empty-state");
                publicationsContainer.getChildren().add(emptyLabel);
                return;
            }
            for (Publications pub : mesPublications) {
                publicationsContainer.getChildren().add(createPublicationCard(pub));
            }
        } catch (Exception e) {
            System.err.println("Error loading my publications: " + e.getMessage());
        }
    }

    @FXML
    private void goToStudent(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Views/Student.fxml"));
            Stage stage = getStage();
            if (stage != null) { stage.setScene(new Scene(root)); stage.setTitle("Rechercher des Services"); }
        } catch (Exception e) {
            System.err.println("Error navigating to student: " + e.getMessage());
            showAlert("Erreur", "Impossible de naviguer: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void goToServices(MouseEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Views/Student.fxml"));
            Stage stage = getStage();
            if (stage != null) { stage.setScene(new Scene(root)); stage.setTitle("Rechercher des Services"); }
        } catch (Exception e) {
            System.err.println("Error navigating to services: " + e.getMessage());
            showAlert("Erreur", "Impossible de naviguer: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void showAllPublications() {
        showingMyPublications = false;
        updateTabStyles();
        reloadBasePublications();
    }

    @FXML
    private void showMyPublications() {
        showingMyPublications = true;
        updateTabStyles();
        reloadBasePublications();
    }

    private void updateTabStyles() {
        String active   = "-fx-background-color: #5D5FEF; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-cursor: hand; -fx-background-radius: 8; -fx-border-width: 0; -fx-font-weight: bold;";
        String inactive = "-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280; -fx-font-size: 14px; -fx-padding: 10 20; -fx-cursor: hand; -fx-background-radius: 8; -fx-border-color: #e5e7eb; -fx-border-width: 1;";
        if (showingMyPublications) {
            if (myPublicationsTab  != null) myPublicationsTab.setStyle(active);
            if (allPublicationsTab != null) allPublicationsTab.setStyle(inactive);
        } else {
            if (allPublicationsTab != null) allPublicationsTab.setStyle(active);
            if (myPublicationsTab  != null) myPublicationsTab.setStyle(inactive);
        }
    }

    private List<Publications> applyFilters(List<Publications> publications) {
        if (typeFilter != null) {
            String val = typeFilter.getValue();
            if (val != null && !val.equals("Tous")) {
                if (val.contains("Ventes"))
                    publications = publications.stream().filter(p -> p.getTypePublication() == TypePublication.VENTE_OBJET).toList();
                else if (val.contains("Demandes"))
                    publications = publications.stream().filter(p -> p.getTypePublication() == TypePublication.DEMANDE_SERVICE).toList();
            }
        }
        if (statusFilter != null) {
            String val = statusFilter.getValue();
            if (val != null && !val.equals("Tous")) {
                StatusPublication target = switch (val) {
                    case "Actif"    -> StatusPublication.ACTIVE;
                    case "En cours" -> StatusPublication.EN_COURS;
                    case "Terminé"  -> StatusPublication.TERMINEE;
                    case "Annulé"   -> StatusPublication.ANNULEE;
                    default         -> null;
                };
                if (target != null) {
                    StatusPublication t = target;
                    publications = publications.stream().filter(p -> p.getStatus() == t).toList();
                }
            }
        }
        return publications;
    }

    @FXML
    private void refreshPublications() {
        loadStatistics();
        reloadBasePublications();
    }

    private Stage getStage() {
        if (publicationsContainer != null && publicationsContainer.getScene() != null)
            return (Stage) publicationsContainer.getScene().getWindow();
        if (searchField != null && searchField.getScene() != null)
            return (Stage) searchField.getScene().getWindow();
        return null;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}