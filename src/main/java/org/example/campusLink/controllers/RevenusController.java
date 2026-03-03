package org.example.campusLink.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import org.example.campusLink.entities.Reservation;
import org.example.campusLink.services.ReservationService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class RevenusController {

    @FXML private LineChart<String, Number> lineChart;
    @FXML private NumberAxis yAxis;
    @FXML private Label totalRevenuLabel;
    @FXML private ComboBox<Integer> yearCombo;
    @FXML private Button refreshBtn;

    private final ReservationService reservationService = new ReservationService();
    private int providerId = 2; // À remplacer par l'ID du prestataire connecté

    @FXML
    public void initialize() {
        // Initialiser le combo des années (2026 et années disponibles)
        yearCombo.setItems(FXCollections.observableArrayList(2026, 2025, 2024));
        yearCombo.getSelectionModel().select(0); // sélectionner 2026 par défaut

        // Charger les données initiales
        loadData();

        // Action du bouton actualiser
        refreshBtn.setOnAction(e -> loadData());

        // Ajouter des tooltips au graphique
        addTooltips();
    }

    public void setProviderId(int providerId) {
        this.providerId = providerId;
        loadData();
    }

    private void loadData() {
        try {
            List<Reservation> reservations = reservationService.getConfirmedReservationsByProvider(providerId);
            Integer selectedYear = yearCombo.getValue();
            if (selectedYear == null) selectedYear = 2026;

            LocalDate start = LocalDate.of(selectedYear, 1, 1);
            LocalDate end = start.plusYears(1).minusDays(1);

            Map<YearMonth, Double> revenusParMois = new TreeMap<>();
            double total = 0.0;

            for (Reservation r : reservations) {
                LocalDate date = r.getDate().toLocalDate();
                if (date.isBefore(start) || date.isAfter(end)) continue;
                YearMonth ym = YearMonth.from(date);
                double price = r.getPrice() != null ? r.getPrice().doubleValue() : 0.0;
                revenusParMois.merge(ym, price, Double::sum);
                total += price;
            }

            totalRevenuLabel.setText(String.format("Revenu total %d : %.2f DT", selectedYear, total));

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Revenus");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
            for (Map.Entry<YearMonth, Double> entry : revenusParMois.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey().format(formatter), entry.getValue()));
            }

            lineChart.getData().clear();
            if (!series.getData().isEmpty()) {
                lineChart.getData().add(series);
            } else {
                // Aucune donnée : afficher un message dans le graphique (optionnel)
                lineChart.setTitle("Aucune donnée pour l'année " + selectedYear);
            }

            // Ajuster automatiquement l'axe Y
            yAxis.setAutoRanging(true);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les données : " + e.getMessage());
        }
    }

    private void addTooltips() {
        lineChart.getData().forEach(series -> {
            for (XYChart.Data<String, Number> data : series.getData()) {
                Tooltip tooltip = new Tooltip(data.getXValue() + " : " + data.getYValue() + " DT");
                Tooltip.install(data.getNode(), tooltip);
                // Au survol, agrandir le point
                data.getNode().setOnMouseEntered(e -> data.getNode().setStyle("-fx-scale: 1.5;"));
                data.getNode().setOnMouseExited(e -> data.getNode().setStyle("-fx-scale: 1;"));
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}