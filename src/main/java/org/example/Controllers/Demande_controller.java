package org.example.Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.campusLink.Services.Gestion_Demande;
import org.example.campusLink.entities.Demandes;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

/**
 * Controller for managing Demandes (Service Requests)
 * Displays cards with modern UI, status management, and filtering
 */
public class Demande_controller {

    @FXML
    private FlowPane demandesContainer;

    @FXML
    private ComboBox<String> filterStatusCombo;

    @FXML
    private Label statsEnAttente;

    @FXML
    private Label statsConfirmee;

    @FXML
    private Label statsRefusee;

    @FXML
    private Label statsTerminee;

    private Gestion_Demande demandeManager;
    private String currentFilter = "TOUS";
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private int currentStudentId = -1; // -1 means show all demandes (admin view)

    @FXML
    public void initialize() {
        System.out.println("Initializing Demande_controller...");
        try {
            demandeManager = new Gestion_Demande();

            // Initialize filter combo if present
            if (filterStatusCombo != null) {
                filterStatusCombo.getItems().addAll(
                        "TOUS", "EN_ATTENTE", "CONFIRMEE", "REFUSEE", "TERMINEE"
                );
                filterStatusCombo.setValue("TOUS");
                filterStatusCombo.setOnAction(e -> {
                    currentFilter = filterStatusCombo.getValue();
                    loadDemandes();
                });
            }

            loadDemandes();
            updateStatistics();
            System.out.println("Demande_controller initialized successfully");
        } catch (Exception e) {
            System.err.println("Error initializing Demande_controller: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les demandes: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /* ================= LOAD DEMANDES ================= */

    private void loadDemandes() {
        try {
            System.out.println("Loading demandes with filter: " + currentFilter);
            List<Demandes> demandesList = demandeManager.afficherDemandes(currentFilter);
            demandesContainer.getChildren().clear();

            if (demandesList == null || demandesList.isEmpty()) {
                System.out.println("No demandes available");
                Label emptyLabel = new Label("Aucune demande disponible.\nCliquez sur '+ Créer une demande' pour commencer.");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280; -fx-padding: 60px; -fx-text-alignment: center;");
                demandesContainer.getChildren().add(emptyLabel);
                return;
            }

            System.out.println("Loaded " + demandesList.size() + " demandes");
            for (Demandes d : demandesList) {
                demandesContainer.getChildren().add(createModernDemandeCard(d));
            }

        } catch (Exception e) {
            System.err.println("Error loading demandes: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du chargement des demandes: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /* ================= MODERN CARD ================= */

    private VBox createModernDemandeCard(Demandes d) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setPrefWidth(380);
        card.setMinHeight(240);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);"
        );

        // ===== HEADER: Service Name + Status Badge =====
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label serviceName = new Label(d.getServiceName() != null ? d.getServiceName() : "Service inconnu");
        serviceName.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        serviceName.setWrapText(true);
        serviceName.setMaxWidth(240);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Status Badge with dynamic colors
        Label statusBadge = createStatusBadge(d.getStatus());

        header.getChildren().addAll(serviceName, spacer, statusBadge);

        // ===== CATEGORY + PRICE =====
        HBox categoryPrice = new HBox(15);
        categoryPrice.setAlignment(Pos.CENTER_LEFT);

        Label category = new Label("📚 " + (d.getCategoryName() != null ? d.getCategoryName() : "Non catégorisé"));
        category.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        Label price = new Label(d.getFormattedPrice());
        price.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #10b981;");

        Region categoryPriceSpacer = new Region();
        HBox.setHgrow(categoryPriceSpacer, javafx.scene.layout.Priority.ALWAYS);

        categoryPrice.getChildren().addAll(category, categoryPriceSpacer, price);

        // ===== STUDENT INFO =====
        Label studentInfo = new Label("👤 " + (d.getStudentName() != null ? d.getStudentName() : "Étudiant inconnu"));
        studentInfo.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");

        // ===== MESSAGE =====
        Label message = new Label(d.getMessage() != null && !d.getMessage().isEmpty()
                ? d.getMessage()
                : "Pas de message");
        message.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280; -fx-wrap-text: true;");
        message.setWrapText(true);
        message.setMaxWidth(340);
        message.setMaxHeight(50);

        // ===== INFO: Date =====
        String dateText = "📅 Créée le: " +
                (d.getCreatedAt() != null ? dateFormat.format(d.getCreatedAt()) : "Date inconnue");

        if (d.getRequestedDate() != null) {
            dateText = "📅 Souhaitée: " + dateFormat.format(d.getRequestedDate());
        }

        Label date = new Label(dateText);
        date.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

        // ===== FOOTER: Actions =====
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        // Action buttons based on status
        if ("EN_ATTENTE".equals(d.getStatus())) {
            Button confirmBtn = createActionButton("✓", "#10b981", "#059669", "Confirmer");
            confirmBtn.setOnAction(e -> updateStatus(d, "CONFIRMEE"));

            Button rejectBtn = createActionButton("✗", "#ef4444", "#dc2626", "Refuser");
            rejectBtn.setOnAction(e -> updateStatus(d, "REFUSEE"));

            Button editBtn = createActionButton("✏", "#3b82f6", "#2563eb", "Modifier");
            editBtn.setOnAction(e -> editDemande(d));

            footer.getChildren().addAll(confirmBtn, rejectBtn, editBtn);

        } else if ("CONFIRMEE".equals(d.getStatus())) {
            Button completeBtn = createActionButton("✓", "#8b5cf6", "#7c3aed", "Terminer");
            completeBtn.setOnAction(e -> updateStatus(d, "TERMINEE"));

            Button editBtn = createActionButton("✏", "#3b82f6", "#2563eb", "Modifier");
            editBtn.setOnAction(e -> editDemande(d));

            footer.getChildren().addAll(completeBtn, editBtn);
        }

        // Always show delete button
        Button deleteBtn = createActionButton("🗑", "#6b7280", "#374151", "Supprimer");
        deleteBtn.setOnAction(e -> deleteDemande(d));
        footer.getChildren().add(deleteBtn);

        // ===== ASSEMBLER LA CARTE =====
        card.getChildren().addAll(header, categoryPrice, studentInfo, message, date, footer);

        // Hover effect
        String borderColor = getStatusBorderColor(d.getStatus());

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, " + borderColor.replace("#", "rgba(") + ", 0.2), 15, 0, 0, 4);" +
                        "-fx-cursor: hand;"
        ));

        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);"
        ));

        return card;
    }

    /* ================= HELPER METHODS ================= */

    private Label createStatusBadge(String status) {
        Label badge = new Label(getStatusLabel(status));
        String style = switch (status) {
            case "EN_ATTENTE" -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;";
            case "CONFIRMEE" -> "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af;";
            case "REFUSEE" -> "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;";
            case "TERMINEE" -> "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;";
            default -> "-fx-background-color: #f3f4f6; -fx-text-fill: #374151;";
        };

        badge.setStyle(style +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 4 10;" +
                "-fx-background-radius: 12;"
        );
        return badge;
    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "EN_ATTENTE" -> "En attente";
            case "CONFIRMEE" -> "Confirmée";
            case "REFUSEE" -> "Refusée";
            case "TERMINEE" -> "Terminée";
            default -> status;
        };
    }

    private String getStatusBorderColor(String status) {
        return switch (status) {
            case "EN_ATTENTE" -> "#f59e0b";
            case "CONFIRMEE" -> "#3b82f6";
            case "REFUSEE" -> "#ef4444";
            case "TERMINEE" -> "#10b981";
            default -> "#6b7280";
        };
    }

    private Button createActionButton(String text, String normalColor, String hoverColor, String tooltip) {
        Button btn = new Button(text);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle(
                "-fx-background-color: " + normalColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 6 12;" +
                        "-fx-background-radius: 6;" +
                        "-fx-cursor: hand;" +
                        "-fx-min-width: 35px;"
        );

        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + hoverColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 6 12;" +
                        "-fx-background-radius: 6;" +
                        "-fx-cursor: hand;" +
                        "-fx-min-width: 35px;"
        ));

        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + normalColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 6 12;" +
                        "-fx-background-radius: 6;" +
                        "-fx-cursor: hand;" +
                        "-fx-min-width: 35px;"
        ));

        return btn;
    }

    /* ================= ACTIONS ================= */

    private void updateStatus(Demandes demande, String newStatus) {
        System.out.println("Updating demande #" + demande.getId() + " to status: " + newStatus);

        try {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirmation");
            confirmAlert.setHeaderText("Changer le statut");
            confirmAlert.setContentText("Voulez-vous vraiment passer cette demande à '" + getStatusLabel(newStatus) + "' ?");

            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        demandeManager.modifierStatut(demande.getId(), newStatus);
                        loadDemandes();
                        updateStatistics();
                        showAlert("Succès", "Statut modifié avec succès!", Alert.AlertType.INFORMATION);
                    } catch (IllegalStateException e) {
                        showAlert("Erreur", e.getMessage(), Alert.AlertType.WARNING);
                    } catch (Exception e) {
                        System.err.println("Error updating status: " + e.getMessage());
                        e.printStackTrace();
                        showAlert("Erreur", "Impossible de modifier le statut: " + e.getMessage(), Alert.AlertType.ERROR);
                    }
                }
            });

        } catch (Exception e) {
            System.err.println("Error in status update: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la modification: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void editDemande(Demandes demande) {
        System.out.println("Editing demande: " + demande.getId());

        try {
            // Dialog for message
            TextInputDialog messageDialog = new TextInputDialog(demande.getMessage());
            messageDialog.setTitle("Modifier la demande");
            messageDialog.setHeaderText("Modifier le message");
            messageDialog.setContentText("Message:");

            Optional<String> messageResult = messageDialog.showAndWait();

            if (messageResult.isPresent()) {
                demande.setMessage(messageResult.get().trim());

                // Dialog for proposed price
                TextInputDialog priceDialog = new TextInputDialog(
                        demande.getProposedPrice() != null ? demande.getProposedPrice().toString() : ""
                );
                priceDialog.setTitle("Modifier la demande");
                priceDialog.setHeaderText("Prix proposé (optionnel)");
                priceDialog.setContentText("Prix:");

                Optional<String> priceResult = priceDialog.showAndWait();

                if (priceResult.isPresent() && !priceResult.get().trim().isEmpty()) {
                    try {
                        BigDecimal newPrice = new BigDecimal(priceResult.get().trim());
                        demandeManager.modifierPrixPropose(demande.getId(), newPrice);
                    } catch (NumberFormatException e) {
                        showAlert("Erreur", "Le prix doit être un nombre valide", Alert.AlertType.ERROR);
                        return;
                    }
                }

                loadDemandes();
                showAlert("Succès", "Demande modifiée avec succès!", Alert.AlertType.INFORMATION);
            }

        } catch (Exception e) {
            System.err.println("Error editing demande: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de modifier la demande: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void deleteDemande(Demandes demande) {
        System.out.println("Deleting demande #" + demande.getId());

        try {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirmation");
            confirmAlert.setHeaderText("Supprimer la demande");
            confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer cette demande ?\n" +
                    "Service: " + demande.getServiceName() + "\nÉtudiant: " + demande.getStudentName());

            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        demandeManager.supprimerDemande(demande.getId());
                        loadDemandes();
                        updateStatistics();
                        showAlert("Succès", "Demande supprimée avec succès", Alert.AlertType.INFORMATION);
                    } catch (Exception e) {
                        System.err.println("Error deleting demande: " + e.getMessage());
                        e.printStackTrace();
                        showAlert("Erreur", "Impossible de supprimer la demande: " + e.getMessage(), Alert.AlertType.ERROR);
                    }
                }
            });

        } catch (Exception e) {
            System.err.println("Error in delete confirmation: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /* ================= STATISTICS ================= */

    private void updateStatistics() {
        try {
            java.util.Map<String, Integer> stats = demandeManager.getStatistiquesDemandes();

            if (statsEnAttente != null) {
                statsEnAttente.setText(String.valueOf(stats.getOrDefault("EN_ATTENTE", 0)));
            }
            if (statsConfirmee != null) {
                statsConfirmee.setText(String.valueOf(stats.getOrDefault("CONFIRMEE", 0)));
            }
            if (statsRefusee != null) {
                statsRefusee.setText(String.valueOf(stats.getOrDefault("REFUSEE", 0)));
            }
            if (statsTerminee != null) {
                statsTerminee.setText(String.valueOf(stats.getOrDefault("TERMINEE", 0)));
            }
        } catch (Exception e) {
            System.err.println("Error updating statistics: " + e.getMessage());
        }
    }

    /* ================= NAVIGATE CREATE ================= */

    @FXML
    private void goToCreateDemande() {
        System.out.println("Navigating to create demande...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Create_Demande.fxml"));
            Scene scene = new Scene(loader.load());

            // Pass student ID to the controller
            CreateDemande_controller controller = loader.getController();
            if (currentStudentId > 0) {
                controller.setCurrentStudentId(currentStudentId);
            }

            Stage stage = (Stage) demandesContainer.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Créer une demande");

        } catch (Exception e) {
            System.err.println("Error navigating to create demande: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la page de création: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Navigate back to student services page
     */
    @FXML
    private void goBackToServices() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/student.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) demandesContainer.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Recherche de Services");
        } catch (Exception e) {
            System.err.println("Error navigating back to services: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de revenir: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /* ================= UTILITIES ================= */

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /* ================= SETTERS ================= */

    /**
     * Set student ID to filter demandes by student
     * Called from Student_controller when navigating to "Mes Demandes"
     */
    public void setStudentId(int studentId) {
        this.currentStudentId = studentId;
        // Reload demandes for this student
        loadDemandesForStudent();
    }

    /**
     * Load demandes for a specific student
     */
    private void loadDemandesForStudent() {
        try {
            System.out.println("Loading demandes for student ID: " + currentStudentId);
            List<Demandes> demandesList = demandeManager.afficherDemandesParEtudiant(currentStudentId);
            demandesContainer.getChildren().clear();

            if (demandesList == null || demandesList.isEmpty()) {
                System.out.println("No demandes for this student");
                Label emptyLabel = new Label("Vous n'avez aucune demande.\nCliquez sur '+ Créer une demande' pour commencer.");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280; -fx-padding: 60px; -fx-text-alignment: center;");
                demandesContainer.getChildren().add(emptyLabel);
                return;
            }

            System.out.println("Loaded " + demandesList.size() + " demandes for student");
            for (Demandes d : demandesList) {
                demandesContainer.getChildren().add(createModernDemandeCard(d));
            }

        } catch (Exception e) {
            System.err.println("Error loading student demandes: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du chargement de vos demandes: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}