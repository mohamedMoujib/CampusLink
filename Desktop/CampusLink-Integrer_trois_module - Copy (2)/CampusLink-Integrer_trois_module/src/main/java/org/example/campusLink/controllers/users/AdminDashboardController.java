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
import org.example.campusLink.entities.*;
import org.example.campusLink.services.users.AuthService;
import org.example.campusLink.services.users.UserService;
import org.example.campusLink.utils.AlertHelper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Controller pour le dashboard admin
 */
public class AdminDashboardController {

    // Root pane for alerts
    @FXML private StackPane rootPane;

    // Header labels
    @FXML private Label lblAdminName;
    @FXML private Label lblAdminEmail;

    // Statistics
    @FXML private Label lblTotalUsers;
    @FXML private Label lblTotalEtudiants;
    @FXML private Label lblTotalPrestataires;
    @FXML private Label lblActiveUsers;
    @FXML private Label lblInactiveUsers;

    // Filters
    @FXML private ToggleGroup userTypeFilter;
    @FXML private TextField txtSearch;

    // TableView
    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colId;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colType;
    @FXML private TableColumn<User, String> colInfo;
    @FXML private TableColumn<User, String> colPhone;
    @FXML private TableColumn<User, String> colStatus;
    @FXML private TableColumn<User, Void> colActions;

    private UserService userService;
    private AuthService authService;
    private Admin currentAdmin;
    private ObservableList<User> allUsers;
    private ObservableList<User> filteredUsers;

    public AdminDashboardController() {
        this.userService = new UserService();
        this.authService = new AuthService();
        this.allUsers = FXCollections.observableArrayList();
        this.filteredUsers = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        System.out.println("🔧 Initialisation AdminDashboardController...");
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

    // ==================== TABLE SETUP ====================

    private void setupTableColumns() {


        // Name Column
        colName.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName()));

        // Email Column
        colEmail.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getEmail()));

        // Type Column with Badge
        colType.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
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
            }
        });
        colType.setCellValueFactory(cellData -> new SimpleStringProperty(""));

        // Info Column (Université ou Service Type)
        colInfo.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            if (user instanceof Etudiant) {
                Etudiant etudiant = (Etudiant) user;
                return new SimpleStringProperty(etudiant.getUniversite() != null ? etudiant.getUniversite() : "-");
            } else if (user instanceof Prestataire) {
                Prestataire prestataire = (Prestataire) user;
                return new SimpleStringProperty(prestataire.getUniversite() != null ? prestataire.getUniversite() : "-");
            }
            return new SimpleStringProperty("-");
        });

        // Phone Column
        colPhone.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPhone() != null ? cellData.getValue().getPhone() : "-"));

        // Status Column with Badge
        colStatus.setCellFactory(column -> new TableCell<User, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    User user = getTableRow().getItem();
                    Label badge = new Label(user.getStatus());

                    switch (user.getStatus()) {
                        case "ACTIVE":
                            badge.getStyleClass().add("status-active");
                            break;
                        case "INACTIVE":
                            badge.getStyleClass().add("status-inactive");
                            break;
                        case "BANNED":
                            badge.getStyleClass().add("status-banned");
                            break;
                    }

                    setGraphic(badge);
                }
            }
        });
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(""));

        // Actions Column with Buttons
        colActions.setCellFactory(column -> new TableCell<User, Void>() {
            private final Button btnActivate = new Button("✅");
            private final Button btnDeactivate = new Button("⏸️");
            private final Button btnBan = new Button("🚫");

            {
                btnActivate.getStyleClass().add("success-button");
                btnActivate.setTooltip(new Tooltip("Activer"));
                btnActivate.setOnAction(e -> handleActivateUser(getTableRow().getItem()));

                btnDeactivate.getStyleClass().add("warning-button");
                btnDeactivate.setTooltip(new Tooltip("Désactiver"));
                btnDeactivate.setOnAction(e -> handleDeactivateUser(getTableRow().getItem()));

                btnBan.getStyleClass().add("danger-button");
                btnBan.setTooltip(new Tooltip("Bannir"));
                btnBan.setOnAction(e -> handleBanUser(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    HBox actions = new HBox(5);
                    actions.setAlignment(Pos.CENTER);
                    actions.getChildren().addAll(btnActivate, btnDeactivate, btnBan);
                    setGraphic(actions);
                }
            }
        });
    }

    private void setupSearchFilter() {
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filterUsers();
        });
    }

    // ==================== DATA LOADING ====================

    private void loadAllUsers() {
        try {
            List<User> users = userService.recuperer();

            // Filtrer pour ne garder que les étudiants et prestataires
            allUsers.clear();
            allUsers.addAll(users.stream()
                    .filter(u -> u instanceof Etudiant || u instanceof Prestataire)
                    .collect(Collectors.toList()));

            filterUsers();
            updateStatistics();

        } catch (SQLException e) {
            showError("Erreur lors du chargement des utilisateurs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        loadAllUsers();
        showSuccess("Liste mise à jour!");
    }

    // ==================== FILTERING ====================

    @FXML
    private void handleFilterChange() {
        filterUsers();
    }

    private void filterUsers() {
        filteredUsers.clear();

        // Get selected filter
        Toggle selectedToggle = userTypeFilter.getSelectedToggle();
        String filterText = selectedToggle != null ? ((ToggleButton) selectedToggle).getText() : "Tous";
        String searchText = txtSearch.getText().toLowerCase();

        filteredUsers.addAll(allUsers.stream()
                .filter(user -> {
                    // Filter by type
                    boolean typeMatch = true;
                    if ("Étudiants".equals(filterText)) {
                        typeMatch = user instanceof Etudiant;
                    } else if ("Prestataires".equals(filterText)) {
                        typeMatch = user instanceof Prestataire;
                    }

                    // Filter by search text
                    boolean searchMatch = searchText.isEmpty() ||
                            user.getName().toLowerCase().contains(searchText) ||
                            user.getEmail().toLowerCase().contains(searchText);

                    return typeMatch && searchMatch;
                })
                .collect(Collectors.toList()));

        tableUsers.setItems(filteredUsers);
        lblTotalUsers.setText("Total: " + filteredUsers.size() + " utilisateur(s)");
    }

    // ==================== STATISTICS ====================

    private void updateStatistics() {
        long totalEtudiants = allUsers.stream()
                .filter(u -> u instanceof Etudiant)
                .count();

        long totalPrestataires = allUsers.stream()
                .filter(u -> u instanceof Prestataire)
                .count();

        long activeUsers = allUsers.stream()
                .filter(u -> "ACTIVE".equals(u.getStatus()))
                .count();

        long inactiveUsers = allUsers.stream()
                .filter(u -> "INACTIVE".equals(u.getStatus()))
                .count();

        lblTotalEtudiants.setText(String.valueOf(totalEtudiants));
        lblTotalPrestataires.setText(String.valueOf(totalPrestataires));
        lblActiveUsers.setText(String.valueOf(activeUsers));
        lblInactiveUsers.setText(String.valueOf(inactiveUsers));
    }

    // ==================== USER ACTIONS ====================

    private void handleActivateUser(User user) {
        if (user == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Activer l'utilisateur");
        confirm.setHeaderText("Activer " + user.getName() + " ?");
        confirm.setContentText("Cet utilisateur pourra se connecter.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    authService.activateAccount(user);
                    loadAllUsers();
                    showSuccess("Utilisateur activé avec succès!");
                } catch (SQLException e) {
                    showError("Erreur: " + e.getMessage());
                }
            }
        });
    }

    private void handleDeactivateUser(User user) {
        if (user == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Désactiver l'utilisateur");
        confirm.setHeaderText("Désactiver " + user.getName() + " ?");
        confirm.setContentText("Cet utilisateur ne pourra plus se connecter.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    authService.deactivateAccount(user);
                    loadAllUsers();
                    showSuccess("Utilisateur désactivé!");
                } catch (SQLException e) {
                    showError("Erreur: " + e.getMessage());
                }
            }
        });
    }

    private void handleBanUser(User user) {
        if (user == null) return;

        Alert confirm = new Alert(Alert.AlertType.WARNING);
        confirm.setTitle("Bannir l'utilisateur");
        confirm.setHeaderText("⚠️ Bannir " + user.getName() + " ?");
        confirm.setContentText("Cette action est sérieuse. L'utilisateur sera banni définitivement.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    authService.banAccount(user);
                    loadAllUsers();
                    showSuccess("Utilisateur banni!");
                } catch (SQLException e) {
                    showError("Erreur: " + e.getMessage());
                }
            }
        });
    }

    // ==================== NAVIGATION ====================

    @FXML
    private void handleNavigateToDashboard() {
        // Déjà sur le dashboard, ne rien faire
        System.out.println("📍 Déjà sur le dashboard");
    }

    @FXML
    private void handleNavigateToProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/users/AdminProfile.fxml"));
            Parent root = loader.load();

            AdminProfileController controller = loader.getController();
            controller.setAdmin(currentAdmin);

            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(new Scene(root));

            System.out.println("✅ Navigation vers AdminProfile");

        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement du profil");
        }
    }

    // ==================== LOGOUT ====================

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

    private void showWarning(String message) {
        if (rootPane != null) {
            AlertHelper.showAlert(rootPane, message, AlertHelper.AlertType.WARNING);
        }
    }
    @FXML
    private void handleNavigateToAdminReviews(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/Views/Reviews/AdminReviews.fxml"));

            Parent rootView = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(rootView));
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Navigation error");
            alert.setContentText("Impossible d'ouvrir AdminReviews.fxml");
            alert.showAndWait();
        }
    }
}