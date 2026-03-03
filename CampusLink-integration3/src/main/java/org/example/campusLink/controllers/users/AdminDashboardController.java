package org.example.campusLink.controllers.users;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.campusLink.controllers.reviews.AdminReviewsController;
import org.example.campusLink.entities.*;
import org.example.campusLink.services.users.AuthService;
import org.example.campusLink.services.users.UserService;
import org.example.campusLink.utils.AlertHelper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class AdminDashboardController {

    @FXML private StackPane rootPane;
    @FXML private Label lblAdminName;
    @FXML private Label lblAdminEmail;
    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalEtudiants;
    @FXML private Label lblTotalPrestataires;
    @FXML private Label lblActiveUsers;
    @FXML private Label lblInactiveUsers;
    @FXML private ToggleGroup userTypeFilter;
    @FXML private TextField txtSearch;
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colId;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colType;
    @FXML private TableColumn<User, String> colInfo;
    @FXML private TableColumn<User, String> colPhone;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, Void> colActions;

    private final UserService userService = new UserService();
    private final AuthService authService = new AuthService();
    private Admin currentAdmin;
    private final ObservableList<User> allUsers      = FXCollections.observableArrayList();
    private final ObservableList<User> filteredUsers = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupSearchFilter();
    }

    public void setAdmin(Admin admin) {
        this.currentAdmin = admin;
        loadAdminInfo();
        loadAllUsers();
    }

    private void loadAdminInfo() {
        if (currentAdmin != null) {
            lblAdminName.setText(currentAdmin.getName());
            lblAdminEmail.setText(currentAdmin.getEmail());
        }
    }

    // ── TABLE SETUP ───────────────────────────────────────────────────────────

    private void setupTableColumns() {
        colName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getName()));

        colEmail.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEmail()));

        colType.setCellValueFactory(c -> new SimpleStringProperty(""));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null
                        || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                User user = getTableRow().getItem();
                Label badge = new Label();
                if (user instanceof Etudiant) {
                    badge.setText("🎓 Étudiant");
                    badge.getStyleClass().add("type-etudiant");
                } else if (user instanceof Prestataire) {
                    badge.setText("💼 Prestataire");
                    badge.getStyleClass().add("type-prestataire");
                }
                setGraphic(badge);
            }
        });

        colInfo.setCellValueFactory(c -> {
            User user = c.getValue();
            if (user instanceof Etudiant e)
                return new SimpleStringProperty(
                        e.getUniversite() != null ? e.getUniversite() : "-");
            if (user instanceof Prestataire p)
                return new SimpleStringProperty(
                        p.getUniversite() != null ? p.getUniversite() : "-");
            return new SimpleStringProperty("-");
        });

        colPhone.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getPhone() != null
                                ? c.getValue().getPhone() : "-"));

        colStatus.setCellValueFactory(c -> new SimpleStringProperty(""));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null
                        || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                User user = getTableRow().getItem();
                Label badge = new Label(user.getStatus());
                switch (user.getStatus()) {
                    case "ACTIVE"   -> badge.getStyleClass().add("status-active");
                    case "INACTIVE" -> badge.getStyleClass().add("status-inactive");
                    case "BANNED"   -> badge.getStyleClass().add("status-banned");
                }
                setGraphic(badge);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnActivate   = new Button("✅");
            private final Button btnDeactivate = new Button("⏸️");
            private final Button btnBan        = new Button("🚫");

            {
                btnActivate.getStyleClass().add("success-button");
                btnActivate.setTooltip(new Tooltip("Activer"));
                btnActivate.setOnAction(e ->
                        handleActivateUser(getTableRow().getItem()));

                btnDeactivate.getStyleClass().add("warning-button");
                btnDeactivate.setTooltip(new Tooltip("Désactiver"));
                btnDeactivate.setOnAction(e ->
                        handleDeactivateUser(getTableRow().getItem()));

                btnBan.getStyleClass().add("danger-button");
                btnBan.setTooltip(new Tooltip("Bannir"));
                btnBan.setOnAction(e ->
                        handleBanUser(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null
                        || getTableRow().getItem() == null) {
                    setGraphic(null); return;
                }
                HBox actions = new HBox(5, btnActivate, btnDeactivate, btnBan);
                actions.setAlignment(Pos.CENTER);
                setGraphic(actions);
            }
        });
    }

    private void setupSearchFilter() {
        txtSearch.textProperty().addListener(
                (obs, old, val) -> filterUsers());
    }

    // ── DATA ──────────────────────────────────────────────────────────────────

    private void loadAllUsers() {
        try {
            List<User> users = userService.recuperer();
            allUsers.clear();
            allUsers.addAll(users.stream()
                    .filter(u -> u instanceof Etudiant || u instanceof Prestataire)
                    .collect(Collectors.toList()));
            filterUsers();
            updateStatistics();
        } catch (SQLException e) {
            showError("Erreur lors du chargement : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        loadAllUsers();
        showSuccess("Liste mise à jour !");
    }

    // ── FILTERS ───────────────────────────────────────────────────────────────

    @FXML
    private void handleFilterChange() {
        filterUsers();
    }

    private void filterUsers() {
        filteredUsers.clear();
        Toggle selected = userTypeFilter.getSelectedToggle();
        String filterText = selected != null
                ? ((ToggleButton) selected).getText() : "Tous";
        String searchText = txtSearch.getText().toLowerCase();

        filteredUsers.addAll(allUsers.stream()
                .filter(user -> {
                    boolean typeMatch = true;
                    if ("Étudiants".equals(filterText))
                        typeMatch = user instanceof Etudiant;
                    else if ("Prestataires".equals(filterText))
                        typeMatch = user instanceof Prestataire;

                    boolean searchMatch = searchText.isEmpty()
                            || user.getName().toLowerCase().contains(searchText)
                            || user.getEmail().toLowerCase().contains(searchText);

                    return typeMatch && searchMatch;
                })
                .collect(Collectors.toList()));

        tableUsers.setItems(filteredUsers);
        lblTotalUsers.setText("Total: " + filteredUsers.size() + " utilisateur(s)");
    }

    private void updateStatistics() {
        long etudiants    = allUsers.stream()
                .filter(u -> u instanceof Etudiant).count();
        long prestataires = allUsers.stream()
                .filter(u -> u instanceof Prestataire).count();
        long active       = allUsers.stream()
                .filter(u -> "ACTIVE".equals(u.getStatus())).count();
        long inactive     = allUsers.stream()
                .filter(u -> "INACTIVE".equals(u.getStatus())).count();

        lblTotalEtudiants.setText(String.valueOf(etudiants));
        lblTotalPrestataires.setText(String.valueOf(prestataires));
        lblActiveUsers.setText(String.valueOf(active));
        lblInactiveUsers.setText(String.valueOf(inactive));
    }

    // ── USER ACTIONS ──────────────────────────────────────────────────────────

    private void handleActivateUser(User user) {
        if (user == null) return;
        showConfirm(
                "Activer " + user.getName() + " ?",
                "Cet utilisateur pourra se connecter.",
                () -> {
                    try {
                        authService.activateAccount(user);
                        loadAllUsers();
                        showSuccess("Utilisateur activé avec succès !");
                    } catch (SQLException e) {
                        showError("Erreur : " + e.getMessage());
                    }
                }
        );
    }

    private void handleDeactivateUser(User user) {
        if (user == null) return;
        showConfirm(
                "Désactiver " + user.getName() + " ?",
                "Cet utilisateur ne pourra plus se connecter.",
                () -> {
                    try {
                        authService.deactivateAccount(user);
                        loadAllUsers();
                        showSuccess("Utilisateur désactivé !");
                    } catch (SQLException e) {
                        showError("Erreur : " + e.getMessage());
                    }
                }
        );
    }

    private void handleBanUser(User user) {
        if (user == null) return;
        showConfirm(
                "Bannir " + user.getName() + " ?",
                "⚠️ L'utilisateur sera banni définitivement.",
                () -> {
                    try {
                        authService.banAccount(user);
                        loadAllUsers();
                        showSuccess("Utilisateur banni !");
                    } catch (SQLException e) {
                        showError("Erreur : " + e.getMessage());
                    }
                }
        );
    }

    // ── NAVIGATION ────────────────────────────────────────────────────────────

    @FXML
    private void handleNavigateToDashboard() {}

    @FXML
    private void handleNavigateToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Views/users/AdminProfile.fxml"));
            Parent root = loader.load();
            AdminProfileController ctrl = loader.getController();
            ctrl.setAdmin(currentAdmin);
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement du profil");
        }
    }

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

    @FXML
    private void handleNavigateToAdminReviews(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Views/Reviews/AdminReviews.fxml"));
            Parent rootView = loader.load();
            AdminReviewsController controller = loader.getController();
            controller.setAdmin(currentAdmin);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(rootView));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible d'ouvrir AdminReviews.fxml");
        }
    }

    // ── ALERT HELPERS ─────────────────────────────────────────────────────────

    private void showSuccess(String message) {
        AlertHelper.showAlert(rootPane, message, AlertHelper.AlertType.SUCCESS);
    }

    private void showError(String message) {
        AlertHelper.showAlert(rootPane, message, AlertHelper.AlertType.ERROR);
    }

    private void showWarning(String message) {
        AlertHelper.showAlert(rootPane, message, AlertHelper.AlertType.WARNING);
    }

    private void showConfirm(String title, String message, Runnable onConfirm) {
        AlertHelper.showConfirm(rootPane, title, message, onConfirm);
    }
}