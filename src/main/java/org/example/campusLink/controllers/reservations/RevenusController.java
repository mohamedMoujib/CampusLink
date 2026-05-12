package org.example.campusLink.controllers.reservations;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import org.example.campusLink.controllers.users.MainLayoutController;
import org.example.campusLink.entities.Reservation;
import org.example.campusLink.entities.User;
import org.example.campusLink.services.reservations.ReservationService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RevenusController {

    @FXML private LineChart<String, Number> lineChart;
    @FXML private NumberAxis yAxis;
    @FXML private Label totalRevenuLabel;
    @FXML private ComboBox<Integer> yearCombo;
    @FXML private Button refreshBtn;

    private MainLayoutController mainLayoutController;
    private final ReservationService reservationService = new ReservationService();
    private int providerId = -1;
    private User currentUser;

    public void setMainLayoutController(MainLayoutController ctrl) {
        this.mainLayoutController = ctrl;
    }

    public void setProviderId(int providerId) {
        this.providerId = providerId;
        loadData();
    }

    public void setUser(User user) {
        this.currentUser = user;
        this.providerId = user.getId();
        loadData();
    }

    @FXML
    public void initialize() {
        yearCombo.setItems(FXCollections.observableArrayList(2026, 2025, 2024));
        yearCombo.getSelectionModel().select(0);
        refreshBtn.setOnAction(e -> loadData());
        yearCombo.setOnAction(e -> loadData());
    }

    private void loadData() {
        if (providerId < 0) return;

        try {
            List<Reservation> reservations =
                    reservationService.getConfirmedReservationsByProvider(providerId);

            Integer selectedYear = yearCombo.getValue();
            if (selectedYear == null) selectedYear = 2026;

            LocalDate start = LocalDate.of(selectedYear, 1, 1);
            LocalDate end   = start.plusYears(1).minusDays(1);

            Map<YearMonth, Double> revenusParMois = new TreeMap<>();
            double total = 0.0;

            for (Reservation r : reservations) {
                LocalDate date = r.getDate().toLocalDate();
                if (date.isBefore(start) || date.isAfter(end)) continue;
                YearMonth ym  = YearMonth.from(date);
                double price  = r.getPrice() != null ? r.getPrice().doubleValue() : 0.0;
                revenusParMois.merge(ym, price, Double::sum);
                total += price;
            }

            totalRevenuLabel.setText(
                    String.format("Revenu total %d : %.2f DT", selectedYear, total));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Revenus");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
            for (Map.Entry<YearMonth, Double> entry : revenusParMois.entrySet()) {
                series.getData().add(
                        new XYChart.Data<>(entry.getKey().format(formatter), entry.getValue()));
            }

            lineChart.getData().clear();
            if (!series.getData().isEmpty()) {
                lineChart.getData().add(series);
                addTooltips();
            } else {
                lineChart.setTitle("Aucune donnée pour " + selectedYear);
            }

            yAxis.setAutoRanging(true);

        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Impossible de charger les données : " + e.getMessage())
                    .showAndWait();
        }
    }

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

    private void addTooltips() {
        lineChart.getData().forEach(series -> {
            for (XYChart.Data<String, Number> data : series.getData()) {
                if (data.getNode() == null) continue;
                Tooltip tooltip = new Tooltip(
                        data.getXValue() + " : " + data.getYValue() + " DT");
                Tooltip.install(data.getNode(), tooltip);
                data.getNode().setOnMouseEntered(
                        e -> data.getNode().setStyle("-fx-scale-x:1.5; -fx-scale-y:1.5;"));
                data.getNode().setOnMouseExited(
                        e -> data.getNode().setStyle("-fx-scale-x:1; -fx-scale-y:1;"));
            }
        });
    }
}