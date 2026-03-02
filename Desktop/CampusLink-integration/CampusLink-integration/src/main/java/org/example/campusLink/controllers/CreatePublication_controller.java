package org.example.campusLink.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.example.campusLink.controllers.users.MainLayoutController;
import org.example.campusLink.services.Gestion_Matching;
import org.example.campusLink.services.Gestion_publication;
import org.example.campusLink.entities.Publications;
import org.json.JSONObject;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

public class CreatePublication_controller implements Initializable {

    private static final String N8N_BASE = System.getenv().getOrDefault("CAMPUSLINK_N8N_URL", "http://localhost:5678");
    private static final String N8N_URL  = N8N_BASE.replaceAll("/$", "") + "/webhook/creer-publication";

    // ── Form fields (LEFT) ───────────────────────────────────────
    @FXML private RadioButton typeVenteRadio;
    @FXML private RadioButton typeDemandeRadio;
    @FXML private TextField   titreField;
    @FXML private TextArea    messageArea;
    @FXML private Label       messageCounter;
    @FXML private TextField   prixField;
    @FXML private TextField   localisationField;
    @FXML private Button      uploadImageButton;
    @FXML private Button      removeImageButton;
    @FXML private Label       imageFileLabel;
    @FXML private ImageView   imagePreview;
    @FXML private CheckBox    termsCheckBox;
    @FXML private Button      submitButton;

    // ── AI panel (RIGHT) ─────────────────────────────────────────
    @FXML private TextArea  aiIdeaField;
    @FXML private Button    generateAiBtn;
    @FXML private Label     aiLoadingLabel;

    @FXML private javafx.scene.layout.VBox aiResultsPanel;
    @FXML private Label aiStatusLabel;
    @FXML private Label aiGeneratedTitre;
    @FXML private Label aiGeneratedMessage;

    @FXML private javafx.scene.layout.VBox aiErrorPanel;
    @FXML private Label aiErrorLabel;

    // ── Old suggestions panel (kept, hidden) ─────────────────────
    @FXML private javafx.scene.layout.VBox suggestionsPanel;
    @FXML private Label  shortTitreLabel;
    @FXML private Label  shortDescLabel;
    @FXML private Button applyShortBtn;
    @FXML private Label  detailTitreLabel;
    @FXML private Label  detailDescLabel;
    @FXML private Button applyDetailBtn;
    @FXML private Label  urgentTitreLabel;
    @FXML private Label  urgentDescLabel;
    @FXML private Button applyUrgentBtn;
    @FXML private Label  conseilsLabel;

    // ── State ─────────────────────────────────────────────────────
    private JSONObject          lastAiResult       = null;
    private String              selectedImageUrl   = null;
    private int                 currentStudentId   = 1;
    private Gestion_publication gestionPublication;
    private javafx.scene.Scene  previousScene      = null;
    private MainLayoutController mainLayoutController;

    // ── Setters ───────────────────────────────────────────────────
    public void setMainLayoutController(MainLayoutController ctrl) {
        this.mainLayoutController = ctrl;
    }
    public void setCurrentStudentId(int id)            { this.currentStudentId = id; }
    public void setPreviousScene(javafx.scene.Scene s) { this.previousScene = s; }

    // ─────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            gestionPublication = new Gestion_publication();
        } catch (Exception e) {
            gestionPublication = null;
        }

        ToggleGroup typeGroup = new ToggleGroup();
        typeVenteRadio.setToggleGroup(typeGroup);
        typeDemandeRadio.setToggleGroup(typeGroup);
        typeDemandeRadio.setSelected(true);

        messageArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal.length();
            if (len > 1000) messageArea.setText(oldVal);
            else messageCounter.setText(len + "/1000");
        });

        if (aiIdeaField != null) {
            aiIdeaField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.isBlank()) {
                    String lower = newVal.toLowerCase();
                    if (lower.contains("je vends") || lower.contains("vente")
                            || lower.contains("à vendre") || lower.contains("a vendre")
                            || lower.contains("vendre mon")) {
                        if (typeVenteRadio != null && !typeVenteRadio.isSelected())
                            typeVenteRadio.setSelected(true);
                    }
                }
            });
        }

        submitButton.disableProperty().bind(termsCheckBox.selectedProperty().not());
        removeImageButton.setVisible(false);
        if (imagePreview != null) imagePreview.setVisible(false);
    }

    // ═══════════════════════════════════════════════════════════════
    // HTTP → n8n
    // ═══════════════════════════════════════════════════════════════

    private JSONObject envoyerAn8n(JSONObject payload) throws Exception {
        String payloadStr = payload.toString();
        System.out.println("→ Envoi payload : " + payloadStr);

        URL url = new URL(N8N_URL);
        System.out.println("→ URL n8n : " + N8N_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "CampusLink/1.0");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(60_000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payloadStr.getBytes(StandardCharsets.UTF_8));
        }

        int statusCode = conn.getResponseCode();
        System.out.println("← HTTP " + statusCode + " de n8n");

        String responseBody;
        if (statusCode >= 200 && statusCode < 300) {
            responseBody = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } else {
            responseBody = new String(
                    conn.getErrorStream() != null ? conn.getErrorStream().readAllBytes() : new byte[0],
                    StandardCharsets.UTF_8);
        }

        System.out.println("← Réponse brute (" + responseBody.length() + " chars) : "
                + (responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody));

        if (responseBody.isEmpty()) {
            if (statusCode >= 200 && statusCode < 300) {
                return new JSONObject().put("success", false)
                        .put("message", "n8n a répondu HTTP " + statusCode + " sans corps. "
                                + "Vérifiez : 1) workflow actif, 2) OpenRouter API key, "
                                + "3) MySQL campusLink, 4) onglet Executions dans n8n.");
            }
            return new JSONObject().put("success", false)
                    .put("message", "Erreur HTTP " + statusCode + " sans détails");
        }

        return new JSONObject(responseBody);
    }

    // ═══════════════════════════════════════════════════════════════
    // A) SOUMISSION MANUELLE
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void submitPublication() {
        if (!validateManualForm()) return;

        String titre          = titreField.getText().trim();
        String message        = messageArea.getText().trim();
        double prix           = Double.parseDouble(prixField.getText().trim());
        String typePublication = typeVenteRadio.isSelected() ? "VENTE_OBJET" : "DEMANDE_SERVICE";
        String localisation   = localisationField.getText().trim();

        final String imagePathToSave = selectedImageUrl != null
                ? selectedImageUrl.replace('\\', '/') : null;

        submitButton.disableProperty().unbind();
        submitButton.setDisable(true);
        submitButton.setText("⏳ Publication...");

        new Thread(() -> {
            try {
                if (gestionPublication == null)
                    throw new IllegalStateException("Gestion_publication indisponible");

                Publications pub = new Publications();
                pub.setStudentId(currentStudentId);
                pub.setTypePublicationFromString(typePublication);
                pub.setTitre(titre);
                pub.setMessage(message);
                pub.setPrixVente(java.math.BigDecimal.valueOf(prix));
                pub.setLocalisation(localisation.isEmpty() ? null : localisation);
                pub.setImageUrl(imagePathToSave);
                pub.setStatus(Publications.StatusPublication.ACTIVE); // ← ACTIVE kept
                gestionPublication.ajouterPublication(pub);
                int pubId = pub.getId();

                Platform.runLater(() -> {
                    submitButton.disableProperty().bind(termsCheckBox.selectedProperty().not());
                    submitButton.setText("📢 Publier");
                    showAlert(Alert.AlertType.INFORMATION, "Publication créée !",
                            "Votre publication a été enregistrée avec succès.\nID : " + pubId);
                    clearForm();
                    runMatchingAsync();
                    goBack();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    submitButton.disableProperty().bind(termsCheckBox.selectedProperty().not());
                    submitButton.setText("📢 Publier");
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Impossible d'enregistrer la publication :\n" + e.getMessage());
                });
            }
        }).start();
    }

    // ═══════════════════════════════════════════════════════════════
    // B) CRÉATION IA AUTO
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void generateAndPublish() {
        String idea = aiIdeaField.getText().trim();

        if (idea.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Description manquante",
                    "Veuillez décrire votre publication avant de continuer.");
            aiIdeaField.requestFocus();
            return;
        }
        if (idea.length() < 10) {
            showAlert(Alert.AlertType.WARNING, "Description trop courte",
                    "Écrivez au moins 10 caractères pour que l'IA génère une publication de qualité.");
            aiIdeaField.requestFocus();
            return;
        }
        if (!validatePriceOnlyForAi()) return;

        String ideaLower = idea.toLowerCase();
        boolean looksLikeSale = ideaLower.contains("je vends") || ideaLower.contains("vente")
                || ideaLower.contains("à vendre") || ideaLower.contains("a vendre")
                || ideaLower.contains("vendre mon") || ideaLower.contains("prix :");
        String typePublication = (typeVenteRadio.isSelected() || looksLikeSale)
                ? "VENTE_OBJET" : "DEMANDE_SERVICE";

        double prix = parsePrix();
        if (prix <= 0) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?:prix\\s*[:=]?\\s*)?(\\d+)(?:\\s*[€e]|\\s*,)")
                    .matcher(idea);
            if (m.find()) prix = Double.parseDouble(m.group(1));
        }

        String localisation = localisationField.getText().trim();

        JSONObject payload = new JSONObject();
        payload.put("student_id",       currentStudentId);
        payload.put("prompt",           idea);
        payload.put("type_publication", typePublication);
        payload.put("prix_vente",       prix);
        if (!localisation.isEmpty())  payload.put("localisation", localisation);
        if (selectedImageUrl != null) payload.put("image_url", selectedImageUrl);

        generateAiBtn.setDisable(true);
        aiLoadingLabel.setVisible(true);
        hideAiPanels();

        new Thread(() -> {
            try {
                JSONObject response = envoyerAn8n(payload);

                Platform.runLater(() -> {
                    generateAiBtn.setDisable(false);
                    aiLoadingLabel.setVisible(false);

                    if (response.optBoolean("success", false)) {
                        int pubId = response.optInt("publication_id", 0);
                        if (pubId <= 0) {
                            String err = "n8n n'a pas retourné publication_id. "
                                    + "Vérifiez le workflow et l'onglet Executions dans n8n.";
                            aiErrorLabel.setText("❌ " + err);
                            aiErrorPanel.setVisible(true);
                            aiErrorPanel.setManaged(true);
                            showAlert(Alert.AlertType.ERROR, "Erreur", err
                                    + "\n\nUtilisez le formulaire manuel (bouton Publier) à la place.");
                            return;
                        }
                        JSONObject pub       = response.optJSONObject("publication");
                        String titreGenere   = pub != null ? pub.optString("titre",   "") : "";
                        String messageGenere = pub != null ? pub.optString("message", "") : "";

                        aiStatusLabel.setText("✅ Publication créée avec succès ! (ID #" + pubId + ")");
                        aiGeneratedTitre.setText("📌 " + titreGenere);
                        aiGeneratedMessage.setText(messageGenere);
                        aiResultsPanel.setVisible(true);
                        aiResultsPanel.setManaged(true);
                        aiIdeaField.clear();
                        runMatchingAsync();
                        return;
                    }

                    String err = response.optString("message", "Erreur inconnue");
                    aiErrorLabel.setText("❌ " + err);
                    aiErrorPanel.setVisible(true);
                    aiErrorPanel.setManaged(true);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    generateAiBtn.setDisable(false);
                    aiLoadingLabel.setVisible(false);
                    aiErrorLabel.setText("❌ n8n indisponible. Lancez n8n ou utilisez le formulaire manuel.");
                    aiErrorPanel.setVisible(true);
                    aiErrorPanel.setManaged(true);
                });
            }
        }).start();
    }

    // ═══════════════════════════════════════════════════════════════
    // VALIDATION
    // ═══════════════════════════════════════════════════════════════

    private boolean validateManualForm() {
        StringBuilder errors = new StringBuilder();

        String titre   = titreField.getText()  != null ? titreField.getText().trim()  : "";
        String message = messageArea.getText() != null ? messageArea.getText().trim() : "";
        String prixStr = prixField.getText()   != null ? prixField.getText().trim()   : "";

        if (!typeVenteRadio.isSelected() && !typeDemandeRadio.isSelected())
            errors.append("• Sélectionnez un type de publication (Vente ou Demande).\n");

        if (titre.isEmpty())           errors.append("• Le titre est obligatoire.\n");
        else if (titre.length() < 3)   errors.append("• Le titre doit contenir au moins 3 caractères.\n");
        else if (titre.length() > 120) errors.append("• Le titre ne peut pas dépasser 120 caractères.\n");

        if (message.isEmpty())            errors.append("• La description est obligatoire.\n");
        else if (message.length() < 10)   errors.append("• La description doit contenir au moins 10 caractères.\n");
        else if (message.length() > 1000) errors.append("• La description ne peut pas dépasser 1000 caractères.\n");

        if (prixStr.isEmpty()) {
            errors.append("• Le prix est obligatoire.\n");
        } else {
            try {
                double prix = Double.parseDouble(prixStr);
                if (prix <= 0)          errors.append("• Le prix doit être supérieur à 0 (ex: 25.00).\n");
                else if (prix > 99999999.99) errors.append("• Le prix est trop élevé.\n");
            } catch (NumberFormatException e) {
                errors.append("• Le prix doit être un nombre valide (ex: 25.00).\n");
            }
        }

        if (!termsCheckBox.isSelected())
            errors.append("• Vous devez accepter les conditions d'utilisation.\n");

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "Formulaire incomplet",
                    "Corrigez les points suivants :\n\n" + errors);
            return false;
        }
        return true;
    }

    private boolean validatePriceOnlyForAi() {
        if (typeDemandeRadio.isSelected()) return true;

        String prixStr = prixField.getText() != null ? prixField.getText().trim() : "";
        if (prixStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Prix manquant",
                    "Pour une vente, le prix est obligatoire.");
            prixField.requestFocus();
            return false;
        }
        try {
            double prix = Double.parseDouble(prixStr);
            if (prix <= 0) {
                showAlert(Alert.AlertType.WARNING, "Prix invalide",
                        "Pour une vente, le prix doit être supérieur à 0 (ex: 25.00).");
                prixField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Prix invalide",
                    "Le prix doit être un nombre valide (ex: 25.00).");
            prixField.requestFocus();
            return false;
        }
        return true;
    }

    // ═══════════════════════════════════════════════════════════════
    // SUGGESTIONS (kept wired)
    // ═══════════════════════════════════════════════════════════════

    @FXML private void applyShortVersion()    { applyVersion("version_courte"); }
    @FXML private void applyDetailedVersion() { applyVersion("version_detaillee"); }
    @FXML private void applyUrgentVersion()   { applyVersion("version_urgente"); }

    private void applyVersion(String key) {
        if (lastAiResult == null) return;
        JSONObject v = lastAiResult.optJSONObject(key);
        if (v != null) {
            titreField.setText(v.optString("titre", ""));
            messageArea.setText(v.optString("description", ""));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // IMAGE HANDLING
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void onUploadImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));

        File file = fc.showOpenDialog(uploadImageButton.getScene().getWindow());
        if (file == null) return;

        try {
            String destFileName = System.currentTimeMillis() + "_"
                    + file.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
            File uploadsDir = new File("uploads/publications");
            uploadsDir.mkdirs();
            File destFile = new File(uploadsDir, destFileName);

            java.nio.file.Files.copy(file.toPath(), destFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            selectedImageUrl = "uploads/publications/" + destFileName;
            imageFileLabel.setText(file.getName());
            imagePreview.setImage(new Image(file.toURI().toString()));
            imagePreview.setVisible(true);
            removeImageButton.setVisible(true);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur image",
                    "Impossible de copier l'image :\n" + e.getMessage());
        }
    }

    @FXML
    private void onRemoveImage() {
        selectedImageUrl = null;
        imageFileLabel.setText("");
        imagePreview.setImage(null);
        imagePreview.setVisible(false);
        removeImageButton.setVisible(false);
    }

    // ═══════════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void goBack() {
        if (mainLayoutController != null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/Views/Publication.fxml"));
                Parent view = loader.load();

                Publication_controller ctrl = loader.getController();
                ctrl.setCurrentStudentId(currentStudentId);
                ctrl.setMainLayoutController(mainLayoutController);

                mainLayoutController.loadContent(view);
                return;
            } catch (Exception e) {
                System.err.println("goBack via mainLayoutController failed: " + e.getMessage());
            }
        }

        // Fallback : previous scene
        if (previousScene != null) {
            javafx.stage.Stage stage = (javafx.stage.Stage) submitButton.getScene().getWindow();
            stage.setScene(previousScene);
            stage.setTitle("Publications");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private void runMatchingAsync() {
        System.out.println("🚀 [MATCHING] runMatchingAsync() appelé pour student #" + currentStudentId);
        new Thread(() -> {
            try {
                System.out.println("🔍 [MATCHING] Thread démarré...");
                Gestion_Matching matching = new Gestion_Matching();
                matching.analyserNouvellesPublications();
            } catch (Exception e) {
                System.err.println("❌ [MATCHING] Erreur : " + e.getMessage());
                e.printStackTrace();
            }
        }, "MatchingThread").start();
    }

    private void hideAiPanels() {
        aiResultsPanel.setVisible(false);   aiResultsPanel.setManaged(false);
        aiErrorPanel.setVisible(false);     aiErrorPanel.setManaged(false);
        suggestionsPanel.setVisible(false); suggestionsPanel.setManaged(false);
    }

    private double parsePrix() {
        try { return Double.parseDouble(prixField.getText().trim()); }
        catch (Exception e) { return 0.0; }
    }

    private void clearForm() {
        titreField.clear();
        messageArea.clear();
        prixField.clear();
        localisationField.clear();
        aiIdeaField.clear();
        termsCheckBox.setSelected(false);
        typeDemandeRadio.setSelected(true);
        onRemoveImage();
        hideAiPanels();
        lastAiResult = null;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}