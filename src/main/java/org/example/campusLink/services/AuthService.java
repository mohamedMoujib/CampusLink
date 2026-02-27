package org.example.campusLink.services;

import java.sql.Connection;
import org.example.campusLink.entities.*;
import org.example.campusLink.utils.MyDatabase;
import org.example.campusLink.utils.PasswordUtil;

import java.sql.SQLException;


public class AuthService {
    private final Connection connection;
    private final UserService userService;
    public AuthService() {
        this.connection = MyDatabase.getInstance().getConnection();
        this.userService = new UserService(this.connection);    }

    // ==================== SIGNUP ====================

    public Etudiant signupEtudiant(Etudiant etudiant) throws SQLException {
        // Valider email unique
        if (userService.getUserByEmail(etudiant.getEmail()) != null) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        // Hash password
        String hashedPassword = PasswordUtil.hashPassword(etudiant.getPassword());
        etudiant.setPassword(hashedPassword);

        // Set default status
        etudiant.setStatus("INACTIVE");

        // Save
        userService.ajouterEtudiant(etudiant);

        return etudiant;
    }

    /**
     * Inscription d'un prestataire
     */
    public Prestataire signupPrestataire(Prestataire prestataire) throws SQLException {
        // Valider email unique
        if (userService.getUserByEmail(prestataire.getEmail()) != null) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        // Hash password
        String hashedPassword = PasswordUtil.hashPassword(prestataire.getPassword());
        prestataire.setPassword(hashedPassword);

        // Set defaults
        prestataire.setStatus("INACTIVE");
        if (prestataire.getTrustPoints() == 0) {
            prestataire.setTrustPoints(0);
        }

        // Save
        userService.ajouterPrestataire(prestataire);

        return prestataire;
    }

    // ==================== LOGIN ====================

    /**
     * Connexion - Retourne User (peut être Etudiant, Prestataire ou Admin)
     */
    public User login(String email, String password, String role) throws SQLException {
        // Récupérer l'utilisateur
        User user = userService.getUserByEmail(email);

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        // Vérifier le mot de passe
        if (!PasswordUtil.checkPassword(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        // Vérifier le rôle
        String userType = getUserType(user);
        if (!userType.equals(role)) {
            throw new IllegalArgumentException("User does not have role " + role);
        }

        // Vérifier le statut
        if ("BANNED".equals(user.getStatus())) {
            throw new IllegalArgumentException("This account is banned");
        }

        return user;
    }

    // ==================== UTILISATION DE INSTANCEOF ====================

    /**
     * Obtenir le type d'utilisateur (ETUDIANT, PRESTATAIRE, ADMIN)
     */
    public String getUserType(User user) {
        if (user instanceof Etudiant) {
            return "ETUDIANT";
        } else if (user instanceof Prestataire) {
            return "PRESTATAIRE";
        } else if (user instanceof Admin) {
            return "ADMIN";
        }
        return "UNKNOWN";
    }

    /**
     * Vérifier si l'utilisateur est un étudiant
     */
    public boolean isEtudiant(User user) {
        return user instanceof Etudiant;
    }

    /**
     * Vérifier si l'utilisateur est un prestataire
     */
    public boolean isPrestataire(User user) {
        return user instanceof Prestataire;
    }

    /**
     * Vérifier si l'utilisateur est un admin
     */
    public boolean isAdmin(User user) {
        return user instanceof Admin;
    }

    /**
     * Caster en Etudiant (avec vérification)
     */
    public Etudiant castToEtudiant(User user) {
        if (user instanceof Etudiant) {
            return (Etudiant) user;
        }
        throw new IllegalArgumentException("L'utilisateur n'est pas un étudiant");
    }

    /**
     * Caster en Prestataire (avec vérification)
     */
    public Prestataire castToPrestataire(User user) {
        if (user instanceof Prestataire) {
            return (Prestataire) user;
        }
        throw new IllegalArgumentException("L'utilisateur n'est pas un prestataire");
    }

    /**
     * Caster en Admin (avec vérification)
     */
    public Admin castToAdmin(User user) {
        if (user instanceof Admin) {
            return (Admin) user;
        }
        throw new IllegalArgumentException("L'utilisateur n'est pas un administrateur");
    }

    // ==================== CHANGE PASSWORD ====================

    /**
     * Changer le mot de passe
     */
    public void changePassword(User user, String currentPassword, String newPassword) throws SQLException {
        // Vérifier le mot de passe actuel
        if (!PasswordUtil.checkPassword(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mot de passe actuel incorrect");
        }

        // Valider le nouveau mot de passe
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères");
        }

        // Hash et update
        String hashedPassword = PasswordUtil.hashPassword(newPassword);
        user.setPassword(hashedPassword);

        userService.modifier(user);
    }

    // ==================== ACTIVATE ACCOUNT ====================

    /**
     * Activer un compte (pour Admin)
     */
    public void activateAccount(User user) throws SQLException {
        user.setStatus("ACTIVE");
        userService.modifier(user);
    }

    /**
     * Désactiver un compte (pour Admin)
     */
    public void deactivateAccount(User user) throws SQLException {
        user.setStatus("INACTIVE");
        userService.modifier(user);
    }

    /**
     * Bannir un compte (pour Admin)
     */
    public void banAccount(User user) throws SQLException {
        user.setStatus("BANNED");
        userService.modifier(user);
    }
}