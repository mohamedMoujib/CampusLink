package org.example.campusLink.services;


import org.example.campusLink.enumeration.Method;
import org.example.campusLink.enumeration.Status;
import org.example.campusLink.utils.MyDatabase;
import org.example.campusLink.entities.Payments;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class ServicesPayments implements IServices<Payments> {
    private Connection connection;
    public ServicesPayments() {
        connection= MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Payments payments) throws SQLException {

        String sql = "INSERT INTO payments (reservation_id, amount, method, status) VALUES (?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, payments.getReservationId());
        ps.setFloat(2, payments.getAmount());
        ps.setString(3, payments.getMethod().toString());
        ps.setString(4, payments.getStatus().toString());

        ps.executeUpdate();
    }

    @Override
    public void modifier(Payments payments) throws SQLException {
        String req = "UPDATE payments SET reservation_id=?, amount=?, method=?, status=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, payments.getReservationId());
        ps.setFloat(2, payments.getAmount());
        ps.setString(3, payments.getMethod().toString());
        ps.setString(4, payments.getStatus().toString());
        ps.setInt(5, payments.getId()); // use the payment ID as the WHERE condition
        ps.executeUpdate();
        System.out.println("Payment modified successfully for ID " + payments.getId());
    }


    @Override
    public void supprimer(Payments payments) throws SQLException {
        String req = "DELETE  FROM payments WHERE reservation_id=?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, payments.getReservationId());
        ps.executeUpdate();
    }

    @Override
    public List<Payments> recuperer() throws SQLException {
        List<Payments> payments = new ArrayList<>();
        String req = "select * from payments ";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            int payId = rs.getInt(1);
            int resId = rs.getInt(2);
            Float amount = rs.getFloat(3);
            Method method = Method.valueOf(rs.getString(4));
            Status status = Status.valueOf(rs.getString(5));
            Payments payment = new Payments(payId, resId, amount, method,status);
            payments.add(payment);

        }

        return payments;
    }

    public void deleteInvoicesForPayment(int paymentId) throws SQLException {
        String sql = "DELETE FROM invoices WHERE payment_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, paymentId);
        ps.executeUpdate();
    }
    public int getLastInsertedPaymentId() throws SQLException {
        String sql = "SELECT id FROM payments ORDER BY id DESC LIMIT 1";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("id");
            } else {
                throw new SQLException("No payment found");
            }
        }
    }


}
