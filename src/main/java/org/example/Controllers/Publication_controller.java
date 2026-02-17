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
import java.util.List;

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
    private int currentStudentId = 1; // TODO: Replace with session
    private boolean showingMyPublications = false;

    @FXML
    public void initialize() {
        System.out.println("Initializing Publication_controller...");
        try {
            gestionPublication = new Gestion_publication();
            setupFilters();
            loadStatistics();
            loadPublications();
            System.out.println("Publication_controller initialized successfully");
        } catch (Exception e) {
            System.err.println("Error initializing Publication_controller: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur d'initialisation: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupFilters() {
        if (typeFilter != null) {
            typeFilter.setItems(FXCollections.observableArrayList(
                    "Tous", "🏷️ Ventes", "🔍 Demandes de services"
            ));
            typeFilter.setValue("Tous");
            typeFilter.setOnAction(e -> loadPublications());
        }
        if (statusFilter != null) {
            statusFilter.setItems(FXCollections.observableArrayList(
                    "Tous", "Actif", "En cours", "Terminé", "Annulé"
            ));
            statusFilter.setValue("Tous");
            statusFilter.setOnAction(e -> loadPublications());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal.length() >= 2) searchPublications(newVal);
                else if (newVal == null || newVal.isEmpty()) loadPublications();
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

    private void loadPublications() {
        try {
            if (publicationsContainer == null) return;
            List<Publications> publications = gestionPublication.afficherPublications();
            publicationsContainer.getChildren().clear();

            if (publications == null || publications.isEmpty()) {
                Label emptyLabel = new Label("Aucune publication disponible pour le moment.");
                emptyLabel.getStyleClass().add("empty-state");
                publicationsContainer.getChildren().add(emptyLabel);
                return;
            }

            publications = applyFilters(publications);
            for (Publications pub : publications) {
                publicationsContainer.getChildren().add(createPublicationCard(pub));
            }
        } catch (Exception e) {
            System.err.println("Error loading publications: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les publications: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void searchPublications(String keyword) {
        try {
            if (publicationsContainer == null) return;
            List<Publications> publications = gestionPublication.rechercherPublications(keyword);
            publicationsContainer.getChildren().clear();

            if (publications == null || publications.isEmpty()) {
                Label emptyLabel = new Label("Aucun résultat pour \"" + keyword + "\"");
                emptyLabel.getStyleClass().add("empty-state");
                publicationsContainer.getChildren().add(emptyLabel);
                return;
            }
            for (Publications pub : publications) {
                publicationsContainer.getChildren().add(createPublicationCard(pub));
            }
        } catch (Exception e) {
            System.err.println("Error searching publications: " + e.getMessage());
        }
    }

    /**
     * Builds a publication card.
     *
     * Assembly order:
     *   1. header      (type badge + status badge)
     *   2. title
     *   3. image       (only when available and file exists)
     *   4. infoBox     (student, location, date)
     *   5. description
     *   6. footer      (price + views + "Voir détails" button + 🗑 delete button)
     *
     * The delete button is only shown for the current student's own publications.
     */
    private VBox createPublicationCard(Publications pub) {
        VBox card = new VBox(12);
        card.getStyleClass().add("publication-card");

        // ===== 1. HEADER =====
        HBox header = new HBox(10);
        header.getStyleClass().add("card-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(pub.getTypeIcon() + " " + pub.getTypePublication().getLabel());
        typeBadge.getStyleClass().add("type-badge");
        typeBadge.getStyleClass().add(pub.isVenteObjet() ? "type-vente" : "type-demande");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        Label statusBadge = new Label(pub.getStatusIcon() + " " + pub.getStatus().getLabel());
        statusBadge.getStyleClass().add("status-badge");
        statusBadge.getStyleClass().add("status-" + pub.getStatus().name().toLowerCase());

        header.getChildren().addAll(typeBadge, spacer1, statusBadge);

        // ===== 2. TITLE =====
        Label title = new Label(pub.getTitre());
        title.getStyleClass().add("card-title");
        title.setMaxWidth(360);
        title.setWrapText(true);

        // ===== 3. IMAGE =====
        ImageView imageView = null;
        if (pub.hasImage()) {
            try {
                File imageFile = new File(pub.getImageUrl());
                System.out.println("Looking for publication image at: " + imageFile.getAbsolutePath());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    imageView = new ImageView(image);
                    imageView.getStyleClass().add("card-image");
                    imageView.setFitWidth(360);
                    imageView.setFitHeight(200);
                    imageView.setPreserveRatio(true);
                    System.out.println("✅ Publication image loaded: " + pub.getImageUrl());
                } else {
                    System.err.println("❌ Publication image not found: " + imageFile.getAbsolutePath());
                }
            } catch (Exception e) {
                System.err.println("Error loading publication image: " + e.getMessage());
            }
        }

        // ===== 4. INFO ROWS =====
        VBox infoBox = new VBox(6);

        HBox studentRow = new HBox(8);
        studentRow.getStyleClass().add("card-info-row");
        Label studentIcon = new Label("👤");
        studentIcon.getStyleClass().add("card-info-icon");
        Label studentText = new Label("Étudiant #" + pub.getStudentId());
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

        // Price + views
        VBox priceBox = new VBox(2);
        priceBox.getStyleClass().add("card-price-container");

        Label price = new Label(pub.getFormattedPrice());
        price.getStyleClass().add("card-price");

        Label views = new Label("👁 " + pub.getVues() + " vues");
        views.getStyleClass().add("card-views");

        priceBox.getChildren().addAll(price, views);

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        // "Voir détails" button
        Button detailsBtn = new Button("Voir détails");
        detailsBtn.getStyleClass().add("btn-contact");
        detailsBtn.setOnAction(e -> viewPublicationDetails(pub));

        footer.getChildren().addAll(priceBox, footerSpacer, detailsBtn);

        // 🗑 Delete button — only shown in "Mes publications" tab
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

            // Hover: red tint
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

        // ===== ASSEMBLE IN CORRECT ORDER =====
        card.getChildren().add(header);
        card.getChildren().add(title);
        if (imageView != null) card.getChildren().add(imageView);
        card.getChildren().addAll(infoBox, description, footer);

        return card;
    }

    /**
     * Confirms then deletes a publication.
     * Removes the card from the UI immediately on success — no full reload needed.
     */
    private void deletePublication(Publications pub, VBox card) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Supprimer la publication");
        confirmAlert.setHeaderText("Supprimer \"" + pub.getTitre() + "\" ?");
        confirmAlert.setContentText("Cette action est irréversible.");

        ButtonType btnDelete = new ButtonType("🗑 Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Annuler",      ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(btnDelete, btnCancel);

        // Style the confirm button red
        confirmAlert.getDialogPane().lookupButton(btnDelete).setStyle(
                "-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold;"
        );

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == btnDelete) {
                try {
                    gestionPublication.supprimerPublication(pub.getId());

                    // Remove the card from the container instantly — no flicker from full reload
                    publicationsContainer.getChildren().remove(card);

                    // Show empty state if no cards remain
                    if (publicationsContainer.getChildren().isEmpty()) {
                        Label emptyLabel = new Label("Aucune publication disponible pour le moment.");
                        emptyLabel.getStyleClass().add("empty-state");
                        publicationsContainer.getChildren().add(emptyLabel);
                    }

                    loadStatistics(); // refresh counters
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
            content.append("Prix: ").append(pub.getFormattedPrice()).append("\n");
            content.append("Statut: ").append(pub.getStatus().getLabel()).append("\n\n");
            content.append("Description:\n").append(pub.getMessage()).append("\n\n");
            if (pub.getLocalisation() != null)
                content.append("Localisation: ").append(pub.getLocalisation()).append("\n");
            content.append("Publié: ").append(pub.getFormattedDate()).append("\n");
            content.append("Vues: ").append(pub.getVues() + 1);

            alert.setContentText(content.toString());
            alert.showAndWait();

            loadPublications();
            loadStatistics();
        } catch (Exception e) {
            System.err.println("Error viewing publication: " + e.getMessage());
        }
    }

    @FXML
    private void goToCreatePublication() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Create_Publication.fxml"));
            Scene scene = new Scene(loader.load());

            CreatePublication_controller controller = loader.getController();
            controller.setCurrentStudentId(currentStudentId);

            Stage stage = getStage();
            if (stage != null) {
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
        loadPublications();
    }

    @FXML
    private void showMyPublications() {
        showingMyPublications = true;
        updateTabStyles();
        loadMyPublications();
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

    private void loadMyPublications() {
        try {
            if (publicationsContainer == null) return;
            List<Publications> publications = gestionPublication.afficherPublicationsParEtudiant(currentStudentId);
            publicationsContainer.getChildren().clear();

            if (publications == null || publications.isEmpty()) {
                Label emptyLabel = new Label("Vous n'avez pas encore de publications.\nCliquez sur '+ Créer une publication' pour commencer.");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280; -fx-padding: 60px; -fx-text-alignment: center;");
                emptyLabel.setWrapText(true);
                publicationsContainer.getChildren().add(emptyLabel);
                return;
            }

            publications = applyFilters(publications);
            for (Publications pub : publications) {
                publicationsContainer.getChildren().add(createPublicationCard(pub));
            }
        } catch (Exception e) {
            System.err.println("Error loading my publications: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger vos publications: " + e.getMessage(), Alert.AlertType.ERROR);
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
        loadPublications();
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