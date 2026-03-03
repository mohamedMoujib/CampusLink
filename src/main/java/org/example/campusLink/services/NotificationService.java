package org.example.campusLink.services;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class NotificationService {
    private static final NotificationService instance = new NotificationService();
    private final ObservableList<String> notifications = FXCollections.observableArrayList();
    private final IntegerProperty unreadCount = new SimpleIntegerProperty();

    private NotificationService() {}

    public static NotificationService getInstance() {
        return instance;
    }

    public void addNotification(String message) {
        notifications.add(message);
        unreadCount.set(notifications.size());
    }

    public ObservableList<String> getNotifications() {
        return notifications;
    }

    public IntegerProperty unreadCountProperty() {
        return unreadCount;
    }

    public void markAllAsRead() {
        unreadCount.set(0);
    }
}