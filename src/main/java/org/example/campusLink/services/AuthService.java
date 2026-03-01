package org.example.campusLink.Services;

import org.example.campusLink.entities.*;
import org.example.campusLink.utils.PasswordUtil;

import java.sql.SQLException;

public class AuthService {

    private final UserService userService;

    public AuthService() {
        // ✅ PLUS de connexion stockée
        this.userService = new UserService();
        System.out.println("✅ AuthService initialisé");
    }

    // ==================== SIGNUP ====================

    public Etudiant signupEtudiant(Etudiant etudiant) throws SQLException {
        // Vérifier email unique
        if (userService.getUserByEmail(etudiant.getEmail()) != null) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        // Hash password
        String hashedPassword = PasswordUtil.hashPassword(etudiant.getPassword());
        etudiant.setPassword(hashedPassword);

        // Default status
        etudiant.setStatus("INACTIVE");

        // Save
        userService.ajouterEtudiant(etudiant);

        return etudiant;
    }

    public Prestataire signupPrestataire(Prestataire prestataire) throws SQLException {
        if (userService.getUserByEmail(prestataire.getEmail()) != null) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        String hashedPassword = PasswordUtil.hashPassword(prestataire.getPassword());
        prestataire.setPassword(hashedPassword);

        prestataire.setStatus("INACTIVE");
        if (prestataire.getTrustPoints() == 0) {
            prestataire.setTrustPoints(0);
        }

        userService.ajouterPrestataire(prestataire);
        return prestataire;
    }

    // ==================== LOGIN ====================

    public User login(String email, String password, String role) throws SQLException {
        User user = userService.getUserByEmail(email);

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (!PasswordUtil.checkPassword(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        String userType = getUserType(user);
        if (!userType.equals(role)) {
            throw new IllegalArgumentException("User does not have role " + role);
        }

        if ("BANNED".equals(user.getStatus())) {
            throw new IllegalArgumentException("This account is banned");
        }

        return user;
    }

    // ==================== TYPE HELPERS ====================

    public String getUserType(User user) {
        if (user instanceof Etudiant) return "ETUDIANT";
        if (user instanceof Prestataire) return "PRESTATAIRE";
        if (user instanceof Admin) return "ADMIN";
        return "UNKNOWN";
    }

    public boolean isEtudiant(User user) { return user instanceof Etudiant; }
    public boolean isPrestataire(User user) { return user instanceof Prestataire; }
    public boolean isAdmin(User user) { return user instanceof Admin; }

    public Etudiant castToEtudiant(User user) {
        if (user instanceof Etudiant e) return e;
        throw new IllegalArgumentException("L'utilisateur n'est pas un étudiant");
    }

    public Prestataire castToPrestataire(User user) {
        if (user instanceof Prestataire p) return p;
        throw new IllegalArgumentException("L'utilisateur n'est pas un prestataire");
    }

    public Admin castToAdmin(User user) {
        if (user instanceof Admin a) return a;
        throw new IllegalArgumentException("L'utilisateur n'est pas un administrateur");
    }

    // ==================== PASSWORD ====================

    public void changePassword(User user, String currentPassword, String newPassword) throws SQLException {
        if (!PasswordUtil.checkPassword(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mot de passe actuel incorrect");
        }

        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères");
        }

        String hashedPassword = PasswordUtil.hashPassword(newPassword);
        user.setPassword(hashedPassword);

        userService.modifier(user);
    }

    // ==================== STATUS ====================

    public void activateAccount(User user) throws SQLException {
        user.setStatus("ACTIVE");
        userService.modifier(user);
    }

    public void deactivateAccount(User user) throws SQLException {
        user.setStatus("INACTIVE");
        userService.modifier(user);
    }

    public void banAccount(User user) throws SQLException {
        user.setStatus("BANNED");
        userService.modifier(user);
    }
}