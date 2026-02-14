package org.example.campusLink.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.campusLink.entities.User;
import org.example.campusLink.services.AuthService;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML private ToggleButton btnEtudiant;
    @FXML private ToggleButton btnPrestataire;
    @FXML private ToggleButton btnAdmin;

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private CheckBox chkRememberMe;

    @FXML private Hyperlink linkForgotPassword;
    @FXML private Hyperlink linkSignup;
    @FXML private Button btnLogin;

    private AuthService authService;
    private String selectedRole = "ETUDIANT";

    // ✅ CORRECTION: Constructeur vide, pas d'initialisation ici!
    public LoginController() {
        System.out.println("✅ LoginController créé");
    }

    @FXML
    public void initialize() {
        System.out.println("🔧 Initialisation du LoginController...");

        // ✅ CORRECTION: Initialiser AuthService ICI, avec gestion d'erreur
        try {
            authService = new AuthService();
            System.out.println("✅ AuthService initialisé");
        } catch (Exception e) {
            System.err.println("⚠️ Erreur AuthService (l'interface fonctionnera quand même):");
            System.err.println("   " + e.getMessage());
            // L'interface se chargera quand même, le login échouera si utilisé
        }

        // Configure role selection
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

        // Add input validation listeners
        txtEmail.textProperty().addListener((obs, old, newVal) -> validateEmail());
        txtPassword.textProperty().addListener((obs, old, newVal) -> validatePassword());

        System.out.println("✅ LoginController initialisé complètement");
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
        // Clear previous errors
        clearErrors();

        // Vérifier que AuthService est disponible
        if (authService == null) {
            showError("Service d'authentification non disponible.\nVérifiez la connexion à la base de données.");
            return;
        }

        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        // Disable button during login
        btnLogin.setDisable(true);
        btnLogin.setText("Connexion...");

        try {
            String email = txtEmail.getText().trim();
            String password = txtPassword.getText();

            // Use AuthService to login
            User user = authService.login(email, password, selectedRole);

            // Check if account is active
           /* if (!user.getStatus().equals("ACTIVE")) {
                showError("Votre compte est " + user.getStatus().toLowerCase() + "!");
                return;
            }*/

            // Login successful
            System.out.println("✅ Connexion réussie: " + user.getName());

            // Navigate to dashboard based on role
            navigateToDashboard(user, selectedRole);

        } catch (SQLException e) {
            String errorMessage = e.getMessage();

            // User-friendly error messages
            if (errorMessage.contains("User not found")) {
                showError("Email ou mot de passe incorrect!");
            } else if (errorMessage.contains("Invalid password")) {
                showError("Email ou mot de passe incorrect!");
            } else if (errorMessage.contains("does not have role")) {
                showError("Vous n'avez pas accès avec le rôle " + selectedRole + "!");
            } else {
                showError("Erreur de connexion: " + errorMessage);
            }

            e.printStackTrace();
        } finally {
            // Re-enable button
            btnLogin.setDisable(false);
            btnLogin.setText("Se connecter");
        }
    }

    @FXML
    private void handleSignup() {
        try {
            // Load signup view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Signup.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnLogin.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);

        } catch (IOException e) {
            showError("Erreur lors du chargement de la page d'inscription");
            e.printStackTrace();
        }
    }

    private boolean validateInputs() {
        boolean valid = true;

        if (txtEmail.getText().trim().isEmpty()) {
            showFieldError(txtEmail, "Email requis");
            valid = false;
        } else if (!isValidEmail(txtEmail.getText())) {
            showFieldError(txtEmail, "Email invalide");
            valid = false;
        }

        if (txtPassword.getText().isEmpty()) {
            showFieldError(txtPassword, "Mot de passe requis");
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
        if (!txtPassword.getText().isEmpty() && txtPassword.getText().length() < 8) {
            txtPassword.getParent().getStyleClass().add("error");
            return false;
        }
        txtPassword.getParent().getStyleClass().remove("error");
        return true;
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void showFieldError(Control field, String message) {
        field.getParent().getStyleClass().add("error");
    }

    private void clearErrors() {
        txtEmail.getParent().getStyleClass().remove("error");
        txtPassword.getParent().getStyleClass().remove("error");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur de connexion");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void navigateToDashboard(User user, String role) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Connexion réussie");
        alert.setHeaderText("Bienvenue " + user.getName() + "!");
        alert.setContentText("Rôle: " + role + "\n\nRedirection vers le tableau de bord...");
        alert.showAndWait();
    }
}