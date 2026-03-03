package org.example.campusLink.controllers;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.example.campusLink.entities.Reservation;
import org.example.campusLink.entities.ReservationStatus;
import org.example.campusLink.services.NotificationService;
import org.example.campusLink.services.ReservationService;
import org.example.campusLink.controllers.CalendarViewController;
import java.io.FileInputStream;
import java.util.List;

public class ReservationsController {

    @FXML private ToggleGroup navGroup;
    @FXML private ListView<Reservation> listReservations;
    @FXML private ImageView imgLogo;
    @FXML private Label lblUserName;
    @FXML private Label lblUserEmail;
    @FXML private Button btnNotification; // assurez-vous que le FXML a fx:id="btnNotification"

    private final ReservationService reservationService = new ReservationService();
    private final ObservableList<Reservation> data = FXCollections.observableArrayList();
    private int currentStudentId = 1; // à remplacer par l'utilisateur connecté
    private int currentProviderId = 2;
    @FXML
    public void initialize() {
        try {
            imgLogo.setImage(new Image(new FileInputStream("C:\\Users\\Ben_Younes\\workspace\\JAVA\\campuslink2\\src\\main\\resources\\images\\p1.png")));
        } catch (Exception ignored) {}

        lblUserName.setText("Thomas Martin");
        lblUserEmail.setText("thomas.martin@univ.fr");

        listReservations.setItems(data);
        listReservations.setCellFactory(lv -> new ReservationCardCell(
                this::onEditReservation,
                this::onDeleteReservation,
                this::onContactReservation,
                this::onCancelReservation,
                this::onUpdateStatus
        ));

        // Lier le texte du bouton au compteur de notifications
        btnNotification.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    int count = NotificationService.getInstance().unreadCountProperty().get();
                    return count > 0 ? "🔔 (" + count + ")" : "🔔";
                }, NotificationService.getInstance().unreadCountProperty())
        );

        // Action au clic sur la cloche
        btnNotification.setOnAction(e -> showNotifications());

        reloadAll();
    }

    private void reloadAll() {
        try {
            List<Reservation> list = reservationService.getAllWithDetails();
            data.setAll(list);
        } catch (Exception e) {
            e.printStackTrace();
            alertError("Erreur chargement réservations : " + e.getMessage());
        }
    }

    // Méthode pour afficher les notifications dans une popup
    private void showNotifications() {
        NotificationService.getInstance().markAllAsRead();

        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        VBox container = new VBox();
        container.getStyleClass().add("notification-popup");
        container.setMaxWidth(350);
        container.setMaxHeight(400);

        // Header
        HBox header = new HBox();
        header.getStyleClass().add("notification-header");
        header.setAlignment(Pos.CENTER_LEFT);
        Label headerLabel = new Label("Notifications");
        headerLabel.getStyleClass().add("notification-header-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button clearAll = new Button("Tout marquer comme lu");
        clearAll.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 11;");
        clearAll.setOnAction(e -> {
            NotificationService.getInstance().markAllAsRead();
            popup.hide();
        });
        header.getChildren().addAll(headerLabel, spacer, clearAll);

        // Liste des notifications
        ListView<String> listView = new ListView<>();
        listView.getStyleClass().add("notification-list");
        listView.setPrefHeight(300);

        ObservableList<String> notifs = NotificationService.getInstance().getNotifications();
        listView.setItems(notifs);
        listView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    VBox itemBox = new VBox(4);
                    itemBox.getStyleClass().add("notification-item");
                    Label message = new Label(item);
                    message.setWrapText(true);
                    Label time = new Label("À l'instant");
                    time.getStyleClass().add("time");
                    itemBox.getChildren().addAll(message, time);
                    setGraphic(itemBox);
                }
            }
        });

        container.getChildren().addAll(header, listView);

        popup.getContent().add(container);

        // Position sous le bouton notification
        popup.show(btnNotification,
                btnNotification.localToScreen(0, btnNotification.getHeight()).getX(),
                btnNotification.localToScreen(0, btnNotification.getHeight()).getY());
    }
    @FXML public void onFilterAll() { reloadAll(); }
    @FXML public void onFilterUpcoming() {
        try { data.setAll(reservationService.getUpcomingWithDetails()); }
        catch (Exception e) { alertError(e.getMessage()); }
    }
    @FXML public void onFilterPast() {
        try { data.setAll(reservationService.getPastWithDetails()); }
        catch (Exception e) { alertError(e.getMessage()); }
    }

    @FXML public void onAddReservation() { openReservationCreateModal(); }

    private void onDeleteReservation(Reservation r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText("Supprimer cette réservation ?");
        ButtonType yes = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yes, no);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == yes) {
                try {
                    reservationService.delete(r.getId());
                    reloadAll();
                } catch (Exception e) {
                    alertError("Erreur suppression : " + e.getMessage());
                }
            }
        });
    }

    private void onContactReservation(Reservation r) {
        int otherUserId = r.getProviderId();
        if (otherUserId <= 0) {
            alertError("Impossible de contacter : prestataire inconnu (ID = " + otherUserId + ").");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Messages.fxml"));
            Scene scene = new Scene(loader.load());

            MessageController controller = loader.getController();
            controller.initData(r, currentStudentId, otherUserId);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Messagerie");
            stage.setScene(scene);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            alertError("Erreur ouverture chat: " + e.getMessage());
        }
    }

    private void onCancelReservation(Reservation r) {
        try {
            reservationService.updateStatus(r.getId(), ReservationStatus.CANCELLED);
            reloadAll();
        } catch (Exception e) {
            alertError(e.getMessage());
        }
    }

    private void onUpdateStatus(Reservation r, ReservationStatus status) {
        try {
            reservationService.updateStatus(r.getId(), status);
            reloadAll();
        } catch (Exception e) {
            alertError(e.getMessage());
        }
    }

    private void openReservationCreateModal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/reservation_create.fxml"));
            Scene scene = new Scene(loader.load());

            ReservationCreateController controller = loader.getController();
            controller.initData(currentStudentId);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Réserver une session");
            stage.setScene(scene);
            stage.showAndWait();

            reloadAll();
        } catch (Exception e) {
            e.printStackTrace();
            alertError("Erreur ouverture popup: " + e.getMessage());
        }
    }


    private void onEditReservation(Reservation r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/reservation_create.fxml"));
            Scene scene = new Scene(loader.load());

            ReservationCreateController controller = loader.getController();
            controller.initForEdit(r);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier une réservation");
            stage.setScene(scene);
            stage.showAndWait();

            reloadAll();
        } catch (Exception e) {
            e.printStackTrace();
            alertError("Erreur ouverture popup: " + e.getMessage());
        }
    }

    private void alertError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    @FXML public void onNavServices() { new Alert(Alert.AlertType.INFORMATION, "Onglet Services (à faire)").showAndWait(); }
    @FXML
    public void onNavRevenus() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/revenus.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setTitle("Revenus");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            alertError("Erreur ouverture revenus: " + e.getMessage());
        }
    }
    @FXML private Button btnCalendar;

    @FXML
    public void onOpenCalendar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/calendar_view.fxml"));
            Scene scene = new Scene(loader.load());

            // Remplacer CalendarController par CalendarViewController
            CalendarViewController controller = loader.getController();
            controller.setProviderId(currentProviderId);

            Stage stage = new Stage();
            stage.setTitle("Calendrier des réservations");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            alertError("Erreur ouverture calendrier: " + e.getMessage());
        }
    }


    @FXML public void onNavAvis() { new Alert(Alert.AlertType.INFORMATION, "Onglet Avis (à faire)").showAndWait(); }
    @FXML public void onNavProfil() { new Alert(Alert.AlertType.INFORMATION, "Onglet Profil (à faire)").showAndWait(); }
}