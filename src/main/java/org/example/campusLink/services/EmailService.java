package org.example.campusLink.Services;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.Random;

public class EmailService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    private static final String EMAIL_FROM = "haouechmoujib2@gmail.com";
    private static final String EMAIL_PASSWORD = "ihuk nadc ynwh nxmr"; // CHANGE THIS

    private Session session;

    public EmailService() {
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
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    public boolean sendAccountVerificationEmail(String toEmail, String userName, String verificationCode) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("CampusLink - Vérification de votre compte");

            message.setContent(
                    buildVerificationEmailHTML(userName, verificationCode),
                    "text/html; charset=utf-8"
            );

            Transport.send(message);
            return true;

        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendPasswordResetEmail(String toEmail, String userName, String resetCode) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("CampusLink - Réinitialisation de votre mot de passe");

            message.setContent(
                    buildPasswordResetEmailHTML(userName, resetCode),
                    "text/html; charset=utf-8"
            );

            Transport.send(message);
            return true;

        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String buildVerificationEmailHTML(String userName, String code) {

        String template = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial; background-color: #f8f9fa; padding: 20px; }
                        .container { background: white; padding: 30px; border-radius: 12px; }
                        .code { font-size: 32px; font-weight: bold; color: #4f46e5; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h2>Bonjour %s !</h2>
                        <p>Utilisez le code ci-dessous pour vérifier votre compte :</p>
                        <div class="code">%s</div>
                        <p>Ce code expire dans 15 minutes.</p>
                    </div>
                </body>
                </html>
                """;

        return String.format(template, userName, code);
    }

    private String buildPasswordResetEmailHTML(String userName, String code) {

        String template = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial; background-color: #f8f9fa; padding: 20px; }
                        .container { background: white; padding: 30px; border-radius: 12px; }
                        .code { font-size: 32px; font-weight: bold; color: #dc2626; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h2>Bonjour %s !</h2>
                        <p>Utilisez le code ci-dessous pour réinitialiser votre mot de passe :</p>
                        <div class="code">%s</div>
                        <p>Ce code expire dans 15 minutes.</p>
                    </div>
                </body>
                </html>
                """;

        return String.format(template, userName, code);
    }
}