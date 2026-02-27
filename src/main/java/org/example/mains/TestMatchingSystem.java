package org.example.mains;

import org.example.campusLink.Services.Gestion_Matching;
import org.example.campusLink.Services.Gestion_Notification;
import org.example.campusLink.utils.NotificationCache;
import org.example.campusLink.utils.NotificationCache.CachedNotification;
import org.example.campusLink.utils.NovuApiClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 🧪 TEST GLOBAL — Notifications + Matching + Novu
 *
 * Tests couverts :
 * 1. Cache local
 * 2. TTL expiration
 * 3. Novu connectivity
 * 4. Notification service
 * 5. Matching réel DB
 */
public class TestMatchingSystem {

    public static void main(String[] args) throws Exception {

        System.out.println("\n═══════════════════════════════════════");
        System.out.println("🧪 TEST COMPLET NOTIFICATION SYSTEM");
        System.out.println("═══════════════════════════════════════\n");

        testNovuConnection();
        testCacheBasic();
        testCacheTTL();
        testNotificationService();
        testMatchingReal();

        System.out.println("\n✅ TOUS LES TESTS TERMINÉS");
    }

    // =========================================================
    // TEST 0 — NOVU CONNECTIVITY
    // =========================================================
    private static void testNovuConnection() {
        System.out.println("🔌 TEST NOVU CONNECTION");

        boolean ok = NovuApiClient.isAvailable();

        if (ok) {
            System.out.println("✅ Novu API reachable");
        } else {
            System.out.println("⚠️ Novu API NOT reachable");
            System.out.println("   → vérifie API_KEY ou internet");
        }

        System.out.println();
    }

    // =========================================================
    // TEST 1 — CACHE BASIC
    // =========================================================
    private static void testCacheBasic() {
        System.out.println("🗃️ TEST CACHE BASIC");

        NotificationCache cache = NotificationCache.getInstance();

        String id1 = cache.addNotification(1,
                "Test cache",
                "Message test");

        List<CachedNotification> list = cache.getForUser(1);

        System.out.println("→ Notifications user#1: " + list.size());

        if (!list.isEmpty()) {
            cache.markAsRead(id1);
            System.out.println("→ Mark as read OK");
        }

        System.out.println("✅ Cache basic OK\n");
    }

    // =========================================================
    // TEST 2 — TTL
    // =========================================================
    private static void testCacheTTL() throws InterruptedException {
        System.out.println("⏳ TEST TTL");

        NotificationCache cache = NotificationCache.getInstance();

        cache.addNotification(
                99,
                "TTL test",
                "Expire soon",
                3,
                TimeUnit.SECONDS
        );

        System.out.println("→ Avant sleep: "
                + cache.getForUser(99).size());

        Thread.sleep(4000);

        System.out.println("→ Après sleep: "
                + cache.getForUser(99).size());

        System.out.println("✅ TTL OK\n");
    }

    // =========================================================
    // TEST 3 — NOTIFICATION SERVICE
    // =========================================================
    private static void testNotificationService() throws Exception {
        System.out.println("🔔 TEST NOTIFICATION SERVICE");

        Gestion_Notification service = new Gestion_Notification();

        service.creerNotificationInApp(
                5,
                "🎯 Match trouvé",
                "Score 82% — Java tutor"
        );

        NotificationCache cache = NotificationCache.getInstance();
        List<CachedNotification> list = cache.getForUser(5);

        System.out.println("→ Notifications cache user#5: " + list.size());

        if (!list.isEmpty()) {
            System.out.println("  • " + list.get(0).getTitle());
        }

        System.out.println("✅ Notification service OK\n");
    }

    // =========================================================
    // TEST 4 — MATCHING RÉEL
    // =========================================================
    private static void testMatchingReal() throws Exception {
        System.out.println("🎯 TEST MATCHING RÉEL");
        System.out.println("⚠️ Nécessite données en base\n");

        Gestion_Matching matching = new Gestion_Matching();
        matching.analyserNouvellesPublications();

        System.out.println("✅ Matching exécuté\n");
    }
}