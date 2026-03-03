package org.example.campusLink.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
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
import org.example.campusLink.controllers.users.MainLayoutController;
import org.example.campusLink.entities.Categorie;
import org.example.campusLink.entities.Services;
import org.example.campusLink.entities.User;
import org.example.campusLink.services.Gestion_Categorie;
import org.example.campusLink.services.Gestion_Service;
import org.example.campusLink.utils.TranslationService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class Service_controller {

    @FXML private FlowPane        servicesContainer;
    @FXML private TextField       searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private Button          translateBtn;   // ✅ global translate button

    private Gestion_Service   serviceManager;
    private Gestion_Categorie categoryManager;
    private List<Services>    baseServices            = new ArrayList<>();
    private MainLayoutController mainLayoutController;
    private int               currentPrestataireId    = 0;

    // ✅ Translation state
    private boolean      isTranslated = false;
    // ✅ Each entry: [titleLabel, descLabel, categoryLabel]
    //    userData on each label = original French text
    private final List<Label[]> cardLabels = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════════
    // SETUP
    // ═══════════════════════════════════════════════════════════════

    public void setMainLayoutController(MainLayoutController ctrl) {
        this.mainLayoutController = ctrl;
    }

    public void setUser(User user) {
        if (user != null) {
            this.currentPrestataireId = user.getId();
            System.out.println("✅ Service_controller: prestataire ID set to #"
                    + currentPrestataireId);
            if (serviceManager != null) reloadBaseServices();
        }
    }

    public void setCurrentPrestataireId(int id) {
        this.currentPrestataireId = id;
        System.out.println("✅ Service_controller: prestataire ID set to #"
                + currentPrestataireId);
        if (serviceManager != null) reloadBaseServices();
    }

    @FXML
    public void initialize() {
        System.out.println("Initializing Service_controller...");
        try {
            serviceManager  = new Gestion_Service();
            categoryManager = new Gestion_Categorie();
            setupFilters();
            if (currentPrestataireId > 0) reloadBaseServices();
            System.out.println("Service_controller initialized successfully");
        } catch (Exception e) {
            System.err.println("Error initializing Service_controller: "
                    + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur",
                    "Impossible de charger les services: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // FILTERS
    // ═══════════════════════════════════════════════════════════════

    private void setupFilters() {
        if (statusFilter != null) {
            statusFilter.setItems(FXCollections.observableArrayList(
                    "Tous", "Actif", "En attente", "Inactif", "Rejeté"));
            statusFilter.setValue("Tous");
            statusFilter.setOnAction(e -> updateDisplayedServices());
        }

        if (categoryFilter != null) {
            try {
                List<Categorie> cats = categoryManager.afficherCategories();
                List<String> names = new ArrayList<>();
                names.add("Toutes");
                for (Categorie c : cats) {
                    if (c != null && c.getName() != null
                            && !c.getName().isBlank())
                        names.add(c.getName());
                }
                categoryFilter.setItems(
                        FXCollections.observableArrayList(names));
                categoryFilter.setValue("Toutes");
                categoryFilter.setOnAction(e -> updateDisplayedServices());
            } catch (Exception e) {
                categoryFilter.setItems(
                        FXCollections.observableArrayList("Toutes"));
                categoryFilter.setValue("Toutes");
                categoryFilter.setOnAction(ev -> updateDisplayedServices());
            }
        }

        if (searchField != null) {
            searchField.textProperty().addListener(
                    (obs, oldVal, newVal) -> updateDisplayedServices());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // LOAD
    // ═══════════════════════════════════════════════════════════════

    private void reloadBaseServices() {
        try {
            System.out.println("Loading services for prestataire #"
                    + currentPrestataireId);
            baseServices = serviceManager
                    .afficherServicesParPrestataire(currentPrestataireId);
            System.out.println("Found " + baseServices.size()
                    + " services for prestataire #" + currentPrestataireId);

            // ✅ Reset translation on reload
            isTranslated = false;
            cardLabels.clear();
            updateTranslateButton();
            updateDisplayedServices();

        } catch (Exception e) {
            System.err.println("Error loading services: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur",
                    "Erreur lors du chargement des services: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    private void updateDisplayedServices() {
        if (servicesContainer == null) return;
        servicesContainer.getChildren().clear();
        cardLabels.clear();       // ✅ reset label registry
        isTranslated = false;
        updateTranslateButton();

        if (baseServices == null || baseServices.isEmpty()) {
            Label lbl = new Label(
                    "Vous n'avez pas encore de services.\n" +
                            "Cliquez sur '+ Créer un nouveau service' pour commencer.");
            lbl.setStyle("-fx-font-size:16px;-fx-text-fill:#6b7280;" +
                    "-fx-padding:60px;-fx-text-alignment:center;");
            lbl.setWrapText(true);
            servicesContainer.getChildren().add(lbl);
            return;
        }

        List<Services> filtered = applyFilters(baseServices);

        if (filtered.isEmpty()) {
            Label lbl = new Label(
                    "Aucun service ne correspond aux filtres sélectionnés.");
            lbl.setStyle("-fx-font-size:16px;-fx-text-fill:#6b7280;" +
                    "-fx-padding:60px;-fx-text-alignment:center;");
            lbl.setWrapText(true);
            servicesContainer.getChildren().add(lbl);
            return;
        }

        System.out.println("Displaying " + filtered.size() + " services");
        for (Services s : filtered) {
            servicesContainer.getChildren().add(createModernServiceCard(s));
        }
    }

    private List<Services> applyFilters(List<Services> services) {
        List<Services> out = services;

        if (statusFilter != null) {
            String val = statusFilter.getValue();
            if (val != null && !val.equals("Tous")) {
                String target = switch (val) {
                    case "Actif"      -> "ACTIF";
                    case "En attente" -> "EN_ATTENTE";
                    case "Inactif"    -> "INACTIF";
                    case "Rejeté"     -> "REJETE";
                    default           -> null;
                };
                if (target != null) {
                    final String t = target;
                    out = out.stream().filter(s -> {
                        String st = s.getStatus();
                        if (st == null || st.isBlank()) st = "ACTIF";
                        return st.equalsIgnoreCase(t);
                    }).toList();
                }
            }
        }

        if (categoryFilter != null) {
            String val = categoryFilter.getValue();
            if (val != null && !val.equals("Toutes")) {
                String wanted = val.toLowerCase(Locale.ROOT);
                out = out.stream().filter(s -> {
                    String cat = s.getCategoryName();
                    return cat != null &&
                            cat.toLowerCase(Locale.ROOT).equals(wanted);
                }).toList();
            }
        }

        String keyword = searchField != null ? searchField.getText() : null;
        if (keyword != null) {
            String k = keyword.trim();
            if (!k.isEmpty()) {
                String needle = k.toLowerCase(Locale.ROOT);
                out = out.stream().filter(s -> {
                    String t1 = s.getTitle();
                    String d  = s.getDescription();
                    String c  = s.getCategoryName();
                    String p  = s.getPrestataireName();
                    return (t1 != null && t1.toLowerCase(Locale.ROOT).contains(needle)) ||
                            (d  != null && d .toLowerCase(Locale.ROOT).contains(needle)) ||
                            (c  != null && c .toLowerCase(Locale.ROOT).contains(needle)) ||
                            (p  != null && p .toLowerCase(Locale.ROOT).contains(needle));
                }).toList();
            }
        }
        return out;
    }

    // ═══════════════════════════════════════════════════════════════
    // CARD BUILDER
    // ═══════════════════════════════════════════════════════════════

    private VBox createModernServiceCard(Services s) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setPrefWidth(380);
        card.setMinHeight(200);
        card.setStyle(
                "-fx-background-color:white;-fx-background-radius:12;" +
                        "-fx-border-color:#e5e7eb;-fx-border-width:1;" +
                        "-fx-border-radius:12;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),10,0,0,2);"
        );

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(
                s.getTitle() != null ? s.getTitle() : "Sans titre");
        titleLbl.setStyle(
                "-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#111827;");
        titleLbl.setWrapText(true);
        titleLbl.setMaxWidth(250);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label statusBadge = new Label(getStatusLabel(s.getStatus()));
        statusBadge.setStyle(getStatusBadgeStyle(s.getStatus()));

        header.getChildren().addAll(titleLbl, spacer, statusBadge);
        card.getChildren().add(header);

        // Image
        if (s.getImage() != null && !s.getImage().isEmpty()) {
            try {
                File imageFile = new File(
                        "uploads/services/" + s.getImage());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(340);
                    imageView.setFitHeight(200);
                    imageView.setPreserveRatio(true);
                    card.getChildren().add(imageView);
                }
            } catch (Exception e) {
                System.err.println("Error loading image: " + e.getMessage());
            }
        }

        // Category
        Label categoryLbl = new Label(s.getCategoryDisplayName());
        categoryLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#6b7280;");

        // Description
        String descText = (s.getDescription() != null
                && !s.getDescription().isEmpty())
                ? s.getDescription() : "Pas de description";
        Label descLbl = new Label(descText);
        descLbl.setStyle(
                "-fx-font-size:14px;-fx-text-fill:#374151;");
        descLbl.setWrapText(true);
        descLbl.setMaxWidth(340);
        descLbl.setMaxHeight(90);

        // ✅ Store originals in userData
        String originalTitle = s.getTitle() != null ? s.getTitle() : "Sans titre";
        String originalDesc  = descText;
        String originalCat   = s.getCategoryDisplayName();

        titleLbl.setUserData(originalTitle);
        descLbl.setUserData(originalDesc);
        categoryLbl.setUserData(originalCat);

        // ✅ Register for batch translation
        cardLabels.add(new Label[]{titleLbl, descLbl, categoryLbl});

        // Info
        HBox info = new HBox(30);
        info.setAlignment(Pos.CENTER_LEFT);
        Label duration     = new Label("1h");
        Label reservations = new Label("0 réservations");
        duration.setStyle("-fx-font-size:13px;-fx-text-fill:#6b7280;");
        reservations.setStyle("-fx-font-size:13px;-fx-text-fill:#6b7280;");
        info.getChildren().addAll(duration, reservations);

        // Footer
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        Label price = new Label(String.format("%.0f€", s.getPrice()));
        price.setStyle("-fx-font-size:24px;-fx-font-weight:bold;-fx-text-fill:#5D5FEF;");


        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, javafx.scene.layout.Priority.ALWAYS);

        Button editBtn = new Button("✏");
        editBtn.setStyle(
                "-fx-background-color:transparent;-fx-font-size:18px;" +
                        "-fx-cursor:hand;-fx-text-fill:#6b7280;");
        editBtn.setOnAction(e -> editService(s));
        editBtn.setOnMouseEntered(e -> editBtn.setStyle(
                "-fx-background-color:#f3f4f6;-fx-font-size:18px;" +
                        "-fx-cursor:hand;-fx-text-fill:#111827;-fx-background-radius:6;"));
        editBtn.setOnMouseExited(e -> editBtn.setStyle(
                "-fx-background-color:transparent;-fx-font-size:18px;" +
                        "-fx-cursor:hand;-fx-text-fill:#6b7280;"));

        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle(
                "-fx-background-color:transparent;-fx-font-size:18px;" +
                        "-fx-cursor:hand;-fx-text-fill:#6b7280;");
        deleteBtn.setOnAction(e -> deleteService(s));
        deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(
                "-fx-background-color:#fee2e2;-fx-font-size:18px;" +
                        "-fx-cursor:hand;-fx-text-fill:#dc2626;-fx-background-radius:6;"));
        deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(
                "-fx-background-color:transparent;-fx-font-size:18px;" +
                        "-fx-cursor:hand;-fx-text-fill:#6b7280;"));

        footer.getChildren().addAll(price, footerSpacer, editBtn, deleteBtn);
        card.getChildren().addAll(categoryLbl, descLbl, info, footer);

        // Hover
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-border-color:#5D5FEF;-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(93,95,239,0.15),15,0,0,4);"  +
                        "-fx-border-radius:12;" +
                        "-fx-effect:dropshadow(gaussian,rgba(16,185,129,0.15),15,0,0,4);" +
                        "-fx-cursor:hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:white;-fx-background-radius:12;" +
                        "-fx-border-color:#e5e7eb;-fx-border-width:1;" +
                        "-fx-border-radius:12;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),10,0,0,2);"));

        return card;
    }

    // ═══════════════════════════════════════════════════════════════
    // ✅ GLOBAL TRANSLATION TOGGLE
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void toggleTranslation() {
        if (cardLabels.isEmpty()) return;

        if (!isTranslated) {
            // ── Translate all FR → EN ─────────────────────────────
            translateBtn.setText("⏳ Traduction en cours...");
            translateBtn.setDisable(true);

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() {
                    for (Label[] labels : cardLabels) {
                        String enTitle = TranslationService.frToEn(
                                (String) labels[0].getUserData());
                        String enDesc  = TranslationService.frToEn(
                                (String) labels[1].getUserData());
                        String enCat   = TranslationService.frToEn(
                                (String) labels[2].getUserData());

                        Platform.runLater(() -> {
                            labels[0].setText(enTitle);
                            labels[1].setText(enDesc);
                            labels[2].setText(enCat);
                        });
                    }
                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                isTranslated = true;
                translateBtn.setDisable(false);
                updateTranslateButton();
            });

            task.setOnFailed(e -> {
                translateBtn.setDisable(false);
                updateTranslateButton();
                showAlert("Erreur",
                        "Traduction impossible. Vérifiez votre connexion.",
                        Alert.AlertType.WARNING);
            });

            new Thread(task).start();

        } else {
            // ── Restore all to French instantly ──────────────────
            for (Label[] labels : cardLabels) {
                labels[0].setText((String) labels[0].getUserData());
                labels[1].setText((String) labels[1].getUserData());
                labels[2].setText((String) labels[2].getUserData());
            }
            isTranslated = false;
            updateTranslateButton();
        }
    }

    private void updateTranslateButton() {
        if (translateBtn == null) return;
        if (isTranslated) {
            translateBtn.setText("🔄 Voir en français");
            translateBtn.setStyle(
                    "-fx-background-color:#EDECFD;-fx-text-fill:#5D5FEF;" +
                            "-fx-font-size:13px;-fx-font-weight:bold;" +
                            "-fx-padding:12 20;-fx-background-radius:8;" +
                            "-fx-cursor:hand;-fx-border-color:#c4b5fd;" +
                            "-fx-border-width:1;-fx-border-radius:8;");
        } else {
            translateBtn.setText("🌐 Traduire en anglais");
            translateBtn.setStyle(
                    "-fx-background-color:#eff6ff;-fx-text-fill:#2563eb;" +
                            "-fx-font-size:13px;-fx-font-weight:bold;" +
                            "-fx-padding:12 20;-fx-background-radius:8;" +
                            "-fx-cursor:hand;-fx-border-color:#bfdbfe;" +
                            "-fx-border-width:1;-fx-border-radius:8;");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // STATUS HELPERS
    // ═══════════════════════════════════════════════════════════════

    private String getStatusLabel(String status) {
        if (status == null) return "Actif";
        return switch (status.toUpperCase()) {
            case "EN_ATTENTE" -> "En attente";
            case "ACTIF"      -> "Actif";
            case "INACTIF"    -> "Inactif";
            case "REJETE"     -> "Rejeté";
            default           -> "Actif";
        };
    }

    private String getStatusBadgeStyle(String status) {
        if (status == null || status.equalsIgnoreCase("ACTIF")) {
            return "-fx-background-color:#d1fae5;-fx-text-fill:#065f46;" +
                    "-fx-font-size:11px;-fx-font-weight:bold;" +
                    "-fx-padding:4 10;-fx-background-radius:12;";
        } else if (status.equalsIgnoreCase("EN_ATTENTE")) {
            return "-fx-background-color:#fef3c7;-fx-text-fill:#92400e;" +
                    "-fx-font-size:11px;-fx-font-weight:bold;" +
                    "-fx-padding:4 10;-fx-background-radius:12;";
        } else {
            return "-fx-background-color:#f3f4f6;-fx-text-fill:#6b7280;" +
                    "-fx-font-size:11px;-fx-font-weight:bold;" +
                    "-fx-padding:4 10;-fx-background-radius:12;";
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════════════════

    private void editService(Services service) {
        try {
            TextInputDialog titleDialog =
                    new TextInputDialog(service.getTitle());
            titleDialog.setTitle("Modifier le service");
            titleDialog.setHeaderText("Modifier: " + service.getTitle());
            titleDialog.setContentText("Nouveau titre:");
            Optional<String> titleResult = titleDialog.showAndWait();

            if (titleResult.isPresent()
                    && !titleResult.get().trim().isEmpty()) {

                TextInputDialog descDialog =
                        new TextInputDialog(service.getDescription());
                descDialog.setTitle("Modifier le service");
                descDialog.setHeaderText("Description");
                descDialog.setContentText("Nouvelle description:");
                Optional<String> descResult = descDialog.showAndWait();

                if (descResult.isPresent()) {
                    TextInputDialog priceDialog =
                            new TextInputDialog(
                                    String.valueOf(service.getPrice()));
                    priceDialog.setTitle("Modifier le service");
                    priceDialog.setHeaderText("Prix");
                    priceDialog.setContentText("Nouveau prix:");
                    Optional<String> priceResult = priceDialog.showAndWait();

                    if (priceResult.isPresent()) {
                        service.setTitle(titleResult.get().trim());
                        service.setDescription(descResult.get().trim());
                        service.setPrice(Double.parseDouble(
                                priceResult.get().trim()));
                        serviceManager.modifierService(service);
                        reloadBaseServices();
                        showAlert("Succès", "Service modifié avec succès!",
                                Alert.AlertType.INFORMATION);
                    }
                }
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Le prix doit être un nombre valide",
                    Alert.AlertType.ERROR);
        } catch (Exception e) {
            System.err.println("Error editing service: " + e.getMessage());
            showAlert("Erreur", "Impossible de modifier: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    private void deleteService(Services service) {
        try {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirmation");
            confirmAlert.setHeaderText("Supprimer le service");
            confirmAlert.setContentText(
                    "Êtes-vous sûr de vouloir supprimer '"
                            + service.getTitle() + "' ?");

            ButtonType btnDelete = new ButtonType(
                    "🗑 Supprimer", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnCancel = new ButtonType(
                    "Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmAlert.getButtonTypes().setAll(btnDelete, btnCancel);
            confirmAlert.getDialogPane().lookupButton(btnDelete).setStyle(
                    "-fx-background-color:#dc2626;-fx-text-fill:white;" +
                            "-fx-font-weight:bold;");

            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == btnDelete) {
                    try {
                        serviceManager.supprimerService(service.getId());
                        reloadBaseServices();
                        showAlert("Succès", "Service supprimé avec succès",
                                Alert.AlertType.INFORMATION);
                    } catch (Exception e) {
                        System.err.println("Error deleting: " + e.getMessage());
                        showAlert("Erreur",
                                "Impossible de supprimer: " + e.getMessage(),
                                Alert.AlertType.ERROR);
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Error in delete: " + e.getMessage());
            showAlert("Erreur",
                    "Erreur lors de la suppression: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void goToCreateService() {
        if (mainLayoutController == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Views/Create_Service.fxml"));
            Parent view = loader.load();
            CreateService_controller ctrl = loader.getController();
            ctrl.setCurrentPrestataireId(currentPrestataireId);
            ctrl.setMainLayoutController(mainLayoutController);
            mainLayoutController.loadContent(view);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void resetFilters() {
        if (searchField    != null) searchField.clear();
        if (statusFilter   != null) statusFilter.setValue("Tous");
        if (categoryFilter != null) categoryFilter.setValue("Toutes");
        updateDisplayedServices();
    }

    private void showAlert(String title, String message,
                           Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}