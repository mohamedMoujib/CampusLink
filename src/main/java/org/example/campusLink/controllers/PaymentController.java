package org.example.campusLink.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.campusLink.entities.Invoices;
import org.example.campusLink.entities.Payments;
import org.example.campusLink.enumeration.Method;
import org.example.campusLink.enumeration.Status;
import org.example.campusLink.services.ServiceInvoices;
import org.example.campusLink.services.ServicesPayments;

import java.sql.SQLException;
import java.sql.Timestamp;

public class PaymentController {

    private ServicesPayments paymentService;
    private ServiceInvoices invoiceService;

    @FXML
    private TextField reservationIdField;

    @FXML
    private TextField amountField;

    @FXML
    private ComboBox<String> methodComboBox;

    @FXML
    private Button submitButton;

    @FXML
    private Label messageLabel;

    @FXML
    private TextField invoicePaymentIdField;

    @FXML
    private TextField invoiceDateField;

    @FXML
    private TextArea invoiceDetailsArea;

    @FXML
    public void initialize() {
        paymentService = new ServicesPayments();
        invoiceService = new ServiceInvoices();

        methodComboBox.getItems().addAll(
                Method.PHYSICAL.toString(),
                Method.VIRTUAL.toString()
        );
        methodComboBox.getSelectionModel().selectFirst();

        submitButton.setOnAction(event -> handlePaymentAndInvoice());
    }

    @FXML
    private void openInvoices(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/View/InvoiceView.fxml")
            );
            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void handlePaymentAndInvoice() {
        try {
            int reservationId = Integer.parseInt(reservationIdField.getText());
            float amount = Float.parseFloat(amountField.getText());
            Method method = Method.valueOf(methodComboBox.getValue());

            Payments payment = new Payments(reservationId, amount, method, Status.PENDING);
            paymentService.ajouter(payment);

            int paymentId = paymentService.getLastInsertedPaymentId();
            payment.setId(paymentId);

            Timestamp now = new Timestamp(System.currentTimeMillis());
            String details = "Paiement de la réservation #" + reservationId +
                    " d'un montant de " + amount +
                    " via " + method;

            Invoices invoice = new Invoices(paymentId, now, details);
            invoiceService.ajouter(invoice);

            invoicePaymentIdField.setText(String.valueOf(paymentId));
            invoiceDateField.setText(now.toString());
            invoiceDetailsArea.setText(details);

            messageLabel.setText("Paiement et facture ajoutés avec succès ✅");
            messageLabel.setStyle("-fx-text-fill: #22c55e;");
            clearForm();

        } catch (NumberFormatException e) {
            messageLabel.setText("Veuillez entrer des nombres valides pour ID réservation et Montant ❌");
            messageLabel.setStyle("-fx-text-fill: #ef4444;");
        } catch (SQLException e) {
            messageLabel.setText("Erreur base de données : " + e.getMessage() + " ❌");
            messageLabel.setStyle("-fx-text-fill: #ef4444;");
            e.printStackTrace();
        }
    }

    private void clearForm() {
        reservationIdField.clear();
        amountField.clear();
        methodComboBox.getSelectionModel().selectFirst();
    }

}
