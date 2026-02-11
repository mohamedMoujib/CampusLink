package org.example;

import org.example.campusLink.entities.Invoices;
import org.example.campusLink.services.ServiceInvoices;


import java.sql.SQLException;
import java.sql.Timestamp;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        ServiceInvoices invoices = new ServiceInvoices();
        Invoices invoice = new Invoices(1, new Timestamp(1770473124L * 1000),"bonjour");
        try{
            invoices.ajouter(invoice);
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
        }
        try{
            invoices.recuperer();
        }
        catch (SQLException e){
            System.out.println(e.getMessage());
        }

    }
}