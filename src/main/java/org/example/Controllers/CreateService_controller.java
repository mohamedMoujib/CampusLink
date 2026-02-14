package org.example.Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import org.example.campusLink.Services.Gestion_Categorie;
import org.example.campusLink.Services.Gestion_Service;
import org.example.campusLink.entities.Categorie;
import org.example.campusLink.entities.Services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for creating new Services
 * Matches database structure: services table with title, description, price, image, prestataire_id, category_id, status
 */
public class CreateService_controller {

    @FXML private TextField titleField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextArea descriptionField;
    @FXML private TextField priceField;
    @FXML private TextField imageField;
    @FXML private Label charCountLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private Gestion_Service gestionService;
    private Gestion_Categorie gestionCategories;

    // Map to store category name -> ID mapping
    private Map<String, Integer> categoryMap = new HashMap<>();

    // TODO: Replace with actual logged-in prestataire ID from session
    private int currentPrestataireId = 1; // Hardcoded for now

    @FXML
    public void initialize() {
        System.out.println("Initializing CreateService_controller...");

        try {
            gestionService = new Gestion_Service();
            gestionCategories = new Gestion_Categorie();

            loadCategories();
            setupCharacterCounter();
            setupPriceField();

            System.out.println("CreateService_controller initialized successfully");

        } catch (Exception e) {
            System.err.println("Error initializing CreateService_controller: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'initialiser le formulaire: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * ✅ Load categories from database
     */
    private void loadCategories() {
        try {
            List<Categorie> categories = gestionCategories.afficherCategories();

            // Clear existing items
            categoryCombo.getItems().clear();
            categoryMap.clear();

            // Add categories to ComboBox
            for (Categorie cat : categories) {
                categoryCombo.getItems().add(cat.getName());
                categoryMap.put(cat.getName(), cat.getId());
            }

            System.out.println("Loaded " + categories.size() + " categories");

        } catch (Exception e) {
            System.err.println("Error loading categories: " + e.getMessage());
            e.printStackTrace();

            // Fallback to hardcoded categories if database fails
            categoryCombo.getItems().addAll(
                    "Programmation",
                    "Mathématiques",
                    "Informatique",
                    "Physique",
                    "Chimie",
                    "Biologie",
                    "Langues",
                    "Histoire-Géographie",
                    "Économie",
                    "Rédaction",
                    "Autre"
            );
        }
    }

    /**
     * ✅ Setup character counter for description
     */
    private void setupCharacterCounter() {
        if (descriptionField != null && charCountLabel != null) {
            descriptionField.textProperty().addListener((observable, oldValue, newValue) -> {
                int length = newValue != null ? newValue.length() : 0;
                charCountLabel.setText(length + "/1000 caractères");

                if (length > 1000) {
                    charCountLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    descriptionField.setText(oldValue); // Prevent exceeding 1000 chars
                } else if (length > 900) {
                    charCountLabel.setStyle("-fx-text-fill: #f59e0b;"); // Orange warning
                } else {
                    charCountLabel.setStyle("-fx-text-fill: #9ca3af;");
                }
            });
        }
    }

    /**
     * ✅ Setup price field to accept only numbers and decimal point
     */
    private void setupPriceField() {
        if (priceField != null) {
            priceField.textProperty().addListener((observable, oldValue, newValue) -> {
                // Allow only numbers and one decimal point
                if (!newValue.matches("\\d*(\\.\\d{0,2})?")) {
                    priceField.setText(oldValue);
                }
            });
        }
    }

    /**
     * ✅ Comprehensive form validation
     */
    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        // Validate title (VARCHAR(100) in DB)
        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            errors.append("• Le titre est obligatoire\n");
        } else if (titleField.getText().trim().length() < 3) {
            errors.append("• Le titre doit contenir au moins 3 caractères\n");
        } else if (titleField.getText().trim().length() > 100) {
            errors.append("• Le titre ne peut pas dépasser 100 caractères (limite de la base de données)\n");
        }

        // Validate category
        if (categoryCombo.getValue() == null || categoryCombo.getValue().isEmpty()) {
            errors.append("• La catégorie est obligatoire\n");
        }

        // Validate description (TEXT in DB - can be NULL but good to have)
        if (descriptionField.getText() != null && descriptionField.getText().trim().length() > 1000) {
            errors.append("• La description ne peut pas dépasser 1000 caractères\n");
        }

        // Validate price (DECIMAL(10,2) in DB - NOT NULL)
        if (priceField.getText() == null || priceField.getText().trim().isEmpty()) {
            errors.append("• Le prix est obligatoire\n");
        } else {
            try {
                double price = Double.parseDouble(priceField.getText().trim());
                if (price <= 0) {
                    errors.append("• Le prix doit être supérieur à 0\n");
                } else if (price > 10000) {
                    errors.append("• Le prix ne peut pas dépasser 10 000€\n");
                }
            } catch (NumberFormatException e) {
                errors.append("• Le prix doit être un nombre valide (ex: 25.00)\n");
            }
        }

        // Validate image (VARCHAR(100) - optional)
        if (imageField != null && imageField.getText() != null &&
                imageField.getText().trim().length() > 100) {
            errors.append("• Le nom de l'image ne peut pas dépasser 100 caractères\n");
        }

        // Show errors if any
        if (errors.length() > 0) {
            showAlert("Erreurs de validation",
                    "Veuillez corriger les erreurs suivantes:\n\n" + errors.toString(),
                    Alert.AlertType.WARNING);
            return false;
        }

        return true;
    }

    /**
     * ✅ SAVE BUTTON - Create the service
     * Database fields: id, title, description, price, image, prestataire_id, category_id, status
     */
    @FXML
    private void saveService() {
        System.out.println("Attempting to save service...");

        // Validate the form
        if (!validateForm()) {
            return;
        }

        // Show confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmer la création");
        confirmDialog.setHeaderText("Créer ce service ?");
        confirmDialog.setContentText(
                "Titre: " + titleField.getText().trim() + "\n" +
                        "Prix: " + priceField.getText().trim() + "€\n" +
                        "Catégorie: " + categoryCombo.getValue() + "\n\n" +
                        "Votre service sera créé avec le statut 'EN_ATTENTE'."
        );

        ButtonType btnConfirm = new ButtonType("✓ Confirmer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("✗ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getButtonTypes().setAll(btnConfirm, btnCancel);

        Optional<ButtonType> result = confirmDialog.showAndWait();

        if (result.isPresent() && result.get() == btnConfirm) {
            try {
                // Create the Services object
                Services service = new Services();

                // Set required fields
                service.setTitle(titleField.getText().trim());
                service.setPrice(Double.parseDouble(priceField.getText().trim()));
                service.setPrestataireId(currentPrestataireId);

                // Set optional fields
                String description = descriptionField.getText();
                service.setDescription(description != null && !description.trim().isEmpty()
                        ? description.trim()
                        : null);

                String imagePath = imageField != null ? imageField.getText() : null;
                service.setImage(imagePath != null && !imagePath.trim().isEmpty()
                        ? imagePath.trim()
                        : null);

                // Get category ID from map
                String selectedCategory = categoryCombo.getValue();
                Integer categoryId = categoryMap.get(selectedCategory);

                if (categoryId != null) {
                    service.setCategoryId(categoryId);
                } else {
                    // Fallback: try to find category by name or use default
                    service.setCategoryId(1); // Default category
                    System.err.println("Warning: Category ID not found for: " + selectedCategory);
                }

                // Status will be set to 'EN_ATTENTE' by default in the database

                // Save to database
                gestionService.ajouterService(service);

                System.out.println("✅ Service created successfully with ID: " + service.getId());

                // Show success message
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Succès");
                successAlert.setHeaderText("Service créé avec succès !");
                successAlert.setContentText(
                        "Votre service \"" + service.getTitle() + "\" a été créé.\n" +
                                "ID: #" + service.getId() + "\n" +
                                "Statut: EN_ATTENTE\n\n" +
                                "Il sera visible après validation par notre équipe."
                );
                successAlert.showAndWait();

                // Return to services page
                goBack();

            } catch (NumberFormatException e) {
                showAlert("Erreur", "Le prix doit être un nombre valide", Alert.AlertType.ERROR);
            } catch (IllegalArgumentException e) {
                // Business logic error from Gestion_Service validation
                showAlert("Erreur de validation", e.getMessage(), Alert.AlertType.WARNING);
            } catch (Exception e) {
                System.err.println("Error saving service: " + e.getMessage());
                e.printStackTrace();
                showAlert("Erreur",
                        "Impossible de créer le service: " + e.getMessage(),
                        Alert.AlertType.ERROR);
            }
        }
    }

    /**
     * ✅ CANCEL BUTTON - Go back to previous page
     */
    @FXML
    private void cancelService() {
        goBack();
    }

    /**
     * ✅ Navigate back with unsaved changes warning
     */
    @FXML
    private void goBack() {
        System.out.println("Going back to services page...");

        try {
            // Check for unsaved changes
            if (hasUnsavedChanges()) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Annuler la création");
                confirmAlert.setHeaderText("Quitter sans enregistrer ?");
                confirmAlert.setContentText(
                        "Vous avez des modifications non enregistrées.\n" +
                                "Êtes-vous sûr de vouloir annuler ?\n\n" +
                                "Toutes les informations saisies seront perdues."
                );

                ButtonType btnYes = new ButtonType("Oui, annuler", ButtonBar.ButtonData.OK_DONE);
                ButtonType btnNo = new ButtonType("Non, continuer", ButtonBar.ButtonData.CANCEL_CLOSE);
                confirmAlert.getButtonTypes().setAll(btnYes, btnNo);

                Optional<ButtonType> result = confirmAlert.showAndWait();

                if (result.isEmpty() || result.get() != btnYes) {
                    return; // User chose to continue editing
                }
            }

            // Navigate back
            Stage stage = (Stage) titleField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/service.fxml"));
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Mes Services");

        } catch (Exception e) {
            System.err.println("Error going back: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur",
                    "Impossible de retourner à la page précédente: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    /**
     * ✅ Check if there are unsaved changes
     */
    private boolean hasUnsavedChanges() {
        return (titleField.getText() != null && !titleField.getText().trim().isEmpty()) ||
                (descriptionField.getText() != null && !descriptionField.getText().trim().isEmpty()) ||
                (priceField.getText() != null && !priceField.getText().trim().isEmpty()) ||
                (categoryCombo.getValue() != null && !categoryCombo.getValue().isEmpty()) ||
                (imageField != null && imageField.getText() != null && !imageField.getText().trim().isEmpty());
    }

    /**
     * ✅ Show alert dialog
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * ✅ Set current prestataire ID (called from previous controller)
     */
    public void setCurrentPrestataireId(int prestataireId) {
        this.currentPrestataireId = prestataireId;
        System.out.println("Current prestataire ID set to: " + prestataireId);
    }
}