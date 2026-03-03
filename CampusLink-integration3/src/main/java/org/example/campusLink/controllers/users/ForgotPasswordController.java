package org.example.campusLink.controllers.users;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.campusLink.entities.User;
import org.example.campusLink.services.users.UserService;
import org.example.campusLink.services.users.VerificationService;
import org.example.campusLink.utils.AlertHelper;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Controller pour la page "Mot de passe oublié"
 */
public class ForgotPasswordController {

    @FXML private StackPane rootPane;
    @FXML private TextField txtEmail;
    @FXML private Label lblEmailError;
    @FXML private Button btnSendCode;
    @FXML private Hyperlink linkLogin;

    private UserService userService;
    private VerificationService verificationService;

    public ForgotPasswordController() {
        System.out.println("✅ ForgotPasswordController créé");
    }

    @FXML
    public void initialize() {
        System.out.println("🔧 Initialisation ForgotPasswordController...");

        try {
            userService = new UserService();
            verificationService = new VerificationService();
            System.out.println("✅ Services initialisés");
        } catch (Exception e) {
            System.err.println("⚠️ Erreur Services: " + e.getMessage());
        }

        // Validation en temps réel
        txtEmail.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty()) validateEmail();
        });
    }

    @FXML
    private void handleSendCode() {
        // Clear errors
        hideError();

        // Validation
        if (!validateEmail()) {
            return;
        }

        btnSendCode.setDisable(true);
        btnSendCode.setText("Envoi en cours...");

        try {
            String email = txtEmail.getText().trim();

            // Vérifier si l'email existe
            User user = userService.getUserByEmail(email);

            if (user == null) {
                showError("Aucun compte associé à cet email");
                btnSendCode.setDisable(false);
                btnSendCode.setText("Envoyer le code");
                return;
            }

            // Envoyer le code par email
            boolean sent = verificationService.sendPasswordResetCode(email, user.getName());

            if (sent) {
                AlertHelper.showAlert(rootPane,
                        "Code envoyé! Vérifiez votre boîte mail.",
                        AlertHelper.AlertType.SUCCESS);

                // Attendre 1 seconde puis naviguer vers la page de vérification
                javafx.application.Platform.runLater(() -> {
                    try {
                        Thread.sleep(1500);
                        navigateToVerifyCode(email);
                    } catch (InterruptedException e) {
                        navigateToVerifyCode(email);
                    }
                });

            } else {
                showError("Erreur lors de l'envoi de l'email. Vérifiez votre connexion.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur de connexion à la base de données");
        } finally {
            btnSendCode.setDisable(false);
            btnSendCode.setText("Envoyer le code");
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/users/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnSendCode.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToVerifyCode(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/users/VerifyCode.fxml"));
            Parent root = loader.load();

            VerifyCodeController controller = loader.getController();
            controller.setEmail(email);
            controller.setVerificationType("PASSWORD_RESET");

            Stage stage = (Stage) btnSendCode.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement de la page");
        }
    }

    // ==================== VALIDATION ====================

    private boolean validateEmail() {
        String email = txtEmail.getText().trim();

        if (email.isEmpty()) {
            showFieldError("Email requis");
            return false;
        }

        if (!isValidEmail(email)) {
            showFieldError("Email invalide");
            return false;
        }

        hideError();
        return true;
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void showFieldError(String message) {
        txtEmail.getStyleClass().add("error");
        lblEmailError.setText(message);
        lblEmailError.setVisible(true);
        lblEmailError.setManaged(true);
    }

    private void hideError() {
        txtEmail.getStyleClass().remove("error");
        lblEmailError.setVisible(false);
        lblEmailError.setManaged(false);
    }

    private void showError(String message) {
        if (rootPane != null) {
            AlertHelper.showAlert(rootPane, message, AlertHelper.AlertType.ERROR);
        }
    }
}