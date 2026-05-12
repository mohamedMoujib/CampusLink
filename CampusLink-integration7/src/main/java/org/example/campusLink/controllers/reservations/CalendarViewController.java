package org.example.campusLink.controllers.reservations;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import org.example.campusLink.controllers.users.MainLayoutController;
import org.example.campusLink.entities.Reservation;
import org.example.campusLink.entities.User;
import org.example.campusLink.services.reservations.ReservationService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private User currentUser;
    private MainLayoutController mainLayoutController;

    private final ReservationService reservationService = new ReservationService();
    private final Map<LocalDate, List<Reservation>> reservationsByDate = new HashMap<>();

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

    public void setUser(User user) {
        this.currentUser = user;
        this.providerId = user.getId();
        updateCalendar();
    }

    public void setProviderId(int providerId) {
        this.providerId = providerId;
        updateCalendar();
    }

    public void setMainLayoutController(MainLayoutController ctrl) {
        this.mainLayoutController = ctrl;
    }

    // ── BACK ──────────────────────────────────────────────────────────────────

    @FXML
    private void onBack() {
        if (mainLayoutController == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Views/reservations/reservations.fxml"));
            Parent view = loader.load();

            ReservationsController ctrl = loader.getController();
            ctrl.setMainLayoutController(mainLayoutController);
            if (currentUser != null) ctrl.setUser(currentUser);

            mainLayoutController.loadContent(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── CALENDAR ──────────────────────────────────────────────────────────────

    private void updateCalendar() {
        if (monthYearLabel == null) return; // not yet initialized
        monthYearLabel.setText(
                currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        loadReservationsForMonth();
        buildCalendarGrid();
    }

    private void loadReservationsForMonth() {
        reservationsByDate.clear();
        try {
            List<Reservation> reservations =
                    reservationService.getProviderReservationsForMonth(providerId, currentMonth);
            for (Reservation r : reservations) {
                LocalDate date = r.getDate().toLocalDate();
                reservationsByDate
                        .computeIfAbsent(date, k -> new ArrayList<>())
                        .add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void buildCalendarGrid() {
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();

        // 7 equal columns
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            calendarGrid.getColumnConstraints().add(cc);
        }

        // Day headers
        String[] dayNames = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(dayNames[i]);
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setStyle("-fx-font-weight:bold; -fx-text-fill:#6b7280;" +
                    "-fx-font-size:12px; -fx-alignment:center;");
            calendarGrid.add(lbl, i, 0);
        }

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int firstDayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // 1=Mon
        int daysInMonth    = currentMonth.lengthOfMonth();

        int row = 1, col = firstDayOfWeek - 1;
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            VBox cell = createDayCell(date);
            calendarGrid.add(cell, col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }
    }

    private VBox createDayCell(LocalDate date) {
        VBox cell = new VBox(4);
        cell.setMinHeight(80);

        boolean isToday = date.equals(LocalDate.now());
        boolean hasReservations = reservationsByDate.containsKey(date);

        cell.setStyle(
                "-fx-background-radius:8; -fx-padding:6; -fx-cursor:hand; " +
                        (isToday
                                ? "-fx-background-color:#ede9fe; -fx-border-color:#6366f1; -fx-border-radius:8; -fx-border-width:2;"
                                : hasReservations
                                ? "-fx-background-color:#f0fdf4; -fx-border-color:#d1fae5; -fx-border-radius:8; -fx-border-width:1;"
                                : "-fx-background-color:#f9fafb; -fx-border-color:#f3f4f6; -fx-border-radius:8; -fx-border-width:1;")
        );

        cell.setOnMouseClicked(e -> showDayDetails(date));
        cell.setOnMouseEntered(e -> cell.setStyle(cell.getStyle() +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),6,0,0,2);"));
        cell.setOnMouseExited(e -> buildCalendarGrid()); // redraw resets hover

        Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumber.setStyle("-fx-font-weight:bold; -fx-font-size:13px; " +
                (isToday ? "-fx-text-fill:#6366f1;" : "-fx-text-fill:#374151;"));
        cell.getChildren().add(dayNumber);

        List<Reservation> dayRes = reservationsByDate.get(date);
        if (dayRes != null && !dayRes.isEmpty()) {
            Label badge = new Label(dayRes.size() + " rés.");
            badge.setStyle("-fx-background-color:#10b981; -fx-text-fill:white;" +
                    "-fx-background-radius:10; -fx-padding:1 6;" +
                    "-fx-font-size:10px;");
            cell.getChildren().add(badge);

            for (int i = 0; i < Math.min(dayRes.size(), 2); i++) {
                Reservation r = dayRes.get(i);
                String name = r.getStudentName() != null
                        ? r.getStudentName().split(" ")[0]
                        : "Étudiant";
                Label preview = new Label("• " + name);
                preview.setStyle("-fx-font-size:10px; -fx-text-fill:#6b7280;");
                preview.setMaxWidth(Double.MAX_VALUE);
                cell.getChildren().add(preview);
            }
            if (dayRes.size() > 2) {
                Label more = new Label("+" + (dayRes.size() - 2) + " autres");
                more.setStyle("-fx-font-size:10px; -fx-text-fill:#9ca3af;");
                cell.getChildren().add(more);
            }
        }

        return cell;
    }

    private void showDayDetails(LocalDate date) {
        List<Reservation> reservations = reservationsByDate.get(date);
        if (reservations == null || reservations.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Aucune réservation pour le " +
                            date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .showAndWait();
            return;
        }

        Popup popup = new Popup();
        VBox content = new VBox(12);
        content.setStyle("-fx-background-color:white; -fx-background-radius:16;" +
                "-fx-padding:20; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.2),20,0,0,8);");
        content.setMinWidth(320);
        content.setMaxHeight(400);

        Label title = new Label("📅 " +
                date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        title.setStyle("-fx-font-weight:bold; -fx-font-size:15px; -fx-text-fill:#1e293b;");

        ListView<Reservation> listView = new ListView<>();
        listView.setPrefHeight(220);
        listView.setItems(FXCollections.observableArrayList(reservations));
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Reservation r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setText(null); return; }
                String time = r.getDate().format(DateTimeFormatter.ofPattern("HH:mm"));
                setText("🕐 " + time + "  |  " + r.getStudentName()
                        + "  —  " + r.getServiceTitle());
            }
        });

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color:#4f46e5; -fx-text-fill:white;" +
                "-fx-background-radius:20; -fx-padding:8 20; -fx-cursor:hand;");
        closeBtn.setOnAction(e -> popup.hide());

        content.getChildren().addAll(title, listView, closeBtn);
        popup.getContent().add(content);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        popup.show(calendarGrid.getScene().getWindow());
    }
}