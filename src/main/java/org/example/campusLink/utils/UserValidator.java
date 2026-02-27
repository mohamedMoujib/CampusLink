package org.example.campusLink.utils;

import java.util.regex.Pattern;


public class UserValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9]{8,15}$"
    );

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^.{8,}$"
    );

    /**
     Valider un email
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Valider un numéro de téléphone
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return false;
        // Accept +216 format or 8-digit local numbers
        return phone.matches("^\\+216\\s?\\d{2}\\s?\\d{3}\\s?\\d{3}$")
                || phone.matches("^\\d{8}$");
    }

    /**
     * Valider un mot de passe
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Valider un nom (non vide, longueur min/max)
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return name.trim().length() >= 2 && name.trim().length() <= 100;
    }

    /**
     * Valider un genre
     */
    public static boolean isValidGender(String gender) {
        if (gender == null) {
            return false;
        }
        return gender.equalsIgnoreCase("Male") ||
                gender.equalsIgnoreCase("Female") ||
                gender.equalsIgnoreCase("M") ||
                gender.equalsIgnoreCase("F");
    }

    /**
     * Valider un statut
     */
    public static boolean isValidStatus(String status) {
        if (status == null) {
            return false;
        }
        return status.equals("ACTIVE") ||
                status.equals("INACTIVE") ||
                status.equals("BANNED");
    }



    /**
     * Valider qu'une chaîne n'est pas vide
     */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Valider une URL (pour photo de profil)
     */
    public static boolean isValidURL(String url) {
        if (url == null || url.trim().isEmpty()) {
            return true; // URL optionnelle
        }
        return url.startsWith("http://") ||
                url.startsWith("https://") ||
                url.endsWith(".jpg") ||
                url.endsWith(".png");
    }

    /**
     * Message d'erreur pour email invalide
     */
    public static String getEmailErrorMessage() {
        return "❌ Email invalide! Format attendu: example@domain.com";
    }

    /**
     * Message d'erreur pour téléphone invalide
     */
    public static String getPhoneErrorMessage() {
        return "❌ Numéro de téléphone invalide! Format: +216 XX XXX XXX";
    }

    /**
     * Message d'erreur pour mot de passe invalide
     */
    public static String getPasswordErrorMessage() {
        return "❌ Mot de passe invalide! Minimum 6 caractères requis.";
    }

    /**
     * Message d'erreur pour nom invalide
     */
    public static String getNameErrorMessage() {
        return "❌ Nom invalide! Le nom doit contenir entre 2 et 100 caractères.";
    }
}