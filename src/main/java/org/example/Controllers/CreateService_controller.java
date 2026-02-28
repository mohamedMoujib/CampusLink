package org.example.Controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.example.campusLink.Services.Gestion_Categorie;
import org.example.campusLink.Services.Gestion_Service;
import org.example.campusLink.entities.Categorie;
import org.example.campusLink.entities.Services;

import javafx.collections.FXCollections;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

/**
 * Controller for creating new Services with image upload.
 * Fields: title, description, price, image
 * Database: id, title, description, price, image, prestataire_id, category_id, status
 */
public class CreateService_controller {

    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private TextField priceField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private Label charCountLabel;

    @FXML private Button uploadImageButton;
    @FXML private ImageView imagePreview;
    @FXML private Label imageFileLabel;
    @FXML private Button removeImageButton;

    @FXML private TextArea aiIdeaField;
    @FXML private Button generateAiBtn;
    @FXML private Label aiLoadingLabel;
    @FXML private VBox aiResultsPanel;
    @FXML private VBox aiErrorPanel;
    @FXML private Label aiStatusLabel;
    @FXML private Label aiGeneratedTitre;
    @FXML private Label aiGeneratedDesc;
    @FXML private Label aiErrorLabel;

    private static final String N8N_BASE = System.getenv().getOrDefault("CAMPUSLINK_N8N_URL", "http://localhost:5678");
    private static final String N8N_SERVICE_URL = N8N_BASE.replaceAll("/$", "") + "/webhook/creer-service";

    private Gestion_Service gestionService;
    private Gestion_Categorie categoryManager;
    private List<Categorie> categories;
    private int currentPrestataireId = 1; // TODO: Replace with session

    private File selectedImageFile;
    private String uploadedImagePath;
    private static final String UPLOAD_DIR = "uploads/services/";
    private static final long MAX_FILE_SIZE = 5_000_000; // 5 MB

    @FXML
    public void initialize() {
        System.out.println("Initializing CreateService_controller...");

        try {
            gestionService = new Gestion_Service();
            categoryManager = new Gestion_Categorie();
            categories = categoryManager.afficherCategories();
            setupCategoryCombo();
            setupCharacterCounter();
            setupPriceField();
            setupImageUpload();
            System.out.println("CreateService_controller initialized successfully");

        } catch (Exception e) {
            System.err.println("Error initializing: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'initialiser: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupCategoryCombo() {
        if (categoryCombo != null && categories != null) {
            java.util.ArrayList<String> names = new java.util.ArrayList<>();
            names.add("Non catégorisé");
            for (Categorie c : categories) {
                if (c.getName() != null) names.add(c.getName());
            }
            categoryCombo.setItems(FXCollections.observableArrayList(names));
            categoryCombo.getSelectionModel().selectFirst();
        }
    }

    private int getSelectedCategoryId() {
        if (categoryCombo == null || categories == null) return 0;
        int idx = categoryCombo.getSelectionModel().getSelectedIndex();
        if (idx <= 0) return 0; // "Non catégorisé"
        idx--;
        if (idx >= 0 && idx < categories.size()) return categories.get(idx).getId();
        return 0;
    }

    private void setupImageUpload() {
        if (imagePreview != null) imagePreview.setVisible(false);
        if (imageFileLabel != null) imageFileLabel.setVisible(false);
        if (removeImageButton != null) removeImageButton.setVisible(false);
    }

    private void setupCharacterCounter() {
        if (descriptionField != null && charCountLabel != null) {
            descriptionField.textProperty().addListener((obs, oldVal, newVal) -> {
                int length = newVal != null ? newVal.length() : 0;
                charCountLabel.setText(length + "/1000 caractères");

                if (length > 1000) {
                    charCountLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    descriptionField.setText(oldVal);
                } else if (length > 900) {
                    charCountLabel.setStyle("-fx-text-fill: #f59e0b;");
                } else {
                    charCountLabel.setStyle("-fx-text-fill: #9ca3af;");
                }
            });
        }
    }

    private void setupPriceField() {
        if (priceField != null) {
            priceField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*(\\.\\d{0,2})?")) {
                    priceField.setText(oldVal);
                }
            });
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            errors.append("• Le titre est obligatoire\n");
        } else if (titleField.getText().trim().length() < 3) {
            errors.append("• Le titre doit contenir au moins 3 caractères\n");
        } else if (titleField.getText().trim().length() > 100) {
            errors.append("• Le titre: max 100 caractères\n");
        }

        if (descriptionField.getText() != null && descriptionField.getText().trim().length() > 1000) {
            errors.append("• Description: max 1000 caractères\n");
        }

        if (priceField.getText() == null || priceField.getText().trim().isEmpty()) {
            errors.append("• Le prix est obligatoire\n");
        } else {
            try {
                double price = Double.parseDouble(priceField.getText().trim());
                if (price <= 0) {
                    errors.append("• Le prix doit être > 0\n");
                } else if (price > 99999999.99) {
                    errors.append("• Prix: max 99,999,999.99€\n");
                }
            } catch (NumberFormatException e) {
                errors.append("• Prix invalide (ex: 25.00)\n");
            }
        }

        if (selectedImageFile != null && selectedImageFile.length() > MAX_FILE_SIZE) {
            errors.append("• L'image est trop volumineuse (max 5 MB)\n");
        }

        if (errors.length() > 0) {
            showAlert("Erreurs", "Corrigez:\n\n" + errors.toString(), Alert.AlertType.WARNING);
            return false;
        }

        return true;
    }

    @FXML
    private void saveService() {
        System.out.println("Saving service...");

        if (!validateForm()) return;

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmer");
        confirmDialog.setHeaderText("Créer ce service ?");
        confirmDialog.setContentText(
                "Titre: " + titleField.getText().trim() + "\n" +
                        "Prix: " + priceField.getText().trim() + "€\n" +
                        (selectedImageFile != null ? "Image: " + selectedImageFile.getName() + "\n" : "") +
                        "\nStatut: EN_ATTENTE"
        );

        ButtonType btnOk = new ButtonType("✓ Créer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("✗ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getButtonTypes().setAll(btnOk, btnCancel);

        Optional<ButtonType> result = confirmDialog.showAndWait();

        if (result.isPresent() && result.get() == btnOk) {
            try {
                if (selectedImageFile != null) {
                    uploadedImagePath = uploadImage(selectedImageFile);
                }

                Services service = new Services();
                service.setTitle(titleField.getText().trim());
                service.setPrice(Double.parseDouble(priceField.getText().trim()));
                service.setPrestataireId(currentPrestataireId);

                String desc = descriptionField.getText();
                service.setDescription(desc != null && !desc.trim().isEmpty() ? desc.trim() : null);

                service.setImage(uploadedImagePath);
                service.setCategoryId(getSelectedCategoryId());

                gestionService.ajouterService(service);

                System.out.println("✅ Service created: ID=" + service.getId());

                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Succès");
                successAlert.setHeaderText("Service créé !");
                successAlert.setContentText(
                        "Service: " + service.getTitle() + "\n" +
                                "ID: #" + service.getId() + "\n" +
                                "Prix: " + service.getPrice() + "€\n" +
                                (uploadedImagePath != null ? "Image: " + uploadedImagePath + "\n" : "") +
                                "Statut: EN_ATTENTE"
                );
                successAlert.showAndWait();

                goBack();

            } catch (NumberFormatException e) {
                showAlert("Erreur", "Prix invalide", Alert.AlertType.ERROR);
            } catch (IOException e) {
                System.err.println("Error uploading image: " + e.getMessage());
                e.printStackTrace();
                showAlert("Erreur", "Impossible d'uploader l'image: " + e.getMessage(), Alert.AlertType.ERROR);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
                showAlert("Erreur", "Impossible de créer: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void goBack() {
        System.out.println("Going back...");

        try {
            if (hasUnsavedChanges()) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Annuler");
                confirmAlert.setHeaderText("Quitter sans enregistrer ?");
                confirmAlert.setContentText("Les modifications seront perdues.");

                ButtonType btnYes = new ButtonType("Oui", ButtonBar.ButtonData.OK_DONE);
                ButtonType btnNo = new ButtonType("Non", ButtonBar.ButtonData.CANCEL_CLOSE);
                confirmAlert.getButtonTypes().setAll(btnYes, btnNo);

                Optional<ButtonType> result = confirmAlert.showAndWait();
                if (result.isEmpty() || result.get() != btnYes) return;
            }

            Stage stage = (Stage) titleField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/service.fxml"));
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Mes Services");

        } catch (Exception e) {
            System.err.println("Error going back: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Navigation impossible: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean hasUnsavedChanges() {
        return (titleField != null && titleField.getText() != null && !titleField.getText().trim().isEmpty()) ||
                (descriptionField != null && descriptionField.getText() != null && !descriptionField.getText().trim().isEmpty()) ||
                (priceField != null && priceField.getText() != null && !priceField.getText().trim().isEmpty()) ||
                (selectedImageFile != null) ||
                (aiIdeaField != null && aiIdeaField.getText() != null && !aiIdeaField.getText().trim().isEmpty());
    }

    private boolean fallbackCreateServiceLocally(String idea, double prix, String imageUrl, Exception n8nError) {
        try {
            String title = idea.length() > 80 ? idea.substring(0, 77) + "..." : idea;
            double priceVal = prix > 0 ? prix : 10.0;
            String imgPath = imageUrl != null ? imageUrl.replace(UPLOAD_DIR, "") : null;

            Services svc = new Services();
            svc.setTitle(title);
            svc.setDescription(idea);
            svc.setPrice(priceVal);
            svc.setPrestataireId(currentPrestataireId);
            svc.setImage(imgPath);
            svc.setCategoryId(getSelectedCategoryId());

            gestionService.ajouterService(svc);
            int id = svc.getId();

            if (aiStatusLabel != null) aiStatusLabel.setText("✅ Service créé (sans IA) ! ID #" + id);
            if (aiGeneratedTitre != null) aiGeneratedTitre.setText("📌 " + title);
            if (aiGeneratedDesc != null) aiGeneratedDesc.setText(idea);
            if (aiResultsPanel != null) {
                aiResultsPanel.setVisible(true);
                aiResultsPanel.setManaged(true);
            }
            if (aiIdeaField != null) aiIdeaField.clear();
            clearFormForExit();
            showAlert("Succès", "Service créé (workflow n8n indisponible, création locale). ID #" + id, Alert.AlertType.INFORMATION);
            goBack();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void hideAiPanels() {
        if (aiResultsPanel != null) { aiResultsPanel.setVisible(false); aiResultsPanel.setManaged(false); }
        if (aiErrorPanel != null) { aiErrorPanel.setVisible(false); aiErrorPanel.setManaged(false); }
    }

    private void clearFormForExit() {
        if (titleField != null) titleField.clear();
        if (descriptionField != null) descriptionField.clear();
        if (priceField != null) priceField.clear();
        if (aiIdeaField != null) aiIdeaField.clear();
        onRemoveImage();
        hideAiPanels();
    }

    // ═══════════════════════════════════════════════════════════════
    // FIX 1 — UTF-8 : utiliser org.json correctement avec unicode
    // ═══════════════════════════════════════════════════════════════
    private JSONObject envoyerAn8n(JSONObject payload) throws Exception {
        // FIX UTF-8 : convertir en String AVANT getBytes pour garder les accents
        String payloadStr = payload.toString();
        System.out.println("→ Envoi payload service : " + payloadStr);

        URL url = new URL(N8N_SERVICE_URL);
        System.out.println("→ URL n8n service : " + N8N_SERVICE_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "CampusLink/1.0");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(60_000);
        conn.setDoOutput(true);

        // FIX UTF-8 : écrire les bytes UTF-8 explicitement
        byte[] bytes = payloadStr.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bytes);
            os.flush();
        }

        int statusCode = conn.getResponseCode();
        String responseBody;
        if (statusCode >= 200 && statusCode < 300) {
            responseBody = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } else {
            responseBody = new String(
                    conn.getErrorStream() != null ? conn.getErrorStream().readAllBytes() : new byte[0],
                    StandardCharsets.UTF_8);
        }

        System.out.println("← HTTP " + statusCode + " | body(" + responseBody.length() + " chars): " + responseBody);

        if (responseBody.isEmpty()) {
            if (statusCode >= 200 && statusCode < 300)
                return new JSONObject().put("success", false)
                        .put("message", "n8n a répondu sans corps (réponse vide). Vérifiez que le workflow est actif et que MySQL est configuré.")
                        .put("_httpStatus", statusCode);
            return new JSONObject().put("success", false)
                    .put("message", "Erreur HTTP " + statusCode)
                    .put("_httpStatus", statusCode);
        }
        try {
            JSONObject json = new JSONObject(responseBody);
            json.put("_httpStatus", statusCode);
            return json;
        } catch (Exception e) {
            return new JSONObject()
                    .put("success", false)
                    .put("message", responseBody.length() > 200 ? responseBody.substring(0, 200) : responseBody)
                    .put("_httpStatus", statusCode);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // FIX 2 — Prix : extraire le prix depuis la description si priceField vide
    // FIX 3 — category_id : envoyer null si 0 pour éviter erreur MySQL
    // ═══════════════════════════════════════════════════════════════
    @FXML
    private void generateAndCreateService() {
        String idea = aiIdeaField != null ? aiIdeaField.getText().trim() : "";
        if (idea.isEmpty()) {
            showAlert("Erreur", "Veuillez décrire votre service avant de continuer.", Alert.AlertType.WARNING);
            if (aiIdeaField != null) aiIdeaField.requestFocus();
            return;
        }
        if (idea.length() < 10) {
            showAlert("Erreur", "Décrivez votre service en au moins 10 caractères.", Alert.AlertType.WARNING);
            if (aiIdeaField != null) aiIdeaField.requestFocus();
            return;
        }

        // FIX 2 — Prix : lire depuis priceField, sinon extraire depuis la description
        double prix = 0;
        try {
            if (priceField != null && priceField.getText() != null && !priceField.getText().trim().isEmpty()) {
                prix = Double.parseDouble(priceField.getText().trim());
                if (prix < 0) prix = 0;
            }
        } catch (NumberFormatException ignored) {}

        // FIX 2 — Si prix toujours 0, essayer d'extraire depuis la description (ex: "25€", "25 €", "25.00€")
        if (prix == 0) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(\\d+(?:[.,]\\d{1,2})?)\\s*€")
                    .matcher(idea);
            if (m.find()) {
                try {
                    prix = Double.parseDouble(m.group(1).replace(",", "."));
                } catch (NumberFormatException ignored) {}
            }
        }

        String imageUrl = null;
        if (selectedImageFile != null) {
            try {
                imageUrl = UPLOAD_DIR + uploadImage(selectedImageFile);
            } catch (IOException e) {
                showAlert("Erreur", "Impossible d'uploader l'image : " + e.getMessage(), Alert.AlertType.ERROR);
                return;
            }
        }

        final String finalIdea = idea;
        final double finalPrix = prix;
        final String finalImageUrl = imageUrl;

        // FIX 3 — category_id : envoyer null si 0
        int categoryId = getSelectedCategoryId();

        JSONObject payload = new JSONObject();
        payload.put("prestataire_id", currentPrestataireId);
        payload.put("prompt", idea);          // UTF-8 géré par JSONObject
        payload.put("prix", prix);
        if (imageUrl != null) payload.put("image_url", imageUrl);
        if (categoryId > 0) {
            payload.put("category_id", categoryId);
        }
        // Ne pas mettre category_id si 0 — n8n mettra NULL en DB

        generateAiBtn.setDisable(true);
        if (aiLoadingLabel != null) aiLoadingLabel.setVisible(true);
        hideAiPanels();

        new Thread(() -> {
            try {
                JSONObject response = envoyerAn8n(payload);
                int statusCode = response.optInt("_httpStatus", 200);

                Platform.runLater(() -> {
                    generateAiBtn.setDisable(false);
                    if (aiLoadingLabel != null) aiLoadingLabel.setVisible(false);

                    boolean webhookUnavailable = (statusCode == 404) ||
                            (statusCode >= 500) ||
                            (response.optString("message", "").toLowerCase().contains("not registered")) ||
                            (response.optString("message", "").toLowerCase().contains("webhook"));
                    if (!response.optBoolean("success", false) && webhookUnavailable) {
                        try {
                            if (fallbackCreateServiceLocally(finalIdea, finalPrix, finalImageUrl, new Exception("Webhook indisponible"))) {
                                return;
                            }
                        } catch (Exception fe) {
                            fe.printStackTrace();
                        }
                    }

                    if (response.optBoolean("success", false)) {
                        int serviceId = response.optInt("service_id", 0);
                        if (serviceId <= 0) {
                            String err = "n8n n'a pas retourné l'ID du service. Vérifiez le workflow n8n et la connexion MySQL.";
                            if (aiErrorLabel != null) aiErrorLabel.setText("❌ " + err);
                            if (aiErrorPanel != null) { aiErrorPanel.setVisible(true); aiErrorPanel.setManaged(true); }
                            showAlert("Erreur", err, Alert.AlertType.ERROR);
                            return;
                        }
                        JSONObject svc = response.optJSONObject("service");

                        if (aiStatusLabel != null) aiStatusLabel.setText("✅ Service créé avec succès ! (ID #" + serviceId + ")");
                        if (aiGeneratedTitre != null) aiGeneratedTitre.setText("📌 " + (svc != null ? svc.optString("title", "Service") : "Service"));
                        if (aiGeneratedDesc != null) aiGeneratedDesc.setText(svc != null ? svc.optString("description", "") : "");

                        if (aiResultsPanel != null) {
                            aiResultsPanel.setVisible(true);
                            aiResultsPanel.setManaged(true);
                        }
                        if (aiIdeaField != null) aiIdeaField.clear();
                        clearFormForExit();

                        showAlert("Succès", "Service créé ! ID #" + serviceId, Alert.AlertType.INFORMATION);
                        goBack();
                    } else {
                        String err = response.optString("message", "Erreur inconnue");
                        if (aiErrorLabel != null) aiErrorLabel.setText("❌ " + err);
                        if (aiErrorPanel != null) {
                            aiErrorPanel.setVisible(true);
                            aiErrorPanel.setManaged(true);
                        }
                        showAlert("Erreur", err, Alert.AlertType.ERROR);
                    }
                });
            } catch (Exception e) {
                Exception ex = e;
                Platform.runLater(() -> {
                    generateAiBtn.setDisable(false);
                    if (aiLoadingLabel != null) aiLoadingLabel.setVisible(false);
                    boolean fallbackOk = false;
                    try {
                        fallbackOk = fallbackCreateServiceLocally(finalIdea, finalPrix, finalImageUrl, ex);
                    } catch (Exception fe) {
                        fe.printStackTrace();
                    }
                    if (!fallbackOk) {
                        String msg = "Impossible de contacter le workflow n8n : " + ex.getMessage();
                        if (aiErrorLabel != null) aiErrorLabel.setText("❌ " + msg);
                        if (aiErrorPanel != null) {
                            aiErrorPanel.setVisible(true);
                            aiErrorPanel.setManaged(true);
                        }
                        showAlert("Erreur", msg, Alert.AlertType.ERROR);
                    }
                });
            }
        }).start();
    }

    @FXML
    private void onUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        Stage stage = (Stage) uploadImageButton.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            if (file.length() > MAX_FILE_SIZE) {
                showAlert("Erreur", "L'image est trop volumineuse (max 5 MB)", Alert.AlertType.WARNING);
                return;
            }
            selectedImageFile = file;
            displayImagePreview(file);
        }
    }

    private void displayImagePreview(File imageFile) {
        try {
            Image image = new Image(imageFile.toURI().toString());

            if (imagePreview != null) {
                imagePreview.setImage(image);
                imagePreview.setVisible(true);
            }
            if (imageFileLabel != null) {
                imageFileLabel.setText(imageFile.getName());
                imageFileLabel.setVisible(true);
            }
            if (removeImageButton != null) {
                removeImageButton.setVisible(true);
            }
            if (uploadImageButton != null) {
                uploadImageButton.setText("📷 Changer l'image");
            }

        } catch (Exception e) {
            System.err.println("Error displaying image: " + e.getMessage());
            showAlert("Erreur", "Impossible d'afficher l'image", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onRemoveImage() {
        selectedImageFile = null;
        uploadedImagePath = null;

        if (imagePreview != null) {
            imagePreview.setImage(null);
            imagePreview.setVisible(false);
        }
        if (imageFileLabel != null) {
            imageFileLabel.setVisible(false);
        }
        if (removeImageButton != null) {
            removeImageButton.setVisible(false);
        }
        if (uploadImageButton != null) {
            uploadImageButton.setText("📷 Ajouter une image (optionnel)");
        }
    }

    private String uploadImage(File imageFile) throws IOException {
        Path uploadDir = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String filename = System.currentTimeMillis() + "_" + imageFile.getName();
        Path targetPath = uploadDir.resolve(filename);

        Files.copy(imageFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("✅ Image uploaded: " + UPLOAD_DIR + filename);

        return filename;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setCurrentPrestataireId(int prestataireId) {
        this.currentPrestataireId = prestataireId;
        System.out.println("Prestataire ID: " + prestataireId);
    }
}