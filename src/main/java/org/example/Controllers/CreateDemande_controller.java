package org.example.Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.campusLink.Services.Gestion_Demande;
import org.example.campusLink.Services.Gestion_Service;
import org.example.campusLink.entities.Demandes;
import org.example.campusLink.entities.Services;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Controller for creating new Demandes (Service Requests)
 * Handles form validation, service selection, and demande submission
 *
 * FEATURES:
 * - ✓ CONFIRMATION BUTTON: Validates and submits demande with confirmation dialog
 * - ✗ ANNULATION BUTTON: Cancels creation with unsaved changes warning
 */
public class CreateDemande_controller {

    // ==================== FXML COMPONENTS ====================

    @FXML
    private ComboBox<Services> serviceCombo;

    @FXML
    private VBox servicePreviewBox;

    @FXML
    private Label previewServiceName;

    @FXML
    private Label previewServiceDescription;

    @FXML
    private Label previewServicePrice;

    @FXML
    private Label previewServiceCategory;

    @FXML
    private Label previewServiceProvider;

    @FXML
    private TextArea messageArea;

    @FXML
    private Label messageCounter;

    @FXML
    private DatePicker requestedDatePicker;

    @FXML
    private TextField proposedPriceField;

    @FXML
    private CheckBox termsCheckBox;

    @FXML
    private Button submitButton;

    // ==================== SERVICES ====================

    private Gestion_Demande demandeManager;
    private Gestion_Service serviceManager;

    // ==================== STATE ====================

    private int currentStudentId = 1; // Default - should be set from session
    private Services selectedService;

    // ==================== INITIALIZATION ====================

    @FXML
    public void initialize() {
        System.out.println("Initializing CreateDemande_controller...");
        try {
            demandeManager = new Gestion_Demande();
            serviceManager = new Gestion_Service();

            setupServiceComboBox();
            setupMessageCounter();
            setupFormValidation();
            setupPriceField();

            System.out.println("CreateDemande_controller initialized successfully");
        } catch (Exception e) {
            System.err.println("Error initializing CreateDemande_controller: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le formulaire: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ==================== SETUP METHODS ====================

    /**
     * Setup service combo box with available services
     */
    private void setupServiceComboBox() {
        try {
            List<Services> servicesList = serviceManager.afficherServices();

            // Custom cell factory to display service name
            serviceCombo.setCellFactory(param -> new ListCell<Services>() {
                @Override
                protected void updateItem(Services service, boolean empty) {
                    super.updateItem(service, empty);
                    if (empty || service == null) {
                        setText(null);
                    } else {
                        setText(service.getTitle() + " - " + service.getFormattedPrice());
                    }
                }
            });

            // Button cell for the selected item
            serviceCombo.setButtonCell(new ListCell<Services>() {
                @Override
                protected void updateItem(Services service, boolean empty) {
                    super.updateItem(service, empty);
                    if (empty || service == null) {
                        setText("Sélectionnez un service...");
                    } else {
                        setText(service.getTitle() + " - " + service.getFormattedPrice());
                    }
                }
            });

            serviceCombo.getItems().addAll(servicesList);
            System.out.println("Loaded " + servicesList.size() + " services into combo box");

        } catch (Exception e) {
            System.err.println("Error loading services: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les services: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Setup message character counter
     */
    private void setupMessageCounter() {
        messageArea.textProperty().addListener((observable, oldValue, newValue) -> {
            int length = newValue.length();
            messageCounter.setText(length + "/500");

            // Change color based on length
            if (length > 500) {
                messageCounter.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                messageArea.setText(oldValue); // Prevent exceeding 500 chars
            } else if (length > 450) {
                messageCounter.setStyle("-fx-text-fill: #f59e0b;");
            } else {
                messageCounter.setStyle("-fx-text-fill: #6b7280;");
            }

            validateForm();
        });
    }

    /**
     * Setup form validation - enable/disable submit button
     */
    private void setupFormValidation() {
        // Listen to all form changes
        serviceCombo.valueProperty().addListener((obs, old, newVal) -> validateForm());
        messageArea.textProperty().addListener((obs, old, newVal) -> validateForm());
        termsCheckBox.selectedProperty().addListener((obs, old, newVal) -> validateForm());
    }

    /**
     * Setup price field to accept only numbers and decimal point
     */
    private void setupPriceField() {
        proposedPriceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d{0,2})?")) {
                proposedPriceField.setText(oldValue);
            }
        });
    }

    /**
     * Validate form and enable/disable submit button
     */
    private void validateForm() {
        boolean isValid = selectedService != null
                && !messageArea.getText().trim().isEmpty()
                && messageArea.getText().length() <= 500
                && termsCheckBox.isSelected();

        submitButton.setDisable(!isValid);
    }

    // ==================== EVENT HANDLERS ====================

    /**
     * Handle service selection - show preview
     */
    @FXML
    private void onServiceSelected() {
        selectedService = serviceCombo.getValue();

        if (selectedService != null) {
            System.out.println("Selected service: " + selectedService.getTitle());

            // Show preview
            servicePreviewBox.setVisible(true);
            servicePreviewBox.setManaged(true);

            // Update preview fields
            previewServiceName.setText(selectedService.getTitle());
            previewServiceDescription.setText(
                    selectedService.getDescription() != null
                            ? selectedService.getDescription()
                            : "Aucune description disponible"
            );
            previewServicePrice.setText(selectedService.getFormattedPrice());
            previewServiceCategory.setText(
                    selectedService.getCategoryName() != null
                            ? "📚 " + selectedService.getCategoryName()
                            : "Non catégorisé"
            );
            previewServiceProvider.setText(
                    "👨‍💼 Prestataire: " + selectedService.getPrestataireName()
            );

            validateForm();
        } else {
            servicePreviewBox.setVisible(false);
            servicePreviewBox.setManaged(false);
        }
    }

    /**
     * ✓ CONFIRMATION BUTTON - Submit the demande
     *
     * WORKFLOW:
     * 1. Validate form data
     * 2. Show confirmation dialog with demande summary
     * 3. Create demande object from form
     * 4. Submit to database via Gestion_Demande
     * 5. Show success message with demande ID
     * 6. Navigate back to demandes list
     */
    @FXML
    private void submitDemande() {
        System.out.println("Submitting demande...");

        try {
            // Double-check validation
            if (!validateFormData()) {
                return;
            }

            // Show confirmation dialog
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Confirmer la demande");
            confirmDialog.setHeaderText("Soumettre votre demande ?");
            confirmDialog.setContentText(
                    "Service: " + selectedService.getTitle() + "\n" +
                            "Prix: " + selectedService.getFormattedPrice() + "\n\n" +
                            "Votre demande sera envoyée au prestataire.\n" +
                            "Voulez-vous continuer ?"
            );

            // Customize button text
            ButtonType btnConfirm = new ButtonType("✓ Confirmer", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnCancel = new ButtonType("✗ Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmDialog.getButtonTypes().setAll(btnConfirm, btnCancel);

            Optional<ButtonType> result = confirmDialog.showAndWait();

            if (result.isPresent() && result.get() == btnConfirm) {
                // Create demande object
                Demandes newDemande = createDemandeFromForm();

                // Submit to database
                demandeManager.ajouterDemande(newDemande);

                // Show success message
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Succès");
                successAlert.setHeaderText("Demande créée !");
                successAlert.setContentText(
                        "Votre demande a été créée avec succès.\n" +
                                "Le prestataire sera notifié et vous recevrez une réponse bientôt.\n\n" +
                                "ID de la demande: #" + newDemande.getId()
                );
                successAlert.showAndWait();

                // Navigate back to demandes list
                goBackToDemandes();
            }

        } catch (IllegalStateException e) {
            // Business logic error (duplicate, invalid service, etc.)
            System.err.println("Validation error: " + e.getMessage());
            showAlert("Attention", e.getMessage(), Alert.AlertType.WARNING);

        } catch (Exception e) {
            // Technical error
            System.err.println("Error submitting demande: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de créer la demande: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * ✗ ANNULATION BUTTON - Cancel and go back
     *
     * WORKFLOW:
     * 1. Check if form has unsaved changes
     * 2. If changes exist, show confirmation dialog
     * 3. If user confirms, navigate back to demandes list
     * 4. If no changes, navigate back directly
     */
    @FXML
    private void goBack() {
        System.out.println("Cancel button clicked");

        // Check if form has been modified
        boolean hasChanges = selectedService != null
                || !messageArea.getText().trim().isEmpty()
                || requestedDatePicker.getValue() != null
                || !proposedPriceField.getText().trim().isEmpty();

        if (hasChanges) {
            // Show confirmation dialog
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Annuler la demande");
            confirmDialog.setHeaderText("Quitter sans enregistrer ?");
            confirmDialog.setContentText(
                    "Vous avez des modifications non enregistrées.\n" +
                            "Êtes-vous sûr de vouloir annuler ?\n\n" +
                            "Toutes les informations saisies seront perdues."
            );

            // Customize buttons
            ButtonType btnYes = new ButtonType("Oui, annuler", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnNo = new ButtonType("Non, continuer", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmDialog.getButtonTypes().setAll(btnYes, btnNo);

            Optional<ButtonType> result = confirmDialog.showAndWait();

            if (result.isPresent() && result.get() == btnYes) {
                goBackToDemandes();
            }
            // If "Non, continuer" is clicked, do nothing (stay on page)

        } else {
            // No changes, go back directly
            goBackToDemandes();
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Validate form data before submission
     */
    private boolean validateFormData() {
        // Service selection
        if (selectedService == null) {
            showAlert("Erreur", "Veuillez sélectionner un service", Alert.AlertType.WARNING);
            return false;
        }

        // Message
        String message = messageArea.getText().trim();
        if (message.isEmpty()) {
            showAlert("Erreur", "Veuillez saisir un message", Alert.AlertType.WARNING);
            messageArea.requestFocus();
            return false;
        }

        if (message.length() > 500) {
            showAlert("Erreur", "Le message ne doit pas dépasser 500 caractères", Alert.AlertType.WARNING);
            messageArea.requestFocus();
            return false;
        }

        // Terms acceptance
        if (!termsCheckBox.isSelected()) {
            showAlert("Erreur", "Veuillez accepter les conditions générales", Alert.AlertType.WARNING);
            return false;
        }

        // Validate proposed price if provided
        String priceText = proposedPriceField.getText().trim();
        if (!priceText.isEmpty()) {
            try {
                BigDecimal price = new BigDecimal(priceText);
                if (price.compareTo(BigDecimal.ZERO) <= 0) {
                    showAlert("Erreur", "Le prix proposé doit être supérieur à zéro", Alert.AlertType.WARNING);
                    proposedPriceField.requestFocus();
                    return false;
                }
            } catch (NumberFormatException e) {
                showAlert("Erreur", "Le prix proposé n'est pas valide", Alert.AlertType.WARNING);
                proposedPriceField.requestFocus();
                return false;
            }
        }

        // Validate date if provided
        LocalDate selectedDate = requestedDatePicker.getValue();
        if (selectedDate != null && selectedDate.isBefore(LocalDate.now())) {
            showAlert("Erreur", "La date souhaitée ne peut pas être dans le passé", Alert.AlertType.WARNING);
            requestedDatePicker.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Create Demandes object from form data
     */
    private Demandes createDemandeFromForm() {
        Demandes demande = new Demandes();

        demande.setStudentId(currentStudentId);
        demande.setServiceId(selectedService.getId());
        demande.setPrestataireId(selectedService.getPrestataireId());
        demande.setMessage(messageArea.getText().trim());

        // Optional: Requested date
        if (requestedDatePicker.getValue() != null) {
            LocalDate date = requestedDatePicker.getValue();
            LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.NOON); // Default to noon
            demande.setRequestedDate(Timestamp.valueOf(dateTime));
        }

        // Optional: Proposed price
        String priceText = proposedPriceField.getText().trim();
        if (!priceText.isEmpty()) {
            try {
                demande.setProposedPrice(new BigDecimal(priceText));
            } catch (NumberFormatException e) {
                System.err.println("Invalid price format: " + priceText);
            }
        }

        return demande;
    }

    /**
     * Navigate back to demandes list
     */
    private void goBackToDemandes() {
        try {
            System.out.println("Navigating back to demandes list...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Demande.fxml"));
            Scene scene = new Scene(loader.load());

            // Pass student ID to the demandes controller
            Demande_controller controller = loader.getController();
            if (currentStudentId > 0) {
                controller.setStudentId(currentStudentId);
            }

            Stage stage = (Stage) serviceCombo.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Mes Demandes");

        } catch (Exception e) {
            System.err.println("Error navigating back: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de revenir à la liste: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== SETTERS ====================

    /**
     * Set current student ID (called from previous controller)
     */
    public void setCurrentStudentId(int studentId) {
        this.currentStudentId = studentId;
        System.out.println("Current student ID set to: " + studentId);
    }

    /**
     * Pre-select a service (if coming from service details page)
     */
    public void preSelectService(int serviceId) {
        try {
            for (Services service : serviceCombo.getItems()) {
                if (service.getId() == serviceId) {
                    serviceCombo.setValue(service);
                    onServiceSelected();
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error pre-selecting service: " + e.getMessage());
        }
    }

    /**
     * Set service data and student ID (called from Student_controller)
     * This method is used when user clicks "Réserver" on a service card
     */
    public void setServiceData(Services service, int studentId) {
        System.out.println("Setting service data: " + service.getTitle() + " for student: " + studentId);

        // Set student ID
        this.currentStudentId = studentId;

        // Pre-select the service in combo box
        if (service != null) {
            // Add the service to combo if not already present
            boolean serviceExists = false;
            for (Services s : serviceCombo.getItems()) {
                if (s.getId() == service.getId()) {
                    serviceExists = true;
                    break;
                }
            }

            if (!serviceExists) {
                serviceCombo.getItems().add(service);
            }

            // Select the service
            serviceCombo.setValue(service);
            onServiceSelected();
        }
    }
}