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
import javafx.scene.Node;

import org.example.campusLink.Services.Gestion_publication;
import org.example.campusLink.entities.Publications;
import org.example.campusLink.entities.Publications.TypePublication;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Controller pour créer une nouvelle publication
 * Support upload d'image et deux types: Vente et Demande
 */
public class CreatePublication_controller {

    @FXML private RadioButton typeVenteRadio;
    @FXML private RadioButton typeDemandeRadio;
    @FXML private ToggleGroup typeGroup;

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

            setupTypeToggle();
            setupMessageCounter();
            setupFormValidation();
            setupPriceField();
            setupImageUpload();

            System.out.println("CreatePublication_controller initialized successfully");

        } catch (Exception e) {
            System.err.println("Error initializing CreatePublication_controller: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur d'initialisation: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Configuration du choix du type
     */
    private void setupTypeToggle() {
        typeGroup = new ToggleGroup();
        typeVenteRadio.setToggleGroup(typeGroup);
        typeDemandeRadio.setToggleGroup(typeGroup);

        // Par défaut: Vente
        typeVenteRadio.setSelected(true);

        typeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            validateForm();
        });
    }

    /**
     * Compteur de caractères pour le message
     */
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

    /**
     * Validation du formulaire
     */
    private void setupFormValidation() {
        titreField.textProperty().addListener((obs, old, newVal) -> validateForm());
        messageArea.textProperty().addListener((obs, old, newVal) -> validateForm());
        prixField.textProperty().addListener((obs, old, newVal) -> validateForm());
        termsCheckBox.selectedProperty().addListener((obs, old, newVal) -> validateForm());
    }

    /**
     * Le champ prix n'accepte que les nombres et décimales
     */
    private void setupPriceField() {
        prixField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d{0,2})?")) {
                prixField.setText(oldValue);
            }
        });
    }

    /**
     * Configuration de l'upload d'image
     */
    private void setupImageUpload() {
        imagePreview.setVisible(false);
        imageFileLabel.setVisible(false);
        removeImageButton.setVisible(false);
    }

    /**
     * Valider le formulaire
     */
    private void validateForm() {
        boolean isValid = !titreField.getText().trim().isEmpty()
                && !messageArea.getText().trim().isEmpty()
                && messageArea.getText().length() <= 1000
                && !prixField.getText().trim().isEmpty()
                && termsCheckBox.isSelected();

        submitButton.setDisable(!isValid);
    }

    /**
     * Upload d'image
     */
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
            // Vérifier la taille
            if (file.length() > MAX_FILE_SIZE) {
                showAlert("Erreur", "L'image est trop volumineuse (max 5 MB)", Alert.AlertType.WARNING);
                return;
            }

            selectedImageFile = file;
            displayImagePreview(file);
        }
    }

    /**
     * Afficher la prévisualisation de l'image
     */
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

    /**
     * Supprimer l'image sélectionnée
     */
    @FXML
    private void onRemoveImage() {
        selectedImageFile = null;
        imagePreview.setImage(null);
        imagePreview.setVisible(false);
        imageFileLabel.setVisible(false);
        removeImageButton.setVisible(false);
        uploadImageButton.setText("📷 Ajouter une image (optionnel)");
    }

    /**
     * Soumettre la publication
     */
    @FXML
    private void submitPublication() {
        System.out.println("Submitting publication...");

        try {
            if (!validateFormData()) {
                return;
            }

            // Confirmation
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Confirmer la publication");
            confirmDialog.setHeaderText("Publier votre annonce ?");

            String typeLabel = typeVenteRadio.isSelected() ? "Vente d'objet" : "Demande de service";
            confirmDialog.setContentText(
                    "Type: " + typeLabel + "\n" +
                            "Titre: " + titreField.getText() + "\n" +
                            "Prix: " + prixField.getText() + "€\n\n" +
                            "Votre publication sera visible par tous les étudiants.\n" +
                            "Voulez-vous continuer ?"
            );

            ButtonType btnConfirm = new ButtonType("✓ Publier", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnCancel = new ButtonType("✗ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmDialog.getButtonTypes().setAll(btnConfirm, btnCancel);

            Optional<ButtonType> result = confirmDialog.showAndWait();

            if (result.isPresent() && result.get() == btnConfirm) {
                // Upload de l'image si présente
                if (selectedImageFile != null) {
                    uploadedImagePath = uploadImage(selectedImageFile);
                }

                // Créer la publication
                Publications newPublication = createPublicationFromForm();

                // Sauvegarder
                gestionPublication.ajouterPublication(newPublication);

                // Message de succès
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Succès");
                successAlert.setHeaderText("Publication créée !");
                successAlert.setContentText(
                        "Votre publication a été créée avec succès.\n" +
                                "Elle est maintenant visible par tous les étudiants.\n\n" +
                                "ID de la publication: #" + newPublication.getId()
                );
                successAlert.showAndWait();

                // Retour à la liste
                goBackToPublications();
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Validation error: " + e.getMessage());
            showAlert("Attention", e.getMessage(), Alert.AlertType.WARNING);

        } catch (Exception e) {
            System.err.println("Error submitting publication: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de créer la publication: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Valider les données du formulaire
     */
    private boolean validateFormData() {
        // Titre
        if (titreField.getText().trim().isEmpty()) {
            showAlert("Erreur", "Veuillez saisir un titre", Alert.AlertType.WARNING);
            titreField.requestFocus();
            return false;
        }

        if (titreField.getText().length() > 200) {
            showAlert("Erreur", "Le titre ne doit pas dépasser 200 caractères", Alert.AlertType.WARNING);
            return false;
        }

        // Message
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

        // Prix
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

        // CGU
        if (!termsCheckBox.isSelected()) {
            showAlert("Erreur", "Veuillez accepter les conditions générales", Alert.AlertType.WARNING);
            return false;
        }

        return true;
    }

    /**
     * Créer l'objet Publication depuis le formulaire
     */
    private Publications createPublicationFromForm() {
        Publications pub = new Publications();

        pub.setStudentId(currentStudentId);
        pub.setTitre(titreField.getText().trim());
        pub.setMessage(messageArea.getText().trim());
        pub.setImageUrl(uploadedImagePath);
        pub.setLocalisation(localisationField.getText().trim());

        // Type
        if (typeVenteRadio.isSelected()) {
            pub.setTypePublication(TypePublication.VENTE_OBJET);
            pub.setPrixVente(new BigDecimal(prixField.getText().trim()));
        } else {
            pub.setTypePublication(TypePublication.DEMANDE_SERVICE);
            pub.setPrixVente(new BigDecimal(prixField.getText().trim()));
        }

        return pub;
    }

    /**
     * Upload l'image sur le serveur
     */
    private String uploadImage(File imageFile) throws IOException {
        // Créer le dossier s'il n'existe pas
        Path uploadDir = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Nom de fichier unique
        String filename = System.currentTimeMillis() + "_" + imageFile.getName();
        Path targetPath = uploadDir.resolve(filename);

        // Copier le fichier
        Files.copy(imageFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return UPLOAD_DIR + filename;
    }

    /**
     * Annuler et retourner
     */
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

            ButtonType btnYes = new ButtonType("Oui, annuler", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnNo = new ButtonType("Non, continuer", ButtonBar.ButtonData.CANCEL_CLOSE);
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
     * Retour à la liste des publications
     */
    private void goBackToPublications() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Publication.fxml"));
            Stage stage = (Stage) titreField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Publications");

        } catch (Exception e) {
            System.err.println("Error navigating back: " + e.getMessage());
            e.printStackTrace();
        }
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

    /**
     * Setter pour l'ID de l'étudiant
     */
    public void setCurrentStudentId(int studentId) {
        this.currentStudentId = studentId;
        System.out.println("Current student ID set to: " + studentId);
    }
}