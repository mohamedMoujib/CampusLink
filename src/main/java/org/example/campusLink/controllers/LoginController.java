package org.example.campusLink.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.campusLink.entities.User;
import org.example.campusLink.services.AuthService;
import org.example.campusLink.utils.AlertHelper;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML private StackPane rootPane;
    @FXML private ToggleButton btnEtudiant;
    @FXML private ToggleButton btnPrestataire;
    @FXML private ToggleButton btnAdmin;

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private CheckBox chkRememberMe;

    @FXML private Hyperlink linkForgotPassword;
    @FXML private Hyperlink linkSignup;
    @FXML private Button btnLogin;

    // Error Labels
    @FXML private Label lblEmailError;
    @FXML private Label lblPasswordError;

    private AuthService authService;
    private String selectedRole = "ETUDIANT";

    public LoginController() {
        System.out.println("✅ LoginController créé");
    }

    @FXML
    public void initialize() {
        System.out.println("🔧 Initialisation du LoginController...");

        try {
            authService = new AuthService();
            System.out.println("✅ AuthService initialisé");
        } catch (Exception e) {
            System.err.println("⚠️ Erreur AuthService: " + e.getMessage());
        }

        btnEtudiant.setOnAction(e -> {
            selectedRole = "ETUDIANT";
            updateRoleButtons();
        });

        btnPrestataire.setOnAction(e -> {
            selectedRole = "PRESTATAIRE";
            updateRoleButtons();
        });

        btnAdmin.setOnAction(e -> {
            selectedRole = "ADMIN";
            updateRoleButtons();
        });

        txtEmail.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty()) validateEmail();
        });

        txtPassword.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty()) validatePassword();
        });

        System.out.println("✅ LoginController initialisé");
    }

    private void updateRoleButtons() {
        btnEtudiant.getStyleClass().remove("role-btn-selected");
        btnPrestataire.getStyleClass().remove("role-btn-selected");
        btnAdmin.getStyleClass().remove("role-btn-selected");

        switch (selectedRole) {
            case "ETUDIANT":
                if (!btnEtudiant.getStyleClass().contains("role-btn-selected")) {
                    btnEtudiant.getStyleClass().add("role-btn-selected");
                }
                break;
            case "PRESTATAIRE":
                if (!btnPrestataire.getStyleClass().contains("role-btn-selected")) {
                    btnPrestataire.getStyleClass().add("role-btn-selected");
                }
                break;
            case "ADMIN":
                if (!btnAdmin.getStyleClass().contains("role-btn-selected")) {
                    btnAdmin.getStyleClass().add("role-btn-selected");
                }
                break;
        }
    }

    @FXML
    private void handleLogin() {
        clearAllErrors();

        if (!validateAllInputs()) {
            return;
        }

        if (authService == null) {
            showError("Service non disponible. Vérifiez la connexion à la base de données.");
            return;
        }

        btnLogin.setDisable(true);
        btnLogin.setText("Connexion...");

        try {
            String email = txtEmail.getText().trim();
            String password = txtPassword.getText();

            User user = authService.login(email, password, selectedRole);

            System.out.println("✅ Connexion réussie: " + user.getName());
            showSuccess("Bienvenue " + user.getName() + "!");

            // ✅ NAVIGATION VERS L'INTERFACE DES AVIS
            // Attendre un peu pour que l'utilisateur voie le message de succès
            Platform.runLater(() -> {
                try {
                    Thread.sleep(1000); // 1 seconde
                    navigateToDashboard(user, selectedRole);
                } catch (InterruptedException e) {
                    navigateToDashboard(user, selectedRole);
                }
            });

        } catch (SQLException e) {
            String errorMessage = e.getMessage();

            if (errorMessage.contains("User not found") || errorMessage.contains("Invalid password")) {
                showFieldError(txtEmail, lblEmailError, "Email ou mot de passe incorrect");
                showFieldError(txtPassword, lblPasswordError, "Email ou mot de passe incorrect");
                showError("Email ou mot de passe incorrect");
            } else if (errorMessage.contains("does not have role")) {
                showError("Vous n'avez pas accès avec le rôle " + selectedRole);
            } else {
                showError("Erreur de connexion");
            }

            e.printStackTrace();
        } finally {
            btnLogin.setDisable(false);
            btnLogin.setText("Se connecter");
        }
    }

    @FXML
    private void handleSignup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Signup.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== NAVIGATION ====================

    private void navigateToDashboard(User user, String role) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TutorReviews.fxml"));
            Parent root = loader.load();

            ReviewController controller = loader.getController();
            // TODO: Ajouter une méthode setUser() dans ReviewController si nécessaire
            // controller.setUser(user);

            Stage stage = (Stage) btnLogin.getScene().getWindow();
            Scene scene = new Scene(root);

            // Agrandir la fenêtre pour le dashboard
            stage.setWidth(1200);
            stage.setHeight(800);
            stage.centerOnScreen();

            stage.setScene(scene);

            System.out.println("✅ Navigation vers ReviewView réussie!");

        } catch (IOException e) {
            System.err.println("❌ Erreur lors de la navigation vers ReviewView:");
            e.printStackTrace();
            showError("Impossible de charger l'interface. Vérifiez que ReviewView.fxml existe.");
        }
    }

    // ==================== VALIDATION ====================

    private boolean validateAllInputs() {
        boolean valid = true;

        if (txtEmail.getText().trim().isEmpty()) {
            showFieldError(txtEmail, lblEmailError, "Email requis");
            valid = false;
        } else if (!isValidEmail(txtEmail.getText())) {
            showFieldError(txtEmail, lblEmailError, "Email invalide");
            valid = false;
        }

        if (txtPassword.getText().isEmpty()) {
            showFieldError(txtPassword, lblPasswordError, "Mot de passe requis");
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

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
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
        hideFieldError(txtEmail, lblEmailError);
        hideFieldError(txtPassword, lblPasswordError);
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

    private void showInfo(String message) {
        if (rootPane != null) {
            AlertHelper.showAlert(rootPane, message, AlertHelper.AlertType.INFO);
        }
    }
}