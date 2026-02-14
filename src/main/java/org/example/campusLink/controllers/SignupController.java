package org.example.campusLink.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.campusLink.entities.User;
import org.example.campusLink.services.AuthService;
import org.example.campusLink.services.UserService;
import org.example.campusLink.utils.PasswordUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;

public class SignupController {

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

    private AuthService authService;
    private UserService userService;
    private String selectedRole = "ETUDIANT";

    public SignupController() {
        authService = new AuthService();
        userService = new UserService();
    }

    @FXML
    public void initialize() {
        cmbGender.getItems().addAll("Male", "Female");
        btnEtudiant.setOnAction(e -> {
            selectedRole = "ETUDIANT";
            updateRoleButtons();
        });

        btnTuteur.setOnAction(e -> {
            selectedRole = "PRESTATAIRE";
            updateRoleButtons();
        });

        // Add validation listeners
        txtEmail.textProperty().addListener((obs, old, newVal) -> validateEmail());
        txtPassword.textProperty().addListener((obs, old, newVal) -> validatePassword());
        txtConfirmPassword.textProperty().addListener((obs, old, newVal) -> validateConfirmPassword());
        txtPhone.textProperty().addListener((obs, old, newVal) -> validatePhone());
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
        // Clear previous errors
        clearErrors();

        // Validate all inputs
        if (!validateAllInputs()) {
            return;
        }

        // Disable button during signup
        btnSignup.setDisable(true);
        btnSignup.setText("Création en cours...");

        try {
            // Create new user
            User user = new User();
            user.setName(txtPrenom.getText().trim() + " " + txtNom.getText().trim());
            user.setEmail(txtEmail.getText().trim());

            // Hash password using PasswordUtil
            String hashedPassword = (txtPassword.getText());
            user.setPassword(hashedPassword);

            user.setPhone(txtPhone.getText().trim());
            user.setUniversite(txtUniversite.getText().trim());
            user.setDateNaissance(new Timestamp(System.currentTimeMillis()));
            user.setGender(cmbGender.getValue());
            user.setFiliere(txtFiliere.getText().trim());
            user.setSpecialization(txtSpecialization.getText().trim());
            user.setAddress(txtAddress.getText().trim());

            user.setProfilePicture("");


            // Use AuthService to signup (creates user and assigns role in transaction)
            authService.signUp(user, selectedRole);

            // Show success message
            showSuccess("Compte créé avec succès!\n\nVous pouvez maintenant vous connecter avec:\n" +
                    "Email: " + user.getEmail() + "\n" +
                    "Rôle: " + selectedRole);

            // Navigate to login
            handleLogin();

        } catch (SQLException e) {
            String errorMessage = e.getMessage();

            // User-friendly error messages
            if (errorMessage.contains("Duplicate entry") || errorMessage.contains("email existe")) {
                showError("Cet email est déjà utilisé!");
            } else if (errorMessage.contains("validation")) {
                showError(errorMessage);
            } else {
                showError("Erreur lors de la création du compte.\n\n" + errorMessage);
            }

            e.printStackTrace();
        } finally {
            // Re-enable button
            btnSignup.setDisable(false);
            btnSignup.setText("Créer mon compte");
        }
    }

    @FXML
    private void handleLogin() {
        try {
            // Load login view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnSignup.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);

        } catch (IOException e) {
            showError("Erreur lors du chargement de la page de connexion");
            e.printStackTrace();
        }
    }

    private boolean validateAllInputs() {
        boolean valid = true;

        // Validate prénom
        if (txtPrenom.getText().trim().isEmpty()) {
            showFieldError(txtPrenom, "Prénom requis");
            valid = false;
        } else if (txtPrenom.getText().trim().length() < 2) {
            showFieldError(txtPrenom, "Prénom trop court");
            valid = false;
        }

        // Validate nom
        if (txtNom.getText().trim().isEmpty()) {
            showFieldError(txtNom, "Nom requis");
            valid = false;
        } else if (txtNom.getText().trim().length() < 2) {
            showFieldError(txtNom, "Nom trop court");
            valid = false;
        }

        // Validate email
        if (txtEmail.getText().trim().isEmpty()) {
            showFieldError(txtEmail, "Email requis");
            valid = false;
        } else if (!isValidEmail(txtEmail.getText())) {
            showFieldError(txtEmail, "Email invalide");
            valid = false;
        } else {
            try {
                if (userService.emailExists(txtEmail.getText().trim())) {
                    showFieldError(txtEmail, "Cet email existe déjà");
                    showError("Cet email est déjà utilisé!");
                    valid = false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Validate phone
        if (txtPhone.getText().trim().isEmpty()) {
            showFieldError(txtPhone, "Téléphone requis");
            valid = false;
        } else if (!isValidPhone(txtPhone.getText())) {
            showFieldError(txtPhone, "Numéro invalide (ex: +216 XX XXX XXX)");
            valid = false;
        }

        // Validate université
        if (txtUniversite.getText().trim().isEmpty()) {
            showFieldError(txtUniversite, "Université requise");
            valid = false;
        }
// Validate gender
        if (cmbGender.getValue() == null) {
            showError("Veuillez sélectionner un genre");
            valid = false;
        }

// Validate filiere
        if (txtFiliere.getText().trim().isEmpty()) {
            showFieldError(txtFiliere, "Filière requise");
            valid = false;
        }

// Validate specialization
        if (txtSpecialization.getText().trim().isEmpty()) {
            showFieldError(txtSpecialization, "Spécialisation requise");
            valid = false;
        }

// Validate address
        if (txtAddress.getText().trim().isEmpty()) {
            showFieldError(txtAddress, "Adresse requise");
            valid = false;
        }

        // Validate password
        if (txtPassword.getText().isEmpty()) {
            showFieldError(txtPassword, "Mot de passe requis");
            valid = false;
        } else if (txtPassword.getText().length() < 6) {
            showFieldError(txtPassword, "Minimum 6 caractères");
            valid = false;
        }

        // Validate confirm password
        if (txtConfirmPassword.getText().isEmpty()) {
            showFieldError(txtConfirmPassword, "Confirmation requise");
            valid = false;
        } else if (!txtPassword.getText().equals(txtConfirmPassword.getText())) {
            showFieldError(txtConfirmPassword, "Les mots de passe ne correspondent pas");
            showError("Les mots de passe ne correspondent pas!");
            valid = false;
        }

        // Validate terms acceptance
        if (!chkTerms.isSelected()) {
            showError("Vous devez accepter les conditions d'utilisation");
            valid = false;
        }

        return valid;
    }

    private boolean validateEmail() {
        if (!txtEmail.getText().isEmpty() && !isValidEmail(txtEmail.getText())) {
            txtEmail.getParent().getStyleClass().add("error");
            return false;
        }
        txtEmail.getParent().getStyleClass().remove("error");
        return true;
    }

    private boolean validatePassword() {
        if (!txtPassword.getText().isEmpty() && txtPassword.getText().length() < 6) {
            txtPassword.getParent().getStyleClass().add("error");
            return false;
        }
        txtPassword.getParent().getStyleClass().remove("error");
        return true;
    }

    private boolean validateConfirmPassword() {
        if (!txtConfirmPassword.getText().isEmpty() &&
                !txtPassword.getText().equals(txtConfirmPassword.getText())) {
            txtConfirmPassword.getParent().getStyleClass().add("error");
            return false;
        }
        txtConfirmPassword.getParent().getStyleClass().remove("error");
        return true;
    }

    private boolean validatePhone() {
        if (!txtPhone.getText().isEmpty() && !isValidPhone(txtPhone.getText())) {
            txtPhone.getParent().getStyleClass().add("error");
            return false;
        }
        txtPhone.getParent().getStyleClass().remove("error");
        return true;
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private boolean isValidPhone(String phone) {
        String cleanPhone = phone.replaceAll("\\s+", "");
        return cleanPhone.matches("^\\+?[0-9]{8,15}$");
    }

    private void showFieldError(Control field, String message) {
        field.getParent().getStyleClass().add("error");
        // TODO: Show tooltip or label with error message
    }

    private void clearErrors() {
        txtPrenom.getParent().getStyleClass().remove("error");
        txtNom.getParent().getStyleClass().remove("error");
        txtEmail.getParent().getStyleClass().remove("error");
        txtPhone.getParent().getStyleClass().remove("error");
        txtUniversite.getStyleClass().remove("error");
        txtPassword.getParent().getStyleClass().remove("error");
        txtConfirmPassword.getParent().getStyleClass().remove("error");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText("✅ Compte créé!");
        alert.setContentText(message);
        alert.showAndWait();
    }
}