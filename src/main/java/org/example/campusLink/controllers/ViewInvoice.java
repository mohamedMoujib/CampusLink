package org.example.campusLink.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.example.campusLink.entities.Invoices;
import org.example.campusLink.services.ServiceInvoices;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ViewInvoice{

    private ServiceInvoices invoiceService;

    @FXML
    private TableView<Invoices> invoiceTable;


    @FXML
    private TableColumn<Invoices, Integer> paymentIdColumn;

    @FXML
    private TableColumn<Invoices, java.sql.Timestamp> dateColumn;

    @FXML
    private TableColumn<Invoices, String> detailsColumn;

    @FXML
    private Label messageLabel;

    private final ServiceInvoices serviceInvoices = new ServiceInvoices();

    @FXML
    public void initialize() {
        invoiceService = new ServiceInvoices();
        paymentIdColumn.setCellValueFactory(new PropertyValueFactory<>("paymentId"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("invoiceDate"));
        detailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));

        loadInvoices();
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
                serviceInvoices.supprimer(selected);
                invoiceTable.getItems().remove(selected);
                messageLabel.setText("Facture supprimée avec succès.");
                messageLabel.setStyle("-fx-text-fill: #22c55e;");
            } catch (Exception e) {
                messageLabel.setText("Erreur lors de la suppression.");
                messageLabel.setStyle("-fx-text-fill: #ef4444;");
            }
        }
    }


    private void loadInvoices() {
        try {
            List<Invoices> invoices = invoiceService.recuperer();
            invoiceTable.setItems(FXCollections.observableArrayList(invoices));
        } catch (SQLException e) {
            messageLabel.setText("Erreur lors du chargement des factures");
            e.printStackTrace();
        }
    }
}
