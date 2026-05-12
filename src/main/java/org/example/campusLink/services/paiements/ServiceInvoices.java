package org.example.campusLink.services.paiements;


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
        // Now includes user_id
        String sql = "INSERT INTO invoices (payment_id, issue_date, details, user_id) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, invoices.getPaymentId());
        ps.setTimestamp(2, invoices.getInvoiceDate());
        ps.setString(3, invoices.getDetails());

        // Handle nullable user_id
        if (invoices.getUserId() != null) {
            ps.setInt(4, invoices.getUserId());
        } else {
            ps.setNull(4, java.sql.Types.INTEGER);
        }

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
        return new ArrayList<>();
    }

    public List<Invoices> recuperer(int currentUserId)
            throws SQLException {

        List<Invoices> invoices = new ArrayList<>();

        String sql =
                "SELECT * FROM invoices WHERE user_id = ?";

        PreparedStatement ps =
                connection.prepareStatement(sql);

        ps.setInt(1, currentUserId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {

            invoices.add(new Invoices(
                    rs.getInt("id"),
                    rs.getInt("payment_id"),
                    rs.getTimestamp("issue_date"),
                    rs.getString("details"),
                    rs.getInt("user_id")
            ));
        }

        return invoices;
    }

}
