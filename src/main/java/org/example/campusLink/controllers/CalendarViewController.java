package org.example.campusLink.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.example.campusLink.entities.Reservation;
import org.example.campusLink.services.ReservationService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarViewController {

    @FXML private Label monthYearLabel;
    @FXML private GridPane calendarGrid;
    @FXML private Button prevMonthBtn;
    @FXML private Button nextMonthBtn;

    private YearMonth currentMonth;
    private int providerId;
    private final ReservationService reservationService = new ReservationService();
    private Map<LocalDate, List<Reservation>> reservationsByDate = new HashMap<>();

    @FXML
    public void initialize() {
        currentMonth = YearMonth.now();
        updateCalendar();

        prevMonthBtn.setOnAction(e -> {
            currentMonth = currentMonth.minusMonths(1);
            updateCalendar();
        });

        nextMonthBtn.setOnAction(e -> {
            currentMonth = currentMonth.plusMonths(1);
            updateCalendar();
        });
    }

    public void setProviderId(int providerId) {
        this.providerId = providerId;
        updateCalendar();
    }

    private void updateCalendar() {
        monthYearLabel.setText(currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        loadReservationsForMonth();
        buildCalendarGrid();
    }

    private void loadReservationsForMonth() {
        reservationsByDate.clear();
        try {
            List<Reservation> reservations = reservationService.getProviderReservationsForMonth(providerId, currentMonth);
            for (Reservation r : reservations) {
                LocalDate date = r.getDate().toLocalDate();
                reservationsByDate.computeIfAbsent(date, k -> new java.util.ArrayList<>()).add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les réservations.");
        }
    }

    private void buildCalendarGrid() {
        calendarGrid.getChildren().clear();

        // En-têtes des jours
        String[] dayNames = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(dayNames[i]);
            dayLabel.getStyleClass().add("calendar-weekday");
            calendarGrid.add(dayLabel, i, 0);
        }

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int firstDayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // 1 = Lundi
        int daysInMonth = currentMonth.lengthOfMonth();

        int row = 1;
        int col = firstDayOfWeek - 1;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            VBox cell = createDayCell(date);
            calendarGrid.add(cell, col, row);

            col++;
            if (col == 7) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createDayCell(LocalDate date) {
        VBox cell = new VBox(5);
        cell.getStyleClass().add("calendar-cell");
        if (date.equals(LocalDate.now())) {
            cell.getStyleClass().add("calendar-cell-today");
        }
        cell.setOnMouseClicked(e -> showDayDetails(date));

        // Numéro du jour
        Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumber.getStyleClass().add("calendar-cell-number");
        cell.getChildren().add(dayNumber);

        // Badge si réservations
        List<Reservation> dayReservations = reservationsByDate.get(date);
        if (dayReservations != null && !dayReservations.isEmpty()) {
            Label badge = new Label(dayReservations.size() + " réservation" + (dayReservations.size() > 1 ? "s" : ""));
            badge.getStyleClass().add("calendar-cell-event-badge");
            cell.getChildren().add(badge);

            // Aperçu des 2 premiers noms
            for (int i = 0; i < Math.min(dayReservations.size(), 2); i++) {
                Reservation r = dayReservations.get(i);
                Label preview = new Label(r.getStudentName().split(" ")[0] + " - " + r.getServiceTitle());
                preview.getStyleClass().add("calendar-event-preview");
                preview.setMaxWidth(90);
                preview.setWrapText(true);
                cell.getChildren().add(preview);
            }
            if (dayReservations.size() > 2) {
                Label more = new Label("...");
                more.getStyleClass().add("calendar-event-preview");
                cell.getChildren().add(more);
            }
        }

        return cell;
    }

    private void showDayDetails(LocalDate date) {
        List<Reservation> reservations = reservationsByDate.get(date);
        if (reservations == null || reservations.isEmpty()) {
            showAlert("Aucune réservation", "Aucune réservation pour cette date.");
            return;
        }

        // Popup élégante
        Popup popup = new Popup();
        VBox content = new VBox(12);
        content.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 8);");
        content.setMaxWidth(350);
        content.setMaxHeight(400);

        Label title = new Label("Réservations du " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #1e293b;");

        ListView<Reservation> listView = new ListView<>();
        listView.setPrefHeight(250);
        listView.setItems(FXCollections.observableArrayList(reservations));
        listView.setCellFactory(lv -> new ListCell<Reservation>() {
            @Override
            protected void updateItem(Reservation r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) {
                    setText(null);
                } else {
                    String time = r.getDate().format(DateTimeFormatter.ofPattern("HH:mm"));
                    String detail = r.getStudentName() + " - " + r.getServiceTitle() + " à " + time;
                    setText(detail);
                }
            }
        });

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #4F46E5; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> popup.hide());

        content.getChildren().addAll(title, listView, closeBtn);
        popup.getContent().add(content);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        // Centrer sur la fenêtre
        popup.show(calendarGrid.getScene().getWindow());
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void onClose() {
        Stage stage = (Stage) calendarGrid.getScene().getWindow();
        stage.close();
    }
}