package org.example.Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.example.campusLink.Services.Gestion_Service;
import org.example.campusLink.entities.Services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
    @FXML private Label charCountLabel;

    @FXML private Button uploadImageButton;
    @FXML private ImageView imagePreview;
    @FXML private Label imageFileLabel;
    @FXML private Button removeImageButton;

    private Gestion_Service gestionService;
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
                service.setCategoryId(0);

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
        return (titleField.getText() != null && !titleField.getText().trim().isEmpty()) ||
                (descriptionField.getText() != null && !descriptionField.getText().trim().isEmpty()) ||
                (priceField.getText() != null && !priceField.getText().trim().isEmpty()) ||
                (selectedImageFile != null);
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