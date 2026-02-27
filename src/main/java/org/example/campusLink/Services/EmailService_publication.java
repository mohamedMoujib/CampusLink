package org.example.campusLink.Services;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Random;

/**
 * Service d'envoi d'emails (Gmail SMTP).
 *
 * Utilisé notamment pour notifier un tuteur lorsqu'une
 * nouvelle publication d'étudiant est compatible avec son service.
 */
public class EmailService_publication {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    // Adresse expéditrice (compte Gmail utilisé)
    private static final String EMAIL_FROM = "haouechmoujib2@gmail.com";

    /**
     * Mot de passe d'application Gmail.
     *
     * IMPORTANT :
     *  - définir la variable d'environnement CAMPUSLINK_GMAIL_APP_PASSWORD
     *  - ne jamais commiter le vrai mot de passe dans le dépôt Git
     */
    private static final String EMAIL_PASSWORD =
            System.getenv("CAMPUSLINK_GMAIL_APP_PASSWORD");

    private final Session session;

    public EmailService_publication() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
            }
        });
    }

    public String generateVerificationCode() {
        Random random = new Random();
        int code = 100_000 + random.nextInt(900_000);
        return String.valueOf(code);
    }

    /**
     * Email de notification : une nouvelle publication est compatible
     * avec le service du tuteur.
     */
    public boolean sendCompatiblePublicationEmail(
            String toEmail,
            String tutorName,
            String serviceTitle,
            String studentName,
            String publicationTitle,
            String publicationDescription,
            double budget,
            double score
    ) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("CampusLink - Nouvelle demande compatible avec votre service");

            String html = buildCompatiblePublicationEmailHTML(
                    tutorName,
                    serviceTitle,
                    studentName,
                    publicationTitle,
                    publicationDescription,
                    budget,
                    score
            );

            message.setContent(html, "text/html; charset=utf-8");
            Transport.send(message);
            return true;

        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String buildCompatiblePublicationEmailHTML(
            String tutorName,
            String serviceTitle,
            String studentName,
            String publicationTitle,
            String publicationDescription,
            double budget,
            double score
    ) {
        String template = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f8f9fa; padding: 20px; }
                        .container { background: #ffffff; padding: 30px; border-radius: 12px;
                                     box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                        .title { font-size: 20px; font-weight: bold; color: #111827; margin-bottom: 10px; }
                        .badge { display: inline-block; padding: 6px 12px; border-radius: 999px;
                                 background-color: #eef2ff; color: #4338ca; font-size: 12px;
                                 font-weight: 600; margin-bottom: 16px; }
                        .section-title { font-size: 14px; font-weight: bold; color: #374151; margin-top: 18px; }
                        .muted { color: #6b7280; font-size: 13px; }
                        .score { font-size: 28px; font-weight: bold; color: #16a34a; margin: 8px 0; }
                        .cta { margin-top: 24px; }
                        .btn { display: inline-block; padding: 10px 18px; border-radius: 999px;
                               background-color: #4f46e5; color: #ffffff; text-decoration: none;
                               font-weight: 600; font-size: 14px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="badge">Nouvelle demande compatible</div>
                        <div class="title">Bonjour %s,</div>
                        <p class="muted">
                            Une nouvelle publication d'étudiant semble correspondre à votre service
                            <strong>"%s"</strong> sur CampusLink.
                        </p>

                        <div class="section-title">Détails de la demande :</div>
                        <p>
                            <strong>Étudiant :</strong> %s<br>
                            <strong>Titre de la demande :</strong> %s<br>
                            <strong>Budget indicatif :</strong> %.2f €<br>
                        </p>
                        <p class="muted">%s</p>

                        <div class="section-title">Score de compatibilité estimé :</div>
                        <div class="score">%.0f&nbsp;%%</div>
                        <p class="muted">
                            Plus le score est élevé, plus la demande correspond à votre service
                            (catégorie, mots-clés, localisation, budget).
                        </p>

                        <div class="cta">
                            <a class="btn" href="#">
                                Ouvrir CampusLink et voir la demande
                            </a>
                        </div>

                        <p class="muted" style="margin-top:28px;">
                            Si vous ne souhaitez plus recevoir ce type de notification,
                            vous pouvez ajuster vos préférences depuis votre profil tuteur.
                        </p>
                    </div>
                </body>
                </html>
                """;

        return String.format(
                template,
                tutorName != null ? tutorName : "Tuteur",
                serviceTitle != null ? serviceTitle : "Votre service",
                studentName != null ? studentName : "Un étudiant",
                publicationTitle != null ? publicationTitle : "Demande d'aide",
                budget,
                publicationDescription != null ? publicationDescription : "",
                score
        );
    }
}

