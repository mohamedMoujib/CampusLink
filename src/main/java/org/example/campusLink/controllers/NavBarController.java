package org.example.campusLink.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.campusLink.controllers.reviews.StudentReviewController;
import org.example.campusLink.controllers.reviews.TutorReviewsController;
import org.example.campusLink.entities.Prestataire;
import org.example.campusLink.entities.User;
import org.example.campusLink.utils.AppSession;
import org.example.Controllers.Publication_controller;
import org.example.Controllers.Student_controller;

import java.io.IOException;

/**
 * Contrôleur commun de la barre de navigation (NavBar.fxml).
 * Inclus dans toutes les pages principales pour une navigation cohérente.
 */
public class NavBarController {

    @FXML private VBox root;
    @FXML private javafx.scene.control.Label userNameLabel;
    @FXML private javafx.scene.control.Label userEmailLabel;
    @FXML private javafx.scene.control.Label spaceLabel;
    @FXML private javafx.scene.layout.HBox studentMenu;
    @FXML private javafx.scene.layout.HBox tuteurMenu;

    @FXML
    public void initialize() {
        User user = AppSession.getCurrentUser();
        if (user != null) {
            if (userNameLabel != null) userNameLabel.setText(user.getName());
            if (userEmailLabel != null) userEmailLabel.setText(user.getEmail());
            if (spaceLabel != null) {
                spaceLabel.setText(user instanceof Prestataire ? "Espace Tuteur" : "Espace Étudiant");
            }
            if (studentMenu != null && tuteurMenu != null) {
                boolean isTuteur = user instanceof Prestataire;
                studentMenu.setVisible(!isTuteur);
                studentMenu.setManaged(!isTuteur);
                tuteurMenu.setVisible(isTuteur);
                tuteurMenu.setManaged(isTuteur);
            }
        }
    }

    private Stage getStage() {
        if (root != null && root.getScene() != null) {
            return (Stage) root.getScene().getWindow();
        }
        return null;
    }

    @FXML
    private void goToHome() {
        User user = AppSession.getCurrentUser();
        if (user instanceof Prestataire) {
            goToTutorDashboard();
        } else {
            goToStudent();
        }
    }

    @FXML
    private void goToStudent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Student.fxml"));
            Parent parent = loader.load();
            Student_controller ctrl = loader.getController();
            if (AppSession.getCurrentUser() != null) ctrl.setUser(AppSession.getCurrentUser());
            Stage stage = getStage();
            if (stage != null) {
                stage.setScene(new Scene(parent));
                stage.setTitle("CampusLink - Rechercher des services");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToPublications() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Publication.fxml"));
            Parent parent = loader.load();
            Publication_controller ctrl = loader.getController();
            if (AppSession.getCurrentUser() != null) ctrl.setCurrentStudentId(AppSession.getCurrentUser().getId());
            Stage stage = getStage();
            if (stage != null) {
                stage.setScene(new Scene(parent));
                stage.setTitle("CampusLink - Publications");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/ProfileView.fxml"));
            Parent parent = loader.load();
            ProfileController ctrl = loader.getController();
            if (AppSession.getCurrentUser() != null) ctrl.setUser(AppSession.getCurrentUser());
            Stage stage = getStage();
            if (stage != null) {
                stage.setScene(new Scene(parent));
                stage.setTitle("CampusLink - Mon profil");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ✅ AJOUTÉ : Navigation vers les avis étudiant
    @FXML
    private void goToStudentReviews() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Reviews/StudentReviews.fxml"));
            Parent parent = loader.load();
            StudentReviewController ctrl = loader.getController();
            if (AppSession.getCurrentUser() != null) ctrl.setUser(AppSession.getCurrentUser());
            Stage stage = getStage();
            if (stage != null) {
                stage.setScene(new Scene(parent));
                stage.setTitle("CampusLink - Mes avis");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToLogin() {
        AppSession.clear();
        try {
            Parent parent = FXMLLoader.load(getClass().getResource("/Views/Login.fxml"));
            Stage stage = getStage();
            if (stage != null) {
                stage.setScene(new Scene(parent));
                stage.setTitle("CampusLink - Connexion");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToService() {
        try {
            Parent parent = FXMLLoader.load(getClass().getResource("/Views/service.fxml"));
            Stage stage = getStage();
            if (stage != null) {
                stage.setScene(new Scene(parent));
                stage.setTitle("CampusLink - Mes services");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToTutorDashboard() {
        try {
            Parent parent = FXMLLoader.load(getClass().getResource("/Views/TutorDashboardView.fxml"));
            Stage stage = getStage();
            if (stage != null) {
                stage.setScene(new Scene(parent));
                stage.setTitle("CampusLink - Tableau de bord");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToTutorReviews() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Reviews/TutorReviews.fxml"));
            Parent parent = loader.load();
            TutorReviewsController ctrl = loader.getController();
            if (AppSession.getCurrentUser() != null) ctrl.setTutorId(AppSession.getCurrentUser().getId());
            Stage stage = getStage();
            if (stage != null) {
                stage.setScene(new Scene(parent));
                stage.setTitle("CampusLink - Avis reçus");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}