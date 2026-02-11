package org.example.campusLink.services;

import org.example.campusLink.entities.Role;
import org.example.campusLink.entities.User;
import org.example.campusLink.utils.MyDatabase;
import org.example.campusLink.utils.UserValidator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements Iservice<User>{

    private Connection connection;
    public UserService () {
        connection = MyDatabase.getInstance().getConnection();
    }


    @Override
    public void ajouter(User user) throws SQLException {

        validateUser(user);

        if (emailExists(user.getEmail())) {
            throw new SQLException("❌ Cet email existe déjà!");
        }

        if (phoneExists(user.getPhone())) {
            throw new SQLException("❌ Ce téléphone existe déjà!");
        }

        String query = "INSERT INTO users (name,email,password,phone,date_naissance,gender," +
                "universite,filiere,specialization,trust_points,profile_picture,address,status) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement pst = connection.prepareStatement(query)) {

            pst.setString(1, user.getName());
            pst.setString(2, user.getEmail());
            pst.setString(3, user.getPassword());
            pst.setString(4, user.getPhone());
            pst.setTimestamp(5, user.getDateNaissance());
            pst.setString(6, user.getGender());
            pst.setString(7, user.getUniversite());
            pst.setString(8, user.getFiliere());
            pst.setString(9, user.getSpecialization());
            pst.setInt(10, 0);
            pst.setString(11, user.getProfilePicture());
            pst.setString(12, user.getAddress());
            pst.setString(13, "INACTIVE");

            pst.executeUpdate();
        }

        // ✅ GUARANTEED ID RETRIEVAL
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT LAST_INSERT_ID()")) {

            if (rs.next()) {
                user.setId(rs.getInt(1));
            } else {
                throw new SQLException("❌ Failed to retrieve generated user ID");
            }
        }
    }

    @Override
    public User getById(int id) throws SQLException {
        if (id <= 0) {
            throw new SQLException("❌ ID invalide!");
        }

        String query = "SELECT * FROM users WHERE id = ?";
        User user = null;

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                user = extractUserFromResultSet(rs);
                user.setRoles(getUserRoles(id));
            }
        }
        return user;
    }

    @Override
    public List<User> recuperer() throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users";

        try (PreparedStatement pst = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = pst.executeQuery() ){
                while (rs.next()) {
                    User user = extractUserFromResultSet(rs);
                    // Load roles for each user
                    user.setRoles(getUserRoles(user.getId()));
                    users.add(user);
                }
            }


        return users;
    }
// Helper method to extract User from ResultSet
private User extractUserFromResultSet(ResultSet rs) throws SQLException {
    User user = new User();
    user.setId(rs.getInt("id"));
    user.setName(rs.getString("name"));
    user.setEmail(rs.getString("email"));
    user.setPassword(rs.getString("password"));
    user.setPhone(rs.getString("phone"));
    user.setDateNaissance(rs.getTimestamp("date_naissance"));
    user.setGender(rs.getString("gender"));
    user.setUniversite(rs.getString("universite"));
    user.setFiliere(rs.getString("filiere"));
    user.setSpecialization(rs.getString("specialization"));
    user.setTrustPoints(rs.getInt("trust_points"));
    user.setProfilePicture(rs.getString("profile_picture"));
    user.setAddress(rs.getString("address"));
    user.setStatus(rs.getString("status"));
    return user;
}

    // Get roles for a specific user
    private List<Role> getUserRoles(int userId) throws SQLException {
        List<Role> roles = new ArrayList<>();
        String query = "SELECT r.* FROM roles r " +
                "INNER JOIN user_roles ur ON r.id = ur.role_id " +
                "WHERE ur.user_id = ?";

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

    @Override
    public void modifier(User user) throws SQLException {

        validateUser(user);
        if (user.getId() <= 0) {
            throw new SQLException("❌ ID invalide!");
        }

        String query = "UPDATE users SET name = ?, email = ?, password = ?, phone = ?, " +
                "date_naissance = ?, gender = ?, universite = ?, filiere = ?, " +
                "specialization = ?, trust_points = ?, profile_picture = ?, " +
                "address = ?, status = ? WHERE id = ?";
        try (
            PreparedStatement pst = connection.prepareStatement(query)) {
                pst.setString(1, user.getName());
                pst.setString(2, user.getEmail());
                pst.setString(3, user.getPassword());
                pst.setString(4, user.getPhone());
                pst.setTimestamp(5, user.getDateNaissance());
                pst.setString(6, user.getGender());
                pst.setString(7, user.getUniversite());
                pst.setString(8, user.getFiliere());
                pst.setString(9, user.getSpecialization());
                pst.setInt(10, user.getTrustPoints());
                pst.setString(11, user.getProfilePicture());
                pst.setString(12, user.getAddress());
                pst.setString(13, user.getStatus());
                pst.setInt(14, user.getId());

                pst.executeUpdate();

            }

    }
    @Override
    public void supprimer(User user) throws SQLException {

        if (user.getId() <= 0) {
            throw new SQLException("❌ ID invalide!");
        }

        String query = "DELETE FROM users WHERE id = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, user.getId());
            pst.executeUpdate();
        }
    }

    // ==================== VALIDATION ====================

    /**
     * ✅ MÉTHODE DE VALIDATION PRINCIPALE
     * Valider toutes les données d'un utilisateur
     */
    private void validateUser(User user) throws SQLException {
        List<String> errors = new ArrayList<>();

        // Valider le nom
        if (!UserValidator.isValidName(user.getName())) {
            errors.add(UserValidator.getNameErrorMessage());
        }

        // Valider l'email
        if (!UserValidator.isValidEmail(user.getEmail())) {
            errors.add(UserValidator.getEmailErrorMessage());
        }

        // Valider le mot de passe
        if (!UserValidator.isValidPassword(user.getPassword())) {
            errors.add(UserValidator.getPasswordErrorMessage());
        }

        // Valider le téléphone (si fourni)
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            if (!UserValidator.isValidPhone(user.getPhone())) {
                errors.add(UserValidator.getPhoneErrorMessage());
            }
        }

        // Valider le genre
        if (user.getGender() != null && !user.getGender().isEmpty()) {
            if (!UserValidator.isValidGender(user.getGender())) {
                errors.add("❌ Genre invalide! Utilisez 'Male' ou 'Female'.");
            }
        }



        // S'il y a des erreurs, les lancer
        if (!errors.isEmpty()) {
            throw new SQLException("Erreurs de validation:\n" + String.join("\n", errors));
        }
    }

    public boolean emailExists(String email) throws SQLException {
        String query = "SELECT COUNT(*) as count FROM users WHERE email = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        }
        return false;
    }

    public boolean phoneExists(String phone) throws SQLException {
        String query = "SELECT COUNT(*) as count FROM users WHERE phone = ?";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, phone);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        }
        return false;
    }

}
