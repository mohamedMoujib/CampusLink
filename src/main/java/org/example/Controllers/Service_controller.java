package org.example.Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import org.example.campusLink.Services.Gestion_Service;
import org.example.campusLink.Services.Gestion_Categorie;
import org.example.campusLink.entities.Services;
import org.example.campusLink.entities.Categorie;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class Service_controller {

    @FXML
    private FlowPane servicesContainer;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> statusFilter;

    @FXML
    private ComboBox<String> categoryFilter;

    private Gestion_Service serviceManager;
    private Gestion_Categorie categoryManager;
    private List<Services> baseServices = new ArrayList<>();

    @FXML
    public void initialize() {
        System.out.println("Initializing Service_controller...");
        try {
            serviceManager = new Gestion_Service();
            categoryManager = new Gestion_Categorie();
            setupFilters();
            reloadBaseServices();
            System.out.println("Service_controller initialized successfully");
        } catch (Exception e) {
            System.err.println("Error initializing Service_controller: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les services: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupFilters() {
        if (statusFilter != null) {
            statusFilter.setItems(FXCollections.observableArrayList(
                    "Tous", "Actif", "En attente", "Inactif", "Rejeté"
            ));
            statusFilter.setValue("Tous");
            statusFilter.setOnAction(e -> updateDisplayedServices());
        }

        if (categoryFilter != null) {
            try {
                List<Categorie> cats = categoryManager.afficherCategories();
                List<String> names = new ArrayList<>();
                names.add("Toutes");
                for (Categorie c : cats) {
                    if (c != null && c.getName() != null && !c.getName().isBlank()) {
                        names.add(c.getName());
                    }
                }
                categoryFilter.setItems(FXCollections.observableArrayList(names));
                categoryFilter.setValue("Toutes");
                categoryFilter.setOnAction(e -> updateDisplayedServices());
            } catch (Exception e) {
                // Fallback: keep filter usable even if categories fail to load
                categoryFilter.setItems(FXCollections.observableArrayList("Toutes"));
                categoryFilter.setValue("Toutes");
                categoryFilter.setOnAction(ev -> updateDisplayedServices());
            }
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> updateDisplayedServices());
        }
    }

    /* ================= LOAD SERVICES ================= */

    private void reloadBaseServices() {
        try {
            System.out.println("Loading services...");
            baseServices = serviceManager.afficherServices();
            updateDisplayedServices();

        } catch (Exception e) {
            System.err.println("Error loading services: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du chargement des services: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateDisplayedServices() {
        if (servicesContainer == null) return;

        servicesContainer.getChildren().clear();

        if (baseServices == null || baseServices.isEmpty()) {
            System.out.println("No services available");
            Label emptyLabel = new Label("Aucun service disponible.\nCliquez sur '+ Créer un nouveau service' pour commencer.");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280; -fx-padding: 60px; -fx-text-alignment: center;");
            emptyLabel.setWrapText(true);
            servicesContainer.getChildren().add(emptyLabel);
            return;
        }

        List<Services> filtered = applyFilters(baseServices);

        if (filtered.isEmpty()) {
            Label emptyLabel = new Label("Aucun service ne correspond aux filtres sélectionnés.");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280; -fx-padding: 60px; -fx-text-alignment: center;");
            emptyLabel.setWrapText(true);
            servicesContainer.getChildren().add(emptyLabel);
            return;
        }

        System.out.println("Loaded " + filtered.size() + " services (filtered)");
        for (Services s : filtered) {
            servicesContainer.getChildren().add(createModernServiceCard(s));
        }
    }

    private List<Services> applyFilters(List<Services> services) {
        List<Services> out = services;

        // Status
        if (statusFilter != null) {
            String val = statusFilter.getValue();
            if (val != null && !val.equals("Tous")) {
                String target = switch (val) {
                    case "Actif" -> "ACTIF";
                    case "En attente" -> "EN_ATTENTE";
                    case "Inactif" -> "INACTIF";
                    case "Rejeté" -> "REJETE";
                    default -> null;
                };
                if (target != null) {
                    out = out.stream().filter(s -> {
                        String st = s.getStatus();
                        if (st == null || st.isBlank()) st = "ACTIF";
                        return st.equalsIgnoreCase(target);
                    }).toList();
                }
            }
        }

        // Category (by name)
        if (categoryFilter != null) {
            String val = categoryFilter.getValue();
            if (val != null && !val.equals("Toutes")) {
                String wanted = val.toLowerCase(Locale.ROOT);
                out = out.stream().filter(s -> {
                    String cat = s.getCategoryName();
                    return cat != null && cat.toLowerCase(Locale.ROOT).equals(wanted);
                }).toList();
            }
        }

        // Keyword
        String keyword = searchField != null ? searchField.getText() : null;
        if (keyword != null) {
            String k = keyword.trim();
            if (!k.isEmpty()) {
                String needle = k.toLowerCase(Locale.ROOT);
                out = out.stream().filter(s -> {
                    String title = s.getTitle();
                    String desc = s.getDescription();
                    String cat = s.getCategoryName();
                    String prest = s.getPrestataireName();
                    return (title != null && title.toLowerCase(Locale.ROOT).contains(needle)) ||
                            (desc != null && desc.toLowerCase(Locale.ROOT).contains(needle)) ||
                            (cat != null && cat.toLowerCase(Locale.ROOT).contains(needle)) ||
                            (prest != null && prest.toLowerCase(Locale.ROOT).contains(needle));
                }).toList();
            }
        }

        return out;
    }

    /* ================= MODERN CARD ================= */

    private VBox createModernServiceCard(Services s) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setPrefWidth(380);
        card.setMinHeight(200);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);"
        );

        // ===== HEADER: Title + Status Badge =====
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(s.getTitle() != null ? s.getTitle() : "Sans titre");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        title.setWrapText(true);
        title.setMaxWidth(250);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label statusBadge = new Label(getStatusLabel(s.getStatus()));
        statusBadge.setStyle(getStatusBadgeStyle(s.getStatus()));

        header.getChildren().addAll(title, spacer, statusBadge);

        // ===== CATEGORY (nom, pas l'ID) =====
        Label category = new Label(s.getCategoryDisplayName());
        category.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        // ===== DESCRIPTION (description complète du service) =====
        String desc = s.getDescription() != null && !s.getDescription().isEmpty() ? s.getDescription() : "Pas de description";
        Label description = new Label(desc);
        description.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151; -fx-wrap-text: true;");
        description.setWrapText(true);
        description.setMaxWidth(340);
        description.setMaxHeight(90);

        // ===== INFO: Duration + Reservations =====
        HBox info = new HBox(30);
        info.setAlignment(Pos.CENTER_LEFT);

        Label duration = new Label("1h");
        duration.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        Label reservations = new Label("0 réservations");
        reservations.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        info.getChildren().addAll(duration, reservations);

        // ===== FOOTER: Price + Actions =====
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        Label price = new Label(String.format("%.0f€", s.getPrice()));
        price.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #10b981;");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, javafx.scene.layout.Priority.ALWAYS);

        Button editBtn = new Button("✏");
        editBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-font-size: 18px;" +
                        "-fx-cursor: hand;" +
                        "-fx-text-fill: #6b7280;"
        );
        editBtn.setOnAction(e -> editService(s));
        editBtn.setOnMouseEntered(e -> editBtn.setStyle(
                "-fx-background-color: #f3f4f6;" +
                        "-fx-font-size: 18px;" +
                        "-fx-cursor: hand;" +
                        "-fx-text-fill: #111827;" +
                        "-fx-background-radius: 6;"
        ));
        editBtn.setOnMouseExited(e -> editBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-font-size: 18px;" +
                        "-fx-cursor: hand;" +
                        "-fx-text-fill: #6b7280;"
        ));

        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-font-size: 18px;" +
                        "-fx-cursor: hand;" +
                        "-fx-text-fill: #6b7280;"
        );
        deleteBtn.setOnAction(e -> deleteService(s));
        deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(
                "-fx-background-color: #fee2e2;" +
                        "-fx-font-size: 18px;" +
                        "-fx-cursor: hand;" +
                        "-fx-text-fill: #dc2626;" +
                        "-fx-background-radius: 6;"
        ));
        deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-font-size: 18px;" +
                        "-fx-cursor: hand;" +
                        "-fx-text-fill: #6b7280;"
        ));

        footer.getChildren().addAll(price, footerSpacer, editBtn, deleteBtn);

        // ===== ASSEMBLE THE CARD IN THE CORRECT ORDER =====
        // 1. Header (title + badge)
        card.getChildren().add(header);

        // 2. Image (only if available) — added AFTER header, BEFORE other content
        if (s.getImage() != null && !s.getImage().isEmpty()) {
            try {
                String imagePath = "uploads/services/" + s.getImage();
                File imageFile = new File(imagePath);

                // Debug: print the absolute path to help diagnose missing images
                System.out.println("Looking for image at: " + imageFile.getAbsolutePath());

                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(340);
                    imageView.setFitHeight(200);
                    imageView.setPreserveRatio(true);
                    imageView.setStyle(
                            "-fx-background-radius: 8;" +
                                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);"
                    );
                    card.getChildren().add(imageView);
                    System.out.println("✅ Image loaded: " + imagePath);
                } else {
                    System.err.println("❌ Image file not found: " + imageFile.getAbsolutePath());
                }
            } catch (Exception e) {
                System.err.println("Error loading service image: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // 3. Remaining card content
        card.getChildren().addAll(category, description, info, footer);

        // ===== HOVER EFFECT =====
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #10b981;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(16,185,129,0.15), 15, 0, 0, 4);" +
                        "-fx-cursor: hand;"
        ));

        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);"
        ));

        return card;
    }

    /**
     * Get status label text
     */
    private String getStatusLabel(String status) {
        if (status == null) return "Actif";
        return switch (status.toUpperCase()) {
            case "EN_ATTENTE" -> "En attente";
            case "ACTIF" -> "Actif";
            case "INACTIF" -> "Inactif";
            case "REJETE" -> "Rejeté";
            default -> "Actif";
        };
    }

    /**
     * Get status badge style
     */
    private String getStatusBadgeStyle(String status) {
        if (status == null || status.equalsIgnoreCase("ACTIF")) {
            return "-fx-background-color: #d1fae5;" +
                    "-fx-text-fill: #065f46;" +
                    "-fx-font-size: 11px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 4 10;" +
                    "-fx-background-radius: 12;";
        } else if (status.equalsIgnoreCase("EN_ATTENTE")) {
            return "-fx-background-color: #fef3c7;" +
                    "-fx-text-fill: #92400e;" +
                    "-fx-font-size: 11px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 4 10;" +
                    "-fx-background-radius: 12;";
        } else {
            return "-fx-background-color: #f3f4f6;" +
                    "-fx-text-fill: #6b7280;" +
                    "-fx-font-size: 11px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-padding: 4 10;" +
                    "-fx-background-radius: 12;";
        }
    }

    /* ================= ACTIONS ================= */

    /**
     * Edit a service
     */
    private void editService(Services service) {
        System.out.println("Editing service: " + service.getTitle());

        try {
            TextInputDialog titleDialog = new TextInputDialog(service.getTitle());
            titleDialog.setTitle("Modifier le service");
            titleDialog.setHeaderText("Modifier: " + service.getTitle());
            titleDialog.setContentText("Nouveau titre:");

            Optional<String> titleResult = titleDialog.showAndWait();

            if (titleResult.isPresent() && !titleResult.get().trim().isEmpty()) {

                TextInputDialog descDialog = new TextInputDialog(service.getDescription());
                descDialog.setTitle("Modifier le service");
                descDialog.setHeaderText("Description");
                descDialog.setContentText("Nouvelle description:");

                Optional<String> descResult = descDialog.showAndWait();

                if (descResult.isPresent()) {

                    TextInputDialog priceDialog = new TextInputDialog(String.valueOf(service.getPrice()));
                    priceDialog.setTitle("Modifier le service");
                    priceDialog.setHeaderText("Prix");
                    priceDialog.setContentText("Nouveau prix:");

                    Optional<String> priceResult = priceDialog.showAndWait();

                    if (priceResult.isPresent()) {
                        service.setTitle(titleResult.get().trim());
                        service.setDescription(descResult.get().trim());
                        service.setPrice(Double.parseDouble(priceResult.get().trim()));

                        serviceManager.modifierService(service);
                        reloadBaseServices();

                        showAlert("Succès", "Service modifié avec succès!", Alert.AlertType.INFORMATION);
                    }
                }
            }

        } catch (NumberFormatException e) {
            showAlert("Erreur", "Le prix doit être un nombre valide", Alert.AlertType.ERROR);
        } catch (Exception e) {
            System.err.println("Error editing service: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de modifier le service: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Delete a service
     */
    private void deleteService(Services service) {
        System.out.println("Deleting service: " + service.getTitle());

        try {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirmation");
            confirmAlert.setHeaderText("Supprimer le service");
            confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer '" + service.getTitle() + "' ?");

            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == javafx.scene.control.ButtonType.OK) {
                    try {
                        serviceManager.supprimerService(service.getId());
                        reloadBaseServices();
                        showAlert("Succès", "Service supprimé avec succès", Alert.AlertType.INFORMATION);
                    } catch (Exception e) {
                        System.err.println("Error deleting service: " + e.getMessage());
                        e.printStackTrace();
                        showAlert("Erreur", "Impossible de supprimer le service: " + e.getMessage(), Alert.AlertType.ERROR);
                    }
                }
            });

        } catch (Exception e) {
            System.err.println("Error in delete confirmation: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /* ================= NAVIGATE CREATE ================= */

    @FXML
    private void goToCreateService() {
        System.out.println("Navigating to create service...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Create_Service.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) servicesContainer.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Créer un service");

        } catch (Exception e) {
            System.err.println("Error navigating to create service: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la page de création: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void resetFilters() {
        if (searchField != null) searchField.clear();
        if (statusFilter != null) statusFilter.setValue("Tous");
        if (categoryFilter != null) categoryFilter.setValue("Toutes");
        updateDisplayedServices();
    }

    /**
     * Utility method to show alerts
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}