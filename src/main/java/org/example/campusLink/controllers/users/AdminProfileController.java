package org.example.campusLink.controllers.users;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.campusLink.entities.Admin;
import org.example.campusLink.services.users.AuthService;
import org.example.campusLink.services.users.UserService;
import org.example.campusLink.utils.AlertHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;

/**
 * Controller pour le profil admin
 */
public class AdminProfileController {

    // Root pane for alerts
    @FXML private StackPane rootPane;

    // Header
    @FXML private Label lblAdminName;
    @FXML private Label lblAdminEmail;
    @FXML private Label lblHeaderAvatar;
    @FXML private ImageView imgHeaderProfile;

    // Profile info
    @FXML private Label lblAccountType;
    @FXML private Label lblProfileAvatar;
    @FXML private ImageView imgProfile;

    // Form fields
    @FXML private TextField txtName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtDepartment;
    @FXML private ComboBox<String> cmbGender;
    @FXML private TextField txtAddress;

    // Security fields
    @FXML private PasswordField txtCurrentPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;

    private Admin currentAdmin;
    private UserService userService;
    private AuthService authService;

    public AdminProfileController() {
        this.userService = new UserService();
        this.authService = new AuthService();
    }

    @FXML
    public void initialize() {
        System.out.println("🔧 Initialisation AdminProfileController...");

        // Setup ComboBox
        cmbGender.getItems().addAll("Male", "Female");
    }

    public void setAdmin(Admin admin) {
        this.currentAdmin = admin;
        loadAdminData();
    }

    private void loadAdminData() {
        if (currentAdmin != null) {
            // Header
            lblAdminName.setText(currentAdmin.getName());
            lblAdminEmail.setText(currentAdmin.getEmail());

            // Form fields
            txtName.setText(currentAdmin.getName());
            txtEmail.setText(currentAdmin.getEmail());
            txtPhone.setText(currentAdmin.getPhone() != null ? currentAdmin.getPhone() : "");
            txtAddress.setText(currentAdmin.getAddress() != null ? currentAdmin.getAddress() : "");

            if (currentAdmin.getGender() != null) {
                cmbGender.setValue(currentAdmin.getGender());
            }

            lblAccountType.setText("Type de compte: Administrateur");

            // Load profile picture
            displayProfileImage();
        }
    }

    // ==================== PROFILE IMAGE ====================

    private void displayProfileImage() {
        if (currentAdmin != null && currentAdmin.getProfilePicture() != null && !currentAdmin.getProfilePicture().isEmpty()) {
            try {
                String imagePath = "src/main/resources/images/profiles/" + currentAdmin.getProfilePicture();
                File imageFile = new File(imagePath);

                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());

                    // Header avatar
                    imgHeaderProfile.setImage(image);
                    imgHeaderProfile.setVisible(true);
                    lblHeaderAvatar.setVisible(false);

                    // Profile avatar
                    imgProfile.setImage(image);
                    imgProfile.setVisible(true);
                    lblProfileAvatar.setVisible(false);
                } else {
                    displayDefaultAvatar();
                }
            } catch (Exception e) {
                e.printStackTrace();
                displayDefaultAvatar();
            }
        } else {
            displayDefaultAvatar();
        }
    }

    private void displayDefaultAvatar() {
        imgHeaderProfile.setVisible(false);
        lblHeaderAvatar.setVisible(true);
        lblHeaderAvatar.setText("👨‍💼");

        imgProfile.setVisible(false);
        lblProfileAvatar.setVisible(true);
        lblProfileAvatar.setText("👨‍💼");
    }

    @FXML
    private void handleChangePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());

        if (selectedFile != null) {
            try {
                // Validate file size (max 5MB)
                long fileSize = selectedFile.length();
                long maxSize = 5 * 1024 * 1024; // 5MB

                if (fileSize > maxSize) {
                    showError("La taille du fichier ne doit pas dépasser 5MB");
                    return;
                }

                // Create directory if not exists
                Path uploadDir = Paths.get("src/main/resources/images/profiles");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                // Generate unique filename
                String extension = selectedFile.getName().substring(selectedFile.getName().lastIndexOf("."));
                String newFileName = "admin_" + currentAdmin.getId() + "_" + System.currentTimeMillis() + extension;

                // Copy file
                Path targetPath = uploadDir.resolve(newFileName);
                Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                // Update admin
                currentAdmin.setProfilePicture(newFileName);
                userService.modifier(currentAdmin);

                // Refresh display
                displayProfileImage();
                showSuccess("Photo mise à jour avec succès!");

            } catch (IOException | SQLException e) {
                e.printStackTrace();
                showError("Erreur lors du téléchargement: " + e.getMessage());
            }
        }
    }

    // ==================== SAVE PROFILE ====================

    @FXML
    private void handleSaveProfile() {
        try {
            // Update admin data
            currentAdmin.setName(txtName.getText().trim());
            currentAdmin.setEmail(txtEmail.getText().trim());
            currentAdmin.setPhone(txtPhone.getText().trim());
            currentAdmin.setGender(cmbGender.getValue());
            currentAdmin.setAddress(txtAddress.getText().trim());

            // Save to database
            userService.modifier(currentAdmin);

            // Update header
            lblAdminName.setText(currentAdmin.getName());
            lblAdminEmail.setText(currentAdmin.getEmail());

            showSuccess("Profil mis à jour avec succès!");

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    // ==================== CHANGE PASSWORD ====================

    @FXML
    private void handleChangePassword() {
        String currentPassword = txtCurrentPassword.getText();
        String newPassword = txtNewPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();

        // Validation
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        if (newPassword.length() < 6) {
            showError("Le nouveau mot de passe doit contenir au moins 6 caractères");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas");
            return;
        }

        try {
            // Change password using AuthService
            authService.changePassword(currentAdmin, currentPassword, newPassword);

            // Clear fields
            txtCurrentPassword.clear();
            txtNewPassword.clear();
            txtConfirmPassword.clear();

            showSuccess("Mot de passe changé avec succès!");

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur lors du changement de mot de passe");
        }
    }

    // ==================== NAVIGATION ====================

    @FXML
    private void handleNavigateToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/users/AdminDashboard.fxml"));
            Parent root = loader.load();

            AdminDashboardController controller = loader.getController();
            controller.setAdmin(currentAdmin);

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));

            System.out.println("✅ Navigation vers Dashboard");

        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement du dashboard");
        }
    }

    @FXML
    private void handleNavigateToProfile() {
        // Déjà sur le profil
        System.out.println("📍 Déjà sur le profil");
    }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Déconnexion");
        alert.setHeaderText("Êtes-vous sûr de vouloir vous déconnecter?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/users/Login.fxml"));
                    Parent root = loader.load();

                    Stage stage = (Stage) rootPane.getScene().getWindow();
                    Scene scene = new Scene(root);

                    stage.setWidth(1000);
                    stage.setHeight(700);
                    stage.centerOnScreen();
                    stage.setScene(scene);

                    System.out.println("✅ Déconnexion admin réussie");

                } catch (IOException e) {
                    e.printStackTrace();
                    showError("Erreur lors de la déconnexion");
                }
            }
        });
    }

    // ==================== ALERTS ====================

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


}