package org.example.campusLink.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.example.campusLink.entities.Reservation;

public class CoordinationController {

    @FXML private Label lblService;
    @FXML private Label lblPrice;
    @FXML private Button payBtn;

    private Reservation reservation;

    public void initData(Reservation r){
        this.reservation = r;
        lblService.setText(r.getServiceTitle());
        lblPrice.setText(r.getPrice()+" DT");
    }

    @FXML
    private void onPay(){
        payBtn.setText("Paiement effectué ✅");
        payBtn.setDisable(true);
    }

    @FXML
    private void onClose(){
        ((Stage)lblService.getScene().getWindow()).close();
    }
}