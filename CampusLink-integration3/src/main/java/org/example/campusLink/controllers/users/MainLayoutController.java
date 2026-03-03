package org.example.campusLink.controllers.users;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.campusLink.controllers.NotificationBellController;
import org.example.campusLink.controllers.Publication_controller;
import org.example.campusLink.controllers.Service_controller;
import org.example.campusLink.controllers.Student_controller;
import org.example.campusLink.controllers.paiements.PaymentController;
import org.example.campusLink.controllers.paiements.ViewInvoice;
import org.example.campusLink.controllers.reservations.EtudiantReservationsController;
import org.example.campusLink.controllers.reservations.MessageController;
import org.example.campusLink.controllers.reservations.ReservationsController;
import org.example.campusLink.controllers.reviews.StudentReviewController;
import org.example.campusLink.controllers.reviews.TutorReviewsController;
import org.example.campusLink.entities.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainLayoutController {

    @FXML private StackPane rootPane;
    @FXML private StackPane contentArea;
    @FXML private HBox navBar;

    @FXML private Label lblUserName;
    @FXML private Label lblUserEmail;
    @FXML private Label lblRoleSpace;
    @FXML private Label lblRoleBadge;
    @FXML private Label lblHeaderAvatar;
    @FXML private ImageView imgHeaderProfile;
    @FXML private NotificationBellController notificationBellController ;

    private User currentUser;
    private Button activeNavBtn;

    private static final String PROFILE_IMAGES_DIR =
            System.getProperty("user.home") + "/campuslink/profiles/";

    // ===== NAV MENUS =====
    private static final Map<String, String> ETUDIANT_NAV = new LinkedHashMap<>();
    private static final Map<String, String> PRESTATAIRE_NAV = new LinkedHashMap<>();

    static {
        ETUDIANT_NAV.put("🔍 Rechercher des services", "/Views/Student.fxml");
        ETUDIANT_NAV.put("📢 Publications",            "/Views/Publication.fxml");
        ETUDIANT_NAV.put("📅 Réservations",            "/Views/reservations/EtudiantReservationsView.fxml");
        ETUDIANT_NAV.put("💬 Messagerie",              "/Views/reservations/messages.fxml");
        ETUDIANT_NAV.put("💳 Factures",               "/Views/paiements/InvoiceView.fxml");
        ETUDIANT_NAV.put("⭐ Avis",                    "/Views/Reviews/StudentReviews.fxml");
        ETUDIANT_NAV.put("👤 Mon profil",              "/Views/users/ProfileView.fxml");

        PRESTATAIRE_NAV.put("🛠 Mes services",         "/Views/service.fxml");
        PRESTATAIRE_NAV.put("📅 Réservations",         "/Views/reservations/reservations.fxml");
        PRESTATAIRE_NAV.put("💬 Messagerie",           "/Views/reservations/messages.fxml");
        PRESTATAIRE_NAV.put("⭐ Avis",                 "/Views/Reviews/TutorReviews.fxml");
        PRESTATAIRE_NAV.put("👤 Mon profil",           "/Views/users/ProfileView.fxml");
    }

    // Called from LoginController after login
    public void setUser(User user) {
        this.currentUser = user;
        setupHeader();
        buildNavBar();
        // Load first tab automatically
        Button firstBtn = (Button) navBar.getChildren().get(0);
        String firstFxml = getNavForUser().values().iterator().next();
        loadView(firstFxml, firstBtn);
    }

    // ===== HEADER =====

    private void setupHeader() {
        lblUserName.setText(currentUser.getName());
        lblUserEmail.setText(currentUser.getEmail());

        if (currentUser instanceof Prestataire) {
            lblRoleSpace.setText("Espace Prestataire");
            lblRoleBadge.setText("Prestataire");
            lblRoleBadge.getStyleClass().setAll("role-badge-prestataire");
        } else {
            lblRoleSpace.setText("Espace Étudiant");
            lblRoleBadge.setText("Étudiant");
            lblRoleBadge.getStyleClass().setAll("role-badge-etudiant");
        }

        loadHeaderAvatar();

        if (notificationBellController != null) {
            notificationBellController.setCurrentUserId(currentUser.getId());
        }
    }

    private void loadHeaderAvatar() {
        String pic = currentUser.getProfilePicture();
        if (pic == null || pic.isEmpty()) {
            showDefaultAvatar();
            return;
        }
        if (pic.startsWith("http://") || pic.startsWith("https://")) {
            try {
                Image img = new Image(pic, true);
                imgHeaderProfile.setImage(img);
                imgHeaderProfile.setVisible(true);
                lblHeaderAvatar.setVisible(false);
            } catch (Exception e) {
                showDefaultAvatar();
            }
        } else {
            Path path = Paths.get(PROFILE_IMAGES_DIR + pic);
            if (Files.exists(path)) {
                imgHeaderProfile.setImage(new Image("file:" + path));
                imgHeaderProfile.setVisible(true);
                lblHeaderAvatar.setVisible(false);
            } else {
                showDefaultAvatar();
            }
        }
    }

    private void showDefaultAvatar() {
        imgHeaderProfile.setVisible(false);
        lblHeaderAvatar.setVisible(true);
    }

    // ===== NAV BAR =====

    private void buildNavBar() {
        navBar.getChildren().clear();
        getNavForUser().forEach((label, fxml) -> {
            Button btn = new Button(label);
            btn.getStyleClass().add("nav-tab");
            btn.setOnAction(e -> loadView(fxml, btn));
            navBar.getChildren().add(btn);
        });
    }

    private Map<String, String> getNavForUser() {
        return (currentUser instanceof Prestataire) ? PRESTATAIRE_NAV : ETUDIANT_NAV;
    }

    // ===== CONTENT SWAP =====

    private void loadView(String fxmlPath, Button navBtn) {
        if (activeNavBtn != null) {
            activeNavBtn.getStyleClass().remove("nav-tab-active");
            if (!activeNavBtn.getStyleClass().contains("nav-tab")) {
                activeNavBtn.getStyleClass().add("nav-tab");
            }
        }
        navBtn.getStyleClass().remove("nav-tab");
        navBtn.getStyleClass().add("nav-tab-active");
        activeNavBtn = navBtn;

        try {
            // ✅ Use fxmlPath parameter, not hardcoded ProfileView
            var resource = getClass().getResource(fxmlPath);
            if (resource == null) {
                showPlaceholder(navBtn.getText());
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();

            Object ctrl = loader.getController();
            if (ctrl instanceof ProfileController) {
                ((ProfileController) ctrl).setUser(currentUser);
            }
            if (ctrl instanceof StudentReviewController) {
                ((StudentReviewController) ctrl).setUser(currentUser);
            }
            if (ctrl instanceof TutorReviewsController) {
                ((TutorReviewsController) ctrl).setTutorId(currentUser.getId());
            }
            if (ctrl instanceof Student_controller) {
                ((Student_controller) ctrl).setUser(currentUser);
            }
            if (ctrl instanceof Publication_controller) {
                ((Publication_controller) ctrl).setUser(currentUser);
                ((Publication_controller) ctrl).setMainLayoutController(this);
            }
            if (ctrl instanceof Service_controller) {
                ((Service_controller) ctrl).setUser(currentUser);
                ((Service_controller) ctrl).setMainLayoutController(this);
            }
            if (ctrl instanceof ReservationsController) {
                ((ReservationsController) ctrl).setUser(currentUser);
                ((ReservationsController) ctrl).setMainLayoutController(this);
            }
            if (ctrl instanceof MessageController) {
                ((MessageController) ctrl).setUser(currentUser);
            }
            if (ctrl instanceof EtudiantReservationsController) {
                ((EtudiantReservationsController) ctrl).setUser(currentUser);
                ((EtudiantReservationsController) ctrl).setMainLayoutController(this);
            }
            if (ctrl instanceof ViewInvoice) {
                ((ViewInvoice) ctrl).setUser(currentUser);
            }
            contentArea.getChildren().setAll(view);

        } catch (IOException e) {
            e.printStackTrace();
            showPlaceholder(navBtn.getText());
        }
    }
    private void showPlaceholder(String pageName) {
        Label lbl = new Label("🚧  " + pageName
                + "\n\nCette section est en cours de développement.");
        lbl.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-text-fill: #9ca3af;" +
                        "-fx-text-alignment: center;" +
                        "-fx-alignment: center;" +
                        "-fx-wrap-text: true;"
        );
        lbl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        contentArea.getChildren().setAll(lbl);
    }

    // ===== LOGOUT =====

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Views/users/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setWidth(500);
            stage.setHeight(700);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Called by ProfileController after photo change to refresh header avatar
    public void refreshAvatar(User updatedUser) {
        this.currentUser = updatedUser;
        loadHeaderAvatar();
    }
    public void loadContent(Parent view) {
        contentArea.getChildren().setAll(view);
    }
}