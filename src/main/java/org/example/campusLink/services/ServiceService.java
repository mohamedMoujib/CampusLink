package org.example.campusLink.services;

import org.example.campusLink.entities.Service;
import org.example.campusLink.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceService {
    private final Connection cnx = MyDatabase.getInstance().getConnection();

    public List<Service> getAll() throws SQLException {
        String sql = "SELECT id, title FROM services ORDER BY title";
        List<Service> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Service s = new Service();
                s.setId(rs.getInt("id"));
                s.setTitle(rs.getString("title"));
                list.add(s);
            }
        }
        return list;
    }

    // Nouvelle méthode pour obtenir les services avec le nom du prestataire
    public List<Service> getAllWithProviderName() throws SQLException {
        String sql = """
        SELECT s.id, s.title, s.description, s.price, s.image, s.prestataire_id, s.category_id,
               u.name AS provider_name
        FROM services s
        JOIN users u ON u.id = s.prestataire_id
        ORDER BY s.title
    """;
        List<Service> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Service s = new Service();
                s.setId(rs.getInt("id"));
                s.setTitle(rs.getString("title"));
                s.setDescription(rs.getString("description"));
                s.setPrice(rs.getBigDecimal("price"));
                s.setImage(rs.getString("image"));
                s.setPrestataireId(rs.getInt("prestataire_id"));
                s.setCategoryId(rs.getInt("category_id"));
                s.setProviderName(rs.getString("provider_name"));
                list.add(s);
            }
        }
        return list;
    }

}