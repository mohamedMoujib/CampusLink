package org.example.campusLink.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.example.campusLink.entities.Invoices;
import org.example.campusLink.services.ServiceInvoices;

import java.awt.event.ActionEvent;
import java.sql.Timestamp;

public class AjouterInvoice {

    @FXML
    private TextField id;

    @FXML
    private TextField txtd;

    @FXML
    private TextField txtde;

    @FXML
    public void addInvoice(javafx.event.ActionEvent actionEvent) {
        ServiceInvoices si = new ServiceInvoices();
        int pay_id = Integer.parseInt(id.getText());
        Timestamp date = Timestamp.valueOf(txtd.getText());
        String details = txtd.getText();
        Invoices inv = new Invoices(pay_id, date, details);
        try{si.ajouter(inv);}
        catch(Exception e){
            System.out.println(e.getMessage());
        }

    }

}
