package org.example.campusLink.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.campusLink.entities.User;
import org.example.campusLink.services.UserService;
import org.example.campusLink.services.VerificationService;
import org.example.campusLink.utils.AlertHelper;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Controller pour la vérification du code
 */
public class VerifyCodeController {

    @FXML private StackPane rootPane;
    @FXML private Text txtMessage;
    @FXML private Text txtEmailDisplay;
    @FXML private TextField txtCode;
    @FXML private Label lblCodeError;
    @FXML private Text txtTimer;
    @FXML private Button btnVerify;
    @FXML private Hyperlink linkResend;
    @FXML private Hyperlink linkBack;

    private String email;
    private String verificationType; // "PASSWORD_RESET" ou "ACCOUNT_VERIFICATION"
    private UserService userService;
    private VerificationService verificationService;
    private Timeline timeline;
    private int timeRemaining = 900; // 15 minutes en secondes

    public VerifyCodeController() {
        System.out.println("✅ VerifyCodeController créé");
    }

    @FXML
    public void initialize() {
        System.out.println("🔧 Initialisation VerifyCodeController...");

        try {
            userService = new UserService();
            verificationService = new VerificationService();
            System.out.println("✅ Services initialisés");
        } catch (Exception e) {
            System.err.println("⚠️ Erreur Services: " + e.getMessage());
        }

        // Limiter l'input à 6 chiffres
        txtCode.textProperty().addListener((obs, old, newVal) -> {
            if (newVal.length() > 6) {
                txtCode.setText(old);
            }
            // Permettre uniquement les chiffres
            if (!newVal.matches("\\d*")) {
                txtCode.setText(newVal.replaceAll("[^\\d]", ""));
            }
            // Cacher l'erreur quand l'utilisateur tape
            if (!newVal.isEmpty()) {
                hideError();
            }
        });
    }

    public void setEmail(String email) {
        this.email = email;
        txtEmailDisplay.setText(email);
    }

    public void setVerificationType(String type) {
        this.verificationType = type;

        if ("PASSWORD_RESET".equals(type)) {
            txtMessage.setText("Entrez le code envoyé pour réinitialiser votre mot de passe");
        } else if ("ACCOUNT_VERIFICATION".equals(type)) {
            txtMessage.setText("Entrez le code envoyé pour activer votre compte");
        }

        // Démarrer le timer
        startTimer();
    }

    private void startTimer() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            timeRemaining--;

            int minutes = timeRemaining / 60;
            int seconds = timeRemaining % 60;
            txtTimer.setText(String.format("%02d:%02d", minutes, seconds));

            if (timeRemaining <= 0) {
                timeline.stop();
                txtTimer.setText("Expiré");
                btnVerify.setDisable(true);
                AlertHelper.showAlert(rootPane,
                        "Le code a expiré. Veuillez en demander un nouveau.",
                        AlertHelper.AlertType.WARNING);
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @FXML
    private void handleVerifyCode() {
        // Validation
        if (txtCode.getText().trim().isEmpty()) {
            showFieldError("Code requis");
            return;
        }

        if (txtCode.getText().trim().length() != 6) {
            showFieldError("Le code doit contenir 6 chiffres");
            return;
        }

        btnVerify.setDisable(true);
        btnVerify.setText("Vérification...");

        try {
            String code = txtCode.getText().trim();

            // Vérifier le code
            boolean valid = verificationService.verifyCode(email, code, verificationType);

            if (valid) {
                // Arrêter le timer
                if (timeline != null) {
                    timeline.stop();
                }

                AlertHelper.showAlert(rootPane,
                        "Code vérifié avec succès!",
                        AlertHelper.AlertType.SUCCESS);

                // Redirection selon le type
                javafx.application.Platform.runLater(() -> {
                    try {
                        Thread.sleep(1000);

                        if ("PASSWORD_RESET".equals(verificationType)) {
                            navigateToResetPassword();
                        } else if ("ACCOUNT_VERIFICATION".equals(verificationType)) {
                            activateAccountAndLogin();
                        }

                    } catch (InterruptedException e) {
                        // Redirection immédiate
                        if ("PASSWORD_RESET".equals(verificationType)) {
                            navigateToResetPassword();
                        } else {
                            activateAccountAndLogin();
                        }
                    }
                });

            } else {
                showFieldError("Code invalide ou expiré");
                AlertHelper.showAlert(rootPane,
                        "Code invalide ou expiré",
                        AlertHelper.AlertType.ERROR);
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertHelper.showAlert(rootPane,
                    "Erreur de connexion",
                    AlertHelper.AlertType.ERROR);
        } finally {
            btnVerify.setDisable(false);
            btnVerify.setText("Vérifier le code");
        }
    }

    @FXML
    private void handleResendCode() {
        linkResend.setDisable(true);

        try {
            User user = userService.getUserByEmail(email);

            if (user == null) {
                AlertHelper.showAlert(rootPane,
                        "Erreur: compte introuvable",
                        AlertHelper.AlertType.ERROR);
                return;
            }

            boolean sent;
            if ("PASSWORD_RESET".equals(verificationType)) {
                sent = verificationService.sendPasswordResetCode(email, user.getName());
            } else {
                sent = verificationService.sendAccountVerificationCode(email, user.getName());
            }

            if (sent) {
                // Réinitialiser le timer
                timeRemaining = 900;
                if (timeline != null) {
                    timeline.stop();
                }
                startTimer();
                btnVerify.setDisable(false);

                AlertHelper.showAlert(rootPane,
                        "Nouveau code envoyé!",
                        AlertHelper.AlertType.SUCCESS);
            } else {
                AlertHelper.showAlert(rootPane,
                        "Erreur lors de l'envoi",
                        AlertHelper.AlertType.ERROR);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            AlertHelper.showAlert(rootPane,
                    "Erreur de connexion",
                    AlertHelper.AlertType.ERROR);
        } finally {
            // Désactiver le bouton pendant 60 secondes pour éviter le spam
            javafx.application.Platform.runLater(() -> {
                try {
                    Thread.sleep(60000);
                    linkResend.setDisable(false);
                } catch (InterruptedException e) {
                    linkResend.setDisable(false);
                }
            });
        }
    }

    @FXML
    private void handleBack() {
        if (timeline != null) {
            timeline.stop();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/ForgotPassword.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnVerify.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToResetPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/ResetPassword.fxml"));
            Parent root = loader.load();

            ResetPasswordController controller = loader.getController();
            controller.setEmail(email);

            Stage stage = (Stage) btnVerify.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void activateAccountAndLogin() {
        try {
            // Activer le compte
            User user = userService.getUserByEmail(email);
            user.setStatus("ACTIVE");
            userService.modifier(user);

            // Rediriger vers login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnVerify.getScene().getWindow();
            stage.setScene(new Scene(root));

            System.out.println("✅ Compte activé avec succès");

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== VALIDATION ====================

    private void showFieldError(String message) {
        txtCode.getStyleClass().add("error");
        lblCodeError.setText(message);
        lblCodeError.setVisible(true);
        lblCodeError.setManaged(true);
    }

    private void hideError() {
        txtCode.getStyleClass().remove("error");
        lblCodeError.setVisible(false);
        lblCodeError.setManaged(false);
    }
}