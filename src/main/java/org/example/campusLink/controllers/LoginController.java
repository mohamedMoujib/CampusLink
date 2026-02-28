package org.example.campusLink.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.campusLink.entities.*;
import org.example.campusLink.Services.AuthService;
import org.example.campusLink.Services.GoogleAuthServices;
import org.example.campusLink.Services.UserService;
import org.example.campusLink.utils.AlertHelper;
import org.example.campusLink.utils.PasswordUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

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
    @FXML private Button btnGoogleLogin;

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
            showError("Veuillez corriger les erreurs dans le formulaire");
            return;
        }

        if (authService == null) {
            AlertHelper.showAlert(rootPane, "Service non disponible. Vérifiez la connexion à la base de données.", AlertHelper.AlertType.ERROR);
            return;
        }

        btnLogin.setDisable(true);
        btnLogin.setText("Connexion...");

        try {
            String email = txtEmail.getText().trim();
            String password = txtPassword.getText();

            // ✅ Pass the selectedRole to login
            User user = authService.login(email, password, selectedRole);

            // Success message
            AlertHelper.showAlert(rootPane, "Bienvenue " + user.getName() + "!", AlertHelper.AlertType.SUCCESS);

            // Navigate to profile
            Platform.runLater(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // ✅ Utilisation de instanceof pour la redirection
                if (user instanceof Admin) {
                    navigateToAdminDashboard((Admin) user);
                } else if (user instanceof Etudiant) {
                    navigateToProfile((Etudiant) user);
                } else if (user instanceof Prestataire) {
                    navigateToProfile((Prestataire) user);
                }
            });
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();

            if (msg.contains("not found") || msg.contains("Incorrect password")) {
                // Highlight the fields in red
                showFieldError(txtEmail, lblEmailError, "Email ou mot de passe incorrect");
                showFieldError(txtPassword, lblPasswordError, "Email ou mot de passe incorrect");

                // Cute alert popup
                AlertHelper.showAlert(rootPane, "Email ou mot de passe incorrect", AlertHelper.AlertType.ERROR);

            } else if (msg.contains("does not have role")) {
                AlertHelper.showAlert(rootPane, "Vous n'avez pas accès avec le rôle " + selectedRole, AlertHelper.AlertType.ERROR);
            } else {
                AlertHelper.showAlert(rootPane, msg, AlertHelper.AlertType.ERROR);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            AlertHelper.showAlert(rootPane, "Erreur de connexion à la base de données", AlertHelper.AlertType.ERROR);
        } finally {
            btnLogin.setDisable(false);
            btnLogin.setText("Se connecter");
        }
    }
    private void navigateToAdminDashboard(Admin admin) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/AdminDashboard.fxml"));
            Parent root = loader.load();

            AdminDashboardController controller = loader.getController();
            controller.setAdmin(admin);

            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.setWidth(1400);
            stage.setHeight(900);
            stage.centerOnScreen();
            stage.setScene(new Scene(root));

            System.out.println("✅ Navigation vers Admin Dashboard");

        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement du dashboard admin");
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

    private void navigateToProfile(User user) {
        try {
            System.out.println("🔍 Navigation vers ProfileView...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/ProfileView.fxml"));
            Parent root = loader.load();

            // Récupérer le controller et passer l'utilisateur
            ProfileController controller = loader.getController();
            controller.setUser(user);

            Stage stage = (Stage) btnLogin.getScene().getWindow();
            Scene scene = new Scene(root);

            // Agrandir la fenêtre
            stage.setWidth(1200);
            stage.setHeight(800);
            stage.centerOnScreen();

            stage.setScene(scene);

            System.out.println("✅ Navigation vers ProfileView réussie!");

        } catch (IOException e) {
            System.err.println("❌ Erreur lors de la navigation:");
            e.printStackTrace();
            showError("Impossible de charger l'interface de profil.");
        }
    }

    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/ForgotPassword.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement de la page");
        }
    }


    @FXML
    private void handleGoogleSignup() {
        System.out.println("🔐 Début authentification Google...");

        btnGoogleLogin.setDisable(true);
        btnGoogleLogin.setText("Connexion en cours...");

        // Lancer dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            final String role = selectedRole; // capture before thread starts

            try {
                // 1. Authentifier avec Google
                GoogleAuthServices googleAuth = new GoogleAuthServices();
                GoogleUser googleUser = googleAuth.authenticate();

                if (googleUser == null) {
                    Platform.runLater(() -> {
                        showError("Échec de l'authentification Google");
                        resetGoogleButton();
                    });
                    return;
                }

                System.out.println("✅ Google Auth réussie: " + googleUser.getEmail());

                // 2. Vérifier si l'utilisateur existe déjà
                UserService userService = new UserService();
                User existingUser = userService.findByEmail(googleUser.getEmail());

                if (existingUser != null) {
                    // Utilisateur existe → Login
                    System.out.println("✅ Utilisateur existant trouvé");

                    Platform.runLater(() -> {
                        showSuccess("Connexion réussie!");
                        handleLogin();
                        navigateToProfile(existingUser);
                    });

                } else {
                    // Nouvel utilisateur → Créer le compte
                    System.out.println("📝 Création nouveau compte Google");

                    User createdUser;

                    if ("PRESTATAIRE".equals(selectedRole)) {
                        Prestataire newUser = new Prestataire();
                        newUser.setName(googleUser.getName());
                        newUser.setEmail(googleUser.getEmail());
                        newUser.setPassword(PasswordUtil.hashPassword(UUID.randomUUID().toString()));
                        newUser.setStatus("ACTIVE");
                        newUser.setPhone("");
                        newUser.setAddress("");
                        newUser.setGender("Male");
                        newUser.setProfilePicture(googleUser.getPictureUrl() != null ? googleUser.getPictureUrl() : "");
                        newUser.setUniversite("");
                        newUser.setFiliere("");
                        newUser.setSpecialization("");
                        newUser.setTrustPoints(0);
                        createdUser = authService.signupPrestataire(newUser);
                    } else {
                        // Default → ETUDIANT
                        Etudiant newUser = new Etudiant();
                        newUser.setName(googleUser.getName());
                        newUser.setEmail(googleUser.getEmail());
                        newUser.setPassword(PasswordUtil.hashPassword(UUID.randomUUID().toString()));
                        newUser.setStatus("ACTIVE");
                        newUser.setPhone("");
                        newUser.setAddress("");
                        newUser.setGender("Male");
                        newUser.setProfilePicture(googleUser.getPictureUrl() != null ? googleUser.getPictureUrl() : "");
                        newUser.setDateNaissance(LocalDate.now());
                        newUser.setUniversite("");
                        newUser.setFiliere("");
                        newUser.setSpecialization("");
                        createdUser = authService.signupEtudiant(newUser);
                    }

                    final User finalUser = createdUser;
                    System.out.println("✅ Nouveau compte créé (" + selectedRole + ")");

                    Platform.runLater(() -> {
                        showSuccess("Compte créé avec succès! Bienvenue " + googleUser.getName());
                        navigateToProfile(finalUser);
                    });
                }

            } catch (SQLException e) {
                System.err.println("❌ Erreur BD: " + e.getMessage());
                e.printStackTrace();

                Platform.runLater(() -> {
                    if (e.getMessage().contains("Duplicate entry")) {
                        showError("Cet email est déjà utilisé");
                    } else {
                        showError("Erreur de base de données");
                    }
                    resetGoogleButton();
                });

            } catch (Exception e) {
                System.err.println("❌ Erreur Google Auth: " + e.getMessage());
                e.printStackTrace();

                Platform.runLater(() -> {
                    showError("Erreur lors de l'authentification Google.\nVeuillez réessayer.");
                    resetGoogleButton();
                });
            }
        }).start();
    }

    /**
     * Réinitialiser le bouton Google
     */
    private void resetGoogleButton() {
        btnGoogleLogin.setDisable(false);
        btnGoogleLogin.setText("Se connecter avec Google");
    }
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
    private void fillCommonFields(User user, GoogleUser googleUser) {

        user.setName(googleUser.getName());
        user.setEmail(googleUser.getEmail());
        user.setProfilePicture(googleUser.getPictureUrl());

        // Default values to avoid validation errors
        user.setPhone("00000000");
        user.setGender("HOMME");
        user.setAddress("Non spécifié");

        user.setPassword("GoogleAuth123!");
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