package org.example.campusLink.services;

import org.example.campusLink.utils.NotificationCache;
import org.example.campusLink.utils.NotificationCache.CachedNotification;
import org.example.campusLink.utils.NovuApiClient;
import org.example.campusLink.utils.MyDatabase;

import javax.mail.*;
import javax.mail.internet.*;
import java.sql.*;
import java.util.List;
import java.util.Properties;

public class Gestion_Notification {

    private Connection getConnection() throws SQLException {
        return MyDatabase.getInstance().getConnection();
    }

    // ── SMTP ──────────────────────────────────────────────────────
    private static final String SMTP_HOST      = "smtp.gmail.com";
    private static final String SMTP_PORT      = "587";
    private static final String EMAIL_FROM     = "haouechmoujib2@gmail.com";
    private static final String EMAIL_PASSWORD = "ihuk nadc ynwh nxmr";

    // ── Cache ─────────────────────────────────────────────────────
    private final NotificationCache cache = NotificationCache.getInstance();

    public Gestion_Notification() throws SQLException {
    }

    // ══════════════════════════════════════════════════════════════
    // 1. NOTIFICATION IN-APP (Cache + Novu)
    // ══════════════════════════════════════════════════════════════

    public void creerNotificationInApp(int userId, String titre, String message) {
        // ① Cache local → alimente la cloche JavaFX
        String cacheId = cache.addNotification(userId, titre, message);
        System.out.println("🔔 [Cache] Notification → id=" + cacheId + " | user #" + userId);

        // ② Novu API → push externe
        boolean sent = NovuApiClient.sendNotification(userId, titre, message);
        if (!sent) {
            System.err.println("⚠️  [Novu] Push non délivré — notification conservée en cache.");
        }
    }

    public void creerNotificationInApp(int userId, String titre,
                                       String message, String email) {
        // ① Cache local
        String cacheId = cache.addNotification(userId, titre, message);
        System.out.println("🔔 [Cache] Notification → id=" + cacheId + " | user #" + userId);

        // ② Novu API
        boolean sent = NovuApiClient.sendNotification(userId, titre, message, email);
        if (!sent) {
            System.err.println("⚠️  [Novu] Push non délivré — tentative email direct.");
            // ③ Fallback SMTP direct
            envoyerEmail(email, titre, message);
        }
    }

    public void creerNotificationInApp(int userId, String titre,
                                       String message, long ttlMinutes) {
        // ① Cache local avec TTL personnalisé
        String cacheId = cache.addNotification(userId, titre, message,
                ttlMinutes, java.util.concurrent.TimeUnit.MINUTES);
        System.out.println("🔔 [Cache] Notification (TTL=" + ttlMinutes + "min) → id="
                + cacheId + " | user #" + userId);

        // ② Novu API
        boolean sent = NovuApiClient.sendNotification(userId, titre, message);
        if (!sent) {
            System.err.println("⚠️  [Novu] Push non délivré — notification conservée en cache.");
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 2. CACHE QUERIES
    // ══════════════════════════════════════════════════════════════

    public List<CachedNotification> getNotificationsForUser(int userId) {
        return cache.getForUser(userId);
    }

    public long getUnreadNotificationsCount(int userId) {
        return cache.getUnreadCount(userId);
    }

    public long getUnreadCountFromApi(int userId) {
        return NovuApiClient.getUnreadCount(userId);
    }

    public void marquerCommeLue(String notificationId) {
        boolean found = cache.markAsRead(notificationId);
        System.out.println(found
                ? "✅ [Cache] Notification lue: " + notificationId
                : "⚠️  [Cache] Notification introuvable: " + notificationId);
    }

    public void marquerCommeLueApi(int userId, String messageId) {
        NovuApiClient.markAsRead(userId, messageId);
    }

    public void marquerToutesCommeLues(int userId) {
        NovuApiClient.markAllAsRead(userId);
        cache.clearForUser(userId);
    }

    public void supprimerNotification(String notificationId) {
        cache.remove(notificationId);
    }

    public void viderNotificationsUser(int userId) {
        cache.clearForUser(userId);
    }

    // ══════════════════════════════════════════════════════════════
    // 3. EMAIL SMTP
    // ══════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════

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
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, destinataire);
            ps.setString(2, sujet);
            ps.setString(3, status);
            ps.executeUpdate();
        }
    }
}