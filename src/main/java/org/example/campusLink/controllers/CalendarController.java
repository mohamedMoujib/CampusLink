package org.example.campusLink.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.campusLink.entities.Reservation;
import org.example.campusLink.services.ReservationService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CalendarController {

    @FXML private DatePicker datePicker;
    @FXML private ListView<Reservation> reservationListView;
    @FXML private Label selectedDateLabel;

    private final ReservationService reservationService = new ReservationService();
    private final ObservableList<Reservation> reservations = FXCollections.observableArrayList();
    private int providerId;

    @FXML
    public void initialize() {
        datePicker.setValue(LocalDate.now());

        // Marquer les jours avec réservations
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (!empty && providerId > 0) {
                    try {
                        List<Reservation> res = reservationService.getProviderReservationsByDate(providerId, date);
                        if (!res.isEmpty()) {
                            this.setStyle("-fx-background-color: #e0f7fa; -fx-border-color: #00acc1;");
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // Charger les réservations quand la date change
        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null && providerId > 0) {
                loadReservationsForDate(newDate);
            }
        });

        reservationListView.setItems(reservations);
        reservationListView.setCellFactory(lv -> new ListCell<Reservation>() {
            @Override
            protected void updateItem(Reservation r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) {
                    setText(null);
                } else {
                    String time = r.getDate().format(DateTimeFormatter.ofPattern("HH:mm"));
                    String text = r.getStudentName() + " - " + r.getServiceTitle() + " - " + time + " à " + r.getLocalisation();
                    setText(text);
                }
            }
        });
    }

    public void setProviderId(int providerId) {
        this.providerId = providerId;
        if (datePicker != null && datePicker.getValue() != null) {
            loadReservationsForDate(datePicker.getValue());
        }
    }

    private void loadReservationsForDate(LocalDate date) {
        try {
            List<Reservation> list = reservationService.getProviderReservationsByDate(providerId, date);
            reservations.setAll(list);
            selectedDateLabel.setText("Réservations du " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les réservations.");
        }
    }

    @FXML
    public void onClose() {
        Stage stage = (Stage) datePicker.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}