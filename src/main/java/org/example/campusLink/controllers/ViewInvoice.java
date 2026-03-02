package org.example.campusLink.controllers;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.campusLink.entities.Invoices;
import org.example.campusLink.services.ServiceInvoices;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ViewInvoice {

    private ServiceInvoices invoiceService;

    @FXML private TableView<Invoices> invoiceTable;
    @FXML private TableColumn<Invoices, Integer> paymentIdColumn;
    @FXML private TableColumn<Invoices, java.sql.Timestamp> dateColumn;
    @FXML private TableColumn<Invoices, String> detailsColumn;

    @FXML private TextField searchField;
    @FXML private DatePicker startDatePicker;
    @FXML private Label resultCountLabel;
    @FXML private Label messageLabel;

    private FilteredList<Invoices> filteredData;

    @FXML
    public void initialize() {

        invoiceService = new ServiceInvoices();

        paymentIdColumn.setCellValueFactory(new PropertyValueFactory<>("paymentId"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("invoiceDate"));
        detailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));

        loadInvoices();

        // Double click preview
        invoiceTable.setRowFactory(tv -> {
            TableRow<Invoices> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    Invoices clickedInvoice = row.getItem();
                    showInvoicePreview(clickedInvoice);
                }
            });

            return row;
        });
    }

    private void loadInvoices() {
        try {
            List<Invoices> invoices = invoiceService.recuperer();

            filteredData = new FilteredList<>(
                    FXCollections.observableArrayList(invoices),
                    p -> true
            );

            SortedList<Invoices> sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(invoiceTable.comparatorProperty());

            invoiceTable.setItems(sortedData);

            setupFilters();
            updateResultCount();

        } catch (SQLException e) {
            messageLabel.setText("Erreur lors du chargement des factures");
            e.printStackTrace();
        }
    }

    private void setupFilters() {

        ChangeListener<Object> listener = (obs, oldVal, newVal) -> applyFilters();

        searchField.textProperty().addListener(listener);
        startDatePicker.valueProperty().addListener(listener);
    }

    private void applyFilters() {

        filteredData.setPredicate(invoice -> {

            String searchText = searchField.getText();
            if (searchText != null && !searchText.isBlank()) {
                String lower = searchText.toLowerCase();

                if (!String.valueOf(invoice.getPaymentId()).contains(lower)
                        && (invoice.getDetails() == null ||
                        !invoice.getDetails().toLowerCase().contains(lower))) {
                    return false;
                }
            }

            LocalDate selectedDate = startDatePicker.getValue();
            if (selectedDate != null) {
                LocalDate invoiceDate = invoice.getInvoiceDate()
                        .toLocalDateTime()
                        .toLocalDate();

                if (!invoiceDate.equals(selectedDate)) {
                    return false;
                }
            }

            return true;
        });

        updateResultCount();
    }

    private void updateResultCount() {
        resultCountLabel.setText("Résultats : " + filteredData.size());
    }

    private void showInvoicePreview(Invoices invoice) {

        Stage stage = new Stage();
        stage.setTitle("Aperçu de la facture");

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20; -fx-background-color: white;");

        Label title = new Label("📄 Détails de la facture");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label paymentId = new Label("Payment ID : " + invoice.getPaymentId());
        Label date = new Label("Date : " + invoice.getInvoiceDate());

        Label details = new Label("Détails : " + invoice.getDetails());
        details.setWrapText(true);
        details.setMaxWidth(600);

        Button closeBtn = new Button("Fermer");
        closeBtn.setOnAction(e -> stage.close());

        content.getChildren().addAll(title, paymentId, date, details, closeBtn);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(700, 250);

        Scene scene = new Scene(scrollPane);
        stage.setScene(scene);

        stage.initModality(Modality.APPLICATION_MODAL);
        stage.showAndWait();
    }

    @FXML
    private void deleteInvoice(ActionEvent event) {

        Invoices selected = invoiceTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            messageLabel.setText("Veuillez sélectionner une facture.");
            messageLabel.setStyle("-fx-text-fill: #ef4444;");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la facture");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette facture ?");

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                invoiceService.supprimer(selected);
                invoiceTable.getItems().remove(selected);

                messageLabel.setText("Facture supprimée avec succès.");
                messageLabel.setStyle("-fx-text-fill: #22c55e;");

            } catch (Exception e) {
                messageLabel.setText("Erreur lors de la suppression.");
                messageLabel.setStyle("-fx-text-fill: #ef4444;");
            }
        }
    }

    @FXML
    private void goToPayments(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/View/Payment.fxml")
            );
            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}