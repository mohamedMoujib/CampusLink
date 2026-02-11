package org.example.campusLink.services;

import org.example.campusLink.entities.Role;
import org.example.campusLink.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserRoleService  {

    private Connection connection;

    public UserRoleService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(int userId, int roleId) throws SQLException {
        if (userHasRole(userId, roleId)) return;

        String query = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, roleId);
            pst.executeUpdate();
        }
    }

    public void supprimer(int userId, int roleId) throws SQLException {
        String query = "DELETE FROM user_roles WHERE user_id = ? AND role_id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, roleId);
            pst.executeUpdate();
        }
    }

    public List<Role> recuperer(int userId) throws SQLException {
        List<Role> roles = new ArrayList<>();

        String query = """
            SELECT r.* FROM roles r
            JOIN user_roles ur ON r.id = ur.role_id
            WHERE ur.user_id = ?
        """;

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Role role = new Role();
                role.setId(rs.getInt("id"));
                role.setName(rs.getString("name"));
                roles.add(role);
            }
        }
        return roles;
    }

    public boolean userHasRole(int userId, int roleId) throws SQLException {
        String query = "SELECT COUNT(*) FROM user_roles WHERE user_id = ? AND role_id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, userId);
            pst.setInt(2, roleId);
            ResultSet rs = pst.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }


}

