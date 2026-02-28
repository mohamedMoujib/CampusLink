package org.example.campusLink.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.example.campusLink.entities.User;
import org.example.campusLink.Services.UserService;
import org.example.campusLink.utils.AlertHelper;
import org.example.campusLink.utils.PasswordUtil;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Controller pour réinitialiser le mot de passe
 */
public class ResetPasswordController {

    @FXML private StackPane rootPane;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label lblNewPasswordError;
    @FXML private Label lblConfirmPasswordError;
    @FXML private ProgressBar progressStrength;
    @FXML private Text txtStrength;
    @FXML private Button btnReset;
    @FXML private Hyperlink linkLogin;

    private String email;
    private UserService userService;

    public ResetPasswordController() {
        System.out.println("✅ ResetPasswordController créé");
    }

    @FXML
    public void initialize() {
        System.out.println("🔧 Initialisation ResetPasswordController...");

        try {
            userService = new UserService();
            System.out.println("✅ UserService initialisé");
        } catch (Exception e) {
            System.err.println("⚠️ Erreur UserService: " + e.getMessage());
        }

        // Password strength indicator
        txtNewPassword.textProperty().addListener((obs, old, newVal) -> {
            updatePasswordStrength(newVal);
            if (!newVal.isEmpty()) validateNewPassword();
        });

        txtConfirmPassword.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty()) validateConfirmPassword();
        });
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @FXML
    private void handleResetPassword() {
        // Clear errors
        hideAllErrors();

        // Validation
        if (!validateAllInputs()) {
            AlertHelper.showAlert(rootPane,
                    "Veuillez corriger les erreurs",
                    AlertHelper.AlertType.ERROR);
            return;
        }

        btnReset.setDisable(true);
        btnReset.setText("Réinitialisation...");

        try {
            String newPassword = txtNewPassword.getText();

            // Récupérer l'utilisateur
            User user = userService.getUserByEmail(email);

            if (user == null) {
                AlertHelper.showAlert(rootPane,
                        "Erreur: compte introuvable",
                        AlertHelper.AlertType.ERROR);
                return;
            }

            // Hash le nouveau mot de passe
            String hashedPassword = PasswordUtil.hashPassword(newPassword);
            user.setPassword(hashedPassword);

            // Sauvegarder
            userService.modifier(user);

            AlertHelper.showAlert(rootPane,
                    "Mot de passe réinitialisé avec succès!",
                    AlertHelper.AlertType.SUCCESS);

            // Rediriger vers login après 1.5 secondes
            javafx.application.Platform.runLater(() -> {
                try {
                    Thread.sleep(1500);
                    navigateToLogin();
                } catch (InterruptedException e) {
                    navigateToLogin();
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
            AlertHelper.showAlert(rootPane,
                    "Erreur lors de la réinitialisation",
                    AlertHelper.AlertType.ERROR);
        } finally {
            btnReset.setDisable(false);
            btnReset.setText("Réinitialiser le mot de passe");
        }
    }

    @FXML
    private void handleBackToLogin() {
        navigateToLogin();
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnReset.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== PASSWORD STRENGTH ====================

    private void updatePasswordStrength(String password) {
        int strength = calculatePasswordStrength(password);

        progressStrength.setProgress(strength / 100.0);

        if (strength < 30) {
            txtStrength.setText("Faible");
            txtStrength.setStyle("-fx-fill: #dc2626;");
            progressStrength.setStyle("-fx-accent: #dc2626;");
        } else if (strength < 60) {
            txtStrength.setText("Moyen");
            txtStrength.setStyle("-fx-fill: #f59e0b;");
            progressStrength.setStyle("-fx-accent: #f59e0b;");
        } else {
            txtStrength.setText("Fort");
            txtStrength.setStyle("-fx-fill: #15803d;");
            progressStrength.setStyle("-fx-accent: #15803d;");
        }
    }

    private int calculatePasswordStrength(String password) {
        int strength = 0;

        if (password.length() >= 6) strength += 20;
        if (password.length() >= 8) strength += 10;
        if (password.length() >= 12) strength += 10;

        if (password.matches(".*[a-z].*")) strength += 15;
        if (password.matches(".*[A-Z].*")) strength += 15;
        if (password.matches(".*[0-9].*")) strength += 15;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) strength += 15;

        return Math.min(strength, 100);
    }

    // ==================== VALIDATION ====================

    private boolean validateAllInputs() {
        boolean valid = true;

        if (txtNewPassword.getText().isEmpty()) {
            showFieldError(txtNewPassword, lblNewPasswordError, "Mot de passe requis");
            valid = false;
        } else if (txtNewPassword.getText().length() < 6) {
            showFieldError(txtNewPassword, lblNewPasswordError, "Minimum 6 caractères");
            valid = false;
        }

        if (txtConfirmPassword.getText().isEmpty()) {
            showFieldError(txtConfirmPassword, lblConfirmPasswordError, "Confirmation requise");
            valid = false;
        } else if (!txtNewPassword.getText().equals(txtConfirmPassword.getText())) {
            showFieldError(txtConfirmPassword, lblConfirmPasswordError, "Les mots de passe ne correspondent pas");
            valid = false;
        }

        return valid;
    }

    private boolean validateNewPassword() {
        if (txtNewPassword.getText().length() < 6) {
            showFieldError(txtNewPassword, lblNewPasswordError, "Minimum 6 caractères");
            return false;
        }
        hideFieldError(txtNewPassword, lblNewPasswordError);
        return true;
    }

    private boolean validateConfirmPassword() {
        if (!txtNewPassword.getText().equals(txtConfirmPassword.getText())) {
            showFieldError(txtConfirmPassword, lblConfirmPasswordError, "Ne correspond pas");
            return false;
        }
        hideFieldError(txtConfirmPassword, lblConfirmPasswordError);
        return true;
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

    private void hideAllErrors() {
        hideFieldError(txtNewPassword, lblNewPasswordError);
        hideFieldError(txtConfirmPassword, lblConfirmPasswordError);
    }
}