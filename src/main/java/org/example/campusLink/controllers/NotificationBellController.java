package org.example.campusLink.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.example.campusLink.utils.NotificationCache;
import org.example.campusLink.utils.NotificationCache.CachedNotification;
import org.example.campusLink.utils.NovuApiClient;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class NotificationBellController implements Initializable {

    @FXML private StackPane bellContainer;
    @FXML private Button    bellButton;
    @FXML private Label     badgeLabel;
    @FXML private VBox      notificationPanel;
    @FXML private VBox      notificationList;
    @FXML private VBox      emptyState;

    private int     currentUserId = -1;
    private boolean panelVisible  = false;
    private boolean novuAvailable = false;
    private Timeline pollingTimer;

    private final NotificationCache cache = NotificationCache.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Check Novu availability in background — don't block UI
        Thread.ofVirtual().start(() -> {
            novuAvailable = NovuApiClient.isAvailable();
            System.out.println(novuAvailable
                    ? "✅ [Bell] Novu disponible"
                    : "⚠️  [Bell] Novu indisponible — mode cache activé");
        });
        startPolling();
    }

    public void setUserId(int userId) {
        this.currentUserId = userId;
        if (novuAvailable) {
            Thread.ofVirtual().start(() ->
                    NovuApiClient.ensureSubscriber(userId, null));
        }
        refreshBadge();
    }

    public void stopPolling() {
        if (pollingTimer != null) pollingTimer.stop();
    }

    // ── POLLING every 15 seconds ──────────────────────────────────────────────

    private void startPolling() {
        pollingTimer = new Timeline(
                new KeyFrame(Duration.seconds(15), e -> refreshBadge())
        );
        pollingTimer.setCycleCount(Timeline.INDEFINITE);
        pollingTimer.play();
    }

    private void refreshBadge() {
        if (currentUserId < 0) return;
        Thread.ofVirtual().start(() -> {
            long count = getBadgeCount();
            Platform.runLater(() -> updateBadge(count));
        });
    }

    // Try Novu first, fall back to cache
    private long getBadgeCount() {
        if (novuAvailable) {
            long novuCount = NovuApiClient.getUnreadCount(currentUserId);
            if (novuCount >= 0) return novuCount;
            // Novu failed this call — fall through to cache
        }
        return cache.getUnreadCount(currentUserId);
    }

    private void updateBadge(long count) {
        if (count <= 0) {
            badgeLabel.setVisible(false);
            badgeLabel.setManaged(false);
        } else {
            badgeLabel.setText(count > 99 ? "99+" : String.valueOf(count));
            badgeLabel.setVisible(true);
            badgeLabel.setManaged(true);
        }
    }

    // ── PANEL ─────────────────────────────────────────────────────────────────

    @FXML
    private void togglePanel() {
        if (panelVisible) closePanel();
        else openPanel();
    }

    private void openPanel() {
        notificationPanel.setVisible(true);
        notificationPanel.setManaged(true);
        panelVisible = true;
        loadNotifications();
    }

    @FXML
    public void closePanel() {
        notificationPanel.setVisible(false);
        notificationPanel.setManaged(false);
        panelVisible = false;
    }

    // ── LOAD NOTIFICATIONS ────────────────────────────────────────────────────

    private void loadNotifications() {
        if (currentUserId < 0) return;
        notificationList.getChildren().clear();

        if (novuAvailable) {
            // Load from Novu in background
            Thread.ofVirtual().start(() -> {
                String json = NovuApiClient.getNotificationsJson(currentUserId);
                boolean novuHasData = json != null && json.contains("\"_id\"");

                if (novuHasData) {
                    Platform.runLater(() -> renderFromNovu(json));
                } else {
                    // Novu empty or failed — show cache
                    Platform.runLater(() -> renderFromCache());
                }
            });
        } else {
            renderFromCache();
        }
    }

    // ── RENDER FROM NOVU JSON ─────────────────────────────────────────────────

    private void renderFromNovu(String json) {
        notificationList.getChildren().clear();
        String[] parts = json.split("\\{\"_id\":");
        int count = 0;

        for (int i = 1; i < parts.length; i++) {
            String part   = "{\"_id\":" + parts[i];
            String id      = extractString(part, "_id");
            String content = extractString(part, "content");
            String seen    = extractString(part, "seen");
            String created = extractString(part, "createdAt");

            if (id != null && !id.isEmpty()) {
                boolean isUnread = !"true".equals(seen);
                notificationList.getChildren().add(
                        buildNovuItem(id, content, isUnread, created));
                count++;
            }
        }

        if (count == 0) showEmptyState();
        else hideEmptyState();
    }

    private VBox buildNovuItem(String id, String content,
                               boolean isUnread, String time) {
        VBox item = buildItemShell(isUnread);

        Label contentLabel = new Label(content != null ? content : "Notification");
        contentLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#111827;");
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(270);

        HBox footer = buildFooter(formatNovuTime(time), isUnread, e -> {
            Thread.ofVirtual().start(() ->
                    NovuApiClient.markAsRead(currentUserId, id));
            markItemRead(item);
            refreshBadge();
        });

        item.getChildren().addAll(contentLabel, footer);
        return item;
    }

    // ── RENDER FROM CACHE ─────────────────────────────────────────────────────

    private void renderFromCache() {
        notificationList.getChildren().clear();
        List<CachedNotification> notifications = cache.getForUser(currentUserId);

        if (notifications.isEmpty()) {
            showEmptyState();
            return;
        }

        hideEmptyState();
        for (CachedNotification n : notifications) {
            notificationList.getChildren().add(buildCacheItem(n));
        }
    }

    private VBox buildCacheItem(CachedNotification notif) {
        boolean isUnread = "UNREAD".equals(notif.getStatus());
        VBox item = buildItemShell(isUnread);

        Label titleLabel = new Label(notif.getTitle());
        titleLabel.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#111827;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(270);

        Label msgLabel = new Label(notif.getMessage());
        msgLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#374151;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(270);

        String time = notif.getCreatedAt()
                .format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));

        HBox footer = buildFooter(time, isUnread, e -> {
            cache.markAsRead(notif.getId());
            markItemRead(item);
            refreshBadge();
        });

        item.getChildren().addAll(titleLabel, msgLabel, footer);
        return item;
    }

    // ── SHARED UI HELPERS ─────────────────────────────────────────────────────

    private VBox buildItemShell(boolean isUnread) {
        VBox item = new VBox(6);
        item.setPadding(new Insets(12, 15, 12, 15));
        item.setStyle(isUnread
                ? "-fx-background-color:#f0f4ff;-fx-background-radius:8;" +
                "-fx-border-color:#e0e7ff;-fx-border-radius:8;-fx-border-width:1;"
                : "-fx-background-color:#fafafa;-fx-background-radius:8;" +
                "-fx-border-color:#f3f4f6;-fx-border-radius:8;-fx-border-width:1;");
        return item;
    }

    private HBox buildFooter(String time, boolean isUnread,
                             javafx.event.EventHandler<javafx.event.ActionEvent> onRead) {
        HBox footer = new HBox(6);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#9ca3af;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button readBtn = new Button(isUnread ? "✓ Marquer lu" : "✓ Lu");
        readBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#4f46e5;" +
                "-fx-font-size:11px; -fx-cursor:hand; -fx-padding:2 6;");
        readBtn.setDisable(!isUnread);
        readBtn.setOnAction(onRead);

        footer.getChildren().addAll(timeLabel, spacer, readBtn);
        return footer;
    }

    private void markItemRead(VBox item) {
        item.setStyle("-fx-background-color:#fafafa;-fx-background-radius:8;" +
                "-fx-border-color:#f3f4f6;-fx-border-radius:8;-fx-border-width:1;");
        // Disable the read button inside footer
        item.getChildren().stream()
                .filter(n -> n instanceof HBox)
                .map(n -> (HBox) n)
                .flatMap(h -> h.getChildren().stream())
                .filter(n -> n instanceof Button)
                .map(n -> (Button) n)
                .forEach(b -> { b.setText("✓ Lu"); b.setDisable(true); });
    }

    // ── ACTIONS ───────────────────────────────────────────────────────────────

    @FXML
    private void markAllAsRead() {
        if (currentUserId < 0) return;
        Thread.ofVirtual().start(() -> {
            if (novuAvailable) NovuApiClient.markAllAsRead(currentUserId);
            cache.getForUser(currentUserId)
                    .forEach(n -> cache.markAsRead(n.getId()));
            Platform.runLater(() -> {
                updateBadge(0);
                loadNotifications();
            });
        });
    }

    @FXML
    private void clearAllNotifications() {
        if (currentUserId < 0) return;
        cache.clearForUser(currentUserId);
        notificationList.getChildren().clear();
        updateBadge(0);
        showEmptyState();
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private void showEmptyState() {
        emptyState.setVisible(true);
        emptyState.setManaged(true);
    }

    private void hideEmptyState() {
        emptyState.setVisible(false);
        emptyState.setManaged(false);
    }

    private String formatNovuTime(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            String[] p = iso.replace("\"", "").split("T");
            if (p.length == 2) {
                String[] d = p[0].split("-");
                return d[2] + "/" + d[1] + " " + p[1].substring(0, 5);
            }
        } catch (Exception ignored) {}
        return iso;
    }

    private String extractString(String json, String key) {
        try {
            String s = "\"" + key + "\":\"";
            int i = json.indexOf(s);
            if (i == -1) return null;
            int start = i + s.length();
            int end   = json.indexOf("\"", start);
            return end == -1 ? null : json.substring(start, end);
        } catch (Exception e) { return null; }
    }
}