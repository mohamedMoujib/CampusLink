package org.example.campusLink.services;


import org.example.campusLink.entities.Role;
import org.example.campusLink.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class RoleService implements Iservice<Role> {

    private Connection connection;

    public RoleService() {
        connection = MyDatabase.getInstance().getConnection();
    }
    public RoleService(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void ajouter(Role role) throws SQLException {
        String query = "INSERT INTO roles (name) VALUES (?)";

        try (PreparedStatement pst = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, role.getName());
            pst.executeUpdate();

            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                role.setId(rs.getInt(1));
            }
        }
    }



    @Override
    public Role getById(int id) throws SQLException {
        String query = "SELECT * FROM roles WHERE id = ?";
        Role role = null;

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                role = new Role();
                role.setId(rs.getInt("id"));
                role.setName(rs.getString("name"));
            }
        }
        return role;
    }


    @Override
    public List<Role> recuperer() throws SQLException {
        List<Role> roles = new ArrayList<>();
        String query = "SELECT * FROM roles";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                Role role = new Role();
                role.setId(rs.getInt("id"));
                role.setName(rs.getString("name"));
                roles.add(role);
            }
        }
        return roles;
    }



    @Override
    public void modifier(Role role) throws SQLException {
        String query = "UPDATE roles SET name = ? WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, role.getName());
            pst.setInt(2, role.getId());
            pst.executeUpdate();
        }
    }


    @Override
    public void supprimer(Role role) throws SQLException {
        String query = "DELETE FROM roles WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, role.getId());
            pst.executeUpdate();
        }
    }


    public Role getRoleByName(String name) throws SQLException {
        String query = "SELECT * FROM roles WHERE name = ?";
        Role role = null;

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, name);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                role = new Role();
                role.setId(rs.getInt("id"));
                role.setName(rs.getString("name"));
            }
        }
        return role;
    }



}