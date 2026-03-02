package org.example.campusLink.services;

import org.example.campusLink.entities.Demande;
import org.example.campusLink.entities.DemandeStatus;
import org.example.campusLink.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DemandeService {
    private final Connection cnx = MyDatabase.getInstance().getConnection();

    public int add(Demande d) throws SQLException {
        String sql = "INSERT INTO demandes (student_id, service_id, prestataire_id, message, requested_date, proposed_price, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getStudentId());
            ps.setInt(2, d.getServiceId());
            ps.setInt(3, d.getPrestataireId());
            ps.setString(4, d.getMessage());

            if (d.getRequestedDate() == null) ps.setNull(5, Types.TIMESTAMP);
            else ps.setTimestamp(5, Timestamp.valueOf(d.getRequestedDate()));

            if (d.getProposedPrice() == null) ps.setNull(6, Types.DECIMAL);
            else ps.setBigDecimal(6, d.getProposedPrice());

            ps.setString(7, d.getStatus().name());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public List<Demande> getAll() throws SQLException {
        String sql = "SELECT * FROM demandes ORDER BY created_at DESC";
        List<Demande> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Demande> getByStudent(int studentId) throws SQLException {
        String sql = "SELECT * FROM demandes WHERE student_id=? ORDER BY created_at DESC";
        List<Demande> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<Demande> getByPrestataire(int prestataireId) throws SQLException {
        String sql = "SELECT * FROM demandes WHERE prestataire_id=? ORDER BY created_at DESC";
        List<Demande> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, prestataireId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public boolean updateStatus(int id, DemandeStatus status) throws SQLException {
        String sql = "UPDATE demandes SET status=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM demandes WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Demande map(ResultSet rs) throws SQLException {
        Demande d = new Demande();
        d.setId(rs.getInt("id"));
        d.setStudentId(rs.getInt("student_id"));
        d.setServiceId(rs.getInt("service_id"));
        d.setPrestataireId(rs.getInt("prestataire_id"));
        d.setMessage(rs.getString("message"));

        Timestamp req = rs.getTimestamp("requested_date");
        d.setRequestedDate(req != null ? req.toLocalDateTime() : null);

        d.setProposedPrice(rs.getBigDecimal("proposed_price"));
        d.setStatus(DemandeStatus.valueOf(rs.getString("status")));

        Timestamp created = rs.getTimestamp("created_at");
        d.setCreatedAt(created != null ? created.toLocalDateTime() : LocalDateTime.now());

        return d;
    }
}
