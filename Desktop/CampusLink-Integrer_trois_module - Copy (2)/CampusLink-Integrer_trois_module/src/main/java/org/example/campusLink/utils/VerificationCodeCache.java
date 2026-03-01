package org.example.campusLink.utils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class VerificationCodeCache {

    private static VerificationCodeCache instance;

    // Structure pour stocker un code avec son expiration
    private static class CodeEntry {
        String code;
        LocalDateTime expiresAt;
        String type; // "PASSWORD_RESET" ou "ACCOUNT_VERIFICATION"

        CodeEntry(String code, LocalDateTime expiresAt, String type) {
            this.code = code;
            this.expiresAt = expiresAt;
            this.type = type;
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    // Cache: email -> CodeEntry
    private final Map<String, CodeEntry> codeCache = new ConcurrentHashMap<>();

    // Nettoyage automatique des codes expirés
    private final ScheduledExecutorService cleanupScheduler;

    private VerificationCodeCache() {
        // Nettoyer les codes expirés toutes les minutes
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
        cleanupScheduler.scheduleAtFixedRate(
                this::cleanupExpiredCodes,
                1, 1, TimeUnit.MINUTES
        );

        System.out.println("✅ VerificationCodeCache initialisé");
    }

    public static synchronized VerificationCodeCache getInstance() {
        if (instance == null) {
            instance = new VerificationCodeCache();
        }
        return instance;
    }

    /**
     * Stocker un code de vérification
     */
    public void storeCode(String email, String code, String type, int expirationMinutes) {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);
        CodeEntry entry = new CodeEntry(code, expiresAt, type);

        codeCache.put(email.toLowerCase(), entry);

        System.out.println("✅ Code stocké en mémoire pour: " + email + " (expire à " + expiresAt + ")");
    }

    /**
     * Vérifier un code
     */
    public boolean verifyCode(String email, String code, String type) {
        CodeEntry entry = codeCache.get(email.toLowerCase());

        if (entry == null) {
            System.out.println("❌ Aucun code trouvé pour: " + email);
            return false;
        }

        if (entry.isExpired()) {
            System.out.println("❌ Code expiré pour: " + email);
            codeCache.remove(email.toLowerCase());
            return false;
        }

        if (!entry.type.equals(type)) {
            System.out.println("❌ Type de code incorrect pour: " + email);
            return false;
        }

        if (!entry.code.equals(code)) {
            System.out.println("❌ Code incorrect pour: " + email);
            return false;
        }

        // Code valide! Supprimer pour usage unique
        codeCache.remove(email.toLowerCase());
        System.out.println("✅ Code vérifié et supprimé pour: " + email);

        return true;
    }

    /**
     * Vérifier si un code existe pour un email
     */
    public boolean hasValidCode(String email, String type) {
        CodeEntry entry = codeCache.get(email.toLowerCase());

        if (entry == null) {
            return false;
        }

        if (entry.isExpired()) {
            codeCache.remove(email.toLowerCase());
            return false;
        }

        return entry.type.equals(type);
    }

    /**
     * Supprimer les codes d'un email
     */
    public void removeCode(String email) {
        codeCache.remove(email.toLowerCase());
        System.out.println("🗑️ Code supprimé pour: " + email);
    }

    /**
     * Obtenir le temps restant avant expiration (en secondes)
     */
    public long getTimeRemaining(String email) {
        CodeEntry entry = codeCache.get(email.toLowerCase());

        if (entry == null || entry.isExpired()) {
            return 0;
        }

        return java.time.Duration.between(LocalDateTime.now(), entry.expiresAt).getSeconds();
    }

    /**
     * Nettoyer les codes expirés (appelé automatiquement)
     */
    private void cleanupExpiredCodes() {
        int removed = 0;
        for (Map.Entry<String, CodeEntry> mapEntry : codeCache.entrySet()) {
            if (mapEntry.getValue().isExpired()) {
                codeCache.remove(mapEntry.getKey());
                removed++;
            }
        }

        if (removed > 0) {
            System.out.println("🧹 Nettoyage: " + removed + " code(s) expiré(s) supprimé(s)");
        }
    }

    /**
     * Obtenir le nombre de codes en cache
     */
    public int getCacheSize() {
        return codeCache.size();
    }

    /**
     * Vider tout le cache (pour tests)
     */
    public void clearAll() {
        codeCache.clear();
        System.out.println("🗑️ Cache vidé complètement");
    }

    /**
     * Arrêter le scheduler (à appeler à la fermeture de l'app)
     */
    public void shutdown() {
        cleanupScheduler.shutdown();
        System.out.println("⏹️ VerificationCodeCache arrêté");
    }
}