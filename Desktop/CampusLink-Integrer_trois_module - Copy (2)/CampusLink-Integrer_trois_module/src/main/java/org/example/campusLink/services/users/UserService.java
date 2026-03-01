package org.example.campusLink.services.users;

import org.example.campusLink.entities.*;
import org.example.campusLink.services.Iservice;
import org.example.campusLink.utils.MyDatabase;
import org.example.campusLink.utils.UserValidator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class UserService implements Iservice<User> {
    private Connection conn;

    public UserService() {
        conn = MyDatabase.getInstance().getConnection();
    }

    public UserService(Connection connection) {
        this.conn = connection;
    }
    // ==================== CREATE ====================

    @Override
    public void ajouter(User user) throws SQLException {

        validateUser(user);

        if (user instanceof Etudiant) {
            ajouterEtudiant((Etudiant) user);
        } else if (user instanceof Prestataire) {
            ajouterPrestataire((Prestataire) user);
        } else if (user instanceof Admin) {
            ajouterAdmin((Admin) user);
        } else {
            throw new IllegalArgumentException("Type d'utilisateur inconnu");
        }
    }


    public void ajouterEtudiant(Etudiant etudiant) throws SQLException {
        String sql = "INSERT INTO users (user_type, name, email, password, phone, " +
                "gender, address, universite, filiere, specialization, status) " +
                "VALUES ('ETUDIANT', ?, ? , ?, ?, ?, ?, ?, ?, ?, ?)";

        try (
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, etudiant.getName());
            ps.setString(2, etudiant.getEmail());
            ps.setString(3, etudiant.getPassword());
            ps.setString(4, etudiant.getPhone());
            ps.setString(5, etudiant.getGender());
            ps.setString(6, etudiant.getAddress());
            ps.setString(7, etudiant.getUniversite());
            ps.setString(8, etudiant.getFiliere());
            ps.setString(9, etudiant.getSpecialization());
            ps.setString(10, "INACTIVE");

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                etudiant.setId(rs.getInt(1));
            }
        }
    }

    public void ajouterPrestataire(Prestataire prestataire) throws SQLException {
        String sql = "INSERT INTO users (user_type, name, email, password, phone, " +
                "gender,address, universite, filiere, specialization, trust_points, status) " +
                "VALUES ('PRESTATAIRE', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, prestataire.getName());
            ps.setString(2, prestataire.getEmail());
            ps.setString(3, prestataire.getPassword());
            ps.setString(4, prestataire.getPhone());
            ps.setString(5, prestataire.getGender());
            ps.setString(6, prestataire.getAddress());
            ps.setString(7, prestataire.getUniversite());
            ps.setString(8, prestataire.getFiliere());
            ps.setString(9, prestataire.getSpecialization());
            ps.setInt(10, prestataire.getTrustPoints());
            ps.setString(11, "INACTIVE");

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                prestataire.setId(rs.getInt(1));
            }
        }
    }

    public void ajouterAdmin(Admin admin) throws SQLException {
        String sql = "INSERT INTO users (user_type, name, email, password, phone, " +
                "gender,address,  status) " +
                "VALUES ('ADMIN', ? ,? , ?, ?, ?, ?, ?)";

        try (
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, admin.getName());
            ps.setString(2, admin.getEmail());
            ps.setString(3, admin.getPassword());
            ps.setString(4, admin.getPhone());
            ps.setString(5, admin.getGender());
            ps.setString(6, admin.getAddress());
            ps.setString(7, "ACTIVE");

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                admin.setId(rs.getInt(1));
            }
        }
    }

    // ==================== READ ====================

    @Override
    public User getById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return createUserFromResultSet(rs);
            }
        }
        return null;
    }

    @Override
    public List<User> recuperer() throws SQLException {
        String sql = "SELECT * FROM users";
        List<User> users = new ArrayList<>();

        try (
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(createUserFromResultSet(rs));
            }
        }
        return users;
    }

    public User getUserByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return createUserFromResultSet(rs);
            }
        }
        return null;
    }


    public List<Etudiant> getAllEtudiants() throws SQLException {
        String sql = "SELECT * FROM users WHERE user_type = 'ETUDIANT'";
        List<Etudiant> etudiants = new ArrayList<>();

        try (
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                etudiants.add(createEtudiant(rs));
            }
        }
        return etudiants;
    }

    public List<Prestataire> getAllPrestataires() throws SQLException {
        String sql = "SELECT * FROM users WHERE user_type = 'PRESTATAIRE'";
        List<Prestataire> prestataires = new ArrayList<>();

        try (
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                prestataires.add(createPrestataire(rs));
            }
        }
        return prestataires;
    }

    // ==================== UPDATE ====================

    @Override
    public void modifier(User user) throws SQLException {

        validateUser(user);   // ✅ Validation avant update

        if (user instanceof Etudiant) {
            modifierEtudiant((Etudiant) user);
        } else if (user instanceof Prestataire) {
            modifierPrestataire((Prestataire) user);
        } else if (user instanceof Admin) {
            modifierAdmin((Admin) user);
        }
    }


    private void modifierEtudiant(Etudiant etudiant) throws SQLException {
        String sql = "UPDATE users SET name=?, email=?, password=?, phone=?, " +
                "gender=?, universite=?, filiere=?, specialization=?, " +
                "profile_picture=?, address=?, status=? WHERE id=?";

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, etudiant.getName());
            ps.setString(2, etudiant.getEmail());
            ps.setString(3, etudiant.getPassword());
            ps.setString(4, etudiant.getPhone());
            ps.setString(5, etudiant.getGender());
            ps.setString(6, etudiant.getUniversite());
            ps.setString(7, etudiant.getFiliere());
            ps.setString(8, etudiant.getSpecialization());
            ps.setString(9, etudiant.getProfilePicture());
            ps.setString(10, etudiant.getAddress());
            ps.setString(11, etudiant.getStatus());
            ps.setInt(12, etudiant.getId());

            ps.executeUpdate();
        }
    }

    private void modifierPrestataire(Prestataire prestataire) throws SQLException {
        String sql = "UPDATE users SET name=?, email=?, password=?, phone=?, " +
                "gender=?, universite=?, specialization=?, filiere=? , " +
                "trust_points=?, profile_picture=?, address=?, status=? WHERE id=?";

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, prestataire.getName());
            ps.setString(2, prestataire.getEmail());
            ps.setString(3, prestataire.getPassword());
            ps.setString(4, prestataire.getPhone());
            ps.setString(5, prestataire.getGender());
            ps.setString(6, prestataire.getUniversite());
            ps.setString(8, prestataire.getSpecialization());
            ps.setString(7, prestataire.getFiliere());
            ps.setInt(9, prestataire.getTrustPoints());
            ps.setString(10, prestataire.getProfilePicture());
            ps.setString(11, prestataire.getAddress());
            ps.setString(12, prestataire.getStatus());
            ps.setInt(13, prestataire.getId());

            ps.executeUpdate();
        }
    }

    private void modifierAdmin(Admin admin) throws SQLException {
        String sql = "UPDATE users SET name=?, email=?, password=?, phone=?, " +
                "gender=?,  profile_picture=?, address=?, status=? WHERE id=?";

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, admin.getName());
            ps.setString(2, admin.getEmail());
            ps.setString(3, admin.getPassword());
            ps.setString(4, admin.getPhone());
            ps.setString(5, admin.getGender());
            ps.setString(7, admin.getProfilePicture());
            ps.setString(8, admin.getAddress());
            ps.setString(9, admin.getStatus());
            ps.setInt(10, admin.getId());

            ps.executeUpdate();
        }
    }

    // ==================== DELETE ====================

    @Override
    public void supprimer(User user) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";

        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, user.getId());
            ps.executeUpdate();
        }
    }

    // ==================== HELPER METHODS ====================

    private User createUserFromResultSet(ResultSet rs) throws SQLException {
        String userType = rs.getString("user_type");

        switch (userType) {
            case "ETUDIANT":
                return createEtudiant(rs);
            case "PRESTATAIRE":
                return createPrestataire(rs);
            case "ADMIN":
                return createAdmin(rs);
            default:
                throw new IllegalArgumentException("Type d'utilisateur inconnu: " + userType);
        }
    }

    private Etudiant createEtudiant(ResultSet rs) throws SQLException {
        Etudiant etudiant = new Etudiant();
        etudiant.setId(rs.getInt("id"));
        etudiant.setName(rs.getString("name"));
        etudiant.setEmail(rs.getString("email"));
        etudiant.setPassword(rs.getString("password"));
        etudiant.setPhone(rs.getString("phone"));
        etudiant.setGender(rs.getString("gender"));
        etudiant.setUniversite(rs.getString("universite"));
        etudiant.setFiliere(rs.getString("filiere"));
        etudiant.setSpecialization(rs.getString("specialization"));
        etudiant.setProfilePicture(rs.getString("profile_picture"));
        etudiant.setAddress(rs.getString("address"));
        etudiant.setStatus(rs.getString("status"));
        return etudiant;
    }

    private Prestataire createPrestataire(ResultSet rs) throws SQLException {
        Prestataire prestataire = new Prestataire();
        prestataire.setId(rs.getInt("id"));
        prestataire.setName(rs.getString("name"));
        prestataire.setEmail(rs.getString("email"));
        prestataire.setPassword(rs.getString("password"));
        prestataire.setPhone(rs.getString("phone"));
        prestataire.setGender(rs.getString("gender"));
        prestataire.setUniversite(rs.getString("universite"));
        prestataire.setFiliere(rs.getString("filiere"));
        prestataire.setSpecialization(rs.getString("specialization"));
        prestataire.setTrustPoints(rs.getInt("trust_points"));
        prestataire.setProfilePicture(rs.getString("profile_picture"));
        prestataire.setAddress(rs.getString("address"));
        prestataire.setStatus(rs.getString("status"));
        return prestataire;
    }

    private Admin createAdmin(ResultSet rs) throws SQLException {
        Admin admin = new Admin();
        admin.setId(rs.getInt("id"));
        admin.setName(rs.getString("name"));
        admin.setEmail(rs.getString("email"));
        admin.setPassword(rs.getString("password"));
        admin.setPhone(rs.getString("phone"));
        admin.setGender(rs.getString("gender"));
        admin.setProfilePicture(rs.getString("profile_picture"));
        admin.setAddress(rs.getString("address"));
        admin.setStatus(rs.getString("status"));
        return admin;
    }

    private void validateUser(User user) {

        if (!UserValidator.isValidName(user.getName())) {
            throw new IllegalArgumentException(UserValidator.getNameErrorMessage());
        }

        if (!UserValidator.isValidEmail(user.getEmail())) {
            throw new IllegalArgumentException(UserValidator.getEmailErrorMessage());
        }

        if (!UserValidator.isValidPhone(user.getPhone())) {
            throw new IllegalArgumentException(UserValidator.getPhoneErrorMessage());
        }

        if (!UserValidator.isValidPassword(user.getPassword())) {
            throw new IllegalArgumentException(UserValidator.getPasswordErrorMessage());
        }

        if (!UserValidator.isValidGender(user.getGender())) {
            throw new IllegalArgumentException("❌ Genre invalide !");
        }

        if (user.getStatus() != null &&
                !UserValidator.isValidStatus(user.getStatus())) {
            throw new IllegalArgumentException("❌ Statut invalide !");
        }

        if (!UserValidator.isValidURL(user.getProfilePicture())) {
            throw new IllegalArgumentException("❌ URL de photo invalide !");
        }
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try {
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return createUserFromResultSet(rs); // ✅ loads ALL fields
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }}