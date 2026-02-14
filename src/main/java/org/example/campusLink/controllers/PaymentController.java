package org.example.campusLink.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.campusLink.entities.Payments;
import org.example.campusLink.enumeration.Method;
import org.example.campusLink.enumeration.Status;
import org.example.campusLink.services.ServicesPayments;

import java.sql.SQLException;

public class PaymentController {

    private ServicesPayments paymentService;

    @FXML
    private TextField reservationIdField;

    @FXML
    private TextField amountField;

    @FXML
    private ComboBox<String> methodComboBox;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private Button submitButton;

    @FXML
    private Label messageLabel;

    @FXML
    public void initialize() {
        paymentService = new ServicesPayments();

        methodComboBox.getItems().addAll(
                Method.PHYSICAL.toString(),
                Method.VIRTUAL.toString()
        );
        statusComboBox.getItems().addAll(
                Status.PENDING.toString(),
                Status.PAID.toString(),
                Status.CANCELLED.toString()
        );

        methodComboBox.getSelectionModel().selectFirst();
        statusComboBox.getSelectionModel().selectFirst();

        submitButton.setOnAction(event -> handleSubmit());
    }

    private void handleSubmit() {
        try {
            int reservationId = Integer.parseInt(reservationIdField.getText());
            float amount = Float.parseFloat(amountField.getText());
            Method method = Method.valueOf(methodComboBox.getValue());
            Status status = Status.valueOf(statusComboBox.getValue());

            Payments payment = new Payments(reservationId, amount, method, status);

            paymentService.ajouter(payment);

            messageLabel.setText("Payment added successfully ✅");
            clearForm();
        } catch (NumberFormatException e) {
            messageLabel.setText("Please enter valid numbers for Reservation ID and Amount ❌");
        } catch (SQLException e) {
            messageLabel.setText("Database error: " + e.getMessage() + " ❌");
            e.printStackTrace();
        }
    }

    private void clearForm() {
        reservationIdField.clear();
        amountField.clear();
        methodComboBox.getSelectionModel().selectFirst();
        statusComboBox.getSelectionModel().selectFirst();
    }
}
