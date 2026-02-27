package org.example.campusLink.services;

import org.example.campusLink.utils.VerificationCodeCache;

/**
 * Service de gestion des codes de vérification (EN MÉMOIRE - pas de BD!)
 */
public class VerificationService {

    private final EmailService emailService;
    private final VerificationCodeCache codeCache;

    public VerificationService() {
        this.emailService = new EmailService();
        this.codeCache = VerificationCodeCache.getInstance();
    }

    /**
     * Créer et envoyer un code de vérification de compte
     */
    public boolean sendAccountVerificationCode(String email, String userName) {
        // Générer le code
        String code = emailService.generateVerificationCode();

        // Stocker EN MÉMOIRE avec expiration de 15 minutes
        codeCache.storeCode(email, code, "ACCOUNT_VERIFICATION", 15);

        // Envoyer l'email
        return emailService.sendAccountVerificationEmail(email, userName, code);
    }

    /**
     * Créer et envoyer un code de réinitialisation de mot de passe
     */
    public boolean sendPasswordResetCode(String email, String userName) {
        // Générer le code
        String code = emailService.generateVerificationCode();

        // Stocker EN MÉMOIRE avec expiration de 15 minutes
        codeCache.storeCode(email, code, "PASSWORD_RESET", 15);

        // Envoyer l'email
        return emailService.sendPasswordResetEmail(email, userName, code);
    }

    /**
     * Vérifier un code
     */
    public boolean verifyCode(String email, String code, String type) {
        return codeCache.verifyCode(email, code, type);
    }

    /**
     * Vérifier si un code existe pour un email
     */
    public boolean hasValidCode(String email, String type) {
        return codeCache.hasValidCode(email, type);
    }

    /**
     * Supprimer le code d'un email
     */
    public void deleteCodeForEmail(String email) {
        codeCache.removeCode(email);
    }

    /**
     * Obtenir le temps restant avant expiration (en secondes)
     */
    public long getTimeRemaining(String email) {
        return codeCache.getTimeRemaining(email);
    }
}