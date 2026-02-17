package org.example.Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.example.campusLink.Services.Gestion_publication;
import org.example.campusLink.entities.Publications;
import org.example.campusLink.entities.Publications.TypePublication;
import org.example.campusLink.entities.Publications.StatusPublication;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class CreatePublication_controller {

    @FXML private RadioButton typeVenteRadio;
    @FXML private RadioButton typeDemandeRadio;

    @FXML private TextField titreField;
    @FXML private TextArea messageArea;
    @FXML private Label messageCounter;
    @FXML private TextField prixField;
    @FXML private TextField localisationField;

    @FXML private Button uploadImageButton;
    @FXML private ImageView imagePreview;
    @FXML private Label imageFileLabel;
    @FXML private Button removeImageButton;

    @FXML private CheckBox termsCheckBox;
    @FXML private Button submitButton;
    @FXML private Button deleteButton;

    private Gestion_publication gestionPublication;
    private int currentStudentId = 1; // TODO: Replace with session
    private File selectedImageFile;
    private String uploadedImagePath;

    private static final String UPLOAD_DIR = "uploads/publications/";
    private static final long MAX_FILE_SIZE = 5_000_000; // 5 MB

    @FXML
    public void initialize() {
        System.out.println("Initializing CreatePublication_controller...");

        try {
            gestionPublication = new Gestion_publication();
            setupMessageCounter();
            setupFormValidation();
            setupPriceField();
            setupImageUpload();
            validateForm();
            System.out.println("CreatePublication_controller initialized successfully");

        } catch (Exception e) {
            System.err.println("Error initializing CreatePublication_controller: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur d'initialisation: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupMessageCounter() {
        messageArea.textProperty().addListener((observable, oldValue, newValue) -> {
            int length = newValue.length();
            messageCounter.setText(length + "/1000");

            if (length > 1000) {
                messageCounter.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                messageArea.setText(oldValue);
            } else if (length > 900) {
                messageCounter.setStyle("-fx-text-fill: #f59e0b;");
            } else {
                messageCounter.setStyle("-fx-text-fill: #6b7280;");
            }

            validateForm();
        });
    }

    private void setupFormValidation() {
        titreField.textProperty().addListener((obs, old, newVal) -> validateForm());
        messageArea.textProperty().addListener((obs, old, newVal) -> validateForm());
        prixField.textProperty().addListener((obs, old, newVal) -> validateForm());
        termsCheckBox.selectedProperty().addListener((obs, old, newVal) -> validateForm());
    }

    private void setupPriceField() {
        prixField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d{0,2})?")) {
                prixField.setText(oldValue);
            }
        });
    }

    private void setupImageUpload() {
        imagePreview.setVisible(false);
        imageFileLabel.setVisible(false);
        removeImageButton.setVisible(false);
    }

    private void validateForm() {
        boolean isValid = titreField != null && !titreField.getText().trim().isEmpty()
                && messageArea != null && !messageArea.getText().trim().isEmpty()
                && messageArea.getText().length() <= 1000
                && prixField != null && !prixField.getText().trim().isEmpty()
                && termsCheckBox != null && termsCheckBox.isSelected();

        if (submitButton != null) {
            submitButton.setDisable(!isValid);
        }
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
            imagePreview.setImage(image);
            imagePreview.setVisible(true);
            imageFileLabel.setText(imageFile.getName());
            imageFileLabel.setVisible(true);
            removeImageButton.setVisible(true);
            uploadImageButton.setText("Changer l'image");

        } catch (Exception e) {
            System.err.println("Error displaying image: " + e.getMessage());
            showAlert("Erreur", "Impossible d'afficher l'image", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onRemoveImage() {
        selectedImageFile = null;
        imagePreview.setImage(null);
        imagePreview.setVisible(false);
        imageFileLabel.setVisible(false);
        removeImageButton.setVisible(false);
        uploadImageButton.setText("📷 Ajouter une image (optionnel)");
    }

    @FXML
    private void submitPublication() {
        System.out.println("Submitting publication...");

        try {
            if (!validateFormData()) return;

            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Confirmer la publication");
            confirmDialog.setHeaderText("Publier votre annonce ?");
            confirmDialog.setContentText(
                    "Titre: " + titreField.getText() + "\n" +
                            "Prix: " + prixField.getText() + "€\n\n" +
                            "Votre publication sera visible par tous les étudiants.\n" +
                            "Voulez-vous continuer ?"
            );

            ButtonType btnConfirm = new ButtonType("✓ Publier", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnCancel  = new ButtonType("✗ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmDialog.getButtonTypes().setAll(btnConfirm, btnCancel);

            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isEmpty() || result.get() != btnConfirm) return;

            if (selectedImageFile != null) {
                uploadedImagePath = uploadImage(selectedImageFile);
            }

            Publications newPublication = createPublicationFromForm();

            System.out.println("=== PUBLICATION OBJECT CREATED ===");
            System.out.println("Type: "       + newPublication.getTypePublication());
            System.out.println("Status: "     + newPublication.getStatus());
            System.out.println("Student ID: " + newPublication.getStudentId());
            System.out.println("Title: "      + newPublication.getTitre());
            System.out.println("Price: "      + newPublication.getPrixVente());

            gestionPublication.ajouterPublication(newPublication);

            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Succès");
            successAlert.setHeaderText("Publication créée !");
            successAlert.setContentText(
                    "Votre publication a été créée avec succès.\n" +
                            "Elle est maintenant visible par tous les étudiants.\n\n" +
                            "ID de la publication: #" + newPublication.getId()
            );
            successAlert.showAndWait();

            goBackToPublications();

        } catch (Exception e) {
            System.err.println("Error creating publication: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la création: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean validateFormData() {
        if (titreField.getText().trim().isEmpty()) {
            showAlert("Erreur", "Veuillez saisir un titre", Alert.AlertType.WARNING);
            titreField.requestFocus();
            return false;
        }
        if (titreField.getText().length() > 200) {
            showAlert("Erreur", "Le titre ne doit pas dépasser 200 caractères", Alert.AlertType.WARNING);
            return false;
        }

        String message = messageArea.getText().trim();
        if (message.isEmpty()) {
            showAlert("Erreur", "Veuillez saisir une description", Alert.AlertType.WARNING);
            messageArea.requestFocus();
            return false;
        }
        if (message.length() > 1000) {
            showAlert("Erreur", "La description ne doit pas dépasser 1000 caractères", Alert.AlertType.WARNING);
            return false;
        }

        String priceText = prixField.getText().trim();
        if (priceText.isEmpty()) {
            showAlert("Erreur", "Veuillez saisir un prix", Alert.AlertType.WARNING);
            prixField.requestFocus();
            return false;
        }
        try {
            BigDecimal price = new BigDecimal(priceText);
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                showAlert("Erreur", "Le prix doit être supérieur à zéro", Alert.AlertType.WARNING);
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Le prix n'est pas valide", Alert.AlertType.WARNING);
            return false;
        }

        if (!termsCheckBox.isSelected()) {
            showAlert("Erreur", "Veuillez accepter les conditions générales", Alert.AlertType.WARNING);
            return false;
        }

        return true;
    }

    private Publications createPublicationFromForm() {
        Publications pub = new Publications();
        pub.setStudentId(currentStudentId);
        pub.setTitre(titreField.getText().trim());
        pub.setMessage(messageArea.getText().trim());
        pub.setImageUrl(uploadedImagePath);

        String localisation = localisationField.getText().trim();
        pub.setLocalisation(localisation.isEmpty() ? null : localisation);

        pub.setPrixVente(new BigDecimal(prixField.getText().trim()));
        pub.setTypePublication(TypePublication.VENTE_OBJET);
        pub.setStatus(StatusPublication.ACTIVE);

        return pub;
    }

    private String uploadImage(File imageFile) throws IOException {
        Path uploadDir = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String filename   = System.currentTimeMillis() + "_" + imageFile.getName();
        Path   targetPath = uploadDir.resolve(filename);
        Files.copy(imageFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return UPLOAD_DIR + filename;
    }

    @FXML
    private void goBack() {
        boolean hasChanges = !titreField.getText().trim().isEmpty()
                || !messageArea.getText().trim().isEmpty()
                || selectedImageFile != null;

        if (hasChanges) {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Annuler la publication");
            confirmDialog.setHeaderText("Quitter sans enregistrer ?");
            confirmDialog.setContentText("Toutes les informations saisies seront perdues.");

            ButtonType btnYes = new ButtonType("Oui, annuler",   ButtonBar.ButtonData.OK_DONE);
            ButtonType btnNo  = new ButtonType("Non, continuer", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmDialog.getButtonTypes().setAll(btnYes, btnNo);

            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isPresent() && result.get() == btnYes) {
                goBackToPublications();
            }
        } else {
            goBackToPublications();
        }
    }

    /**
     * Navigate back to the publications list.
     *
     * FIX: The original path was "/Publication.fxml" which returned null
     * because all FXML files live under /Views/. The correct path is
     * "/Views/Publication.fxml" — matching every other navigation call
     * in the project.
     */
    private void goBackToPublications() {
        try {
            // ✅ FIXED: was "/Publication.fxml" — missing /Views/ prefix
            Parent root = FXMLLoader.load(getClass().getResource("/Views/Publication.fxml"));

            Stage stage = getStage();
            if (stage != null) {
                stage.setScene(new Scene(root));
                stage.setTitle("Publications");
            } else {
                System.err.println("Could not find stage to navigate back!");
            }

        } catch (Exception e) {
            System.err.println("Error navigating back: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Convenience: find the current Stage from any available scene node. */
    private Stage getStage() {
        if (titreField != null && titreField.getScene() != null)
            return (Stage) titreField.getScene().getWindow();
        if (messageArea != null && messageArea.getScene() != null)
            return (Stage) messageArea.getScene().getWindow();
        if (uploadImageButton != null && uploadImageButton.getScene() != null)
            return (Stage) uploadImageButton.getScene().getWindow();
        return null;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setCurrentStudentId(int studentId) {
        this.currentStudentId = studentId;
        System.out.println("Current student ID set to: " + studentId);
    }

    @FXML
    private void deletePublication() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("⚠️ Confirmation de suppression");
        confirmDialog.setHeaderText("Supprimer cette publication ?");
        confirmDialog.setContentText(
                "Cette action est irréversible.\n" +
                        "Êtes-vous sûr de vouloir supprimer cette publication ?"
        );

        ButtonType btnDelete = new ButtonType("🗑 Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Annuler",      ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getButtonTypes().setAll(btnDelete, btnCancel);

        confirmDialog.getDialogPane().lookupButton(btnDelete).setStyle(
                "-fx-background-color: #dc2626; -fx-text-fill: white; -fx-font-weight: bold;"
        );

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == btnDelete) {
                try {
                    showAlert("Info", "Fonctionnalité de suppression à implémenter", Alert.AlertType.INFORMATION);
                    goBackToPublications();
                } catch (Exception e) {
                    System.err.println("Error deleting: " + e.getMessage());
                    showAlert("Erreur", "Impossible de supprimer: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }
}