package org.example.campusLink.services;

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
        emailService        = new EmailService_publication();
    }

    /**
     * Analyser les nouvelles publications et notifier les prestataires compatibles.
     * Appelé après chaque création de publication (manuelle ou IA).
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
                // FIX: seuil abaissé à 30 pour détecter les correspondances partielles
                if (match.getScore() >= 30) {
                    System.out.printf("   ✅ Match validé! Service #%d – Score: %.1f%%%n",
                            match.getService().id, match.getScore());

                    if (!matchDejaExistant(pub.id, match.getService().id)) {
                        notifierPrestataire(pub, match);
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

    // ═══════════════════════════════════════════════════════════════
    // REQUÊTES SQL — FIX STATUS
    // ═══════════════════════════════════════════════════════════════

    /**
     * FIX PRINCIPAL : status = 'ACTIVE' au lieu de 'EN_ATTENTE'
     * Récupère les publications DEMANDE_SERVICE des 60 dernières minutes.
     */
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
                p.prix_vente,
                p.service_id,
                p.category_id
            FROM publications p
            WHERE p.type_publication = 'DEMANDE_SERVICE'
              AND p.status IN ('ACTIVE', 'EN_ATTENTE', 'EN_COURS')
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
                // FIX: utiliser prix_vente si proposed_price est null
                double proposedPrice = rs.getDouble("proposed_price");
                if (rs.wasNull()) proposedPrice = rs.getDouble("prix_vente");
                pub.proposedPrice = proposedPrice;
                pub.serviceId     = (Integer) rs.getObject("service_id");
                pub.categoryId    = (Integer) rs.getObject("category_id");
                publications.add(pub);
            }
        }
        return publications;
    }

    /**
     * FIX : inclure tous les statuts valides pour les services
     * (EN_ATTENTE = en attente de validation admin, mais le service existe bien)
     */
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
            WHERE s.status IN ('EN_ATTENTE', 'CONFIRMEE')
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

    // ═══════════════════════════════════════════════════════════════
    // CALCUL DE COMPATIBILITÉ
    // ═══════════════════════════════════════════════════════════════

    private List<MatchResult> trouverServicesCompatibles(PublicationData pub,
                                                         List<ServiceData> services)
            throws SQLException {
        List<MatchResult> results = new ArrayList<>();
        for (ServiceData svc : services) {
            double score = calculerScoreCompatibilite(pub, svc);
            System.out.printf("      Service #%d '%s' → score=%.1f%n",
                    svc.id, svc.title, score);
            if (score > 0) results.add(new MatchResult(svc, score));
        }
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return results;
    }

    private double calculerScoreCompatibilite(PublicationData pub, ServiceData svc) {
        double score = 0;
        score += calculerScoreMotsCles(pub, svc);   // 0–50 pts (priorité aux mots-clés)
        score += calculerScoreCategorie(pub, svc);  // 0–30 pts
        score += calculerScorePrix(pub, svc);       // 0–15 pts
        score += calculerScoreLocalisation(pub, svc); // 0–5 pts
        return Math.min(score, 100);
    }

    /**
     * Mots-clés → 0–50 pts
     * FIX: poids augmenté + comparaison titre du service contre titre+message de la publication
     */
    private double calculerScoreMotsCles(PublicationData pub, ServiceData svc) {
        String pubTexte = ((pub.titre  != null ? pub.titre  : "") + " "
                + (pub.message != null ? pub.message : "")).toLowerCase();
        String svcTexte = ((svc.title       != null ? svc.title       : "") + " "
                + (svc.description != null ? svc.description : "")).toLowerCase();

        // Correspondance directe : titre du service dans le texte de la publication
        if (svc.title != null && pubTexte.contains(svc.title.toLowerCase())) {
            return 50;
        }

        Set<String> motsPub = extraireMotsImportants(pubTexte);
        Set<String> motsSvc = extraireMotsImportants(svcTexte);

        if (motsPub.isEmpty() || motsSvc.isEmpty()) return 0;

        Set<String> communs = new HashSet<>(motsPub);
        communs.retainAll(motsSvc);

        if (communs.isEmpty()) return 0;

        System.out.println("         Mots communs: " + communs);
        double ratio = (double) communs.size() / Math.max(motsPub.size(), motsSvc.size());
        return Math.min(ratio * 50, 50);
    }

    /** Catégorie → 0–30 pts */
    private double calculerScoreCategorie(PublicationData pub, ServiceData svc) {
        // Même catégorie exacte
        if (pub.categoryId != null && pub.categoryId.equals(svc.categoryId)) return 30;

        String pubTexte = ((pub.titre  != null ? pub.titre  : "") + " "
                + (pub.message != null ? pub.message : "")).toLowerCase();

        if (svc.categoryName != null && !svc.categoryName.isBlank()) {
            String categorie = svc.categoryName.toLowerCase();
            // Nom de la catégorie dans le texte
            if (pubTexte.contains(categorie)) return 25;

            // Mots-clés associés à la catégorie
            Map<String, List<String>> dict = getMotsClesCategorie();
            List<String> motsCles = dict.get(categorie);
            if (motsCles != null) {
                for (String mot : motsCles) {
                    if (pubTexte.contains(mot.toLowerCase())) {
                        System.out.println("         Catégorie matchée via mot-clé: " + mot);
                        return 20;
                    }
                }
            }
        }
        return 0;
    }

    /** Localisation → 0–5 pts */
    private double calculerScoreLocalisation(PublicationData pub, ServiceData svc) {
        return (pub.localisation != null && !pub.localisation.trim().isEmpty()) ? 5 : 2;
    }

    /** Prix → 0–15 pts */
    private double calculerScorePrix(PublicationData pub, ServiceData svc) {
        if (pub.proposedPrice <= 0 || svc.price <= 0) return 5;
        double diff = Math.abs(pub.proposedPrice - svc.price) / svc.price;
        if (diff <= 0.10) return 15;
        if (diff <= 0.20) return 12;
        if (diff <= 0.30) return 8;
        if (diff <= 0.50) return 4;
        return 0;
    }

    // ═══════════════════════════════════════════════════════════════
    // NOTIFICATION
    // ═══════════════════════════════════════════════════════════════

    private void notifierPrestataire(PublicationData pub, MatchResult match) throws SQLException {
        ServiceData svc   = match.getService();
        double      score = match.getScore();

        System.out.println("   📧 Notification prestataire #" + svc.prestataireId
                + " (" + svc.prestataireName + ")");

        // Notification in-app (toujours, même sans email)
        String titreNotif = "🎯 Nouvelle demande compatible avec votre service !";
        String corpsNotif = String.format(
                "Un étudiant recherche : \"%s\"\n" +
                        "Compatible avec votre service : \"%s\"\n" +
                        "Score de compatibilité : %d%%",
                pub.titre, svc.title, (int) score);

        try {
            notificationService.creerNotificationInApp(
                    svc.prestataireId,
                    titreNotif,
                    corpsNotif
            );
        } catch (Exception e) {
            System.err.println("   ⚠️  Notification in-app échouée : " + e.getMessage());
        }

        // Email si disponible
        if (svc.prestataireEmail != null && !svc.prestataireEmail.isEmpty()) {
            try {
                String descForEmail = pub.message != null
                        ? (pub.message.length() > 300
                        ? pub.message.substring(0, 297) + "..."
                        : pub.message)
                        : "";

                emailService.sendCompatiblePublicationEmail(
                        svc.prestataireEmail,
                        svc.prestataireName,
                        svc.title,
                        "Étudiant #" + pub.studentId,
                        pub.titre,
                        descForEmail,
                        pub.proposedPrice,
                        score
                );
                System.out.println("   📧 Email envoyé à : " + svc.prestataireEmail);
            } catch (Exception e) {
                System.err.println("   ⚠️  Email échoué : " + e.getMessage());
            }
        } else {
            System.out.println("   ℹ️  Pas d'email pour ce prestataire — notification in-app seulement");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HISTORIQUE MATCHING
    // ═══════════════════════════════════════════════════════════════

    private void enregistrerMatch(int publicationId, int serviceId,
                                  double score) throws SQLException {
        // Essayer avec la table matching_history si elle existe,
        // sinon logguer seulement (table optionnelle)
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
            System.out.println("   💾 Match enregistré en base");
        } catch (SQLException e) {
            // Table matching_history peut ne pas exister — non bloquant
            System.err.println("   ⚠️  matching_history indisponible : " + e.getMessage());
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
        } catch (SQLException e) {
            // Table inexistante → considérer que le match n'existe pas
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ═══════════════════════════════════════════════════════════════

    private Set<String> extraireMotsImportants(String texte) {
        Set<String> stopWords = Set.of(
                "le","la","les","un","une","des","de","du","et","ou",
                "pour","avec","dans","sur","par","est","sont","a","à",
                "ce","cette","ces","mon","ma","mes","ton","ta","tes",
                "son","sa","ses","qui","que","quoi","dont","où","je",
                "tu","il","elle","nous","vous","ils","elles","me","te",
                "se","moi","toi","lui","leur","eux","aussi","mais","donc",
                "car","ni","or","si","bien","plus","très","tout","même"
        );
        Set<String> mots = new HashSet<>();
        for (String mot : texte.split("[\\s,;:.!?()\\[\\]'\"-]+")) {
            mot = mot.replaceAll("[^a-zàâäéèêëïîôùûü]", "").toLowerCase();
            if (mot.length() > 2 && !stopWords.contains(mot)) mots.add(mot);
        }
        return mots;
    }

    private Map<String, List<String>> getMotsClesCategorie() {
        Map<String, List<String>> dict = new HashMap<>();
        dict.put("programmation", Arrays.asList(
                "code","python","java","javascript","web","app","application",
                "algorithme","sql","développement","site","php","html","css",
                "react","angular","backend","frontend","programme","logiciel",
                "développer","coder","informatique","ordinateur","programmation"));
        dict.put("mathématiques", Arrays.asList(
                "math","algèbre","géométrie","calcul","équation","fonction",
                "statistiques","probabilité","trigonométrie","analyse","intégrale",
                "dérivée","matrice","vecteur","maths","mathématique"));
        dict.put("physique", Arrays.asList(
                "mécanique","électricité","optique","thermodynamique",
                "cinématique","force","énergie","électromagnétisme","ondes","physique"));
        dict.put("chimie", Arrays.asList(
                "réaction","molécule","atome","organique","inorganique",
                "équilibre","acide","base","oxydation","réduction","laboratoire","chimie"));
        dict.put("langues", Arrays.asList(
                "anglais","espagnol","allemand","français","grammaire",
                "vocabulaire","conversation","traduction","conjugaison","toefl","langue"));
        dict.put("rédaction", Arrays.asList(
                "essai","dissertation","rapport","mémoire","thèse",
                "article","correction","relecture","style","orthographe","rédaction","écriture"));
        dict.put("cours", Arrays.asList(
                "aide","soutien","tutorat","cours","leçon","apprentissage",
                "formation","enseignement","explication","exercice","tuteur","soutenir"));
        return dict;
    }

    // ═══════════════════════════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════════════════════════

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
        double      getScore()   { return score; }
    }
}