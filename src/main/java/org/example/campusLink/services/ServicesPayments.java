package org.example.campusLink.services;


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

        String sql = "INSERT INTO invoices (reservation_id, amount, method, status) VALUES (?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, payments.getReservationId());
        ps.setFloat(2, payments.getAmount());
        ps.setString(3, payments.getMethod().toString());
        ps.setString(4, payments.getStatus().toString());

        ps.executeUpdate();
    }

    @Override
    public void modifier(Payments payments) throws SQLException {
        String req = "update payments set reservation_id=?, amount=?, method=?, status=?, where reservation_id=?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, payments.getReservationId());
        ps.setFloat(2, payments.getAmount());
        ps.setString(3, payments.getMethod().toString());
        ps.setString(4, payments.getStatus().toString());
        ps.executeUpdate();
        System.out.println("payments modifie");
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
            String method = rs.getString(4);
            String status = rs.getString(5);
            Payments payment = new Payments(payId, resId, amount, method,status);
            payments.add(payment);

        }

        return payments;
    }
}
