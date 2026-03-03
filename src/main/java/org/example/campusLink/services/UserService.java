package org.example.campusLink.services;

import org.example.campusLink.entities.*;
import org.example.campusLink.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;

public class UserService {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    public User getById(int id) throws SQLException {

        String sql = "SELECT * FROM users WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapUser(rs);
            }
        }
        return null;
    }

    private User mapUser(ResultSet rs) throws SQLException {

        String type = rs.getString("user_type");

        int id = rs.getInt("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String password = rs.getString("password");
        String phone = rs.getString("phone");

        LocalDate dateNaissance = rs.getDate("date_naissance") != null
                ? rs.getDate("date_naissance").toLocalDate()
                : null;

        String gender = rs.getString("gender");
        String profilePicture = rs.getString("profile_picture");
        String address = rs.getString("address");
        String status = rs.getString("status");

        switch (type) {

            case "ADMIN":
                return new Admin(id, name, email, password, phone,
                        dateNaissance, gender, profilePicture,
                        address, status, null);

            case "ETUDIANT":
                return new Etudiant(id, name, email, password, phone,
                        dateNaissance, gender, profilePicture,
                        address, status,
                        rs.getString("universite"),
                        rs.getString("filiere"),
                        rs.getString("specialization"));

            case "PRESTATAIRE":
                return new Prestataire(id, name, email, password, phone,
                        dateNaissance, gender, profilePicture,
                        address, status,
                        rs.getString("universite"),
                        rs.getString("filiere"),
                        rs.getString("specialization"),
                        rs.getInt("trust_points"));

            default:
                throw new IllegalArgumentException("Type utilisateur inconnu");
        }
    }
    public String getUserNameById(int id) throws Exception {

        String sql = "SELECT name FROM users WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("name");
            }
        }

        return "Utilisateur";
    }
}