package org.example.campusLink.controllers.paiements;

import javafx.animation.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.example.campusLink.entities.Invoices;
import org.example.campusLink.entities.User;
import org.example.campusLink.services.paiements.ServiceInvoices;
import org.example.campusLink.utils.AlertHelper;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ViewInvoice {

    @FXML private StackPane rootPane;

    // Stats
    @FXML private Label lblTotalFactures;
    @FXML private Label resultCountLabel;

    // Filters
    @FXML private TextField searchField;
    @FXML private DatePicker startDatePicker;

    // Table
    @FXML private TableView<Invoices> invoiceTable;
    @FXML private TableColumn<Invoices, Integer> paymentIdColumn;
    @FXML private TableColumn<Invoices, java.sql.Timestamp> dateColumn;
    @FXML private TableColumn<Invoices, String> detailsColumn;

    // Preview overlay
    @FXML private VBox previewOverlay;
    @FXML private VBox previewCard;
    @FXML private Label previewPaymentId;
    @FXML private Label previewDate;
    @FXML private Label previewDetails;

    private final ServiceInvoices invoiceService = new ServiceInvoices();
    private FilteredList<Invoices> filteredData;
    private User currentUser;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupDoubleClick();
        setupFilters();
    }

    public void setUser(User user) {
        this.currentUser = user;
        loadInvoices();
    }

    // ── TABLE ─────────────────────────────────────────────────────────────────

    private void setupTableColumns() {
        paymentIdColumn.setCellValueFactory(
                new PropertyValueFactory<>("paymentId"));
        dateColumn.setCellValueFactory(
                new PropertyValueFactory<>("invoiceDate"));
        detailsColumn.setCellValueFactory(
                new PropertyValueFactory<>("details"));

        // Style rows alternately
        invoiceTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Invoices item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    setStyle(getIndex() % 2 == 0
                            ? "-fx-background-color:white;"
                            : "-fx-background-color:#fafafa;");
                }
            }
        });
    }

    private void setupDoubleClick() {
        invoiceTable.setRowFactory(tv -> {
            TableRow<Invoices> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty() && e.getClickCount() == 2)
                    showPreview(row.getItem());
            });
            return row;
        });
    }

    // ── DATA ──────────────────────────────────────────────────────────────────

    private void loadInvoices() {
        try {
            List<Invoices> invoices = invoiceService.recuperer();

            filteredData = new FilteredList<>(
                    FXCollections.observableArrayList(invoices), p -> true);

            SortedList<Invoices> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(
                    invoiceTable.comparatorProperty());

            invoiceTable.setItems(sortedData);
            updateCounts();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement des factures.");
        }
    }

    @FXML
    private void onRefresh() {
        loadInvoices();
        showSuccess("Liste actualisée !");
    }

    // ── FILTERS ───────────────────────────────────────────────────────────────

    private void setupFilters() {
        ChangeListener<Object> listener = (obs, old, val) -> applyFilters();
        searchField.textProperty().addListener(listener);
        startDatePicker.valueProperty().addListener(listener);
    }

    private void applyFilters() {
        if (filteredData == null) return;

        filteredData.setPredicate(invoice -> {
            // Search filter
            String search = searchField.getText();
            if (search != null && !search.isBlank()) {
                String lower = search.toLowerCase();
                boolean matchId = String.valueOf(
                        invoice.getPaymentId()).contains(lower);
                boolean matchDetails = invoice.getDetails() != null
                        && invoice.getDetails().toLowerCase().contains(lower);
                if (!matchId && !matchDetails) return false;
            }

            // Date filter
            LocalDate selected = startDatePicker.getValue();
            if (selected != null) {
                LocalDate invoiceDate = invoice.getInvoiceDate()
                        .toLocalDateTime().toLocalDate();
                if (!invoiceDate.equals(selected)) return false;
            }

            return true;
        });

        updateCounts();
    }

    @FXML
    private void onClearFilters() {
        searchField.clear();
        startDatePicker.setValue(null);
    }

    private void updateCounts() {
        int count = filteredData != null ? filteredData.size() : 0;
        resultCountLabel.setText(count + " facture(s)");
        lblTotalFactures.setText(String.valueOf(count));
    }

    // ── ACTIONS ───────────────────────────────────────────────────────────────

    @FXML
    private void deleteInvoice() {
        Invoices selected = invoiceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Veuillez sélectionner une facture.");
            return;
        }

        AlertHelper.showConfirm(
                rootPane,
                "Supprimer la facture #" + selected.getPaymentId() + " ?",
                "Cette action est irréversible.",
                () -> {
                    try {
                        invoiceService.supprimer(selected);
                        loadInvoices();
                        showSuccess("Facture supprimée avec succès !");
                    } catch (Exception e) {
                        showError("Erreur lors de la suppression.");
                    }
                }
        );
    }

    @FXML
    private void previewInvoice() {
        Invoices selected = invoiceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Veuillez sélectionner une facture.");
            return;
        }
        showPreview(selected);
    }

    // ── PREVIEW OVERLAY ───────────────────────────────────────────────────────

    private void showPreview(Invoices invoice) {
        previewPaymentId.setText(String.valueOf(invoice.getPaymentId()));
        previewDate.setText(invoice.getInvoiceDate() != null
                ? invoice.getInvoiceDate().toString() : "—");
        previewDetails.setText(invoice.getDetails() != null
                ? invoice.getDetails() : "—");

        previewOverlay.setVisible(true);
        previewOverlay.setManaged(true);

        // Animate card in
        ScaleTransition scale = new ScaleTransition(
                Duration.millis(250), previewCard);
        scale.setFromX(0.85); scale.setFromY(0.85);
        scale.setToX(1.0);    scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(
                Duration.millis(200), previewOverlay);
        fade.setFromValue(0); fade.setToValue(1);

        new ParallelTransition(scale, fade).play();
    }

    @FXML
    private void onClosePreview() {
        FadeTransition fade = new FadeTransition(
                Duration.millis(180), previewOverlay);
        fade.setFromValue(1); fade.setToValue(0);
        fade.setOnFinished(e -> {
            previewOverlay.setVisible(false);
            previewOverlay.setManaged(false);
        });
        fade.play();
    }

    // ── ALERT HELPERS ─────────────────────────────────────────────────────────

    private void showSuccess(String msg) {
        AlertHelper.showAlert(rootPane, msg, AlertHelper.AlertType.SUCCESS);
    }

    private void showError(String msg) {
        AlertHelper.showAlert(rootPane, msg, AlertHelper.AlertType.ERROR);
    }

    private void showWarning(String msg) {
        AlertHelper.showAlert(rootPane, msg, AlertHelper.AlertType.WARNING);
    }
}