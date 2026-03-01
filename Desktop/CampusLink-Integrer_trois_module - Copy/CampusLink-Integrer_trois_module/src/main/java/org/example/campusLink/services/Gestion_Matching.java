package org.example.campusLink.services;

import org.example.campusLink.utils.MyDatabase;
import org.example.campusLink.utils.NotificationCache;
import org.example.campusLink.utils.NovuApiClient;

import java.sql.*;
import java.util.*;

public class Gestion_Matching {

    private final Gestion_Notification notificationService;
    private final EmailService_publication emailService;
    private final NotificationCache notifCache = NotificationCache.getInstance();

    private Connection getConnection() throws SQLException {
        return MyDatabase.getInstance().getConnection();
    }

    public Gestion_Matching() throws SQLException {
        notificationService = new Gestion_Notification();
        emailService = new EmailService_publication();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MAIN ENTRY POINT
    // ══════════════════════════════════════════════════════════════════════════

    public void analyserNouvellesPublications() throws SQLException {
        System.out.println("🔍 ========== DÉMARRAGE ANALYSE MATCHING ==========");
        System.out.println("📅 Date/Heure: " + new java.util.Date());

        List<PublicationData> publications = getPublicationsRecentes();
        System.out.println("📋 Publications DEMANDE_SERVICE récentes : " + publications.size());

        List<ServiceData> services = getServicesActifs();
        System.out.println("🎓 Services actifs : " + services.size());

        if (publications.isEmpty()) {
            System.out.println("ℹ️  Aucune publication récente à analyser");
            return;
        }
        if (services.isEmpty()) {
            System.out.println("⚠️  Aucun service actif disponible");
            return;
        }

        int totalMatches = 0;

        for (PublicationData pub : publications) {
            System.out.println("\n🔎 Analyse publication #" + pub.id + " : " + pub.titre);

            List<MatchResult> matches = trouverServicesCompatibles(pub, services);
            System.out.println("   ➜ " + matches.size() + " service(s) compatible(s)");

            for (MatchResult match : matches) {
                if (match.getScore() >= 60) {
                    System.out.printf("   ✅ Match validé! Service #%d – Score: %.1f%%%n",
                            match.getService().id, match.getScore());

                    if (!matchDejaExistant(pub.id, match.getService().id)) {
                        notifierTuteur(pub, match);
                        enregistrerMatch(pub.id, match.getService().id, match.getScore());
                        totalMatches++;
                    } else {
                        System.out.println("   ℹ️  Match déjà enregistré, notification ignorée");
                    }
                }
            }
        }

        System.out.println("\n✅ ========== ANALYSE TERMINÉE ==========");
        System.out.println("📊 " + totalMatches + " nouveau(x) match(s) trouvé(s)");
        System.out.println("================================================\n");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DATA QUERIES
    // ══════════════════════════════════════════════════════════════════════════

    private List<PublicationData> getPublicationsRecentes() throws SQLException {
        List<PublicationData> publications = new ArrayList<>();

        String sql = """
            SELECT
                p.id,
                p.student_id,
                p.titre,
                p.message,
                p.localisation,
                p.proposed_price,
                p.service_id,
                p.category_id
            FROM publications p
            WHERE p.type_publication = 'DEMANDE_SERVICE'
              AND p.status IN ('ACTIVE', 'EN_ATTENTE')
              AND p.created_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
            ORDER BY p.created_at DESC
        """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PublicationData pub = new PublicationData();
                pub.id            = rs.getInt("id");
                pub.studentId     = rs.getInt("student_id");
                pub.titre         = rs.getString("titre");
                pub.message       = rs.getString("message");
                pub.localisation  = rs.getString("localisation");
                pub.proposedPrice = rs.getDouble("proposed_price");
                pub.serviceId     = (Integer) rs.getObject("service_id");
                pub.categoryId    = (Integer) rs.getObject("category_id");
                publications.add(pub);
            }
        }
        return publications;
    }

    private List<ServiceData> getServicesActifs() throws SQLException {
        List<ServiceData> services = new ArrayList<>();

        String sql = """
            SELECT
                s.id,
                s.title,
                s.description,
                s.price,
                s.prestataire_id,
                s.category_id,
                c.name  AS category_name,
                u.email AS prestataire_email,
                u.name  AS prestataire_name
            FROM services s
            LEFT JOIN categories c ON s.category_id = c.id
            LEFT JOIN users u ON s.prestataire_id = u.id
            WHERE s.status IN ('EN_ATTENTE', 'CONFIRMEE', 'ACTIF')
            ORDER BY s.id DESC
        """;

        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ServiceData svc = new ServiceData();
                svc.id               = rs.getInt("id");
                svc.title            = rs.getString("title");
                svc.description      = rs.getString("description");
                svc.price            = rs.getDouble("price");
                svc.prestataireId    = rs.getInt("prestataire_id");
                svc.categoryId       = rs.getInt("category_id");
                svc.categoryName     = rs.getString("category_name");
                svc.prestataireEmail = rs.getString("prestataire_email");
                svc.prestataireName  = rs.getString("prestataire_name");
                services.add(svc);
            }
        }
        return services;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MATCHING ALGORITHM
    // ══════════════════════════════════════════════════════════════════════════

    private List<MatchResult> trouverServicesCompatibles(PublicationData pub,
                                                         List<ServiceData> services)
            throws SQLException {
        List<MatchResult> results = new ArrayList<>();
        for (ServiceData svc : services) {
            double score = calculerScoreCompatibilite(pub, svc);
            if (score > 0) results.add(new MatchResult(svc, score));
        }
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return results;
    }

    private double calculerScoreCompatibilite(PublicationData pub, ServiceData svc) {
        double score = 0;
        score += calculerScoreCategorie(pub, svc);
        score += calculerScoreMotsCles(pub, svc);
        score += calculerScoreLocalisation(pub, svc);
        score += calculerScorePrix(pub, svc);
        return Math.min(score, 100);
    }

    private double calculerScoreCategorie(PublicationData pub, ServiceData svc) {
        if (pub.categoryId != null && pub.categoryId.equals(svc.categoryId)) return 40;
        if (svc.categoryName != null) {
            String texte     = (pub.titre + " " + pub.message).toLowerCase();
            String categorie = svc.categoryName.toLowerCase();
            if (texte.contains(categorie)) return 35;
            Map<String, List<String>> dict = getMotsClesCategorie();
            if (dict.containsKey(categorie)) {
                for (String mot : dict.get(categorie)) {
                    if (texte.contains(mot.toLowerCase())) return 30;
                }
            }
        }
        return 0;
    }

    private double calculerScoreMotsCles(PublicationData pub, ServiceData svc) {
        Set<String> motsPub = extraireMotsImportants(
                (pub.titre + " " + pub.message).toLowerCase());
        Set<String> motsSvc = extraireMotsImportants(
                (svc.title + " " + (svc.description != null ? svc.description : "")).toLowerCase());
        Set<String> communs = new HashSet<>(motsPub);
        communs.retainAll(motsSvc);
        if (communs.isEmpty()) return 0;
        double ratio = (double) communs.size() / Math.max(motsPub.size(), motsSvc.size());
        return Math.min(ratio * 30, 30);
    }

    private double calculerScoreLocalisation(PublicationData pub, ServiceData svc) {
        return (pub.localisation != null && !pub.localisation.trim().isEmpty()) ? 10 : 5;
    }

    private double calculerScorePrix(PublicationData pub, ServiceData svc) {
        if (pub.proposedPrice == 0 || svc.price == 0) return 5;
        double diff = Math.abs(pub.proposedPrice - svc.price) / svc.price;
        if (diff <= 0.10) return 15;
        if (diff <= 0.20) return 12;
        if (diff <= 0.30) return 8;
        if (diff <= 0.50) return 4;
        return 0;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NOTIFICATION — 3 channels: Email + Cache (instant bell) + Novu (persistent)
    // ══════════════════════════════════════════════════════════════════════════

    private void notifierTuteur(PublicationData pub, MatchResult match) throws SQLException {
        ServiceData svc   = match.getService();
        double      score = match.getScore();

        if (svc.prestataireEmail == null || svc.prestataireEmail.isEmpty()) {
            System.out.println("   ⚠️  Email du tuteur introuvable, notification ignorée");
            return;
        }

        String notifTitle   = "🎯 Nouvelle demande compatible avec votre service !";
        String notifMessage = String.format("Score: %d%% — \"%s\" correspond à votre service \"%s\"",
                (int) score, pub.titre, svc.title);

        try {
            // ── 1. EMAIL ──────────────────────────────────────────────────────
            String studentDisplayName = "Étudiant #" + pub.studentId;
            String descriptionForEmail = pub.message != null
                    ? (pub.message.length() > 300
                    ? pub.message.substring(0, 297) + "..."
                    : pub.message)
                    : "";

            emailService.sendCompatiblePublicationEmail(
                    svc.prestataireEmail,
                    svc.prestataireName,
                    svc.title,
                    studentDisplayName,
                    pub.titre,
                    descriptionForEmail,
                    pub.proposedPrice,
                    score
            );
            System.out.println("   📧 Email envoyé à : " + svc.prestataireName
                    + " (" + svc.prestataireEmail + ")");

            // ── 2. NOTIFICATION CACHE (instant bell in JavaFX) ────────────────
            // This is picked up immediately by NotificationBellController polling
            notifCache.addNotification(svc.prestataireId, notifTitle, notifMessage);
            System.out.println("   🔔 Cache notification ajoutée pour user #" + svc.prestataireId);

            // ── 3. NOVU (persistent across sessions) ─────────────────────────
            // Runs in virtual thread so it doesn't block the matching loop
            final String email = svc.prestataireEmail;
            final int    uid   = svc.prestataireId;
            final String title = notifTitle;
            final String msg   = notifMessage;
            Thread.ofVirtual().start(() -> {
                boolean sent = NovuApiClient.sendNotification(uid, title, msg, email);
                if (sent) {
                    System.out.println("   🌐 Novu notification envoyée pour user #" + uid);
                } else {
                    System.out.println("   ⚠️  Novu indisponible — notification cache uniquement");
                }
            });

            // ── 4. LEGACY in-app (keep for backward compat) ──────────────────
            notificationService.creerNotificationInApp(
                    svc.prestataireId,
                    notifTitle,
                    notifMessage
            );

        } catch (Exception e) {
            System.err.println("   ❌ Erreur notification : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MATCHING HISTORY
    // ══════════════════════════════════════════════════════════════════════════

    private void enregistrerMatch(int publicationId, int serviceId,
                                  double score) throws SQLException {
        String sql = """
            INSERT INTO matching_history
                (publication_id, service_id, compatibility_score, notified, created_at)
            VALUES (?, ?, ?, TRUE, NOW())
            ON DUPLICATE KEY UPDATE
                compatibility_score = VALUES(compatibility_score),
                notified = TRUE,
                updated_at = NOW()
        """;
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, publicationId);
            ps.setInt(2, serviceId);
            ps.setDouble(3, score);
            ps.executeUpdate();
        }
    }

    private boolean matchDejaExistant(int publicationId, int serviceId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM matching_history " +
                "WHERE publication_id = ? AND service_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, publicationId);
            ps.setInt(2, serviceId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ══════════════════════════════════════════════════════════════════════════

    private Set<String> extraireMotsImportants(String texte) {
        Set<String> stopWords = Set.of(
                "le","la","les","un","une","des","de","du","et","ou",
                "pour","avec","dans","sur","par","est","sont","a","à",
                "ce","cette","ces","mon","ma","mes","ton","ta","tes",
                "son","sa","ses","qui","que","quoi","dont","où"
        );
        Set<String> mots = new HashSet<>();
        for (String mot : texte.split("\\s+")) {
            mot = mot.replaceAll("[^a-zàâäéèêëïîôùûü]", "").toLowerCase();
            if (mot.length() > 3 && !stopWords.contains(mot)) mots.add(mot);
        }
        return mots;
    }

    private Map<String, List<String>> getMotsClesCategorie() {
        Map<String, List<String>> dict = new HashMap<>();
        dict.put("programmation", Arrays.asList(
                "code","python","java","javascript","web","app","application",
                "algorithme","sql","développement","site","php","html","css",
                "react","angular","backend","frontend"));
        dict.put("mathématiques", Arrays.asList(
                "math","algèbre","géométrie","calcul","équation","fonction",
                "statistiques","probabilité","trigonométrie","analyse","intégrale"));
        dict.put("physique", Arrays.asList(
                "mécanique","électricité","optique","thermodynamique",
                "cinématique","force","énergie","électromagnétisme","ondes"));
        dict.put("chimie", Arrays.asList(
                "réaction","molécule","atome","organique","inorganique",
                "équilibre","acide","base","oxydation","réduction","laboratoire"));
        dict.put("langues", Arrays.asList(
                "anglais","espagnol","allemand","français","grammaire",
                "vocabulaire","conversation","traduction","conjugaison","toefl"));
        dict.put("rédaction", Arrays.asList(
                "essai","dissertation","rapport","mémoire","thèse",
                "article","correction","relecture","style","orthographe"));
        dict.put("cours", Arrays.asList(
                "aide","soutien","tutorat","cours","leçon","apprentissage",
                "formation","enseignement","explication","exercice"));
        return dict;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DATA CLASSES
    // ══════════════════════════════════════════════════════════════════════════

    private static class PublicationData {
        int id, studentId;
        String titre, message, localisation;
        double proposedPrice;
        Integer serviceId, categoryId;
    }

    private static class ServiceData {
        int id, prestataireId, categoryId;
        String title, description, categoryName, prestataireEmail, prestataireName;
        double price;
    }

    private static class MatchResult {
        private final ServiceData service;
        private final double score;
        MatchResult(ServiceData s, double sc) { service = s; score = sc; }
        ServiceData getService() { return service; }
        double getScore()        { return score; }
    }
}