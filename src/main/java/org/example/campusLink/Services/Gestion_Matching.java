package org.example.campusLink.Services;

import org.example.campusLink.utils.MyDatabase;

import java.sql.*;
import java.util.*;

public class Gestion_Matching {

    private final Gestion_Notification notificationService;
    private final EmailService_publication emailService;

    private Connection getConnection() throws SQLException {
        return MyDatabase.getInstance().getConnection();
    }

    public Gestion_Matching() throws SQLException {
        notificationService = new Gestion_Notification();
        emailService = new EmailService_publication();
    }
    /**
     * Analyser les nouvelles publications et notifier les tuteurs compatibles.
     * Appelé toutes les 5 minutes par MatchingScheduler.
     */
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
    /** Publications DEMANDE_SERVICE actives des 60 dernières minutes. */
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
              AND p.status = 'EN_ATTENTE'
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

    /** Tous les services actifs avec infos prestataire et catégorie. */
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
                svc.id                = rs.getInt("id");
                svc.title             = rs.getString("title");
                svc.description       = rs.getString("description");
                svc.price             = rs.getDouble("price");
                svc.prestataireId     = rs.getInt("prestataire_id");
                svc.categoryId        = rs.getInt("category_id");
                svc.categoryName      = rs.getString("category_name");
                svc.prestataireEmail  = rs.getString("prestataire_email");
                svc.prestataireName   = rs.getString("prestataire_name");
                services.add(svc);
            }
        }
        return services;
    }
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

    /** Score total 0–100 basé sur catégorie, mots-clés, localisation, prix. */
    private double calculerScoreCompatibilite(PublicationData pub, ServiceData svc) {
        double score = 0;
        score += calculerScoreCategorie(pub, svc);
        score += calculerScoreMotsCles(pub, svc);
        score += calculerScoreLocalisation(pub, svc);
        score += calculerScorePrix(pub, svc);
        return Math.min(score, 100);
    }

    /** Catégorie → 0–40 pts */
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

    /** Mots-clés → 0–30 pts */
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

    /** Localisation → 0–15 pts */
    private double calculerScoreLocalisation(PublicationData pub, ServiceData svc) {
        return (pub.localisation != null && !pub.localisation.trim().isEmpty()) ? 10 : 5;
    }

    /** Prix → 0–15 pts */
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
    // NOTIFICATION
    // ══════════════════════════════════════════════════════════════════════════

    /** Notifier le tuteur par email + cache in-app + push API. */
    private void notifierTuteur(PublicationData pub, MatchResult match) throws SQLException {
        ServiceData svc   = match.getService();
        double      score = match.getScore();

        if (svc.prestataireEmail == null || svc.prestataireEmail.isEmpty()) {
            System.out.println("   ⚠️  Email du tuteur introuvable, notification ignorée");
            return;
        }

        try {
            // Email spécifique "publication compatible"
            String studentDisplayName = "Étudiant #" + pub.studentId;
            String descriptionForEmail = pub.message != null
                    ? (pub.message.length() > 300 ? pub.message.substring(0, 297) + "..." : pub.message)
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

            // Cache in-app + push API (géré dans creerNotificationInApp)
            notificationService.creerNotificationInApp(
                    svc.prestataireId,
                    "🎯 Nouvelle publication compatible avec votre service !",
                    "Score: " + (int) score + "% – " + pub.titre
            );

            System.out.println("   📧 Tuteur notifié : " + svc.prestataireName
                    + " (" + svc.prestataireEmail + ")");
        } catch (Exception e) {
            System.err.println("   ❌ Erreur notification : " + e.getMessage());
        }
    }

    private String genererExplicationMatch(double score) {
        if (score >= 90) return "Correspondance PARFAITE — catégorie, mots-clés et budget concordent exactement.";
        if (score >= 75) return "Correspondance TRÈS BONNE — la majorité des critères concordent.";
        if (score >= 60) return "Correspondance CORRECTE — plusieurs éléments correspondent à votre offre.";
        return "Cette publication pourrait vous intéresser.";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HISTORIQUE MATCHING (base de données)
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
    // UTILITAIRES
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