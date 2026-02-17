package org.example.campusLink.controllers;

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
import org.example.campusLink.entities.User;
import org.example.campusLink.services.UserService;
import org.example.campusLink.utils.AlertHelper;
import org.example.campusLink.utils.PasswordUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;

public class ProfileController {

    // Root pane for alerts
    @FXML private StackPane rootPane;  // IMPORTANT: Le BorderPane parent doit être dans un StackPane

    // Header labels
    @FXML private Label lblUserName;
    @FXML private Label lblUserEmail;
    @FXML private Label lblAccountType;
    @FXML private Label lblHeaderAvatar;
    @FXML private Label lblProfileAvatar;

    // Profile images
    @FXML private ImageView imgHeaderProfile;
    @FXML private ImageView imgProfile;

    // Profile fields
    @FXML private TextField txtPrenom;
    @FXML private TextField txtNom;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtUniversite;
    @FXML private TextField txtFiliere;
    @FXML private TextField txtSpecialization;
    @FXML private TextField txtAddress;
    @FXML private ComboBox<String> cmbGender;

    // Password fields
    @FXML private PasswordField txtCurrentPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;

    private UserService userService;
    private User currentUser;
    private String currentImagePath;

    private static final String PROFILE_IMAGES_DIR = "src/main/resources/images/profiles/";

    public ProfileController() {
        userService = new UserService();
    }

    @FXML
    public void initialize() {
        System.out.println("🔧 Initialisation du ProfileController...");

        cmbGender.getItems().addAll("Femme", "Homme");
        createProfileImagesDirectory();
    }

    public void setUser(User user) {
        this.currentUser = user;
        loadUserData();
    }

    private void loadUserData() {
        if (currentUser != null) {
            lblUserName.setText(currentUser.getName());
            lblUserEmail.setText(currentUser.getEmail());

            String accountType = "Étudiant";
            if (currentUser.getRoles() != null && !currentUser.getRoles().isEmpty()) {
                String roleName = currentUser.getRoles().get(0).getName();
                if (roleName.equals("PRESTATAIRE")) {
                    accountType = "Prestataire";
                } else if (roleName.equals("ADMIN")) {
                    accountType = "Administrateur";
                }
            }
            lblAccountType.setText("Type de compte: " + accountType);

            String[] nameParts = currentUser.getName().split(" ", 2);
            txtPrenom.setText(nameParts.length > 0 ? nameParts[0] : "");
            txtNom.setText(nameParts.length > 1 ? nameParts[1] : "");

            txtEmail.setText(currentUser.getEmail());
            txtPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
            txtUniversite.setText(currentUser.getUniversite() != null ? currentUser.getUniversite() : "");
            txtFiliere.setText(currentUser.getFiliere() != null ? currentUser.getFiliere() : "");
            txtSpecialization.setText(currentUser.getSpecialization() != null ? currentUser.getSpecialization() : "");
            txtAddress.setText(currentUser.getAddress() != null ? currentUser.getAddress() : "");

            if (currentUser.getGender() != null && !currentUser.getGender().isEmpty()) {
                cmbGender.setValue(currentUser.getGender().equals("Male") ? "Homme" : "Femme");
            }

            loadProfilePicture();
        }
    }

    // ==================== IMAGE UPLOAD ====================

    @FXML
    private void handleChangePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                new FileChooser.ExtensionFilter("JPG", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) txtPrenom.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                long fileSize = selectedFile.length();
                long maxSize = 5 * 1024 * 1024;

                if (fileSize > maxSize) {
                    showError("Image trop volumineuse (max 5MB)");
                    return;
                }

                String fileExtension = getFileExtension(selectedFile.getName());
                String newFileName = "profile_" + currentUser.getId() + "_" + System.currentTimeMillis() + fileExtension;

                Path sourcePath = selectedFile.toPath();
                Path destinationPath = Paths.get(PROFILE_IMAGES_DIR + newFileName);

                Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);

                currentImagePath = newFileName;
                displayProfileImage(destinationPath.toString());

                if (currentUser != null) {
                    currentUser.setProfilePicture(newFileName);
                    userService.modifier(currentUser);
                }

                showSuccess("Photo mise à jour!");

            } catch (IOException e) {
                showError("Erreur lors du téléchargement");
                e.printStackTrace();
            } catch (SQLException e) {
                showError("Erreur lors de la sauvegarde");
                e.printStackTrace();
            }
        }
    }

    private void loadProfilePicture() {
        if (currentUser != null && currentUser.getProfilePicture() != null && !currentUser.getProfilePicture().isEmpty()) {
            currentImagePath = currentUser.getProfilePicture();
            String imagePath = PROFILE_IMAGES_DIR + currentImagePath;

            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                displayProfileImage(imagePath);
            } else {
                displayDefaultAvatar();
            }
        } else {
            displayDefaultAvatar();
        }
    }

    private void displayProfileImage(String imagePath) {
        try {
            Image image = new Image("file:" + imagePath);

            if (imgProfile != null) {
                imgProfile.setImage(image);
                imgProfile.setVisible(true);
                if (lblProfileAvatar != null) {
                    lblProfileAvatar.setVisible(false);
                }
            }

            if (imgHeaderProfile != null) {
                imgHeaderProfile.setImage(image);
                imgHeaderProfile.setVisible(true);
                if (lblHeaderAvatar != null) {
                    lblHeaderAvatar.setVisible(false);
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur chargement image: " + e.getMessage());
            displayDefaultAvatar();
        }
    }

    private void displayDefaultAvatar() {
        if (imgProfile != null) {
            imgProfile.setVisible(false);
        }
        if (imgHeaderProfile != null) {
            imgHeaderProfile.setVisible(false);
        }
        if (lblProfileAvatar != null) {
            lblProfileAvatar.setVisible(true);
        }
        if (lblHeaderAvatar != null) {
            lblHeaderAvatar.setVisible(true);
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex);
        }
        return "";
    }

    private void createProfileImagesDirectory() {
        try {
            Path dirPath = Paths.get(PROFILE_IMAGES_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                System.out.println("✅ Dossier créé: " + PROFILE_IMAGES_DIR);
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur création dossier: " + e.getMessage());
        }
    }

    // ==================== SAVE PROFILE ====================

    @FXML
    private void handleSaveProfile() {
        if (!validateProfileFields()) {
            return;
        }

        if (currentUser == null) {
            showError("Utilisateur non connecté");
            return;
        }

        try {
            currentUser.setName(txtPrenom.getText().trim() + " " + txtNom.getText().trim());
            currentUser.setEmail(txtEmail.getText().trim());
            currentUser.setPhone(txtPhone.getText().trim());
            currentUser.setUniversite(txtUniversite.getText().trim());
            currentUser.setFiliere(txtFiliere.getText().trim());
            currentUser.setSpecialization(txtSpecialization.getText().trim());
            currentUser.setAddress(txtAddress.getText().trim());

            String gender = cmbGender.getValue();
            if (gender != null) {
                currentUser.setGender(gender.equals("Homme") ? "Male" : "Female");
            }

            userService.modifier(currentUser);

            lblUserName.setText(currentUser.getName());
            lblUserEmail.setText(currentUser.getEmail());

            showSuccess("Profil mis à jour!");

        } catch (SQLException e) {
            showError("Erreur lors de la sauvegarde");
            e.printStackTrace();
        }
    }

    // ==================== CHANGE PASSWORD ====================

    @FXML
    private void handleChangePassword() {
        String current = txtCurrentPassword.getText();
        String newPass = txtNewPassword.getText();
        String confirm = txtConfirmPassword.getText();

        if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        if (newPass.length() < 6) {
            showError("Minimum 6 caractères");
            return;
        }

        if (!newPass.equals(confirm)) {
            showError("Les mots de passe ne correspondent pas");
            return;
        }

        if (currentUser == null) {
            showError("Utilisateur non connecté");
            return;
        }

        try {
            // Vérifier le mot de passe actuel
            if (!PasswordUtil.checkPassword(current, currentUser.getPassword())) {
                showError("Mot de passe actuel incorrect");
                txtCurrentPassword.clear();
                return;
            }

            // Hasher le nouveau mot de passe
            String hashedPassword = PasswordUtil.hashPassword(newPass);
            currentUser.setPassword(hashedPassword);

            // Sauvegarder
            userService.modifier(currentUser);

            txtCurrentPassword.clear();
            txtNewPassword.clear();
            txtConfirmPassword.clear();

            showSuccess("Mot de passe changé!");

        } catch (SQLException e) {
            showError("Erreur lors du changement");
            e.printStackTrace();
        } catch (Exception e) {
            showError("Erreur inattendue");
            e.printStackTrace();
        }
    }

    // ==================== LOGOUT ====================

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Déconnexion");
        alert.setHeaderText("Êtes-vous sûr de vouloir vous déconnecter?");
        alert.setContentText("Vous serez redirigé vers la page de connexion.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Login.fxml"));
                    Parent root = loader.load();

                    Stage stage = (Stage) txtPrenom.getScene().getWindow();
                    Scene scene = new Scene(root);

                    stage.setWidth(1000);
                    stage.setHeight(700);
                    stage.centerOnScreen();

                    stage.setScene(scene);

                    System.out.println("✅ Déconnexion réussie");

                } catch (IOException e) {
                    e.printStackTrace();
                    showError("Erreur lors de la déconnexion");
                }
            }
        });
    }

    // ==================== VALIDATION ====================

    private boolean validateProfileFields() {
        if (txtPrenom.getText().trim().isEmpty()) {
            showError("Le prénom est requis");
            return false;
        }

        if (txtNom.getText().trim().isEmpty()) {
            showError("Le nom est requis");
            return false;
        }

        if (txtEmail.getText().trim().isEmpty()) {
            showError("L'email est requis");
            return false;
        }

        if (!isValidEmail(txtEmail.getText())) {
            showError("Email invalide");
            return false;
        }

        if (txtPhone.getText().trim().isEmpty()) {
            showError("Le téléphone est requis");
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
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