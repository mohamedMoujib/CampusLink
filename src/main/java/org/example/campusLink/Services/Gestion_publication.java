package org.example.campusLink.Services;

import org.example.campusLink.entities.Publications;
import org.example.campusLink.entities.Publications.TypePublication;
import org.example.campusLink.entities.Publications.StatusPublication;
import org.example.campusLink.utils.MyDatabase;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Gestion_publication {
    private final Connection connection;

    public Gestion_publication() throws SQLException {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void ajouterPublication(Publications pub) throws SQLException {
        if (!pub.isValid()) {
            throw new IllegalArgumentException("Publication invalide");
        }

        String sql = """
            INSERT INTO publications
            (student_id, type_publication, titre, message, image_url, localisation,
             service_id, prestataire_id, requested_date, proposed_price, prix_vente, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, pub.getStudentId());
            ps.setString(2, pub.getTypePublication().name());
            ps.setString(3, pub.getTitre());
            ps.setString(4, pub.getMessage());
            ps.setString(5, pub.getImageUrl());
            ps.setString(6, pub.getLocalisation());

            if (pub.getServiceId() != null) ps.setInt(7, pub.getServiceId());
            else ps.setNull(7, Types.INTEGER);

            if (pub.getPrestataireId() != null) ps.setInt(8, pub.getPrestataireId());
            else ps.setNull(8, Types.INTEGER);

            if (pub.getRequestedDate() != null) ps.setTimestamp(9, pub.getRequestedDate());
            else ps.setNull(9, Types.TIMESTAMP);

            if (pub.getProposedPrice() != null) ps.setBigDecimal(10, pub.getProposedPrice());
            else ps.setNull(10, Types.DECIMAL);

            if (pub.getPrixVente() != null) ps.setBigDecimal(11, pub.getPrixVente());
            else ps.setNull(11, Types.DECIMAL);

            ps.setString(12, pub.getStatus().name());

            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) pub.setId(rs.getInt(1));
        }
    }

    public List<Publications> afficherPublications() throws SQLException {
        return afficherPublications(null, null);
    }

    public List<Publications> afficherPublications(TypePublication typeFilter, StatusPublication statusFilter) throws SQLException {
        List<Publications> publications = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT p.*, s.title as service_name, s.description as service_description,
                   s.price as service_price, c.name as category_name
            FROM publications p
            LEFT JOIN services s ON p.service_id = s.id
            LEFT JOIN categories c ON s.category_id = c.id
            WHERE 1=1
        """);

        if (typeFilter != null) sql.append(" AND p.type_publication = '").append(typeFilter.name()).append("'");
        if (statusFilter != null) sql.append(" AND p.status = '").append(statusFilter.name()).append("'");
        sql.append(" ORDER BY p.created_at DESC");

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {
            while (rs.next()) {
                publications.add(mapResultSetToPublication(rs));
            }
        }
        return publications;
    }

    public List<Publications> afficherPublicationsParEtudiant(int studentId) throws SQLException {
        String sql = """
            SELECT p.*, s.title as service_name, s.description as service_description,
                   s.price as service_price, c.name as category_name
            FROM publications p
            LEFT JOIN services s ON p.service_id = s.id
            LEFT JOIN categories c ON s.category_id = c.id
            WHERE p.student_id = ?
            ORDER BY p.created_at DESC
        """;

        List<Publications> publications = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                publications.add(mapResultSetToPublication(rs));
            }
        }
        return publications;
    }

    public Publications getPublicationById(int id) throws SQLException {
        String sql = """
            SELECT p.*, s.title as service_name, s.description as service_description,
                   s.price as service_price, c.name as category_name
            FROM publications p
            LEFT JOIN services s ON p.service_id = s.id
            LEFT JOIN categories c ON s.category_id = c.id
            WHERE p.id = ?
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToPublication(rs);
            }
        }
        return null;
    }

    public List<Publications> rechercherPublications(String keyword) throws SQLException {
        String sql = """
            SELECT p.*, s.title as service_name, s.description as service_description,
                   s.price as service_price, c.name as category_name
            FROM publications p
            LEFT JOIN services s ON p.service_id = s.id
            LEFT JOIN categories c ON s.category_id = c.id
            WHERE p.titre LIKE ? OR p.message LIKE ?
            ORDER BY p.created_at DESC
        """;

        List<Publications> publications = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                publications.add(mapResultSetToPublication(rs));
            }
        }
        return publications;
    }

    public void modifierStatut(int id, StatusPublication statut) throws SQLException {
        String sql = "UPDATE publications SET status = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, statut.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void incrementerVues(int id) throws SQLException {
        String sql = "UPDATE publications SET vues = vues + 1 WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void supprimerPublication(int id) throws SQLException {
        String sql = "DELETE FROM publications WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int compterPublicationsParStatut(StatusPublication status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM publications WHERE status = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public int compterPublicationsParType(TypePublication type) throws SQLException {
        String sql = "SELECT COUNT(*) FROM publications WHERE type_publication = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, type.name());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private Publications mapResultSetToPublication(ResultSet rs) throws SQLException {
        Publications p = new Publications();
        p.setId(rs.getInt("id"));
        p.setStudentId(rs.getInt("student_id"));
        p.setTypePublicationFromString(rs.getString("type_publication"));
        p.setTitre(rs.getString("titre"));
        p.setMessage(rs.getString("message"));
        p.setImageUrl(rs.getString("image_url"));
        p.setLocalisation(rs.getString("localisation"));

        Integer serviceId = (Integer) rs.getObject("service_id");
        p.setServiceId(serviceId);

        Integer prestataireId = (Integer) rs.getObject("prestataire_id");
        p.setPrestataireId(prestataireId);

        p.setRequestedDate(rs.getTimestamp("requested_date"));
        p.setProposedPrice(rs.getBigDecimal("proposed_price"));
        p.setPrixVente(rs.getBigDecimal("prix_vente"));
        p.setStatusFromString(rs.getString("status"));
        p.setVues(rs.getInt("vues"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));

        p.setServiceName(rs.getString("service_name"));
        p.setServiceDescription(rs.getString("service_description"));
        p.setServicePrice(rs.getBigDecimal("service_price"));
        p.setCategoryName(rs.getString("category_name"));

        return p;
    }
}