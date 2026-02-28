package org.example.campusLink.services;

import org.example.campusLink.entities.Payments;
import org.example.campusLink.enumeration.Method;
import org.example.campusLink.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicesPayments implements IServices<Payments> {

    private Connection connection;

    public ServicesPayments() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Payments payments) throws SQLException {
        String sql = "INSERT INTO payments (reservation_id, amount, method, meeting_lat, meeting_lng, meeting_address) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);

        ps.setInt(1, payments.getReservationId());
        ps.setFloat(2, payments.getAmount());
        ps.setString(3, payments.getMethod().toString());
        ps.setObject(4, payments.getMeetingLat());
        ps.setObject(5, payments.getMeetingLng());
        ps.setString(6, payments.getMeetingAddress());

        ps.executeUpdate();
    }

    @Override
    public void modifier(Payments payments) throws SQLException {
        String sql = "UPDATE payments SET reservation_id=?, amount=?, method=?, meeting_lat=?, meeting_lng=?, meeting_address=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);

        ps.setInt(1, payments.getReservationId());
        ps.setFloat(2, payments.getAmount());
        ps.setString(3, payments.getMethod().toString());
        ps.setObject(4, payments.getMeetingLat());
        ps.setObject(5, payments.getMeetingLng());
        ps.setString(6, payments.getMeetingAddress());
        ps.setInt(7, payments.getId());

        ps.executeUpdate();
    }

    @Override
    public void supprimer(Payments payments) throws SQLException {
        String sql = "DELETE FROM payments WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, payments.getId());
        ps.executeUpdate();
    }

    @Override
    public List<Payments> recuperer() throws SQLException {
        List<Payments> paymentsList = new ArrayList<>();
        String sql = "SELECT * FROM payments";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Payments payment = new Payments(
                    rs.getInt("id"),
                    rs.getInt("reservation_id"),
                    rs.getFloat("amount"),
                    Method.valueOf(rs.getString("method")),
                    rs.getObject("meeting_lat", Double.class),
                    rs.getObject("meeting_lng", Double.class),
                    rs.getString("meeting_address")
            );
            paymentsList.add(payment);
        }

        return paymentsList;
    }

    public int getLastInsertedPaymentId() throws SQLException {
        String sql = "SELECT id FROM payments ORDER BY id DESC LIMIT 1";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        if (rs.next()) {
            return rs.getInt("id");
        }
        throw new SQLException("No payment found");
    }
}