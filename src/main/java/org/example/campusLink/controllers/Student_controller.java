package org.example.campusLink.controllers;

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
import javafx.scene.Node;

import org.example.campusLink.services.Gestion_Service;
import org.example.campusLink.entities.User;
import org.example.campusLink.entities.Services;
import org.example.campusLink.utils.AppSession;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class Student_controller {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categorieCombo;
    @FXML private ComboBox<String> tarifCombo;
    @FXML private ComboBox<String> trierCombo;
    @FXML private GridPane servicesGrid;
    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;

    private Gestion_Service gestionService;
    private User currentUser;
    private int currentStudentId = 1;

    /** Appelé après login ou quand la vue est rechargée (ex. retour depuis Publications). */
    public void setUser(User user) {
        this.currentUser = user;
        if (user != null) {
            currentStudentId = user.getId();
            AppSession.setCurrentUser(user);
            if (userNameLabel != null) userNameLabel.setText(user.getName());
            if (userEmailLabel != null) userEmailLabel.setText(user.getEmail());
        }
    }

    @FXML
    public void initialize() {
        System.out.println("Initializing Student_controller (Search Page)...");
        try {
            gestionService = new Gestion_Service();
            User sessionUser = AppSession.getCurrentUser();
            if (sessionUser != null) {
                currentStudentId = sessionUser.getId();
                currentUser = sessionUser;
                if (userNameLabel != null) userNameLabel.setText(sessionUser.getName());
                if (userEmailLabel != null) userEmailLabel.setText(sessionUser.getEmail());
            }
            setupFilters();
            loadServices();
            System.out.println("Student_controller initialized successfully");
        } catch (Exception e) {
            System.err.println("Error initializing Student_controller: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur d'initialisation: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupFilters() {
        categorieCombo.setItems(FXCollections.observableArrayList(
                "Tous", "Mathématiques", "Informatique", "Physique",
                "Chimie", "Langues", "Rédaction"
        ));
        categorieCombo.setValue("Tous");
        categorieCombo.setOnAction(e -> loadServices());

        tarifCombo.setItems(FXCollections.observableArrayList(
                "Tous les tarifs", "Moins de 15€", "15€ - 25€", "25€ - 35€", "Plus de 35€"
        ));
        tarifCombo.setValue("Tous les tarifs");
        tarifCombo.setOnAction(e -> loadServices());

        trierCombo.setItems(FXCollections.observableArrayList(
                "Meilleure note", "Prix croissant", "Prix décroissant", "Plus récent"
        ));
        trierCombo.setValue("Meilleure note");
        trierCombo.setOnAction(e -> loadServices());

        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadServices());
    }

    private void loadServices() {
        try {
            System.out.println("Loading services...");

            List<Services> services = gestionService.afficherServices();
            List<Services> filtered = applyFilters(services);
            servicesGrid.getChildren().clear();

            if (services == null || services.isEmpty()) {
                Label emptyLabel = new Label("Aucun service disponible pour le moment.");
                emptyLabel.getStyleClass().add("empty-state");
                servicesGrid.add(emptyLabel, 0, 0, 3, 1);
                return;
            }

            if (filtered.isEmpty()) {
                Label emptyLabel = new Label("Aucun service ne correspond aux filtres sélectionnés.");
                emptyLabel.getStyleClass().add("empty-state");
                servicesGrid.add(emptyLabel, 0, 0, 3, 1);
                return;
            }

            int row = 0;
            int col = 0;

            for (Services service : filtered) {
                VBox serviceCard = createServiceCard(service);
                servicesGrid.add(serviceCard, col, row);

                col++;
                if (col == 3) {
                    col = 0;
                    row++;
                }
            }

            System.out.println("Loaded " + filtered.size() + " services (filtered)");

        } catch (Exception e) {
            System.err.println("Error loading services: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les services: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private List<Services> applyFilters(List<Services> services) {
        if (services == null || services.isEmpty()) {
            return List.of();
        }

        var stream = services.stream();

        // Catégorie (approximation par nom de catégorie affiché)
        if (categorieCombo != null) {
            String val = categorieCombo.getValue();
            if (val != null && !"Tous".equalsIgnoreCase(val)) {
                String needle = val.toLowerCase(Locale.ROOT);
                stream = stream.filter(s -> {
                    String cat = s.getCategoryDisplayName();
                    return cat != null && cat.toLowerCase(Locale.ROOT).contains(needle);
                });
            }
        }

        // Tarif
        if (tarifCombo != null) {
            String val = tarifCombo.getValue();
            if (val != null && !val.startsWith("Tous")) {
                stream = stream.filter(s -> {
                    double p = s.getPrice();
                    return switch (val) {
                        case "Moins de 15€" -> p < 15;
                        case "15€ - 25€" -> p >= 15 && p <= 25;
                        case "25€ - 35€" -> p >= 25 && p <= 35;
                        case "Plus de 35€" -> p > 35;
                        default -> true;
                    };
                });
            }
        }

        // Recherche texte
        String keyword = (searchField != null && searchField.getText() != null)
                ? searchField.getText().trim()
                : null;
        if (keyword != null && !keyword.isEmpty()) {
            String needle = keyword.toLowerCase(Locale.ROOT);
            stream = stream.filter(s -> {
                String title = s.getTitle();
                String desc = s.getDescription();
                String cat = s.getCategoryDisplayName();
                String prestataire = s.getPrestataireDisplayName();
                String priceStr = s.getFormattedPrice();

                return (title != null && title.toLowerCase(Locale.ROOT).contains(needle)) ||
                        (desc != null && desc.toLowerCase(Locale.ROOT).contains(needle)) ||
                        (cat != null && cat.toLowerCase(Locale.ROOT).contains(needle)) ||
                        (prestataire != null && prestataire.toLowerCase(Locale.ROOT).contains(needle)) ||
                        (priceStr != null && priceStr.toLowerCase(Locale.ROOT).contains(needle));
            });
        }

        List<Services> out = stream.toList();

        // Tri
        if (trierCombo != null) {
            String val = trierCombo.getValue();
            if (val != null) {
                Comparator<Services> comparator = switch (val) {
                    case "Prix croissant" -> Comparator.comparingDouble(Services::getPrice);
                    case "Prix décroissant" -> Comparator.comparingDouble(Services::getPrice).reversed();
                    case "Plus récent" -> Comparator.comparingInt(Services::getId).reversed();
                    default -> null; // "Meilleure note" ou autre -> garder l'ordre par défaut
                };
                if (comparator != null) {
                    out = out.stream().sorted(comparator).toList();
                }
            }
        }

        return out;
    }

    /**
     * Builds a service card.
     * Assembly order:
     *   1. header  (title + rating)
     *   2. image   (only when the service has one and the file exists)
     *   3. provider
     *   4. infoBox
     *   5. description
     *   6. footer  (price + reserve button)
     */
    private VBox createServiceCard(Services service) {
        VBox card = new VBox(12);
        card.getStyleClass().add("service-card");

        // ===== 1. HEADER: Title + Rating =====
        HBox header = new HBox(10);
        header.getStyleClass().add("card-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(service.getTitle() != null ? service.getTitle() : "Service");
        title.getStyleClass().add("card-title");
        title.setMaxWidth(280);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox rating = new HBox(5);
        rating.getStyleClass().add("card-rating");
        Label star = new Label("⭐");
        star.getStyleClass().add("rating-star");
        Label ratingValue = new Label("4.9");
        ratingValue.getStyleClass().add("rating-value");
        rating.getChildren().addAll(star, ratingValue);

        header.getChildren().addAll(title, spacer, rating);

        // ===== 2. IMAGE (if available) =====
        // Build all nodes first, then add them in order at the end.
        ImageView imageView = null;
        if (service.getImage() != null && !service.getImage().isEmpty()) {
            String imagePath = "uploads/services/" + service.getImage();
            File imageFile = new File(imagePath);

            System.out.println("Looking for image at: " + imageFile.getAbsolutePath());

            if (imageFile.exists()) {
                try {
                    Image image = new Image(imageFile.toURI().toString());
                    imageView = new ImageView(image);
                    imageView.setFitWidth(360);
                    imageView.setFitHeight(180);
                    imageView.setPreserveRatio(true);
                    imageView.setStyle(
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);"
                    );
                    System.out.println("✅ Image loaded: " + imagePath);
                } catch (Exception e) {
                    System.err.println("Error loading image: " + e.getMessage());
                    imageView = null;
                }
            } else {
                System.err.println("❌ Image not found: " + imageFile.getAbsolutePath());
            }
        }

        // ===== 3. PROVIDER (nom du tuteur, pas l'ID) =====
        Label provider = new Label(service.getPrestataireDisplayName());
        provider.getStyleClass().add("card-provider");

        // ===== 4. INFO ROWS =====
        VBox infoBox = new VBox(6);

        // Ligne localisation masquée (École Polytechnique retirée)

        HBox durationRow = new HBox(8);
        durationRow.getStyleClass().add("card-info-row");
        Label durationIcon = new Label("🕐");
        durationIcon.getStyleClass().add("card-info-icon");
        Label durationText = new Label("1h");
        durationText.getStyleClass().add("card-info-text");
        durationRow.getChildren().addAll(durationIcon, durationText);

        HBox subjectRow = new HBox(8);
        subjectRow.getStyleClass().add("card-info-row");
        Label subjectIcon = new Label("📚");
        subjectIcon.getStyleClass().add("card-info-icon");
        Label subjectText = new Label(service.getCategoryDisplayName());
        subjectText.getStyleClass().add("card-info-text");
        subjectRow.getChildren().addAll(subjectIcon, subjectText);

        infoBox.getChildren().addAll(durationRow, subjectRow);

        // ===== 5. DESCRIPTION (description complète du service) =====
        String desc = service.getDescription() != null && !service.getDescription().isEmpty()
                ? service.getDescription()
                : "Aide personnalisée avec méthodes pédagogiques adaptées.";
        Label description = new Label(desc);
        description.getStyleClass().add("card-description");
        description.setWrapText(true);
        description.setMaxWidth(360);
        description.setMaxHeight(90);

        // ===== 6. FOOTER: Price + Reserve Button =====
        HBox footer = new HBox(15);
        footer.getStyleClass().add("card-footer");

        VBox priceBox = new VBox(2);
        priceBox.getStyleClass().add("card-price-container");

        Label price = new Label(String.format("%.0f€", service.getPrice()));
        price.getStyleClass().add("card-price");

        Label reviews = new Label("127 avis");
        reviews.getStyleClass().add("card-reviews");

        priceBox.getChildren().addAll(price, reviews);

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Button reserveBtn = new Button("Réserver");
        reserveBtn.getStyleClass().add("btn-reserve");


        footer.getChildren().addAll(priceBox, footerSpacer, reserveBtn);

        // ===== ASSEMBLE IN CORRECT ORDER =====
        card.getChildren().add(header);

        if (imageView != null) {
            card.getChildren().add(imageView); // only added when image loaded successfully
        }

        card.getChildren().addAll(provider, infoBox, description, footer);

        return card;
    }

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
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Mes Services");

        } catch (Exception e) {
            System.err.println("Error navigating to services: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de naviguer: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }




}