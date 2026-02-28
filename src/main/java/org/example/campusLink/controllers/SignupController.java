package org.example.campusLink.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.campusLink.entities.Etudiant;
import org.example.campusLink.entities.Prestataire;
import org.example.campusLink.entities.User;
import org.example.campusLink.Services.AuthService;
import org.example.campusLink.Services.UserService;
import org.example.campusLink.Services.VerificationService;
import org.example.campusLink.utils.AlertHelper;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

public class SignupController {

    @FXML private StackPane rootPane;
    @FXML private ToggleButton btnEtudiant;
    @FXML private ToggleButton btnTuteur;

    @FXML private TextField txtPrenom;
    @FXML private TextField txtNom;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtUniversite;
    @FXML private ComboBox<String> cmbGender;
    @FXML private TextField txtFiliere;
    @FXML private TextField txtSpecialization;
    @FXML private TextField txtAddress;

    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;

    @FXML private CheckBox chkTerms;
    @FXML private Button btnSignup;
    @FXML private Hyperlink linkLogin;

    // Error Labels
    @FXML private Label lblPrenomError;
    @FXML private Label lblNomError;
    @FXML private Label lblEmailError;
    @FXML private Label lblPhoneError;
    @FXML private Label lblUniversiteError;
    @FXML private Label lblGenderError;
    @FXML private Label lblFiliereError;
    @FXML private Label lblSpecializationError;
    @FXML private Label lblAddressError;
    @FXML private Label lblPasswordError;
    @FXML private Label lblConfirmPasswordError;

    private AuthService authService;
    private UserService userService;
    private VerificationService verificationService;
    private String selectedRole = "ETUDIANT";

    public SignupController() {
        System.out.println("✅ SignupController créé");
    }

    @FXML
    public void initialize() {
        System.out.println("🔧 Initialisation du SignupController...");

        try {
            authService = new AuthService();
            userService = new UserService();
            verificationService = new VerificationService();
            System.out.println("✅ Services initialisés");
        } catch (Exception e) {
            System.err.println("⚠️ Erreur Services: " + e.getMessage());
        }

        cmbGender.getItems().addAll("Male", "Female");

        btnEtudiant.setOnAction(e -> {
            selectedRole = "ETUDIANT";
            updateRoleButtons();
        });

        btnTuteur.setOnAction(e -> {
            selectedRole = "PRESTATAIRE";
            updateRoleButtons();
        });

        txtEmail.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty()) validateEmail();
        });
        txtPassword.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty()) validatePassword();
        });
        txtConfirmPassword.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty()) validateConfirmPassword();
        });
        txtPhone.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty()) validatePhone();
        });
    }

    private void updateRoleButtons() {
        btnEtudiant.getStyleClass().remove("role-btn-selected");
        btnTuteur.getStyleClass().remove("role-btn-selected");

        if (selectedRole.equals("ETUDIANT")) {
            if (!btnEtudiant.getStyleClass().contains("role-btn-selected")) {
                btnEtudiant.getStyleClass().add("role-btn-selected");
            }
        } else {
            if (!btnTuteur.getStyleClass().contains("role-btn-selected")) {
                btnTuteur.getStyleClass().add("role-btn-selected");
            }
        }
    }

    @FXML
    private void handleSignup() {
        clearAllErrors();

        if (!validateAllInputs()) {
            showError("Veuillez corriger les erreurs dans le formulaire");
            return;
        }

        if (authService == null) {
            showError("Service non disponible. Vérifiez la connexion à la base de données.");
            return;
        }

        btnSignup.setDisable(true);
        btnSignup.setText("Création en cours...");

        try {
            User user;
            String email = txtEmail.getText().trim();
            String userName = txtPrenom.getText().trim() + " " + txtNom.getText().trim();

            if (selectedRole.equals("ETUDIANT")) {
                Etudiant etu = new Etudiant();
                etu.setUniversite(txtUniversite.getText().trim());
                etu.setFiliere(txtFiliere.getText().trim());
                etu.setSpecialization(txtSpecialization.getText().trim());

                etu.setName(userName);
                etu.setEmail(email);
                etu.setPassword(txtPassword.getText());
                etu.setPhone(txtPhone.getText().trim());
                etu.setAddress(txtAddress.getText().trim());
                etu.setGender(cmbGender.getValue());
                etu.setProfilePicture("");
                etu.setDateNaissance(LocalDate.now());

                // Créer le compte (status = INACTIVE)
                user = authService.signupEtudiant(etu);

            } else { // PRESTATAIRE
                Prestataire prest = new Prestataire();
                prest.setTrustPoints(0);

                prest.setName(userName);
                prest.setEmail(email);
                prest.setPassword(txtPassword.getText());
                prest.setPhone(txtPhone.getText().trim());
                prest.setAddress(txtAddress.getText().trim());
                prest.setGender(cmbGender.getValue());
                prest.setUniversite(txtUniversite.getText().trim());
                prest.setFiliere(txtFiliere.getText().trim());
                prest.setSpecialization(txtSpecialization.getText().trim());
                prest.setProfilePicture("");
                prest.setDateNaissance(LocalDate.now());

                // Créer le compte (status = INACTIVE)
                user = authService.signupPrestataire(prest);
            }

            System.out.println("✅ Compte créé: " + email + " (INACTIVE)");

            // Envoyer le code de vérification par email
            btnSignup.setText("Envoi du code...");
            boolean emailSent = verificationService.sendAccountVerificationCode(email, userName);

            if (emailSent) {
                showSuccess("Compte créé! Vérifiez votre email.");

                // Rediriger vers la page de vérification après 1.5 secondes
                javafx.application.Platform.runLater(() -> {
                    try {
                        Thread.sleep(1500);
                        navigateToVerifyCode(email);
                    } catch (InterruptedException e) {
                        navigateToVerifyCode(email);
                    }
                });

            } else {
                showError("Erreur lors de l'envoi de l'email. Vérifiez votre configuration.");

                // Rediriger vers login après 2 secondes
                javafx.application.Platform.runLater(() -> {
                    try {
                        Thread.sleep(2000);
                        handleLogin();
                    } catch (InterruptedException e) {
                        handleLogin();
                    }
                });
            }

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry") || e.getMessage().contains("email existe")) {
                showFieldError(txtEmail, lblEmailError, "Cet email est déjà utilisé");
                showError("Cet email est déjà utilisé");
            } else {
                showError("Erreur lors de la création du compte");
            }
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } finally {
            btnSignup.setDisable(false);
            btnSignup.setText("Créer mon compte");
        }
    }

    /**
     * Naviguer vers la page de vérification du code
     */
    private void navigateToVerifyCode(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/VerifyCode.fxml"));
            Parent root = loader.load();

            VerifyCodeController controller = loader.getController();
            controller.setEmail(email);
            controller.setVerificationType("ACCOUNT_VERIFICATION");

            Stage stage = (Stage) btnSignup.getScene().getWindow();
            stage.setScene(new Scene(root));

            System.out.println("✅ Navigation vers VerifyCode pour activation de compte");

        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement de la page");
        }
    }

    @FXML
    private void handleLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnSignup.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private boolean validateAllInputs() {
        boolean valid = true;

        if (txtPrenom.getText().trim().isEmpty()) {
            showFieldError(txtPrenom, lblPrenomError, "Prénom requis");
            valid = false;
        } else if (txtPrenom.getText().trim().length() < 2) {
            showFieldError(txtPrenom, lblPrenomError, "Prénom trop court");
            valid = false;
        }

        if (txtNom.getText().trim().isEmpty()) {
            showFieldError(txtNom, lblNomError, "Nom requis");
            valid = false;
        } else if (txtNom.getText().trim().length() < 2) {
            showFieldError(txtNom, lblNomError, "Nom trop court");
            valid = false;
        }

        if (txtEmail.getText().trim().isEmpty()) {
            showFieldError(txtEmail, lblEmailError, "Email requis");
            valid = false;
        } else if (!isValidEmail(txtEmail.getText())) {
            showFieldError(txtEmail, lblEmailError, "Email invalide");
            valid = false;
        } else {
            try {
                if (userService != null && userService.getUserByEmail(txtEmail.getText().trim()) != null) {
                    showFieldError(txtEmail, lblEmailError, "Cet email existe déjà");
                    valid = false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (txtPhone.getText().trim().isEmpty()) {
            showFieldError(txtPhone, lblPhoneError, "Téléphone requis");
            valid = false;
        } else if (!isValidPhone(txtPhone.getText())) {
            showFieldError(txtPhone, lblPhoneError, "Format invalide");
            valid = false;
        }

        if (cmbGender.getValue() == null) {
            showFieldError(cmbGender, lblGenderError, "Genre requis");
            valid = false;
        }

        if (txtAddress.getText().trim().isEmpty()) {
            showFieldError(txtAddress, lblAddressError, "Adresse requise");
            valid = false;
        }

        if (txtUniversite.getText().trim().isEmpty()) {
            showFieldError(txtUniversite, lblUniversiteError, "Université requise");
            valid = false;
        }

        if (txtFiliere.getText().trim().isEmpty()) {
            showFieldError(txtFiliere, lblFiliereError, "Filière requise");
            valid = false;
        }

        if (txtSpecialization.getText().trim().isEmpty()) {
            showFieldError(txtSpecialization, lblSpecializationError, "Spécialisation requise");
            valid = false;
        }

        if (txtPassword.getText().isEmpty()) {
            showFieldError(txtPassword, lblPasswordError, "Mot de passe requis");
            valid = false;
        } else if (txtPassword.getText().length() < 6) {
            showFieldError(txtPassword, lblPasswordError, "Minimum 6 caractères");
            valid = false;
        }

        if (txtConfirmPassword.getText().isEmpty()) {
            showFieldError(txtConfirmPassword, lblConfirmPasswordError, "Confirmation requise");
            valid = false;
        } else if (!txtPassword.getText().equals(txtConfirmPassword.getText())) {
            showFieldError(txtConfirmPassword, lblConfirmPasswordError, "Ne correspond pas");
            valid = false;
        }

        if (!chkTerms.isSelected()) {
            showWarning("Vous devez accepter les conditions");
            valid = false;
        }

        return valid;
    }

    private boolean validateEmail() {
        if (!isValidEmail(txtEmail.getText())) {
            showFieldError(txtEmail, lblEmailError, "Email invalide");
            return false;
        }
        hideFieldError(txtEmail, lblEmailError);
        return true;
    }

    private boolean validatePassword() {
        if (txtPassword.getText().length() < 6) {
            showFieldError(txtPassword, lblPasswordError, "Minimum 6 caractères");
            return false;
        }
        hideFieldError(txtPassword, lblPasswordError);
        return true;
    }

    private boolean validateConfirmPassword() {
        if (!txtPassword.getText().equals(txtConfirmPassword.getText())) {
            showFieldError(txtConfirmPassword, lblConfirmPasswordError, "Ne correspond pas");
            return false;
        }
        hideFieldError(txtConfirmPassword, lblConfirmPasswordError);
        return true;
    }

    private boolean validatePhone() {
        if (!isValidPhone(txtPhone.getText())) {
            showFieldError(txtPhone, lblPhoneError, "Format invalide");
            return false;
        }
        hideFieldError(txtPhone, lblPhoneError);
        return true;
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isValidPhone(String phone) {
        String cleanPhone = phone.replaceAll("\\s+", "");
        return cleanPhone.matches("^\\+?[0-9]{8,15}$");
    }

    private void showFieldError(Control field, Label errorLabel, String message) {
        if (!field.getStyleClass().contains("error")) {
            field.getStyleClass().add("error");
        }
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideFieldError(Control field, Label errorLabel) {
        field.getStyleClass().remove("error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void clearAllErrors() {
        hideFieldError(txtPrenom, lblPrenomError);
        hideFieldError(txtNom, lblNomError);
        hideFieldError(txtEmail, lblEmailError);
        hideFieldError(txtPhone, lblPhoneError);
        hideFieldError(cmbGender, lblGenderError);
        hideFieldError(txtAddress, lblAddressError);
        hideFieldError(txtUniversite, lblUniversiteError);
        hideFieldError(txtFiliere, lblFiliereError);
        hideFieldError(txtSpecialization, lblSpecializationError);
        hideFieldError(txtPassword, lblPasswordError);
        hideFieldError(txtConfirmPassword, lblConfirmPasswordError);
    }

    // ==================== CUSTOM ALERTS ====================

    private void showSuccess(String message) {
        if (rootPane != null) {
            AlertHelper.showAlert(rootPane, message, AlertHelper.AlertType.SUCCESS);
        }
    }

    private void showError(String message) {
        if (rootPane != null) {
            AlertHelper.showAlert(rootPane, message, AlertHelper.AlertType.ERROR);
        }
    }

    private void showWarning(String message) {
        if (rootPane != null) {
            AlertHelper.showAlert(rootPane, message, AlertHelper.AlertType.WARNING);
        }
    }
}