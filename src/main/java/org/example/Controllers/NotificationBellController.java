package org.example.Controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.example.campusLink.utils.NovuApiClient;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * 🔔 NOTIFICATION BELL CONTROLLER — Novu API
 *
 * - Bell icon with red badge counter (updated every 30s from Novu)
 * - Click bell → opens notification panel
 * - Notifications loaded from Novu API
 * - Mark as read / mark all / clear all
 *
 * HOW TO USE:
 *   1. Add to your toolbar FXML:
 *      <fx:include source="NotificationBell.fxml" fx:id="notificationBell"/>
 *
 *   2. In your main controller:
 *      @FXML private NotificationBellController notificationBellController;
 *      notificationBellController.setUserId(currentUser.getId());
 */
public class NotificationBellController implements Initializable {

    @FXML private StackPane bellContainer;
    @FXML private Button    bellButton;
    @FXML private Label     badgeLabel;
    @FXML private VBox      notificationPanel;
    @FXML private VBox      notificationList;
    @FXML private VBox      emptyState;

    private int     currentUserId = 1;
    private boolean panelVisible  = false;
    private Timeline pollingTimer;

    // ══════════════════════════════════════════════════════════════════════════
    // INIT
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        startPolling();
    }

    /** Call this after login with the logged-in user's id. */
    public void setUserId(int userId) {
        this.currentUserId = userId;
        // Register user as Novu subscriber
        Thread.ofVirtual().start(() ->
                NovuApiClient.ensureSubscriber(userId, null));
        refreshBadge();
    }

    public void stopPolling() {
        if (pollingTimer != null) pollingTimer.stop();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POLLING — refresh badge every 30 seconds
    // ══════════════════════════════════════════════════════════════════════════

    private void startPolling() {
        pollingTimer = new Timeline(
                new KeyFrame(Duration.seconds(30), e -> refreshBadge())
        );
        pollingTimer.setCycleCount(Timeline.INDEFINITE);
        pollingTimer.play();
        refreshBadge(); // immediate first call
    }

    private void refreshBadge() {
        Thread.ofVirtual().start(() -> {
            long count = NovuApiClient.getUnreadCount(currentUserId);
            Platform.runLater(() -> updateBadge(count));
        });
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

    // ══════════════════════════════════════════════════════════════════════════
    // PANEL OPEN / CLOSE
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void togglePanel() {
        if (panelVisible) closePanel();
        else openPanel();
    }

    private void openPanel() {
        loadNotifications();
        notificationPanel.setVisible(true);
        notificationPanel.setManaged(true);
        panelVisible = true;
    }

    @FXML
    public void closePanel() {
        notificationPanel.setVisible(false);
        notificationPanel.setManaged(false);
        panelVisible = false;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LOAD NOTIFICATIONS FROM NOVU API
    // ══════════════════════════════════════════════════════════════════════════

    private void loadNotifications() {
        Thread.ofVirtual().start(() -> {
            String json = NovuApiClient.getNotificationsJson(currentUserId);
            Platform.runLater(() -> renderNotifications(json));
        });
    }

    private void renderNotifications(String json) {
        notificationList.getChildren().clear();

        if (json == null || json.isEmpty() || !json.contains("\"_id\"")) {
            showEmptyState();
            return;
        }

        // Split Novu response by notification objects
        String[] parts = json.split("\\{\"_id\":");
        int count = 0;

        for (int i = 1; i < parts.length; i++) {
            String part = "{\"_id\":" + parts[i];

            String id      = extractString(part, "_id");
            String title   = extractString(part, "content");  // Novu field
            String seen    = extractString(part, "seen");
            String created = extractString(part, "createdAt");

            if (id != null && !id.isEmpty()) {
                boolean isUnread = !"true".equals(seen);
                VBox item = buildNotificationItem(id, title, isUnread, created);
                notificationList.getChildren().add(item);
                count++;
            }
        }

        if (count == 0) showEmptyState();
        else hideEmptyState();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BUILD NOTIFICATION ITEM
    // ══════════════════════════════════════════════════════════════════════════

    private VBox buildNotificationItem(String id, String content,
                                       boolean isUnread, String time) {
        VBox item = new VBox(6);
        item.getStyleClass().add("notif-item");
        if (isUnread) item.getStyleClass().add("notif-item-unread");
        item.setPadding(new Insets(12, 15, 12, 15));

        // Title row with unread dot
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        if (isUnread) {
            Region dot = new Region();
            dot.getStyleClass().add("unread-dot");
            titleRow.getChildren().add(dot);
        }

        Label contentLabel = new Label(content != null ? content : "Notification");
        contentLabel.getStyleClass().add("notif-title");
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(260);
        titleRow.getChildren().add(contentLabel);

        // Footer: time + buttons
        HBox footer = new HBox(6);
        footer.setAlignment(Pos.CENTER_LEFT);

        Label timeLabel = new Label(formatTime(time));
        timeLabel.getStyleClass().add("notif-time");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Mark read button
        Button readBtn = new Button(isUnread ? "✓ Lire" : "✓ Lu");
        readBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#667eea;" +
                "-fx-font-size:11px;-fx-cursor:hand;-fx-padding:2px 6px;");
        readBtn.setDisable(!isUnread);
        readBtn.setOnAction(e -> {
            Thread.ofVirtual().start(() ->
                    NovuApiClient.markAsRead(currentUserId, id));
            item.getStyleClass().remove("notif-item-unread");
            readBtn.setText("✓ Lu");
            readBtn.setDisable(true);
            refreshBadge();
        });

        footer.getChildren().addAll(timeLabel, spacer, readBtn);
        item.getChildren().addAll(titleRow, footer);
        return item;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void markAllAsRead() {
        Thread.ofVirtual().start(() -> {
            NovuApiClient.markAllAsRead(currentUserId);
            Platform.runLater(() -> {
                loadNotifications();
                updateBadge(0);
            });
        });
    }

    @FXML
    private void clearAllNotifications() {
        // Novu doesn't support delete — just mark all as read and clear UI
        markAllAsRead();
        notificationList.getChildren().clear();
        showEmptyState();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private void showEmptyState() {
        emptyState.setVisible(true);
        emptyState.setManaged(true);
    }

    private void hideEmptyState() {
        emptyState.setVisible(false);
        emptyState.setManaged(false);
    }

    private String formatTime(String iso) {
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