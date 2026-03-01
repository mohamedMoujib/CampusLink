package org.example.campusLink.Services;

import org.example.campusLink.entities.Categorie;
import org.example.campusLink.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for managing Categories
 * Handles all database operations for the categories table
 */
public class Gestion_Categorie {

    private Connection getConnection() throws SQLException {
        return MyDatabase.getInstance().getConnection();
    }

    /**
     * ✅ Add a new category
     */
    public void ajouterCategorie(Categorie c) throws SQLException {

        if (c.getName() == null || c.getName().isBlank()) {
            throw new IllegalArgumentException("Le nom de la catégorie est obligatoire");
        }

        if (c.getName().length() > 100) {
            throw new IllegalArgumentException("Le nom ne peut pas dépasser 100 caractères");
        }

        String sql = """
            INSERT INTO categories (name, description)
            VALUES (?, ?)
        """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getName().trim());
            ps.setString(2, c.getDescription() != null ? c.getDescription().trim() : null);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = ps.getGeneratedKeys();
                if (generatedKeys.next()) {
                    c.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    /**
     * ✅ Display all categories
     */
    public List<Categorie> afficherCategories() throws SQLException {
        List<Categorie> list = new ArrayList<>();
        String sql = "SELECT * FROM categories ORDER BY name ASC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Categorie c = new Categorie();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setDescription(rs.getString("description"));
                list.add(c);
            }
        }
        return list;
    }

    /**
     * ✅ Get a category by ID
     */
    public Categorie getCategorieById(int id) throws SQLException {
        String sql = "SELECT * FROM categories WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Categorie c = new Categorie();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setDescription(rs.getString("description"));
                return c;
            }
        }

        throw new SQLException("Catégorie introuvable (ID: " + id + ")");
    }

    /**
     * ✅ Get a category by name
     */
    public Categorie getCategorieByName(String name) throws SQLException {
        String sql = "SELECT * FROM categories WHERE name = ? LIMIT 1";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Categorie c = new Categorie();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setDescription(rs.getString("description"));
                return c;
            }
        }

        return null; // Not found
    }

    /**
     * ✅ Update a category
     */
    public void modifierCategorie(Categorie c) throws SQLException {

        if (c.getId() <= 0) {
            throw new IllegalArgumentException("ID de catégorie invalide");
        }

        if (c.getName() == null || c.getName().isBlank()) {
            throw new IllegalArgumentException("Le nom de la catégorie est obligatoire");
        }

        if (c.getName().length() > 100) {
            throw new IllegalArgumentException("Le nom ne peut pas dépasser 100 caractères");
        }

        String sql = """
            UPDATE categories 
            SET name = ?, description = ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, c.getName().trim());
            ps.setString(2, c.getDescription() != null ? c.getDescription().trim() : null);
            ps.setInt(3, c.getId());

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("Catégorie introuvable (ID: " + c.getId() + ")");
            }
        }
    }

    /**
     * ✅ Delete a category
     */
    public void supprimerCategorie(int id) throws SQLException {

        // Check if category is used by services
        String checkSql = "SELECT COUNT(*) FROM services WHERE category_id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(checkSql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                throw new IllegalStateException(
                        "Impossible de supprimer une catégorie utilisée par des services"
                );
            }
        }

        String deleteSql = "DELETE FROM categories WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(deleteSql)) {
            ps.setInt(1, id);
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("Catégorie introuvable (ID: " + id + ")");
            }
        }
    }

    /**
     * ✅ Count total categories
     */
    public int compterCategories() throws SQLException {
        String sql = "SELECT COUNT(*) FROM categories";

        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * ✅ Search categories by name
     */
    public List<Categorie> rechercherCategories(String keyword) throws SQLException {
        List<Categorie> list = new ArrayList<>();
        String sql = "SELECT * FROM categories WHERE name LIKE ? OR description LIKE ? ORDER BY name ASC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            String searchPattern = "%" + keyword + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Categorie c = new Categorie();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setDescription(rs.getString("description"));
                list.add(c);
            }
        }
        return list;
    }

    /**
     * ✅ Get categories with service count
     */
    public List<Categorie> getCategoriesAvecNombreServices() throws SQLException {
        List<Categorie> list = new ArrayList<>();
        String sql = """
            SELECT c.*, COUNT(s.id) as service_count
            FROM categories c
            LEFT JOIN services s ON c.id = s.category_id
            GROUP BY c.id, c.name, c.description
            ORDER BY c.name ASC
        """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Categorie c = new Categorie();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setDescription(rs.getString("description"));
                // Note: service_count could be added to Categories entity if needed
                list.add(c);
            }
        }
        return list;
    }
}