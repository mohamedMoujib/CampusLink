package org.example.campusLink.services;


import org.example.campusLink.utils.MyDatabase;
import org.example.campusLink.entities.Invoices;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceInvoices implements IServices<Invoices> {
    private Connection connection;
    public ServiceInvoices() {
        connection= MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Invoices invoices) throws SQLException {

        String sql = "INSERT INTO invoices (payment_id, issue_date, details) VALUES (?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, invoices.getPaymentId());
        ps.setTimestamp(2, invoices.getInvoiceDate());
        ps.setString(3, invoices.getDetails());

        ps.executeUpdate();
    }

    @Override
    public void modifier(Invoices invoices) throws SQLException {
        String req = "UPDATE invoices SET payment_id=?, issue_date=?, details=? WHERE payment_id=?";
        PreparedStatement ps = connection.prepareStatement(req);

        ps.setInt(1, invoices.getPaymentId());
        ps.setTimestamp(2, invoices.getInvoiceDate());
        ps.setString(3, invoices.getDetails());
        ps.setInt(4, invoices.getPaymentId());

        ps.executeUpdate();
        System.out.println("invoice modifie");
    }


    @Override
    public void supprimer(Invoices invoices) throws SQLException {
        String req = "DELETE  FROM invoices WHERE payment_id=?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, invoices.getPaymentId());
        ps.executeUpdate();
    }

    @Override
    public List<Invoices> recuperer() throws SQLException {
        List<Invoices> invoices = new ArrayList<>();
        String req = "select * from invoices ";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            int invId = rs.getInt(1);
            int PayId = rs.getInt(2);
            Timestamp invDate = rs.getTimestamp(3);
            String details = rs.getString(4);
            Invoices invoice = new Invoices(invId, PayId, invDate, details);
            invoices.add(invoice);

        }

        return invoices;
    }

}
