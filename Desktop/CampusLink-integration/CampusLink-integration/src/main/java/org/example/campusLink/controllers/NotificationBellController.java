package org.example.campusLink.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import org.example.campusLink.utils.NotificationCache;
import org.example.campusLink.utils.NotificationCache.CachedNotification;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.*;

public class NotificationBellController implements Initializable {

    @FXML private Button bellButton;
    @FXML private Label  badgeLabel;

    private Popup popup;
    private VBox  notificationList;
    private VBox  emptyState;
    private int   currentUserId = -1;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "NotifBell-Poller");
                t.setDaemon(true);
                return t;
            });

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        scheduler.scheduleAtFixedRate(
                () -> Platform.runLater(this::refreshBadge),
                0, 10, TimeUnit.SECONDS);
    }

    public void setCurrentUserId(int id) {
        this.currentUserId = id;
        refreshBadge();
    }

    // ── Build popup only when first needed ────────────────────────

    private void ensurePopup() {
        if (popup != null) return;

        popup = new Popup();
        popup.setAutoHide(true);
        popup.setAutoFix(true);

        // Root panel
        VBox panel = new VBox();
        panel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e5e7eb;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.18),16,0,0,4);"
        );
        panel.setPrefWidth(340);
        panel.setMaxHeight(460);

        // ── Header ────────────────────────────────────────────────
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-padding:12 16;-fx-border-color:#f3f4f6;-fx-border-width:0 0 1 0;");

        Label titleLbl = new Label("🔔 Notifications");
        titleLbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#111827;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button markAllBtn = new Button("Tout lire");
        markAllBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:#4f46e5;" +
                        "-fx-font-size:11px;-fx-cursor:hand;-fx-padding:0;");
        markAllBtn.setOnAction(e -> markAllAsRead());

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:#9ca3af;" +
                        "-fx-font-size:12px;-fx-cursor:hand;-fx-padding:2 6;");
        closeBtn.setOnAction(e -> popup.hide());

        header.getChildren().addAll(titleLbl, spacer, markAllBtn, closeBtn);

        // ── Notification list ─────────────────────────────────────
        notificationList = new VBox(6);
        notificationList.setStyle("-fx-padding:10;");

        ScrollPane scroll = new ScrollPane(notificationList);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(340);
        scroll.setStyle(
                "-fx-background-color:transparent;" +
                        "-fx-background:transparent;" +
                        "-fx-padding:0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Empty state ───────────────────────────────────────────
        emptyState = new VBox(6);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setStyle("-fx-padding:30;");
        emptyState.setVisible(false);
        emptyState.setManaged(false);

        Label emptyIcon = new Label("🔕");
        emptyIcon.setStyle("-fx-font-size:28px;");
        Label emptyTxt = new Label("Aucune notification");
        emptyTxt.setStyle("-fx-text-fill:#6b7280;-fx-font-size:12px;");
        Label emptySub = new Label("Les nouvelles demandes\napparaîtront ici.");
        emptySub.setStyle("-fx-text-fill:#9ca3af;-fx-font-size:11px;-fx-text-alignment:center;");
        emptySub.setWrapText(true);
        emptyState.getChildren().addAll(emptyIcon, emptyTxt, emptySub);

        panel.getChildren().addAll(header, scroll, emptyState);
        popup.getContent().add(panel);
    }

    // ── Toggle ────────────────────────────────────────────────────

    @FXML
    private void togglePanel() {
        ensurePopup();

        if (popup.isShowing()) {
            popup.hide();
            return;
        }

        loadNotifications();

        // Position below the bell button
        var bounds = bellButton.localToScreen(bellButton.getBoundsInLocal());
        popup.show(bellButton,
                bounds.getMaxX() - 340,  // right-align
                bounds.getMaxY() + 6);   // just below button
    }

    // ── Load ──────────────────────────────────────────────────────

    private void loadNotifications() {
        notificationList.getChildren().clear();

        List<CachedNotification> list =
                NotificationCache.getInstance().getForUser(currentUserId);

        System.out.println("🔔 [Bell] user #" + currentUserId
                + " → " + list.size() + " notification(s)");

        if (list.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        for (CachedNotification n : list) {
            notificationList.getChildren().add(buildCard(n, fmt));
        }
        refreshBadge();
    }

    // ── Card ──────────────────────────────────────────────────────

    private VBox buildCard(CachedNotification notif, DateTimeFormatter fmt) {
        boolean unread = "UNREAD".equals(notif.getStatus());

        VBox card = new VBox(4);
        card.setStyle(
                "-fx-background-color:" + (unread ? "#f0f4ff" : "#ffffff") + ";" +
                        "-fx-background-radius:8;" +
                        "-fx-border-color:" + (unread ? "#c7d2fe" : "#e5e7eb") + ";" +
                        "-fx-border-radius:8;-fx-border-width:1;" +
                        "-fx-padding:10 12;-fx-cursor:hand;"
        );

        // Title row
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(notif.getTitle());
        titleLbl.setStyle(
                "-fx-font-size:12px;" +
                        "-fx-font-weight:" + (unread ? "bold" : "normal") + ";" +
                        "-fx-text-fill:#111827;");
        titleLbl.setMaxWidth(210);
        titleLbl.setWrapText(true);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label time = new Label(notif.getCreatedAt().format(fmt));
        time.setStyle("-fx-font-size:10px;-fx-text-fill:#9ca3af;");

        row.getChildren().addAll(titleLbl, sp, time);

        // Message
        Label msg = new Label(notif.getMessage());
        msg.setStyle("-fx-font-size:11px;-fx-text-fill:#4b5563;");
        msg.setWrapText(true);
        msg.setMaxWidth(300);

        card.getChildren().addAll(row, msg);

        // Mark as read button
        if (unread) {
            Button markBtn = new Button("✓ Marquer comme lu");
            markBtn.setStyle(
                    "-fx-background-color:transparent;-fx-text-fill:#4f46e5;" +
                            "-fx-font-size:10px;-fx-cursor:hand;-fx-padding:2 0 0 0;");
            markBtn.setOnAction(e -> {
                NotificationCache.getInstance().markAsRead(notif.getId());
                loadNotifications();
            });
            card.getChildren().add(markBtn);
        }

        card.setOnMouseClicked(e -> {
            if (unread) {
                NotificationCache.getInstance().markAsRead(notif.getId());
                loadNotifications();
            }
        });

        return card;
    }

    // ── Badge ─────────────────────────────────────────────────────

    private void refreshBadge() {
        if (currentUserId < 0) return;
        long unread = NotificationCache.getInstance().getUnreadCount(currentUserId);
        System.out.println("🔔 [Badge] user #" + currentUserId + " → " + unread + " unread");

        if (unread > 0) {
            badgeLabel.setText(unread > 99 ? "99+" : String.valueOf(unread));
            badgeLabel.setVisible(true);
            badgeLabel.setManaged(true);
        } else {
            badgeLabel.setVisible(false);
            badgeLabel.setManaged(false);
        }
    }

    // ── Mark all read ─────────────────────────────────────────────

    private void markAllAsRead() {
        NotificationCache.getInstance().clearForUser(currentUserId);
        loadNotifications();
        refreshBadge();
    }
}