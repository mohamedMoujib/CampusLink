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

import org.example.campusLink.controllers.reviews.StudentReviewController;
import org.example.campusLink.controllers.reviews.TutorReviewsController;
import org.example.campusLink.entities.*;
import org.example.campusLink.Services.AuthService;
import org.example.campusLink.Services.UserService;
import org.example.campusLink.utils.AlertHelper;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.SQLException;

public class ProfileController {

    @FXML private StackPane rootPane;

    // ✅ REMOVED: lblUserName, lblUserEmail, imgHeaderProfile, lblHeaderAvatar
    //    These fx:ids do not exist in ProfileView.fxml and caused NullPointerException.
    //    The user name/email header is rendered inside NavBar.fxml by NavBarController.

    @FXML private Label lblAccountType;
    @FXML private Label lblProfileAvatar;

    @FXML private ImageView imgProfile;

    @FXML private TextField txtPrenom;
    @FXML private TextField txtNom;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private TextField txtUniversite;
    @FXML private TextField txtFiliere;
    @FXML private TextField txtSpecialization;
    @FXML private TextField txtAddress;
    @FXML private ComboBox<String> cmbGender;

    @FXML private PasswordField txtCurrentPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;

    private final UserService userService = new UserService();
    private final AuthService authService = new AuthService();

    private User currentUser;

    private static final String PROFILE_IMAGES_DIR =
            System.getProperty("user.home") + "/campuslink/profiles/";

    @FXML
    public void initialize() {
        cmbGender.getItems().addAll("Femme", "Homme");
        createProfileImagesDirectory();
    }

    public void setUser(User user) {
        this.currentUser = user;
        loadUserData();
    }

    private void loadUserData() {
        if (currentUser == null) return;

        // lblAccountType IS in the FXML — safe to use
        String accountType;
        if (currentUser instanceof Etudiant) {
            accountType = "Étudiant";
            Etudiant etu = (Etudiant) currentUser;
            txtUniversite.setText(nullSafe(etu.getUniversite()));
            txtFiliere.setText(nullSafe(etu.getFiliere()));
            txtSpecialization.setText(nullSafe(etu.getSpecialization()));
        } else if (currentUser instanceof Prestataire) {
            accountType = "Prestataire";
            Prestataire pres = (Prestataire) currentUser;
            txtUniversite.setText(nullSafe(pres.getUniversite()));
            txtFiliere.setText(nullSafe(pres.getFiliere()));
            txtSpecialization.setText(nullSafe(pres.getSpecialization()));
        } else if (currentUser instanceof Admin) {
            accountType = "Administrateur";
            txtUniversite.setText("");
            txtFiliere.setText("");
            txtSpecialization.setText("");
        } else {
            accountType = "Utilisateur";
            txtUniversite.setText("");
            txtFiliere.setText("");
            txtSpecialization.setText("");
        }

        lblAccountType.setText("Type de compte: " + accountType);

        String[] nameParts = currentUser.getName().split(" ", 2);
        txtPrenom.setText(nameParts.length > 0 ? nameParts[0] : "");
        txtNom.setText(nameParts.length > 1 ? nameParts[1] : "");

        txtEmail.setText(currentUser.getEmail());
        txtPhone.setText(nullSafe(currentUser.getPhone()));
        txtAddress.setText(nullSafe(currentUser.getAddress()));

        if (currentUser.getGender() != null) {
            cmbGender.setValue(
                    currentUser.getGender().equalsIgnoreCase("Male") ? "Homme" : "Femme"
            );
        }

        loadProfilePicture();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    // ==================== IMAGE ====================

    @FXML
    private void handleChangePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) rootPane.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        try {
            if (file.length() > 5 * 1024 * 1024) { showError("Image trop volumineuse (max 5MB)"); return; }

            String mime = Files.probeContentType(file.toPath());
            if (mime == null || !mime.startsWith("image/")) { showError("Fichier invalide"); return; }

            String extension = file.getName().substring(file.getName().lastIndexOf("."));
            String newName = "profile_" + currentUser.getId() + "_" + System.currentTimeMillis() + extension;

            Path dest = Paths.get(PROFILE_IMAGES_DIR + newName);
            Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

            if (currentUser.getProfilePicture() != null) {
                Files.deleteIfExists(Paths.get(PROFILE_IMAGES_DIR + currentUser.getProfilePicture()));
            }

            currentUser.setProfilePicture(newName);
            userService.modifier(currentUser);
            displayProfileImage(dest.toString());
            showSuccess("Photo mise à jour");

        } catch (Exception e) {
            showError("Erreur image");
            e.printStackTrace();
        }
    }

    private void loadProfilePicture() {
        String pic = currentUser.getProfilePicture();
        if (pic == null || pic.isEmpty()) { displayDefaultAvatar(); return; }

        if (pic.startsWith("http://") || pic.startsWith("https://")) {
            try {
                Image image = new Image(pic, true);
                imgProfile.setImage(image);
                imgProfile.setVisible(true);
                lblProfileAvatar.setVisible(false);
            } catch (Exception e) { displayDefaultAvatar(); }
        } else {
            Path path = Paths.get(PROFILE_IMAGES_DIR + pic);
            if (Files.exists(path)) displayProfileImage(path.toString());
            else displayDefaultAvatar();
        }
    }

    private void displayProfileImage(String path) {
        Image image = new Image("file:" + path);
        imgProfile.setImage(image);
        imgProfile.setVisible(true);
        lblProfileAvatar.setVisible(false);
    }

    private void displayDefaultAvatar() {
        imgProfile.setVisible(false);
        lblProfileAvatar.setVisible(true);
    }

    private void createProfileImagesDirectory() {
        try { Files.createDirectories(Paths.get(PROFILE_IMAGES_DIR)); } catch (IOException e) { e.printStackTrace(); }
    }

    // ==================== SAVE PROFILE ====================

    @FXML
    private void handleSaveProfile() {
        if (!validateProfileFields()) return;

        try {
            User existing = userService.getUserByEmail(txtEmail.getText().trim());
            if (existing != null && existing.getId() != currentUser.getId()) {
                showError("Email déjà utilisé"); return;
            }

            currentUser.setName(txtPrenom.getText().trim() + " " + txtNom.getText().trim());
            currentUser.setEmail(txtEmail.getText().trim());
            currentUser.setPhone(txtPhone.getText().trim());
            currentUser.setAddress(txtAddress.getText().trim());

            if (cmbGender.getValue() != null) {
                currentUser.setGender(cmbGender.getValue().equals("Homme") ? "Male" : "Female");
            }

            if (currentUser instanceof Etudiant) {
                Etudiant etu = (Etudiant) currentUser;
                etu.setUniversite(txtUniversite.getText().trim());
                etu.setFiliere(txtFiliere.getText().trim());
                etu.setSpecialization(txtSpecialization.getText().trim());
            } else if (currentUser instanceof Prestataire) {
                Prestataire pres = (Prestataire) currentUser;
                pres.setUniversite(txtUniversite.getText().trim());
                pres.setFiliere(txtFiliere.getText().trim());
                pres.setSpecialization(txtSpecialization.getText().trim());
            }

            userService.modifier(currentUser);
            // ✅ REMOVED: lblUserName.setText(...) and lblUserEmail.setText(...)
            //    Those labels are in NavBar, not in this FXML.
            showSuccess("Profil mis à jour");

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur sauvegarde");
        }
    }

    // ==================== CHANGE PASSWORD ====================

    @FXML
    private void handleChangePassword() {
        try {
            authService.changePassword(currentUser, txtCurrentPassword.getText(), txtNewPassword.getText());
            txtCurrentPassword.clear();
            txtNewPassword.clear();
            txtConfirmPassword.clear();
            showSuccess("Mot de passe changé");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("Erreur changement mot de passe");
        }
    }

    // ==================== NAVIGATION ====================

    @FXML
    private void handleNavigateToReviews() {
        try {
            if (currentUser instanceof Prestataire) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Reviews/TutorReviews.fxml"));
                Parent root = loader.load();
                TutorReviewsController controller = loader.getController();
                controller.setTutorId(currentUser.getId());
                Stage stage = (Stage) rootPane.getScene().getWindow();
                stage.setScene(new Scene(root));

            } else if (currentUser instanceof Etudiant) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Reviews/StudentReviews.fxml"));
                Parent root = loader.load();
                StudentReviewController controller = loader.getController();
                controller.setUser(currentUser);
                Stage stage = (Stage) rootPane.getScene().getWindow();
                stage.setScene(new Scene(root));
            }

        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors de la navigation vers les avis");
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (IOException e) {
            showError("Erreur déconnexion");
        }
    }

    // ==================== VALIDATION ====================

    private boolean validateProfileFields() {
        if (txtPrenom.getText().trim().isEmpty()) { showError("Prénom requis"); return false; }
        if (txtNom.getText().trim().isEmpty())    { showError("Nom requis"); return false; }
        if (!txtEmail.getText().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            showError("Email invalide"); return false;
        }
        return true;
    }

    private void showSuccess(String msg) { AlertHelper.showAlert(rootPane, msg, AlertHelper.AlertType.SUCCESS); }
    private void showError(String msg)   { AlertHelper.showAlert(rootPane, msg, AlertHelper.AlertType.ERROR); }
}