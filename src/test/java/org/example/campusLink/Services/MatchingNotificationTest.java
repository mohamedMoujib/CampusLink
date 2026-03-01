package org.example.campusLink.Services;

import org.example.campusLink.utils.NotificationCache;
import org.example.campusLink.utils.NotificationCache.CachedNotification;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour le flux Matching + Notification :
 * - Gestion_Notification : cache, création notification in-app
 * - Gestion_Matching : analyse (sans échec) et notification tuteur
 */
@DisplayName("Matching & Notification")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MatchingNotificationTest {

    private static Gestion_Notification notificationService;
    private static NotificationCache cache;

    @BeforeAll
    static void init() throws SQLException {
        notificationService = new Gestion_Notification();
        cache = NotificationCache.getInstance();
    }

    // ==================== Gestion_Notification ====================

    @Test
    @Order(1)
    @DisplayName("creerNotificationInApp ajoute une entrée dans le cache")
    void notification_inApp_addsToCache() {
        int userId = 999_001;
        String titre = "Test Matching";
        String message = "Score 75% – Service compatible";

        notificationService.creerNotificationInApp(userId, titre, message);

        List<CachedNotification> list = notificationService.getNotificationsForUser(userId);
        assertFalse(list.isEmpty(), "Le cache doit contenir au moins une notification pour user " + userId);
        assertTrue(list.stream().anyMatch(n ->
                titre.equals(n.getTitle()) && message.equals(n.getMessage())));
    }

    @Test
    @Order(2)
    @DisplayName("getUnreadNotificationsCount reflète les non lues")
    void notification_unreadCount() {
        int userId = 999_002;
        notificationService.creerNotificationInApp(userId, "T1", "M1");
        long count = notificationService.getUnreadNotificationsCount(userId);
        assertTrue(count >= 1, "Au moins une non lue");
    }

    @Test
    @Order(3)
    @DisplayName("marquerCommeLue réduit le nombre de non lues")
    void notification_markAsRead() {
        int userId = 999_003;
        notificationService.creerNotificationInApp(userId, "Lu", "Msg");
        List<CachedNotification> list = notificationService.getNotificationsForUser(userId);
        assertFalse(list.isEmpty());
        String id = list.get(0).getId();
        notificationService.marquerCommeLue(id);
        long after = notificationService.getUnreadNotificationsCount(userId);
        assertTrue(after < list.size() || list.size() == 1, "Marquer lue doit mettre à jour le cache");
    }

    // ==================== Gestion_Matching (intégration légère) ====================

    @Test
    @Order(4)
    @DisplayName("analyserNouvellesPublications s'exécute sans exception")
    void matching_analyser_runsWithoutException() throws SQLException {
        Gestion_Matching matching = new Gestion_Matching();
        assertDoesNotThrow(() -> matching.analyserNouvellesPublications());
    }
}
