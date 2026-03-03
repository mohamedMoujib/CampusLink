package org.example.campusLink.controllers.reservations;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.campusLink.controllers.paiements.PaymentController;
import org.example.campusLink.controllers.users.MainLayoutController;
import org.example.campusLink.entities.Reservation;
import org.example.campusLink.entities.ReservationStatus;
import org.example.campusLink.entities.User;
import org.example.campusLink.services.reservations.ReservationService;

import java.time.LocalDateTime;
import java.util.List;

public class EtudiantReservationsController {

    @FXML private ListView<Reservation> listReservations;
    @FXML private Label lblTotal;
    @FXML private Label lblPending;
    @FXML private Label lblConfirmed;
    @FXML private Label lblCancelled;
    @FXML private Button btnAll;
    @FXML private Button btnUpcoming;
    @FXML private Button btnPast;

    private final ReservationService reservationService = new ReservationService();
    private final ObservableList<Reservation> data = FXCollections.observableArrayList();

    private MainLayoutController mainLayoutController;
    private User currentUser;

    private static final String STYLE_ACTIVE =
            "-fx-background-color:#4f46e5; -fx-text-fill:white;" +
                    "-fx-background-radius:20; -fx-padding:6 16; -fx-cursor:hand;";
    private static final String STYLE_INACTIVE =
            "-fx-background-color:#f3f4f6; -fx-text-fill:#374151;" +
                    "-fx-background-radius:20; -fx-padding:6 16; -fx-cursor:hand;";

    @FXML
    public void initialize() {
        listReservations.setItems(data);
        listReservations.setCellFactory(lv -> new ReservationCardCell(
                this::onEdit,
                this::onCancel,
                this::onContact,
                this::onCoordination   // ✅ new coordination callback
        ));
    }

    public void setUser(User user) {
        this.currentUser = user;
        reloadAll();
        updateStats();
    }

    public void setMainLayoutController(MainLayoutController ctrl) {
        this.mainLayoutController = ctrl;
    }

    // ── LOAD ─────────────────────────────────────────────────────────────────

    private void reloadAll() {
        try {
            List<Reservation> list =
                    reservationService.getStudentReservations(currentUser.getId());
            data.setAll(list);
        } catch (Exception e) {
            e.printStackTrace();
            alertError("Erreur chargement : " + e.getMessage());
        }
    }

    private void updateStats() {
        try {
            List<Reservation> all =
                    reservationService.getStudentReservations(currentUser.getId());
            long pending   = all.stream()
                    .filter(r -> r.getStatus() == ReservationStatus.PENDING).count();
            long confirmed = all.stream()
                    .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED).count();
            long cancelled = all.stream()
                    .filter(r -> r.getStatus() == ReservationStatus.CANCELLED).count();

            lblTotal.setText(String.valueOf(all.size()));
            lblPending.setText(String.valueOf(pending));
            lblConfirmed.setText(String.valueOf(confirmed));
            lblCancelled.setText(String.valueOf(cancelled));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── FILTERS ───────────────────────────────────────────────────────────────

    @FXML
    private void onFilterAll() {
        setActiveFilter(btnAll);
        reloadAll();
    }

    @FXML
    private void onFilterUpcoming() {
        setActiveFilter(btnUpcoming);
        try {
            List<Reservation> all =
                    reservationService.getStudentReservations(currentUser.getId());
            data.setAll(all.stream()
                    .filter(r -> r.getDate().isAfter(LocalDateTime.now()))
                    .toList());
        } catch (Exception e) { alertError(e.getMessage()); }
    }

    @FXML
    private void onFilterPast() {
        setActiveFilter(btnPast);
        try {
            List<Reservation> all =
                    reservationService.getStudentReservations(currentUser.getId());
            data.setAll(all.stream()
                    .filter(r -> r.getDate().isBefore(LocalDateTime.now()))
                    .toList());
        } catch (Exception e) { alertError(e.getMessage()); }
    }

    private void setActiveFilter(Button active) {
        btnAll.setStyle(STYLE_INACTIVE);
        btnUpcoming.setStyle(STYLE_INACTIVE);
        btnPast.setStyle(STYLE_INACTIVE);
        active.setStyle(STYLE_ACTIVE);
    }

    // ── NEW RESERVATION ───────────────────────────────────────────────────────

    @FXML
    private void onNewReservation() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Views/reservations/reservation_create.fxml"));
            Parent root = loader.load();
            ReservationCreateController ctrl = loader.getController();
            ctrl.initData(currentUser.getId());
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Nouvelle réservation");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            reloadAll();
            updateStats();
        } catch (Exception e) {
            e.printStackTrace();
            alertError("Erreur ouverture formulaire : " + e.getMessage());
        }
    }

    // ── CARD ACTIONS ──────────────────────────────────────────────────────────

    private void onEdit(Reservation r) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Views/reservations/reservation_create.fxml"));
            Parent root = loader.load();
            ReservationCreateController ctrl = loader.getController();
            ctrl.initForEdit(r);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier la réservation");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            reloadAll();
            updateStats();
        } catch (Exception e) {
            e.printStackTrace();
            alertError("Erreur modification : " + e.getMessage());
        }
    }

    private void onCancel(Reservation r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Annuler la réservation \"" + r.getServiceTitle() + "\" ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    reservationService.updateStatus(r.getId(), ReservationStatus.CANCELLED);
                    reloadAll();
                    updateStats();
                } catch (Exception e) { alertError(e.getMessage()); }
            }
        });
    }

    private void onContact(Reservation r) {
        if (mainLayoutController == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Views/reservations/messages.fxml"));
            Parent view = loader.load();
            MessageController ctrl = loader.getController();
            ctrl.setUser(currentUser);
            mainLayoutController.loadContent(view);
            ctrl.openConversationWithUser(r.getProviderId());
        } catch (Exception e) {
            e.printStackTrace();
            alertError("Erreur messagerie : " + e.getMessage());
        }
    }

    // ✅ Opens PaymentController as modal, passes reservation data
    private void onCoordination(Reservation r) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Views/paiements/Payment.fxml"));
            Parent root = loader.load();

            PaymentController ctrl = loader.getController();
            ctrl.initData(r, currentUser);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Coordination & Paiement");
            stage.setScene(new Scene(root, 1000, 750));
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            alertError("Erreur coordination : " + e.getMessage());
        }
    }

    private void alertError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}