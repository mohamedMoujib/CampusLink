package org.example.Controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.example.campusLink.Services.Gestion_Matching;
import org.example.campusLink.Services.Gestion_publication;
import org.example.campusLink.entities.Publications;
import org.json.JSONObject;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

/**
 * Controller for CreatePublication.fxml
 *
 * FIXES APPLIQUÉS :
 *  1. UTF-8 forcé sur toutes les requêtes HTTP (plus de caractères cassés)
 *  2. Accepte HTTP 200 ET 201 en réponse de n8n
 *  3. Lecture correcte du body de réponse (plus de "0 chars")
 *  4. Mapping correct des types de publication
 *  5. Suppression de la dépendance Gestion_IA — HTTP natif Java
 */
public class CreatePublication_controller implements Initializable {

    // ── URL n8n (override via CAMPUSLINK_N8N_URL, ex: http://localhost:5678) ──
    private static final String N8N_BASE = System.getenv().getOrDefault("CAMPUSLINK_N8N_URL", "http://localhost:5678");
    private static final String N8N_URL = N8N_BASE.replaceAll("/$", "") + "/webhook/creer-publication";

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
    private JSONObject lastAiResult     = null;
    private String     selectedImageUrl = null;
    private int        currentStudentId = 1;
    private Gestion_publication gestionPublication;
    private javafx.scene.Scene previousScene = null;

    public void setCurrentStudentId(int id)             { this.currentStudentId = id; }
    public void setPreviousScene(javafx.scene.Scene s)  { this.previousScene = s; }

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

        // Auto-sélection du type si l'utilisateur tape "je vends", "vente", "à vendre" dans l'idée IA
        if (aiIdeaField != null) {
            aiIdeaField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.isBlank()) {
                    String lower = newVal.toLowerCase();
                    if (lower.contains("je vends") || lower.contains("vente") || lower.contains("à vendre")
                            || lower.contains("a vendre") || lower.contains("vendre mon")) {
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
    // MÉTHODE HTTP CENTRALE — FIX UTF-8 + FIX CODE 201
    // ═══════════════════════════════════════════════════════════════

    /**
     * Envoie un payload JSON à n8n et retourne la réponse parsée.
     * FIX 1 : charset=UTF-8 forcé partout
     * FIX 2 : accepte HTTP 200 et 201
     * FIX 3 : lit le body même pour les codes non-200
     */
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
        conn.setReadTimeout(60_000); // OpenRouter peut prendre 30-45 s
        conn.setDoOutput(true);

        // FIX UTF-8 : écrire en UTF-8 explicitement
        try (OutputStream os = conn.getOutputStream()) {
            byte[] bytes = payloadStr.getBytes(StandardCharsets.UTF_8);
            os.write(bytes);
        }

        int statusCode = conn.getResponseCode();
        System.out.println("← HTTP " + statusCode + " de n8n");

        // FIX CODE 201 : lire le body pour 200 ET 201
        String responseBody;
        if (statusCode >= 200 && statusCode < 300) {
            // FIX UTF-8 : lire en UTF-8 explicitement
            responseBody = new String(
                    conn.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
        } else {
            // Lire le body d'erreur aussi
            responseBody = new String(
                    conn.getErrorStream() != null
                            ? conn.getErrorStream().readAllBytes()
                            : new byte[0],
                    StandardCharsets.UTF_8
            );
        }

        System.out.println("← Réponse brute (" + responseBody.length() + " chars) : " + (responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody));
        if (responseBody.isEmpty() && statusCode >= 200 && statusCode < 300) {
            String cl = conn.getHeaderField("Content-Length");
            System.out.println("⚠ Body vide ! Content-Length=" + cl + " | Vérifiez que curl utilise la MÊME URL : " + N8N_URL);
        }
        if (responseBody.isEmpty()) {
            // Réponse vide = n8n n'a pas retourné de corps (workflow incomplet, timeout, etc.)
            if (statusCode >= 200 && statusCode < 300) {
                return new JSONObject().put("success", false)
                        .put("message", "n8n a répondu HTTP 200 sans corps (0 chars). Vérifiez : 1) workflow actif, 2) OpenRouter API key, 3) MySQL campusLink, 4) onglet Executions dans n8n pour les erreurs.");
            } else {
                return new JSONObject().put("success", false)
                        .put("message", "Erreur HTTP " + statusCode + " sans détails");
            }
        }

        return new JSONObject(responseBody);
    }

    // ═══════════════════════════════════════════════════════════════
    // A) SOUMISSION MANUELLE
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void submitPublication() {
        if (!validateManualForm()) return;

        String titre   = titreField.getText().trim();
        String message = messageArea.getText().trim();
        double prix    = Double.parseDouble(prixField.getText().trim());

        // FIX MAPPING TYPE : utiliser les valeurs exactes attendues par n8n
        String typePublication = typeVenteRadio.isSelected() ? "VENTE_OBJET" : "DEMANDE_SERVICE";
        String localisation    = localisationField.getText().trim();

        JSONObject payload = new JSONObject();
        payload.put("student_id",       currentStudentId);
        payload.put("titre",            titre);
        payload.put("message",          message);
        payload.put("type_publication", typePublication);
        payload.put("prix_vente",       prix);
        payload.put("status",           "ACTIVE");
        if (!localisation.isEmpty())  payload.put("localisation", localisation);
        if (selectedImageUrl != null) payload.put("image_url", selectedImageUrl);

        submitButton.disableProperty().unbind();
        submitButton.setDisable(true);
        submitButton.setText("⏳ Publication...");

        // Capturer le chemin image pour le thread (éviter problèmes de closure)
        final String imagePathToSave = selectedImageUrl != null
                ? selectedImageUrl.replace('\\', '/')
                : null;

        // Mode manuel : création directe en base (fiable, fonctionne toujours)
        new Thread(() -> {
            try {
                if (gestionPublication != null) {
                    Publications pub = new Publications();
                    pub.setStudentId(currentStudentId);
                    pub.setTypePublicationFromString(typePublication);
                    pub.setTitre(titre);
                    pub.setMessage(message);
                    pub.setPrixVente(java.math.BigDecimal.valueOf(prix));
                    pub.setLocalisation(localisation.isEmpty() ? null : localisation);
                    pub.setImageUrl(imagePathToSave);
                    pub.setStatus(Publications.StatusPublication.EN_COURS);
                    gestionPublication.ajouterPublication(pub);
                    int pubId = pub.getId();
                    Platform.runLater(() -> {
                        submitButton.disableProperty().bind(termsCheckBox.selectedProperty().not());
                        submitButton.setText("📢 Publier");
                        showAlert(Alert.AlertType.INFORMATION, "Publication créée !",
                                "Votre publication a été enregistrée avec succès.\nID : " + pubId);
                        clearForm();
                        // Déclencher le matching et donc les notifications tuteur
                        runMatchingAsync();
                    });
                } else {
                    throw new IllegalStateException("Gestion_publication indisponible");
                }
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

        // On réutilise les validations de base : prix valide, titre facultatif (l'IA peut le générer),
        // mais on impose au moins un prix cohérent pour une VENTE_OBJET.
        if (!validatePriceOnlyForAi()) return;

        // Type : auto-détecter "vente" si description contient "je vends", "vente", etc.
        String ideaLower = idea.toLowerCase();
        boolean looksLikeSale = ideaLower.contains("je vends") || ideaLower.contains("vente")
                || ideaLower.contains("à vendre") || ideaLower.contains("a vendre")
                || ideaLower.contains("vendre mon") || ideaLower.contains("prix :");
        String typePublication = (typeVenteRadio.isSelected() || looksLikeSale)
                ? "VENTE_OBJET" : "DEMANDE_SERVICE";

        double prix = parsePrix();
        if (prix <= 0) {
            // Extraire le prix de la description (ex: "350€", "Prix : 350€", "350, négociable")
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?:prix\\s*[:=]?\\s*)?(\\d+)(?:\\s*[€e]|\\s*,)");
            java.util.regex.Matcher m = p.matcher(idea);
            if (m.find()) prix = Double.parseDouble(m.group(1));
        }
        String localisation    = localisationField.getText().trim();

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
                            String err = "n8n n'a pas retourné publication_id. Vérifiez le workflow et l'onglet Executions dans n8n.";
                            aiErrorLabel.setText("❌ " + err);
                            aiErrorPanel.setVisible(true);
                            aiErrorPanel.setManaged(true);
                            showAlert(Alert.AlertType.ERROR, "Erreur", err + "\n\nUtilisez le formulaire manuel (bouton Publier) pour créer la publication.");
                            return;
                        }
                        JSONObject pub = response.optJSONObject("publication");
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

                    // n8n a répondu mais erreur : pas de fallback local (tâches indépendantes)
                    String err = response.optString("message", "Erreur inconnue");
                    aiErrorLabel.setText("❌ " + err);
                    aiErrorPanel.setVisible(true);
                    aiErrorPanel.setManaged(true);
                });

            } catch (Exception e) {
                Exception ex = e;
                Platform.runLater(() -> {
                    generateAiBtn.setDisable(false);
                    aiLoadingLabel.setVisible(false);
                    // n8n injoignable : pas de fallback local (tâches indépendantes)
                    aiErrorLabel.setText("❌ n8n indisponible. Lancez n8n ou utilisez le formulaire manuel (bouton Publier) pour créer une publication.");
                    aiErrorPanel.setVisible(true);
                    aiErrorPanel.setManaged(true);
                });
            }
        }).start();
    }

    /**
     * Validation complète du formulaire manuel (avant appel n8n).
     */
    private boolean validateManualForm() {
        StringBuilder errors = new StringBuilder();

        String titre   = titreField.getText() != null ? titreField.getText().trim() : "";
        String message = messageArea.getText() != null ? messageArea.getText().trim() : "";
        String prixStr = prixField.getText() != null ? prixField.getText().trim() : "";

        // Type
        if (!typeVenteRadio.isSelected() && !typeDemandeRadio.isSelected()) {
            errors.append("• Sélectionnez un type de publication (Vente ou Demande).\n");
        }

        // Titre
        if (titre.isEmpty()) {
            errors.append("• Le titre est obligatoire.\n");
        } else if (titre.length() < 3) {
            errors.append("• Le titre doit contenir au moins 3 caractères.\n");
        } else if (titre.length() > 120) {
            errors.append("• Le titre ne peut pas dépasser 120 caractères.\n");
        }

        // Message
        if (message.isEmpty()) {
            errors.append("• La description est obligatoire.\n");
        } else if (message.length() < 10) {
            errors.append("• La description doit contenir au moins 10 caractères.\n");
        } else if (message.length() > 1000) {
            errors.append("• La description ne peut pas dépasser 1000 caractères.\n");
        }

        // Prix
        if (prixStr.isEmpty()) {
            errors.append("• Le prix est obligatoire.\n");
        } else {
            try {
                double prix = Double.parseDouble(prixStr);
                if (prix <= 0) {
                    errors.append("• Le prix doit être supérieur à 0 (ex: 25.00).\n");
                } else if (prix > 99999999.99) {
                    errors.append("• Le prix est trop élevé.\n");
                }
            } catch (NumberFormatException e) {
                errors.append("• Le prix doit être un nombre valide (ex: 25.00).\n");
            }
        }

        // Conditions d'utilisation
        if (!termsCheckBox.isSelected()) {
            errors.append("• Vous devez accepter les conditions d'utilisation.\n");
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "Formulaire incomplet", "Corrigez les points suivants :\n\n" + errors);
            return false;
        }
        return true;
    }

    /**
     * Validation minimale pour la création pilotée par l'IA :
     * on impose seulement un prix cohérent si c'est une vente.
     */
    private boolean validatePriceOnlyForAi() {
        String prixStr = prixField.getText() != null ? prixField.getText().trim() : "";

        // Si DEMANDE_SERVICE, le prix peut être 0 ou laissé vide (sera géré côté backend).
        if (typeDemandeRadio.isSelected()) {
            return true;
        }

        // Pour VENTE_OBJET, on exige un prix > 0
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
            String timestamp    = String.valueOf(System.currentTimeMillis());
            String destFileName = timestamp + "_" + file.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
            File   uploadsDir   = new File("uploads/publications");
            uploadsDir.mkdirs();
            File destFile = new File(uploadsDir, destFileName);

            java.nio.file.Files.copy(
                    file.toPath(), destFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Chemin relatif avec / (compatible tous OS)
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
        javafx.stage.Stage stage = (javafx.stage.Stage) submitButton.getScene().getWindow();
        if (previousScene != null) {
            stage.setScene(previousScene);
            stage.setTitle("Publications");
        } else {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/Views/Publications.fxml"));
                stage.setScene(new javafx.scene.Scene(loader.load()));
                stage.setTitle("Publications");
            } catch (Exception e) {
                stage.hide();
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Lance le moteur de matching en arrière-plan afin de notifier
     * automatiquement les tuteurs dont le service est compatible avec
     * les nouvelles publications DEMANDE_SERVICE.
     */
    private void runMatchingAsync() {
        new Thread(() -> {
            try {
                Gestion_Matching matching = new Gestion_Matching();
                matching.analyserNouvellesPublications();
            } catch (Exception e) {
                System.err.println("❌ Erreur matching automatique : " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void hideAiPanels() {
        aiResultsPanel.setVisible(false);
        aiResultsPanel.setManaged(false);
        aiErrorPanel.setVisible(false);
        aiErrorPanel.setManaged(false);
        suggestionsPanel.setVisible(false);
        suggestionsPanel.setManaged(false);
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