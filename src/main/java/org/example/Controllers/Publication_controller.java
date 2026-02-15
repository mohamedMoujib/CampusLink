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

/**
 * Controller pour la page Publications
 * Affiche toutes les publications des étudiants (ventes et demandes)
 */
public class Publication_controller {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private FlowPane publicationsContainer;

    // Tab buttons
    @FXML private Button allPublicationsTab;
    @FXML private Button myPublicationsTab;
    @FXML private Label publicationsCountLabel;

    // Statistiques (optional - may not be in FXML)
    @FXML private Label statsTotal;
    @FXML private Label statsVentes;
    @FXML private Label statsDemandes;
    @FXML private Label statsActives;

    private Gestion_publication gestionPublication;
    private int currentStudentId = 1; // TODO: Replace with session
    private boolean showingMyPublications = false; // Track which view is active

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

    /**
     * Configuration des filtres
     */
    private void setupFilters() {
        // Type de publication
        if (typeFilter != null) {
            typeFilter.setItems(FXCollections.observableArrayList(
                    "Tous",
                    "🏷️ Ventes",
                    "🔍 Demandes de services"
            ));
            typeFilter.setValue("Tous");
            typeFilter.setOnAction(e -> loadPublications());
        }

        // Statut
        if (statusFilter != null) {
            statusFilter.setItems(FXCollections.observableArrayList(
                    "Tous",
                    "Actif",
                    "En cours",
                    "Terminé",
                    "Annulé"
            ));
            statusFilter.setValue("Tous");
            statusFilter.setOnAction(e -> loadPublications());
        }

        // Recherche
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal.length() >= 2) {
                    searchPublications(newVal);
                } else if (newVal == null || newVal.isEmpty()) {
                    loadPublications();
                }
            });
        }
    }

    /**
     * Charger les statistiques
     */
    private void loadStatistics() {
        try {
            List<Publications> all = gestionPublication.afficherPublications();

            int total = all.size();
            int ventes = (int) all.stream()
                    .filter(p -> p.getTypePublication() == TypePublication.VENTE_OBJET)
                    .count();
            int demandes = (int) all.stream()
                    .filter(p -> p.getTypePublication() == TypePublication.DEMANDE_SERVICE)
                    .count();
            int actives = (int) all.stream()
                    .filter(p -> p.getStatus() == StatusPublication.ACTIVE)
                    .count();

            if (statsTotal != null) statsTotal.setText(String.valueOf(total));
            if (statsVentes != null) statsVentes.setText(String.valueOf(ventes));
            if (statsDemandes != null) statsDemandes.setText(String.valueOf(demandes));
            if (statsActives != null) statsActives.setText(String.valueOf(actives));

        } catch (Exception e) {
            System.err.println("Error loading statistics: " + e.getMessage());
        }
    }

    /**
     * Charger et afficher les publications
     */
    private void loadPublications() {
        try {
            System.out.println("Loading publications...");

            if (publicationsContainer == null) {
                System.err.println("publicationsContainer is null!");
                return;
            }

            List<Publications> publications = gestionPublication.afficherPublications();
            publicationsContainer.getChildren().clear();

            if (publications == null || publications.isEmpty()) {
                Label emptyLabel = new Label("Aucune publication disponible pour le moment.");
                emptyLabel.getStyleClass().add("empty-state");
                publicationsContainer.getChildren().add(emptyLabel);
                updatePublicationsCount(0);
                return;
            }

            // Apply filters
            publications = applyFilters(publications);

            // Ajouter les cartes
            for (Publications publication : publications) {
                VBox publicationCard = createPublicationCard(publication);
                publicationsContainer.getChildren().add(publicationCard);
            }

            updatePublicationsCount(publications.size());
            System.out.println("Loaded " + publications.size() + " publications");

        } catch (Exception e) {
            System.err.println("Error loading publications: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les publications: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Rechercher des publications
     */
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

            for (Publications publication : publications) {
                VBox publicationCard = createPublicationCard(publication);
                publicationsContainer.getChildren().add(publicationCard);
            }

        } catch (Exception e) {
            System.err.println("Error searching publications: " + e.getMessage());
        }
    }

    /**
     * Créer une carte de publication
     */
    private VBox createPublicationCard(Publications pub) {
        VBox card = new VBox(12);
        card.getStyleClass().add("publication-card");

        // ===== HEADER: Type + Titre =====
        HBox header = new HBox(10);
        header.getStyleClass().add("card-header");
        header.setAlignment(Pos.CENTER_LEFT);

        // Badge type
        Label typeBadge = new Label(pub.getTypeIcon() + " " + pub.getTypePublication().getLabel());
        typeBadge.getStyleClass().add("type-badge");
        if (pub.isVenteObjet()) {
            typeBadge.getStyleClass().add("type-vente");
        } else {
            typeBadge.getStyleClass().add("type-demande");
        }

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        // Badge statut
        Label statusBadge = new Label(pub.getStatusIcon() + " " + pub.getStatus().getLabel());
        statusBadge.getStyleClass().add("status-badge");
        statusBadge.getStyleClass().add("status-" + pub.getStatus().name().toLowerCase());

        header.getChildren().addAll(typeBadge, spacer1, statusBadge);

        // ===== TITRE =====
        Label title = new Label(pub.getTitre());
        title.getStyleClass().add("card-title");
        title.setMaxWidth(360);
        title.setWrapText(true);

        // ===== IMAGE (si disponible) =====
        if (pub.hasImage()) {
            ImageView imageView = new ImageView();
            imageView.getStyleClass().add("card-image");
            imageView.setFitWidth(360);
            imageView.setFitHeight(200);
            imageView.setPreserveRatio(true);

            try {
                File imageFile = new File(pub.getImageUrl());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    imageView.setImage(image);
                    card.getChildren().add(imageView);
                }
            } catch (Exception e) {
                System.err.println("Error loading image: " + e.getMessage());
            }
        }

        // ===== INFOS =====
        VBox infoBox = new VBox(6);

        // Étudiant
        HBox studentRow = new HBox(8);
        studentRow.getStyleClass().add("card-info-row");
        Label studentIcon = new Label("👤");
        studentIcon.getStyleClass().add("card-info-icon");
        Label studentText = new Label("Étudiant #" + pub.getStudentId());
        studentText.getStyleClass().add("card-info-text");
        studentRow.getChildren().addAll(studentIcon, studentText);

        // Localisation
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

        // Date
        HBox dateRow = new HBox(8);
        dateRow.getStyleClass().add("card-info-row");
        Label dateIcon = new Label("🕐");
        dateIcon.getStyleClass().add("card-info-icon");
        Label dateText = new Label(pub.getRelativeTime());
        dateText.getStyleClass().add("card-info-text");
        dateRow.getChildren().addAll(dateIcon, dateText);

        infoBox.getChildren().addAll(studentRow, dateRow);

        // ===== DESCRIPTION =====
        Label description = new Label(pub.getMessagePreview(150));
        description.getStyleClass().add("card-description");
        description.setWrapText(true);
        description.setMaxWidth(360);
        description.setMaxHeight(60);

        // ===== FOOTER: Prix + Vues + Bouton =====
        HBox footer = new HBox(15);
        footer.getStyleClass().add("card-footer");

        // Prix + vues
        VBox priceBox = new VBox(2);
        priceBox.getStyleClass().add("card-price-container");

        Label price = new Label(pub.getFormattedPrice());
        price.getStyleClass().add("card-price");

        Label views = new Label("👁 " + pub.getVues() + " vues");
        views.getStyleClass().add("card-views");

        priceBox.getChildren().addAll(price, views);

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        // Bouton Contacter
        Button contactBtn = new Button("Voir détails");
        contactBtn.getStyleClass().add("btn-contact");
        contactBtn.setOnAction(e -> viewPublicationDetails(pub));

        footer.getChildren().addAll(priceBox, footerSpacer, contactBtn);

        // ===== ASSEMBLER LA CARTE =====
        card.getChildren().addAll(header, title, infoBox, description, footer);

        return card;
    }

    /**
     * Voir les détails d'une publication
     */
    private void viewPublicationDetails(Publications pub) {
        try {
            // Incrémenter le compteur de vues
            gestionPublication.incrementerVues(pub.getId());

            // Afficher les détails dans un dialogue
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Détails de la Publication");
            alert.setHeaderText(pub.getTypeIcon() + " " + pub.getTitre());

            StringBuilder content = new StringBuilder();
            content.append("Type: ").append(pub.getTypePublication().getLabel()).append("\n");
            content.append("Prix: ").append(pub.getFormattedPrice()).append("\n");
            content.append("Statut: ").append(pub.getStatus().getLabel()).append("\n\n");

            content.append("Description:\n").append(pub.getMessage()).append("\n\n");

            if (pub.getLocalisation() != null) {
                content.append("Localisation: ").append(pub.getLocalisation()).append("\n");
            }

            content.append("Publié: ").append(pub.getFormattedDate()).append("\n");
            content.append("Vues: ").append(pub.getVues() + 1);

            alert.setContentText(content.toString());
            alert.showAndWait();

            // Rafraîchir l'affichage
            loadPublications();
            loadStatistics();

        } catch (Exception e) {
            System.err.println("Error viewing publication: " + e.getMessage());
        }
    }

    /**
     * Navigation vers la page de création
     */
    @FXML
    private void goToCreatePublication() {
        try {
            System.out.println("Navigating to create publication...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Create_Publication.fxml"));
            Scene scene = new Scene(loader.load());

            // Passer l'ID de l'étudiant
            CreatePublication_controller controller = loader.getController();
            controller.setCurrentStudentId(currentStudentId);

            // Get stage from any available component
            Stage stage = null;
            if (publicationsContainer != null && publicationsContainer.getScene() != null) {
                stage = (Stage) publicationsContainer.getScene().getWindow();
            } else if (searchField != null && searchField.getScene() != null) {
                stage = (Stage) searchField.getScene().getWindow();
            }

            if (stage != null) {
                stage.setScene(scene);
                stage.setTitle("Nouvelle Publication");
            } else {
                System.err.println("Could not find stage!");
            }

        } catch (Exception e) {
            System.err.println("Error navigating to create publication: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * ✅ CORRECTION: Navigation vers mes publications (MouseEvent)
     */
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
                VBox card = createPublicationCard(pub);
                publicationsContainer.getChildren().add(card);
            }

        } catch (Exception e) {
            System.err.println("Error loading my publications: " + e.getMessage());
        }
    }

    /**
     * ✅ CORRECTION: Retour à la page étudiant (MouseEvent)
     */
    @FXML
    private void goToStudent(MouseEvent event) {
        try {
            System.out.println("Navigating to student page...");

            Parent root = FXMLLoader.load(getClass().getResource("/Views/Student.fxml"));

            Stage stage = null;
            if (publicationsContainer != null && publicationsContainer.getScene() != null) {
                stage = (Stage) publicationsContainer.getScene().getWindow();
            } else if (searchField != null && searchField.getScene() != null) {
                stage = (Stage) searchField.getScene().getWindow();
            }

            if (stage != null) {
                stage.setScene(new Scene(root));
                stage.setTitle("Rechercher des Services");
            }

        } catch (Exception e) {
            System.err.println("Error navigating to student: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de naviguer: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Navigation vers la page de recherche des services (MouseEvent)
     */
    @FXML
    private void goToServices(MouseEvent event) {
        try {
            System.out.println("Navigating to services page...");

            Parent root = FXMLLoader.load(getClass().getResource("/Views/Student.fxml"));

            Stage stage = null;
            if (publicationsContainer != null && publicationsContainer.getScene() != null) {
                stage = (Stage) publicationsContainer.getScene().getWindow();
            } else if (searchField != null && searchField.getScene() != null) {
                stage = (Stage) searchField.getScene().getWindow();
            }

            if (stage != null) {
                stage.setScene(new Scene(root));
                stage.setTitle("Rechercher des Services");
            }

        } catch (Exception e) {
            System.err.println("Error navigating to services: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de naviguer: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Show all publications (tab switch)
     */
    @FXML
    private void showAllPublications() {
        System.out.println("Showing all publications...");
        showingMyPublications = false;

        // Update tab styles
        updateTabStyles();

        // Load all publications
        loadPublications();
    }

    /**
     * Show only current user's publications (tab switch)
     */
    @FXML
    private void showMyPublications() {
        System.out.println("Showing my publications...");
        showingMyPublications = true;

        // Update tab styles
        updateTabStyles();

        // Load user's publications
        loadMyPublications();
    }

    /**
     * Update tab button styles based on active tab
     */
    private void updateTabStyles() {
        String activeStyle = "-fx-background-color: #5D5FEF; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-cursor: hand; -fx-background-radius: 8; -fx-border-width: 0; -fx-font-weight: bold;";
        String inactiveStyle = "-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280; -fx-font-size: 14px; -fx-padding: 10 20; -fx-cursor: hand; -fx-background-radius: 8; -fx-border-color: #e5e7eb; -fx-border-width: 1;";

        if (showingMyPublications) {
            if (myPublicationsTab != null) myPublicationsTab.setStyle(activeStyle);
            if (allPublicationsTab != null) allPublicationsTab.setStyle(inactiveStyle);
        } else {
            if (allPublicationsTab != null) allPublicationsTab.setStyle(activeStyle);
            if (myPublicationsTab != null) myPublicationsTab.setStyle(inactiveStyle);
        }
    }

    /**
     * Load only current user's publications
     */
    private void loadMyPublications() {
        try {
            System.out.println("Loading my publications for student ID: " + currentStudentId);

            if (publicationsContainer == null) {
                System.err.println("publicationsContainer is null!");
                return;
            }

            List<Publications> publications = gestionPublication.afficherPublicationsParEtudiant(currentStudentId);
            publicationsContainer.getChildren().clear();

            if (publications == null || publications.isEmpty()) {
                Label emptyLabel = new Label("Vous n'avez pas encore de publications.\nCliquez sur '+ Créer une publication' pour commencer.");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280; -fx-padding: 60px; -fx-text-alignment: center;");
                emptyLabel.setWrapText(true);
                publicationsContainer.getChildren().add(emptyLabel);
                updatePublicationsCount(0);
                return;
            }

            // Apply filters if any
            publications = applyFilters(publications);

            // Add publication cards
            for (Publications publication : publications) {
                VBox publicationCard = createPublicationCard(publication);
                publicationsContainer.getChildren().add(publicationCard);
            }

            updatePublicationsCount(publications.size());
            System.out.println("Loaded " + publications.size() + " publications for current user");

        } catch (Exception e) {
            System.err.println("Error loading my publications: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger vos publications: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Apply filters to publications list
     */
    private List<Publications> applyFilters(List<Publications> publications) {
        // Apply type filter
        if (typeFilter != null) {
            String typeFilterValue = typeFilter.getValue();
            if (typeFilterValue != null && !typeFilterValue.equals("Tous")) {
                if (typeFilterValue.contains("Ventes")) {
                    publications = publications.stream()
                            .filter(p -> p.getTypePublication() == TypePublication.VENTE_OBJET)
                            .toList();
                } else if (typeFilterValue.contains("Demandes")) {
                    publications = publications.stream()
                            .filter(p -> p.getTypePublication() == TypePublication.DEMANDE_SERVICE)
                            .toList();
                }
            }
        }

        // Apply status filter
        if (statusFilter != null) {
            String statusValue = statusFilter.getValue();
            if (statusValue != null && !statusValue.equals("Tous")) {
                StatusPublication targetStatus = switch (statusValue) {
                    case "Actif" -> StatusPublication.ACTIVE;
                    case "En cours" -> StatusPublication.EN_COURS;
                    case "Terminé" -> StatusPublication.TERMINEE;
                    case "Annulé" -> StatusPublication.ANNULEE;
                    default -> null;
                };
                if (targetStatus != null) {
                    StatusPublication finalTargetStatus = targetStatus;
                    publications = publications.stream()
                            .filter(p -> p.getStatus() == finalTargetStatus)
                            .toList();
                }
            }
        }

        return publications;
    }

    /**
     * Update publications count label
     */
    private void updatePublicationsCount(int count) {
        if (publicationsCountLabel != null) {
            String text = count + " publication" + (count > 1 ? "s" : "");
            publicationsCountLabel.setText(text);
        }
    }

    /**
     * Rafraîchir les publications
     */
    @FXML
    private void refreshPublications() {
        loadStatistics();
        loadPublications();
    }

    /**
     * Afficher une alerte
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}