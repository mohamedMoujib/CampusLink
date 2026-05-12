package org.example.campusLink.controllers;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.Node;

import org.example.campusLink.services.Gestion_Service;
import org.example.campusLink.entities.User;
import org.example.campusLink.entities.Services;
import org.example.campusLink.utils.AppSession;
import org.example.campusLink.utils.TranslationService;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class Student_controller {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> categorieCombo;
    @FXML private ComboBox<String> tarifCombo;
    @FXML private ComboBox<String> trierCombo;
    @FXML private GridPane         servicesGrid;
    @FXML private Label            userNameLabel;
    @FXML private Label            userEmailLabel;
    @FXML private Button           translateBtn;   // ✅ global translate button

    private Gestion_Service gestionService;
    private User            currentUser;
    private int             currentStudentId = 1;

    // ✅ Track state
    private boolean        isTranslated = false;

    // ✅ Each entry: [titleLabel, descLabel, categoryLabel]
    // userData on each label holds the original French text
    private final List<Label[]> cardLabels = new ArrayList<>();

    // ═══════════════════════════════════════════════════════════════
    // SETUP
    // ═══════════════════════════════════════════════════════════════

    public void setUser(User user) {
        this.currentUser = user;
        if (user != null) {
            currentStudentId = user.getId();
            AppSession.setCurrentUser(user);
            if (userNameLabel  != null) userNameLabel.setText(user.getName());
            if (userEmailLabel != null) userEmailLabel.setText(user.getEmail());
        }
    }

    @FXML
    public void initialize() {
        System.out.println("Initializing Student_controller...");
        try {
            gestionService = new Gestion_Service();
            User sessionUser = AppSession.getCurrentUser();
            if (sessionUser != null) {
                currentStudentId = sessionUser.getId();
                currentUser      = sessionUser;
                if (userNameLabel  != null) userNameLabel.setText(sessionUser.getName());
                if (userEmailLabel != null) userEmailLabel.setText(sessionUser.getEmail());
            }
            setupFilters();
            loadServices();
            System.out.println("Student_controller initialized successfully");
        } catch (Exception e) {
            System.err.println("Error initializing: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur",
                    "Erreur d'initialisation: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    private void setupFilters() {
        categorieCombo.setItems(FXCollections.observableArrayList(
                "Tous", "Mathématiques", "Informatique", "Physique",
                "Chimie", "Langues", "Rédaction"));
        categorieCombo.setValue("Tous");
        categorieCombo.setOnAction(e -> loadServices());

        tarifCombo.setItems(FXCollections.observableArrayList(
                "Tous les tarifs", "Moins de 15€",
                "15€ - 25€", "25€ - 35€", "Plus de 35€"));
        tarifCombo.setValue("Tous les tarifs");
        tarifCombo.setOnAction(e -> loadServices());

        trierCombo.setItems(FXCollections.observableArrayList(
                "Meilleure note", "Prix croissant",
                "Prix décroissant", "Plus récent"));
        trierCombo.setValue("Meilleure note");
        trierCombo.setOnAction(e -> loadServices());

        searchField.textProperty().addListener(
                (obs, o, n) -> loadServices());
    }

    // ═══════════════════════════════════════════════════════════════
    // LOAD
    // ═══════════════════════════════════════════════════════════════

    private void loadServices() {
        try {
            System.out.println("Loading services...");
            List<Services> services = gestionService.afficherServices();
            List<Services> filtered = applyFilters(services);

            servicesGrid.getChildren().clear();
            cardLabels.clear();      // ✅ reset on every reload
            isTranslated = false;
            updateTranslateButton(); // ✅ reset button text/style

            if (services == null || services.isEmpty()) {
                Label lbl = new Label("Aucun service disponible.");
                lbl.getStyleClass().add("empty-state");
                servicesGrid.add(lbl, 0, 0, 3, 1);
                return;
            }
            if (filtered.isEmpty()) {
                Label lbl = new Label(
                        "Aucun service ne correspond aux filtres.");
                lbl.getStyleClass().add("empty-state");
                servicesGrid.add(lbl, 0, 0, 3, 1);
                return;
            }

            int row = 0, col = 0;
            for (Services s : filtered) {
                servicesGrid.add(createServiceCard(s), col, row);
                col++;
                if (col == 3) { col = 0; row++; }
            }
            System.out.println("Loaded " + filtered.size() + " services");

        } catch (Exception e) {
            System.err.println("Error loading services: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur",
                    "Impossible de charger les services: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    private List<Services> applyFilters(List<Services> services) {
        if (services == null || services.isEmpty()) return List.of();

        var stream = services.stream();

        if (categorieCombo != null) {
            String val = categorieCombo.getValue();
            if (val != null && !"Tous".equalsIgnoreCase(val)) {
                String needle = val.toLowerCase(Locale.ROOT);
                stream = stream.filter(s -> {
                    String cat = s.getCategoryDisplayName();
                    return cat != null &&
                            cat.toLowerCase(Locale.ROOT).contains(needle);
                });
            }
        }

        if (tarifCombo != null) {
            String val = tarifCombo.getValue();
            if (val != null && !val.startsWith("Tous")) {
                stream = stream.filter(s -> {
                    double p = s.getPrice();
                    return switch (val) {
                        case "Moins de 15€"  -> p < 15;
                        case "15€ - 25€"     -> p >= 15 && p <= 25;
                        case "25€ - 35€"     -> p >= 25 && p <= 35;
                        case "Plus de 35€"   -> p > 35;
                        default              -> true;
                    };
                });
            }
        }

        String keyword = (searchField != null
                && searchField.getText() != null)
                ? searchField.getText().trim() : null;
        if (keyword != null && !keyword.isEmpty()) {
            String needle = keyword.toLowerCase(Locale.ROOT);
            stream = stream.filter(s -> {
                String t = s.getTitle();
                String d = s.getDescription();
                String c = s.getCategoryDisplayName();
                String p = s.getPrestataireDisplayName();
                return (t != null && t.toLowerCase(Locale.ROOT).contains(needle)) ||
                        (d != null && d.toLowerCase(Locale.ROOT).contains(needle)) ||
                        (c != null && c.toLowerCase(Locale.ROOT).contains(needle)) ||
                        (p != null && p.toLowerCase(Locale.ROOT).contains(needle));
            });
        }

        List<Services> out = stream.toList();

        if (trierCombo != null) {
            String val = trierCombo.getValue();
            if (val != null) {
                Comparator<Services> cmp = switch (val) {
                    case "Prix croissant"   ->
                            Comparator.comparingDouble(Services::getPrice);
                    case "Prix décroissant" ->
                            Comparator.comparingDouble(Services::getPrice)
                                    .reversed();
                    case "Plus récent"      ->
                            Comparator.comparingInt(Services::getId).reversed();
                    default -> null;
                };
                if (cmp != null) out = out.stream().sorted(cmp).toList();
            }
        }
        return out;
    }

    // ═══════════════════════════════════════════════════════════════
    // CARD BUILDER
    // ═══════════════════════════════════════════════════════════════

    private VBox createServiceCard(Services service) {
        VBox card = new VBox(12);
        card.getStyleClass().add("service-card");

        // Header
        HBox header = new HBox(10);
        header.getStyleClass().add("card-header");
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(
                service.getTitle() != null ? service.getTitle() : "Service");
        titleLbl.getStyleClass().add("card-title");
        titleLbl.setMaxWidth(280);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox ratingBox = new HBox(5);
        ratingBox.getStyleClass().add("card-rating");
        Label star = new Label("⭐");
        star.getStyleClass().add("rating-star");
        Label ratingVal = new Label("4.9");
        ratingVal.getStyleClass().add("rating-value");
        ratingBox.getChildren().addAll(star, ratingVal);

        header.getChildren().addAll(titleLbl, spacer, ratingBox);

        // Image
        ImageView imageView = null;
        if (service.getImage() != null && !service.getImage().isEmpty()) {
            File imageFile = new File(
                    "uploads/services/" + service.getImage());
            if (imageFile.exists()) {
                try {
                    Image img = new Image(imageFile.toURI().toString());
                    imageView = new ImageView(img);
                    imageView.setFitWidth(360);
                    imageView.setFitHeight(180);
                    imageView.setPreserveRatio(true);
                } catch (Exception e) {
                    System.err.println("Error loading image: "
                            + e.getMessage());
                }
            }
        }

        // Provider
        Label providerLbl = new Label(
                service.getPrestataireDisplayName());
        providerLbl.getStyleClass().add("card-provider");

        // Info rows
        VBox infoBox = new VBox(6);

        HBox durationRow = new HBox(8);
        durationRow.getStyleClass().add("card-info-row");
        durationRow.getChildren().addAll(icon("🕐"), new Label("1h"));

        HBox subjectRow = new HBox(8);
        subjectRow.getStyleClass().add("card-info-row");
        Label categoryLbl = new Label(service.getCategoryDisplayName());
        categoryLbl.getStyleClass().add("card-info-text");
        subjectRow.getChildren().addAll(icon("📚"), categoryLbl);

        infoBox.getChildren().addAll(durationRow, subjectRow);

        // Description
        String descText = (service.getDescription() != null
                && !service.getDescription().isEmpty())
                ? service.getDescription()
                : "Aide personnalisée avec méthodes pédagogiques adaptées.";
        Label descLbl = new Label(descText);
        descLbl.getStyleClass().add("card-description");
        descLbl.setWrapText(true);
        descLbl.setMaxWidth(360);
        descLbl.setMaxHeight(90);

        // Footer
        HBox footer = new HBox(15);
        footer.getStyleClass().add("card-footer");

        VBox priceBox = new VBox(2);
        priceBox.getStyleClass().add("card-price-container");
        Label priceLbl = new Label(
                String.format("%.0f€", service.getPrice()));
        priceLbl.getStyleClass().add("card-price");
        Label reviewsLbl = new Label("127 avis");
        reviewsLbl.getStyleClass().add("card-reviews");
        priceBox.getChildren().addAll(priceLbl, reviewsLbl);

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);



        footer.getChildren().addAll(priceBox, footerSpacer);

        // ✅ Store originals in userData for restore
        String originalTitle = service.getTitle() != null
                ? service.getTitle() : "Service";
        String originalDesc  = descText;
        String originalCat   = service.getCategoryDisplayName();

        titleLbl.setUserData(originalTitle);
        descLbl.setUserData(originalDesc);
        categoryLbl.setUserData(originalCat);

        // ✅ Register in global list for batch translation
        cardLabels.add(new Label[]{titleLbl, descLbl, categoryLbl});

        // Assemble card
        card.getChildren().add(header);
        if (imageView != null) card.getChildren().add(imageView);
        card.getChildren().addAll(providerLbl, infoBox, descLbl, footer);

        return card;
    }

    private Label icon(String emoji) {
        Label l = new Label(emoji);
        l.getStyleClass().add("card-info-icon");
        return l;
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

                        // Update each card on FX thread as it arrives
                        javafx.application.Platform.runLater(() -> {
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
                    "-fx-background-color:#f0fdf4;" +
                            "-fx-text-fill:#16a34a;" +
                            "-fx-font-size:13px;-fx-font-weight:bold;" +
                            "-fx-padding:12 20;-fx-background-radius:8;" +
                            "-fx-cursor:hand;-fx-border-color:#bbf7d0;" +
                            "-fx-border-width:1;-fx-border-radius:8;");
        } else {
            translateBtn.setText("🌐 Traduire en anglais");
            translateBtn.setStyle(
                    "-fx-background-color:#eff6ff;" +
                            "-fx-text-fill:#2563eb;" +
                            "-fx-font-size:13px;-fx-font-weight:bold;" +
                            "-fx-padding:12 20;-fx-background-radius:8;" +
                            "-fx-cursor:hand;-fx-border-color:#bfdbfe;" +
                            "-fx-border-width:1;-fx-border-radius:8;");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void showAlert(String title, String message,
                           Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void goToServices(javafx.event.ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/Views/service.fxml"));
            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Mes Services");
        } catch (Exception e) {
            System.err.println("Error navigating: " + e.getMessage());
            showAlert("Erreur",
                    "Impossible de naviguer: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }
}