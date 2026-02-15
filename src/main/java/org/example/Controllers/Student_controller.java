package org.example.Controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.Node;

import org.example.campusLink.Services.Gestion_Service;
import org.example.campusLink.entities.Services;

import java.util.List;

public class Student_controller {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categorieCombo;
    @FXML private ComboBox<String> tarifCombo;
    @FXML private ComboBox<String> trierCombo;
    @FXML private GridPane servicesGrid;

    private Gestion_Service gestionService;

    // TODO: Replace with actual logged-in student ID from session
    private int currentStudentId = 1; // Hardcoded for now

    @FXML
    public void initialize() {
        System.out.println("Initializing Student_controller (Search Page)...");

        try {
            gestionService = new Gestion_Service();

            setupFilters();
            loadServices();

            System.out.println("Student_controller initialized successfully");

        } catch (Exception e) {
            System.err.println("Error initializing Student_controller: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur d'initialisation: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * ✅ Configuration des filtres
     */
    private void setupFilters() {
        // Catégories
        categorieCombo.setItems(FXCollections.observableArrayList(
                "Tous",
                "Mathématiques",
                "Informatique",
                "Physique",
                "Chimie",
                "Langues",
                "Rédaction"
        ));
        categorieCombo.setValue("Tous");
        categorieCombo.setOnAction(e -> loadServices());

        // Tarifs
        tarifCombo.setItems(FXCollections.observableArrayList(
                "Tous les tarifs",
                "Moins de 15€",
                "15€ - 25€",
                "25€ - 35€",
                "Plus de 35€"
        ));
        tarifCombo.setValue("Tous les tarifs");
        tarifCombo.setOnAction(e -> loadServices());

        // Tri
        trierCombo.setItems(FXCollections.observableArrayList(
                "Meilleure note",
                "Prix croissant",
                "Prix décroissant",
                "Plus récent"
        ));
        trierCombo.setValue("Meilleure note");
        trierCombo.setOnAction(e -> loadServices());

        // Recherche
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            loadServices();
        });
    }

    /**
     * ✅ Charger et afficher les services
     */
    private void loadServices() {
        try {
            System.out.println("Loading services...");

            List<Services> services = gestionService.afficherServices();
            servicesGrid.getChildren().clear();

            if (services == null || services.isEmpty()) {
                Label emptyLabel = new Label("Aucun service disponible pour le moment.");
                emptyLabel.getStyleClass().add("empty-state");
                servicesGrid.add(emptyLabel, 0, 0, 3, 1);
                return;
            }

            // Créer une grille 3 colonnes
            int row = 0;
            int col = 0;

            for (Services service : services) {
                VBox serviceCard = createServiceCard(service);
                servicesGrid.add(serviceCard, col, row);

                col++;
                if (col == 3) {
                    col = 0;
                    row++;
                }
            }

            System.out.println("Loaded " + services.size() + " services");

        } catch (Exception e) {
            System.err.println("Error loading services: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les services: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * ✅ Créer une carte de service (design de l'image)
     */
    private VBox createServiceCard(Services service) {
        VBox card = new VBox(12);
        card.getStyleClass().add("service-card");

        // ===== HEADER: Titre + Note =====
        HBox header = new HBox(10);
        header.getStyleClass().add("card-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(service.getTitle() != null ? service.getTitle() : "Service");
        title.getStyleClass().add("card-title");
        title.setMaxWidth(280);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Note avec étoile
        HBox rating = new HBox(5);
        rating.getStyleClass().add("card-rating");
        Label star = new Label("⭐");
        star.getStyleClass().add("rating-star");
        Label ratingValue = new Label("4.9");
        ratingValue.getStyleClass().add("rating-value");
        rating.getChildren().addAll(star, ratingValue);

        header.getChildren().addAll(title, spacer, rating);

        // ===== PROVIDER =====
        Label provider = new Label("Prestataire #" + service.getPrestataireId());
        provider.getStyleClass().add("card-provider");

        // ===== INFOS: Lieu, Durée, Matière =====
        VBox infoBox = new VBox(6);

        // Lieu
        HBox locationRow = new HBox(8);
        locationRow.getStyleClass().add("card-info-row");
        Label locationIcon = new Label("📍");
        locationIcon.getStyleClass().add("card-info-icon");
        Label locationText = new Label("École Polytechnique");
        locationText.getStyleClass().add("card-info-text");
        locationRow.getChildren().addAll(locationIcon, locationText);

        // Durée
        HBox durationRow = new HBox(8);
        durationRow.getStyleClass().add("card-info-row");
        Label durationIcon = new Label("🕐");
        durationIcon.getStyleClass().add("card-info-icon");
        Label durationText = new Label("1h");
        durationText.getStyleClass().add("card-info-text");
        durationRow.getChildren().addAll(durationIcon, durationText);

        // Matière
        HBox subjectRow = new HBox(8);
        subjectRow.getStyleClass().add("card-info-row");
        Label subjectIcon = new Label("📚");
        subjectIcon.getStyleClass().add("card-info-icon");
        Label subjectText = new Label("Catégorie #" + service.getCategoryId());
        subjectText.getStyleClass().add("card-info-text");
        subjectRow.getChildren().addAll(subjectIcon, subjectText);

        infoBox.getChildren().addAll(locationRow, durationRow, subjectRow);

        // ===== DESCRIPTION =====
        Label description = new Label(
                service.getDescription() != null && !service.getDescription().isEmpty()
                        ? service.getDescription()
                        : "Aide personnalisée avec méthodes pédagogiques adaptées."
        );
        description.getStyleClass().add("card-description");
        description.setWrapText(true);
        description.setMaxWidth(360);
        description.setMaxHeight(60);

        // ===== FOOTER: Prix + Avis + Bouton =====
        HBox footer = new HBox(15);
        footer.getStyleClass().add("card-footer");

        // Prix + avis
        VBox priceBox = new VBox(2);
        priceBox.getStyleClass().add("card-price-container");

        Label price = new Label(String.format("%.0f€", service.getPrice()));
        price.getStyleClass().add("card-price");

        Label reviews = new Label("127 avis");
        reviews.getStyleClass().add("card-reviews");

        priceBox.getChildren().addAll(price, reviews);

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        // Bouton Réserver
        Button reserveBtn = new Button("Réserver");
        reserveBtn.getStyleClass().add("btn-reserve");
        reserveBtn.setOnAction(e -> goToCreateDemande(service));

        footer.getChildren().addAll(priceBox, footerSpacer, reserveBtn);

        // ===== ASSEMBLER LA CARTE =====
        card.getChildren().addAll(header, provider, infoBox, description, footer);

        return card;
    }

    /**
     * ✅ Navigate to create demand page with pre-filled service
     */
    private void goToCreateDemande(Services service) {
        try {
            System.out.println("Navigating to create demande for service: " + service.getTitle());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Create_Demande.fxml"));
            Scene scene = new Scene(loader.load());

            // Pass service data to the controller
            CreateDemande_controller controller = loader.getController();
            controller.setServiceData(service, currentStudentId);

            Stage stage = (Stage) servicesGrid.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Créer une demande");

        } catch (Exception e) {
            System.err.println("Error navigating to create demande: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * ✅ Méthode utilitaire pour afficher des alertes
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /* ================= NAVIGATION ================= */

    @FXML
    private void goToServices(javafx.event.ActionEvent event) {
        try {
            System.out.println("Navigating to services management...");

            Parent root = FXMLLoader.load(getClass().getResource("/Views/service.fxml"));
            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Mes Services");

        } catch (Exception e) {
            System.err.println("Error navigating to services: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de naviguer: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * ✅ CORRECTION: Méthode pour gérer le clic de souris (MouseEvent)
     */
    @FXML
    private void goToMesDemandes(MouseEvent event) {
        try {
            System.out.println("Navigating to mes demandes...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Demande.fxml"));
            Scene scene = new Scene(loader.load());

            // Pass student ID to the controller
            Demande_controller controller = loader.getController();
            controller.setStudentId(currentStudentId);

            Stage stage = (Stage) servicesGrid.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Mes Demandes");

        } catch (Exception e) {
            System.err.println("Error navigating to mes demandes: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de naviguer: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * ✅ CORRECTION: Méthode pour naviguer vers Publications (MouseEvent)
     */
    @FXML
    private void goToPublications(MouseEvent event) {
        try {
            System.out.println("Navigating to publications...");

            Parent root = FXMLLoader.load(getClass().getResource("/Views/Publication.fxml"));
            Stage stage = (Stage) servicesGrid.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Publications Étudiantes");

        } catch (Exception e) {
            System.err.println("Error navigating to publications: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de naviguer vers Publications: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}