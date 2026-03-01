package org.example.campusLink.utils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 🗃️ NOTIFICATION CACHE - IN-MEMORY STORE WITH AUTO-EXPIRY
 *
 * Stores notifications in memory.
 * Each notification is automatically removed after its TTL expires (default: 30 min).
 * Thread-safe singleton.
 */
public class NotificationCache {

    // ── Singleton ──────────────────────────────────────────────────────────────
    private static NotificationCache instance;

    public static synchronized NotificationCache getInstance() {
        if (instance == null) {
            instance = new NotificationCache();
        }
        return instance;
    }

    // ── Config ─────────────────────────────────────────────────────────────────
    /** Default TTL: 30 minutes */
    public static final long DEFAULT_TTL_MINUTES = 30;

    /** Cleanup sweep every 5 minutes */
    private static final long CLEANUP_INTERVAL_MINUTES = 5;

    // ── Storage ────────────────────────────────────────────────────────────────
    private final ConcurrentHashMap<String, CachedNotification> cache = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleaner =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "NotificationCache-Cleaner");
                t.setDaemon(true);
                return t;
            });

    // ── Constructor ────────────────────────────────────────────────────────────
    private NotificationCache() {
        cleaner.scheduleAtFixedRate(
                this::evictExpired,
                CLEANUP_INTERVAL_MINUTES,
                CLEANUP_INTERVAL_MINUTES,
                TimeUnit.MINUTES
        );
        System.out.println("✅ NotificationCache initialized" +
                " (TTL=" + DEFAULT_TTL_MINUTES + "min, cleanup every " + CLEANUP_INTERVAL_MINUTES + "min)");
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Add a notification with the default TTL. Returns generated id. */
    public String addNotification(int userId, String title, String message) {
        return addNotification(userId, title, message, DEFAULT_TTL_MINUTES, TimeUnit.MINUTES);
    }

    /** Add a notification with a custom TTL. Returns generated id. */
    public String addNotification(int userId, String title, String message,
                                  long ttl, TimeUnit unit) {
        String id = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(unit.toSeconds(ttl));
        CachedNotification notif = new CachedNotification(
                id, userId, title, message, LocalDateTime.now(), expiresAt);
        cache.put(id, notif);
        System.out.println("🔔 [Cache] Notification added → user #" + userId +
                " | expires: " + expiresAt);
        return id;
    }

    /** All active notifications for a user, newest first. */
    public List<CachedNotification> getForUser(int userId) {
        evictExpired();
        return cache.values().stream()
                .filter(n -> n.getUserId() == userId && !n.isExpired())
                .sorted(Comparator.comparing(CachedNotification::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    /** Count of unread notifications for a user. */
    public long getUnreadCount(int userId) {
        evictExpired();
        return cache.values().stream()
                .filter(n -> n.getUserId() == userId && !n.isExpired() && "UNREAD".equals(n.getStatus()))
                .count();
    }

    /** Mark a notification as read. */
    public boolean markAsRead(String notificationId) {
        CachedNotification n = cache.get(notificationId);
        if (n != null && !n.isExpired()) {
            n.setStatus("READ");
            return true;
        }
        return false;
    }

    /** Immediately remove a notification. */
    public void remove(String notificationId) {
        cache.remove(notificationId);
        System.out.println("🗑️  [Cache] Removed notification: " + notificationId);
    }

    /** Remove all notifications for a user. */
    public void clearForUser(int userId) {
        cache.entrySet().removeIf(e -> e.getValue().getUserId() == userId);
        System.out.println("🗑️  [Cache] Cleared all notifications for user #" + userId);
    }

    public int size() { return cache.size(); }

    // ── Internal ───────────────────────────────────────────────────────────────
    private void evictExpired() {
        int before = cache.size();
        cache.entrySet().removeIf(e -> e.getValue().isExpired());
        int removed = before - cache.size();
        if (removed > 0)
            System.out.println("🧹 [Cache] Evicted " + removed + " expired notification(s). Remaining: " + cache.size());
    }

    // ── Model ──────────────────────────────────────────────────────────────────
    public static class CachedNotification {
        private final String id;
        private final int userId;
        private final String title;
        private final String message;
        private final LocalDateTime createdAt;
        private final LocalDateTime expiresAt;
        private volatile String status;

        public CachedNotification(String id, int userId, String title, String message,
                                  LocalDateTime createdAt, LocalDateTime expiresAt) {
            this.id        = id;
            this.userId    = userId;
            this.title     = title;
            this.message   = message;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.status    = "UNREAD";
        }

        public boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); }

        public String getId()               { return id; }
        public int getUserId()              { return userId; }
        public String getTitle()            { return title; }
        public String getMessage()          { return message; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public String getStatus()           { return status; }
        public void setStatus(String s)     { this.status = s; }

        @Override
        public String toString() {
            return "CachedNotification{id=" + id + ", user=" + userId +
                    ", title='" + title + "', status=" + status + ", expires=" + expiresAt + "}";
        }
    }
}