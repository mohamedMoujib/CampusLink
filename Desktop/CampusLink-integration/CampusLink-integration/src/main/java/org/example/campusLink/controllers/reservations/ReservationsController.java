package org.example.campusLink.controllers.reservations;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import org.example.campusLink.controllers.users.MainLayoutController;
import org.example.campusLink.entities.Reservation;
import org.example.campusLink.entities.ReservationStatus;
import org.example.campusLink.entities.User;
import org.example.campusLink.services.reservations.ReservationService;

import java.util.List;

public class ReservationsController {

    @FXML private ListView<Reservation> listReservations;
    @FXML private Label lblTotalCount;
    @FXML private Label lblPendingCount;
    @FXML private Label lblConfirmedCount;
    @FXML private Label lblTotalRevenu;
    @FXML private Button btnAll;
    @FXML private Button btnUpcoming;
    @FXML private Button btnPast;

    private final ReservationService reservationService = new ReservationService();
    private final ObservableList<Reservation> data = FXCollections.observableArrayList();

    private MainLayoutController mainLayoutController;
    private User currentUser;
    private int currentPrestataireId = -1;

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
                this::onDelete,
                this::onContact
        ));
    }

    public void setUser(User user) {
        this.currentUser = user;
        this.currentPrestataireId = user.getId();
        reloadAll();
        updateStats();
    }

    public void setMainLayoutController(MainLayoutController ctrl) {
        this.mainLayoutController = ctrl;
    }

    // ── LOAD ─────────────────────────────────────────────────────────────────

    private void reloadAll() {
        try {
            // getAllWithDetails() returns ALL reservations — filter by provider
            List<Reservation> all = reservationService.getAllWithDetails();
            List<Reservation> mine = all.stream()
                    .filter(r -> r.getProviderId() == currentPrestataireId)
                    .toList();
            data.setAll(mine);
        } catch (Exception e) {
            e.printStackTrace();
            alertError("Erreur chargement : " + e.getMessage());
        }
    }

    private void updateStats() {
        try {
            List<Reservation> all = reservationService.getAllWithDetails();
            List<Reservation> mine = all.stream()
                    .filter(r -> r.getProviderId() == currentPrestataireId)
                    .toList();

            long pending   = mine.stream()
                    .filter(r -> r.getStatus() == ReservationStatus.PENDING).count();
            long confirmed = mine.stream()
                    .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED).count();
            double revenu  = mine.stream()

                    .mapToDouble(r -> r.getPrice() != null ? r.getPrice().doubleValue() : 0)
                    .sum();

            lblTotalCount.setText(String.valueOf(mine.size()));
            lblPendingCount.setText(String.valueOf(pending));
            lblConfirmedCount.setText(String.valueOf(confirmed));
            lblTotalRevenu.setText(String.format("%.0f DT", revenu));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void onOpenCalendar() {
        if (mainLayoutController == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Views/reservations/calendar_view.fxml"));
            Parent view = loader.load();

            CalendarViewController ctrl = loader.getController();
            ctrl.setMainLayoutController(mainLayoutController);
            if (currentUser != null) ctrl.setUser(currentUser);
            else ctrl.setProviderId(currentPrestataireId);

            mainLayoutController.loadContent(view);
        } catch (Exception e) {
            e.printStackTrace();
            alertError("Erreur ouverture calendrier : " + e.getMessage());
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
            // Uses existing method in ReservationService
            List<Reservation> list =
                    reservationService.getUpcomingReservationsForProvider(currentPrestataireId);
            data.setAll(list);
        } catch (Exception e) { alertError(e.getMessage()); }
    }

    @FXML
    private void onFilterPast() {
        setActiveFilter(btnPast);
        try {
            List<Reservation> all = reservationService.getAllWithDetails();
            List<Reservation> past = all.stream()
                    .filter(r -> r.getProviderId() == currentPrestataireId
                            && r.getDate().isBefore(java.time.LocalDateTime.now()))
                    .toList();
            data.setAll(past);
        } catch (Exception e) { alertError(e.getMessage()); }
    }

    private void setActiveFilter(Button active) {
        btnAll.setStyle(STYLE_INACTIVE);
        btnUpcoming.setStyle(STYLE_INACTIVE);
        btnPast.setStyle(STYLE_INACTIVE);
        active.setStyle(STYLE_ACTIVE);
    }

    // ── REVENUS ───────────────────────────────────────────────────────────────

    @FXML
    private void onOpenRevenus() {
        if (mainLayoutController == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Views/reservations/revenus.fxml"));
            Parent view = loader.load();

            RevenusController ctrl = loader.getController();
            ctrl.setMainLayoutController(mainLayoutController);
            if (currentUser != null) ctrl.setUser(currentUser);
            else ctrl.setProviderId(currentPrestataireId);

            mainLayoutController.loadContent(view);
        } catch (Exception e) {
            e.printStackTrace();
            alertError("Erreur ouverture revenus : " + e.getMessage());
        }
    }

    // ── CARD ACTIONS ──────────────────────────────────────────────────────────

    private void onUpdateStatus(Reservation r, ReservationStatus status) {
        try {
            reservationService.updateStatus(r.getId(), status);
            reloadAll();
            updateStats();
        } catch (Exception e) { alertError(e.getMessage()); }
    }

    private void onEdit(Reservation r) {
        // Prestataires only accept/refuse — editing is student's role
        new Alert(Alert.AlertType.INFORMATION,
                "Utilisez les boutons Accepter/Refuser pour gérer cette réservation.")
                .showAndWait();
    }

    private void onDelete(Reservation r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer la réservation #" + r.getId() + " ?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    reservationService.delete(r.getId());
                    reloadAll();
                    updateStats();
                } catch (Exception e) { alertError(e.getMessage()); }
            }
        });
    }

    private void onContact(Reservation r) {
        new Alert(Alert.AlertType.INFORMATION,
                "Messagerie pour la réservation #" + r.getId() + " — à intégrer.")
                .showAndWait();
    }

    private void onCancel(Reservation r) {
        onUpdateStatus(r, ReservationStatus.CANCELLED);
    }

    private void alertError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}