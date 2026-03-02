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
import java.nio.file.*;
import java.sql.SQLException;

public class AdminProfileController {

    @FXML private StackPane rootPane;
    @FXML private Label lblAdminName;
    @FXML private Label lblAdminEmail;
    @FXML private Label lblHeaderAvatar;
    @FXML private ImageView imgHeaderProfile;
    @FXML private Label lblAccountType;
    @FXML private Label lblProfileAvatar;
    @FXML private ImageView imgProfile;
    @FXML private TextField txtName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtDepartment;
    @FXML private ComboBox<String> cmbGender;
    @FXML private TextField txtAddress;
    @FXML private PasswordField txtCurrentPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;

    private Admin currentAdmin;
    private final UserService userService = new UserService();
    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        cmbGender.getItems().addAll("Male", "Female");
    }

    public void setAdmin(Admin admin) {
        this.currentAdmin = admin;
        loadAdminData();
    }

    private void loadAdminData() {
        if (currentAdmin == null) return;

        lblAdminName.setText(currentAdmin.getName());
        lblAdminEmail.setText(currentAdmin.getEmail());
        txtName.setText(currentAdmin.getName());
        txtEmail.setText(currentAdmin.getEmail());
        txtPhone.setText(currentAdmin.getPhone() != null
                ? currentAdmin.getPhone() : "");
        txtAddress.setText(currentAdmin.getAddress() != null
                ? currentAdmin.getAddress() : "");
        if (currentAdmin.getGender() != null)
            cmbGender.setValue(currentAdmin.getGender());

        lblAccountType.setText("Type de compte : Administrateur");
        displayProfileImage();
    }

    // ── PROFILE IMAGE ─────────────────────────────────────────────────────────

    private void displayProfileImage() {
        if (currentAdmin.getProfilePicture() != null
                && !currentAdmin.getProfilePicture().isEmpty()) {
            try {
                File imageFile = new File(
                        "src/main/resources/images/profiles/"
                                + currentAdmin.getProfilePicture());
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    imgHeaderProfile.setImage(image);
                    imgHeaderProfile.setVisible(true);
                    lblHeaderAvatar.setVisible(false);
                    imgProfile.setImage(image);
                    imgProfile.setVisible(true);
                    lblProfileAvatar.setVisible(false);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        displayDefaultAvatar();
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
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo de profil");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        File file = fc.showOpenDialog(rootPane.getScene().getWindow());
        if (file == null) return;

        if (file.length() > 5 * 1024 * 1024) {
            showError("La taille du fichier ne doit pas dépasser 5 MB");
            return;
        }

        try {
            Path uploadDir = Paths.get("src/main/resources/images/profiles");
            if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);

            String ext = file.getName()
                    .substring(file.getName().lastIndexOf("."));
            String newName = "admin_" + currentAdmin.getId()
                    + "_" + System.currentTimeMillis() + ext;

            Files.copy(file.toPath(), uploadDir.resolve(newName),
                    StandardCopyOption.REPLACE_EXISTING);

            currentAdmin.setProfilePicture(newName);
            userService.modifier(currentAdmin);
            displayProfileImage();
            showSuccess("Photo mise à jour avec succès !");

        } catch (IOException | SQLException e) {
            e.printStackTrace();
            showError("Erreur lors du téléchargement : " + e.getMessage());
        }
    }

    // ── SAVE PROFILE ──────────────────────────────────────────────────────────

    @FXML
    private void handleSaveProfile() {
        try {
            currentAdmin.setName(txtName.getText().trim());
            currentAdmin.setEmail(txtEmail.getText().trim());
            currentAdmin.setPhone(txtPhone.getText().trim());
            currentAdmin.setGender(cmbGender.getValue());
            currentAdmin.setAddress(txtAddress.getText().trim());
            // ✅ Don't touch password here — it's changed separately via handleChangePassword()
            // currentAdmin.password is already set from login, keep it as-is

            userService.modifier(currentAdmin);

            lblAdminName.setText(currentAdmin.getName());
            lblAdminEmail.setText(currentAdmin.getEmail());
            showSuccess("Profil mis à jour avec succès !");

        } catch (IllegalArgumentException e) {
            // ✅ Catch validation errors from validateUser()
            showError(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }
    // ── CHANGE PASSWORD ───────────────────────────────────────────────────────

    @FXML
    private void handleChangePassword() {
        String current = txtCurrentPassword.getText();
        String next    = txtNewPassword.getText();
        String confirm = txtConfirmPassword.getText();

        if (current.isEmpty() || next.isEmpty() || confirm.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }
        if (next.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères");
            return;
        }
        if (!next.equals(confirm)) {
            showError("Les mots de passe ne correspondent pas");
            return;
        }

        try {
            authService.changePassword(currentAdmin, current, next);
            txtCurrentPassword.clear();
            txtNewPassword.clear();
            txtConfirmPassword.clear();
            showSuccess("Mot de passe changé avec succès !");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur lors du changement de mot de passe");
        }
    }

    // ── NAVIGATION ────────────────────────────────────────────────────────────

    @FXML
    private void handleNavigateToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Views/users/AdminDashboard.fxml"));
            Parent root = loader.load();
            AdminDashboardController ctrl = loader.getController();
            ctrl.setAdmin(currentAdmin);
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement du dashboard");
        }
    }

    @FXML
    private void handleNavigateToProfile() {}

    @FXML
    private void handleLogout() {
        showConfirm(
                "Déconnexion",
                "Êtes-vous sûr de vouloir vous déconnecter ?",
                () -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(
                                getClass().getResource("/Views/users/Login.fxml"));
                        Parent root = loader.load();
                        Stage stage = (Stage) rootPane.getScene().getWindow();
                        stage.setWidth(1000);
                        stage.setHeight(700);
                        stage.centerOnScreen();
                        stage.setScene(new Scene(root));
                    } catch (IOException e) {
                        e.printStackTrace();
                        showError("Erreur lors de la déconnexion");
                    }
                }
        );
    }

    // ── ALERT HELPERS ─────────────────────────────────────────────────────────

    private void showSuccess(String message) {
        AlertHelper.showAlert(rootPane, message, AlertHelper.AlertType.SUCCESS);
    }

    private void showError(String message) {
        AlertHelper.showAlert(rootPane, message, AlertHelper.AlertType.ERROR);
    }

    private void showConfirm(String title, String message, Runnable onConfirm) {
        AlertHelper.showConfirm(rootPane, title, message, onConfirm);
    }
}