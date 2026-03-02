package org.example.campusLink.mains;

import org.example.campusLink.services.EmailService_publication;
import org.example.campusLink.services.Gestion_Matching;
import org.example.campusLink.services.Gestion_Notification;
import org.example.campusLink.utils.NotificationCache;
import org.example.campusLink.utils.NotificationCache.CachedNotification;
import org.example.campusLink.utils.NovuApiClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestMatchingSystem {

    public static void main(String[] args) throws Exception {

        System.out.println("\n═══════════════════════════════════════");
        System.out.println("🧪 TEST COMPLET NOTIFICATION SYSTEM");
        System.out.println("═══════════════════════════════════════\n");

        testEmailServicePublication();
        testNovuConnection();
        testCacheBasic();
        testCacheTTL();
        testNotificationService();
        testMatchingReal();

        System.out.println("\n✅ TOUS LES TESTS TERMINÉS");
    }

    // =========================================================
    // TEST 0 — EMAIL SERVICE PUBLICATION
    // =========================================================
    private static void testEmailServicePublication() {
        System.out.println("📧 TEST EmailService_publication");

        EmailService_publication pubEmailService = new EmailService_publication();
        boolean sent = pubEmailService.sendCompatiblePublicationEmail(
                "medaliabidi24@gmail.com",
                "dali abidi",
                "cours anglais",
                "Étudiant #20",
                "cours anglais urgent",
                "Je cherche un cours d'anglais pour préparer mon examen TOEFL.",
                50.0,
                67.0
        );
        System.out.println("→ Envoi email: " + (sent ? "✅ OK" : "❌ échoué"));
        System.out.println("✅ EmailService_publication testé\n");
    }

    // =========================================================
    // TEST 1 — NOVU CONNECTION
    // =========================================================
    private static void testNovuConnection() {
        System.out.println("🔌 TEST NOVU CONNECTION");

        boolean ok = NovuApiClient.isAvailable();
        if (ok) {
            System.out.println("✅ Novu API reachable");
        } else {
            System.out.println("⚠️  Novu API NOT reachable");
        }
        System.out.println();
    }

    // =========================================================
    // TEST 2 — CACHE BASIC
    // =========================================================
    private static void testCacheBasic() {
        System.out.println("🗃️  TEST CACHE BASIC");

        NotificationCache cache = NotificationCache.getInstance();
        String id1 = cache.addNotification(1, "Test cache", "Message test");

        List<CachedNotification> list = cache.getForUser(1);
        System.out.println("→ Notifications user#1: " + list.size());

        if (!list.isEmpty()) {
            cache.markAsRead(id1);
            System.out.println("→ Mark as read OK");
        }

        System.out.println("✅ Cache basic OK\n");
    }

    // =========================================================
    // TEST 3 — TTL
    // =========================================================
    private static void testCacheTTL() throws InterruptedException {
        System.out.println("⏳ TEST TTL");

        NotificationCache cache = NotificationCache.getInstance();
        cache.addNotification(99, "TTL test", "Expire soon", 3, TimeUnit.SECONDS);

        System.out.println("→ Avant sleep: " + cache.getForUser(99).size());
        Thread.sleep(4000);
        System.out.println("→ Après sleep: " + cache.getForUser(99).size());

        System.out.println("✅ TTL OK\n");
    }

    // =========================================================
    // TEST 4 — NOTIFICATION SERVICE
    // =========================================================
    private static void testNotificationService() throws Exception {
        System.out.println("🔔 TEST NOTIFICATION SERVICE");

        Gestion_Notification service = new Gestion_Notification();
        service.creerNotificationInApp(5, "🎯 Match trouvé", "Score 82% — Java tutor");

        NotificationCache cache = NotificationCache.getInstance();
        List<CachedNotification> list = cache.getForUser(5);
        System.out.println("→ Notifications cache user#5: " + list.size());

        if (!list.isEmpty()) {
            System.out.println("  • " + list.get(0).getTitle());
        }

        System.out.println("✅ Notification service OK\n");
    }

    // =========================================================
    // TEST 5 — MATCHING RÉEL
    // =========================================================
    private static void testMatchingReal() throws Exception {
        System.out.println("🎯 TEST MATCHING RÉEL + NOTIFICATION");

        Gestion_Matching matching = new Gestion_Matching();
        matching.analyserNouvellesPublications();

        NotificationCache cache = NotificationCache.getInstance();
        System.out.println("→ Cache user#1 : " + cache.getForUser(1).size());
        System.out.println("→ Cache user#5 : " + cache.getForUser(5).size());
        System.out.println("→ Cache user#21: " + cache.getForUser(21).size());

        System.out.println("✅ Matching testé\n");
    }
}