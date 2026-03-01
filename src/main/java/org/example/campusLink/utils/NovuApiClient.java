package org.example.campusLink.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 🌐 NOVU NOTIFICATION API CLIENT — Pure Java
 *
 * Novu is a free notification API that supports:
 *   - In-app notifications (bell icon)
 *   - Email notifications
 *   - No SDK needed — pure REST API calls
 *
 * ══════════════════════════════════════════════════
 * SETUP (one time):
 * ══════════════════════════════════════════════════
 * 1. Go to https://web.novu.co and create a free account
 * 2. Go to Settings → API Keys → copy your API Key
 * 3. Paste it in API_KEY below
 *
 * 4. Create a Subscriber (represents a user):
 *    POST https://api.novu.co/v1/subscribers
 *    (done automatically by ensureSubscriber())
 *
 * 5. Create a Workflow in Novu dashboard:
 *    - Go to Workflows → Create Workflow
 *    - Name it "match-notification"
 *    - Add an "In-App" step
 *    - Add an "Email" step (optional)
 *    - Note the workflow triggerIdentifier (e.g. "match-notification")
 *    - Paste it in WORKFLOW_ID below
 * ══════════════════════════════════════════════════
 */
public class NovuApiClient {

    // ── Config — fill these in ─────────────────────────────────────────────────
    private static final String API_KEY     = "47722025c57f909f569b93ebc266cc5d";
    private static final String WORKFLOW_ID = "onboarding-demo-workflow"; // your workflow trigger ID
    private static final String BASE_URL    = "https://api.novu.co/v1";

    // ── HTTP Client ────────────────────────────────────────────────────────────
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // ══════════════════════════════════════════════════════════════════════════
    // 1. SEND NOTIFICATION TO A USER
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Send an in-app + email notification to a user via Novu.
     *
     * @param userId    CampusLink user id (used as Novu subscriber id)
     * @param title     notification title
     * @param message   notification body
     * @param email     user's email (for email channel)
     * @return true if Novu accepted the request
     */
    public static boolean sendNotification(int userId, String title,
                                           String message, String email) {
        // 1. Make sure the subscriber exists in Novu
        ensureSubscriber(userId, email);

        // 2. Trigger the workflow
        return triggerWorkflow(userId, title, message);
    }

    /** Send notification without email. */
    public static boolean sendNotification(int userId, String title, String message) {
        return sendNotification(userId, title, message, null);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. CREATE / UPDATE SUBSCRIBER
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Register a user as a Novu subscriber if they don't exist yet.
     * Safe to call multiple times — Novu upserts automatically.
     *
     * @param userId CampusLink user id
     * @param email  user's email (can be null)
     */
    public static void ensureSubscriber(int userId, String email) {
        String subscriberId = "user-" + userId;
        String emailField   = (email != null && !email.isEmpty())
                ? "\"email\":\"" + escape(email) + "\","
                : "";

        String json = """
                {
                  "subscriberId": "%s",
                  %s
                  "firstName": "User",
                  "lastName": "%d"
                }
                """.formatted(subscriberId, emailField, userId);

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/subscribers"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "ApiKey " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> res = HTTP.send(req,
                    HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() < 300) {
                System.out.println("✅ [Novu] Subscriber ensured: " + subscriberId);
            } else {
                System.err.println("⚠️  [Novu] Subscriber error: "
                        + res.statusCode() + " → " + res.body());
            }
        } catch (Exception e) {
            System.err.println("❌ [Novu] ensureSubscriber failed: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. TRIGGER WORKFLOW
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Trigger a Novu workflow (sends in-app + email notification).
     */
    private static boolean triggerWorkflow(int userId, String title, String message) {
        String subscriberId = "user-" + userId;

        String json = """
                {
                  "name": "%s",
                  "to": {
                    "subscriberId": "%s"
                  },
                  "payload": {
                    "title": "%s",
                    "message": "%s",
                    "userId": %d
                  }
                }
                """.formatted(WORKFLOW_ID, subscriberId,
                escape(title), escape(message), userId);

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/events/trigger"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "ApiKey " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> res = HTTP.send(req,
                    HttpResponse.BodyHandlers.ofString());

            boolean ok = res.statusCode() >= 200 && res.statusCode() < 300;

            if (ok) {
                System.out.println("✅ [Novu] Notification sent to user #" + userId);
            } else {
                System.err.println("⚠️  [Novu] Trigger failed: "
                        + res.statusCode() + " → " + res.body());
            }
            return ok;

        } catch (Exception e) {
            System.err.println("❌ [Novu] triggerWorkflow failed: " + e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. GET UNREAD COUNT  (for bell badge)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Get the number of unread in-app notifications for a user.
     * Used to update the JavaFX bell badge counter.
     *
     * @return unread count, or -1 if API is unreachable
     */
    public static long getUnreadCount(int userId) {
        String subscriberId = "user-" + userId;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/subscribers/"
                            + subscriberId + "/notifications/unseen"))
                    .header("Authorization", "ApiKey " + API_KEY)
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> res = HTTP.send(req,
                    HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == 200) {
                return parseLong(res.body(), "count");
            }
        } catch (Exception e) {
            System.err.println("⚠️  [Novu] getUnreadCount failed: " + e.getMessage());
        }
        return -1;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. GET NOTIFICATIONS  (for notification panel)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Get all in-app notifications for a user as JSON.
     * Used to populate the JavaFX notification panel.
     *
     * @return JSON string from Novu API
     */
    public static String getNotificationsJson(int userId) {
        String subscriberId = "user-" + userId;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/subscribers/"
                            + subscriberId + "/notifications/feed?limit=20"))
                    .header("Authorization", "ApiKey " + API_KEY)
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> res = HTTP.send(req,
                    HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() == 200) return res.body();

        } catch (Exception e) {
            System.err.println("⚠️  [Novu] getNotifications failed: " + e.getMessage());
        }
        return "";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. MARK AS READ / SEEN
    // ══════════════════════════════════════════════════════════════════════════

    /** Mark a specific notification as read. */
    public static boolean markAsRead(int userId, String messageId) {
        String subscriberId = "user-" + userId;
        String json = "{\"messageId\":\"" + messageId + "\"}";

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/subscribers/"
                            + subscriberId + "/messages/markAs"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "ApiKey " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> res = HTTP.send(req,
                    HttpResponse.BodyHandlers.ofString());
            return res.statusCode() < 300;

        } catch (Exception e) {
            System.err.println("❌ [Novu] markAsRead failed: " + e.getMessage());
            return false;
        }
    }

    /** Mark all notifications as read for a user. */
    public static boolean markAllAsRead(int userId) {
        String subscriberId = "user-" + userId;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/subscribers/"
                            + subscriberId + "/messages/mark-all"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "ApiKey " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> res = HTTP.send(req,
                    HttpResponse.BodyHandlers.ofString());
            return res.statusCode() < 300;

        } catch (Exception e) {
            System.err.println("❌ [Novu] markAllAsRead failed: " + e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 7. HEALTH CHECK
    // ══════════════════════════════════════════════════════════════════════════

    /** Check if Novu API is reachable. */
    public static boolean isAvailable() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/integrations"))
                    .header("Authorization", "ApiKey " + API_KEY)
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            return HTTP.send(req, HttpResponse.BodyHandlers.ofString())
                    .statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private static long parseLong(String json, String key) {
        try {
            String search = "\"" + key + "\":";
            int i = json.indexOf(search);
            if (i == -1) return 0;
            int start = i + search.length();
            int end   = start;
            while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
            String val = json.substring(start, end).trim();
            return val.isEmpty() ? 0 : Long.parseLong(val);
        } catch (Exception e) { return 0; }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}