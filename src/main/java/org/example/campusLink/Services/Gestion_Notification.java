package org.example.campusLink.Services;

import org.example.campusLink.utils.NotificationCache;
import org.example.campusLink.utils.NotificationCache.CachedNotification;
import org.example.campusLink.utils.NovuApiClient;
import org.example.campusLink.utils.MyDatabase;

import javax.mail.*;
import javax.mail.internet.*;
import java.sql.*;
import java.util.List;
import java.util.Properties;

/**
 * 📧 SERVICE DE NOTIFICATION — Novu API + Cache + Email
 *
 * Trois canaux :
 *   1. Cache in-memory (TTL auto-expiry) → pour affichage JavaFX
 *   2. Novu API → push in-app + email via dashboard Novu
 *   3. Email SMTP direct (Gmail) → fallback si Novu indisponible
 */
public class Gestion_Notification {

    private final Connection connection;

    // ── SMTP config (fallback email) ───────────────────────────────────────────
    private static final String SMTP_HOST      = "smtp.gmail.com";
    private static final String SMTP_PORT      = "587";
    private static final String EMAIL_FROM     = "votre-email@gmail.com";
    private static final String EMAIL_PASSWORD = "votre-app-password";

    // ── Cache ──────────────────────────────────────────────────────────────────
    private final NotificationCache cache = NotificationCache.getInstance();

    public Gestion_Notification() throws SQLException {
        connection = MyDatabase.getInstance().getConnection();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. NOTIFICATION IN-APP (Cache + Novu API)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * 🔔 Crée une notification :
     *   - Stockée en cache local (TTL 30min, auto-supprimée)
     *   - Envoyée via Novu API (in-app + email si configuré)
     *
     * @param userId  destinataire
     * @param titre   titre court
     * @param message corps du message
     */
    public void creerNotificationInApp(int userId, String titre, String message) {
        // 1. Cache local (pour affichage immédiat dans JavaFX)
        String cacheId = cache.addNotification(userId, titre, message);
        System.out.println("🔔 [Cache] Notification → id=" + cacheId + " | user #" + userId);

        // 2. Novu API (in-app + email)
        boolean sent = NovuApiClient.sendNotification(userId, titre, message);
        if (!sent) {
            System.err.println("⚠️  [Novu] Push non délivré — notification conservée en cache.");
        }
    }

    /**
     * Notification avec email explicite (utilisé par Gestion_Matching).
     *
     * @param userId  destinataire
     * @param titre   titre
     * @param message message
     * @param email   adresse email du destinataire
     */
    public void creerNotificationInApp(int userId, String titre,
                                       String message, String email) {
        // Cache local
        String cacheId = cache.addNotification(userId, titre, message);
        System.out.println("🔔 [Cache] Notification → id=" + cacheId + " | user #" + userId);

        // Novu (in-app + email)
        boolean sent = NovuApiClient.sendNotification(userId, titre, message, email);
        if (!sent) {
            System.err.println("⚠️  [Novu] Push non délivré — tentative email direct.");
            // Fallback email SMTP
            envoyerEmail(email, titre, message);
        }
    }

    /**
     * Notification avec TTL personnalisé.
     */
    public void creerNotificationInApp(int userId, String titre,
                                       String message, long ttlMinutes) {
        String cacheId = cache.addNotification(userId, titre, message,
                ttlMinutes, java.util.concurrent.TimeUnit.MINUTES);
        System.out.println("🔔 [Cache] Notification (TTL=" + ttlMinutes + "min) → id="
                + cacheId + " | user #" + userId);

        NovuApiClient.sendNotification(userId, titre, message);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. CONSULTATION DU CACHE LOCAL
    // ══════════════════════════════════════════════════════════════════════════

    /** Toutes les notifications actives d'un utilisateur. */
    public List<CachedNotification> getNotificationsForUser(int userId) {
        return cache.getForUser(userId);
    }

    /** Nombre de notifications non lues (cache local). */
    public long getUnreadNotificationsCount(int userId) {
        return cache.getUnreadCount(userId);
    }

    /**
     * Nombre de notifications non lues depuis Novu API.
     * Utilisé pour mettre à jour le badge de la cloche JavaFX.
     */
    public long getUnreadCountFromApi(int userId) {
        return NovuApiClient.getUnreadCount(userId);
    }

    /** Marquer une notification comme lue (cache local). */
    public void marquerCommeLue(String notificationId) {
        boolean found = cache.markAsRead(notificationId);
        System.out.println(found
                ? "✅ [Cache] Notification lue: " + notificationId
                : "⚠️  [Cache] Notification introuvable: " + notificationId);
    }

    /** Marquer une notification comme lue sur Novu API. */
    public void marquerCommeLueApi(int userId, String messageId) {
        NovuApiClient.markAsRead(userId, messageId);
    }

    /** Marquer toutes les notifications comme lues sur Novu API. */
    public void marquerToutesCommeLues(int userId) {
        NovuApiClient.markAllAsRead(userId);
        cache.clearForUser(userId);
    }

    /** Supprimer une notification du cache. */
    public void supprimerNotification(String notificationId) {
        cache.remove(notificationId);
    }

    /** Supprimer toutes les notifications d'un utilisateur du cache. */
    public void viderNotificationsUser(int userId) {
        cache.clearForUser(userId);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. EMAIL SMTP (direct / fallback)
    // ══════════════════════════════════════════════════════════════════════════

    /** Envoyer un email via SMTP Gmail. */
    public void envoyerEmail(String destinataire, String sujet, String contenu) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth",            "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host",            SMTP_HOST);
            props.put("mail.smtp.port",            SMTP_PORT);
            props.put("mail.smtp.ssl.trust",       SMTP_HOST);

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM, "CampusLink"));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(destinataire));
            message.setSubject(sujet);
            message.setContent(creerTemplateHTML(sujet, contenu),
                    "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("✅ Email envoyé à: " + destinataire);
            enregistrerLogEmail(destinataire, sujet, "SENT");

        } catch (Exception e) {
            System.err.println("❌ Erreur email: " + e.getMessage());
            try {
                enregistrerLogEmail(destinataire, sujet, "FAILED: " + e.getMessage());
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS PRIVÉS
    // ══════════════════════════════════════════════════════════════════════════

    private String creerTemplateHTML(String titre, String contenu) {
        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>
              body{font-family:'Segoe UI',sans-serif;background:#f4f4f7;margin:0;padding:0}
              .container{max-width:600px;margin:40px auto;background:#fff;
                border-radius:12px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,.1)}
              .header{background:linear-gradient(135deg,#667eea,#764ba2);
                color:#fff;padding:30px;text-align:center}
              .header h1{margin:0;font-size:24px}
              .content{padding:40px 30px;color:#333;line-height:1.8}
              .highlight{background:#f0f4ff;padding:20px;border-radius:8px;
                margin:20px 0;border-left:4px solid #667eea}
              .footer{background:#f8f9fa;padding:20px;text-align:center;
                color:#6c757d;font-size:13px}
            </style></head><body>
            <div class="container">
              <div class="header"><h1>🎓 CampusLink</h1></div>
              <div class="content">
                <h2>%s</h2>
                <div class="highlight">%s</div>
              </div>
              <div class="footer">
                <p>Vous recevez cet email car vous êtes prestataire sur CampusLink.</p>
              </div>
            </div></body></html>
            """.formatted(titre, contenu.replace("\n", "<br>"));
    }

    private void enregistrerLogEmail(String destinataire, String sujet,
                                     String status) throws SQLException {
        String sql = "INSERT INTO email_logs (recipient, subject, status, sent_at) " +
                "VALUES (?, ?, ?, NOW())";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, destinataire);
            ps.setString(2, sujet);
            ps.setString(3, status);
            ps.executeUpdate();
        }
    }
}