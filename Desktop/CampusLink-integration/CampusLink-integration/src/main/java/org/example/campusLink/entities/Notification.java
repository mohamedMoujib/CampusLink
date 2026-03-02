package org.example.campusLink.entities;

import java.time.LocalDateTime;

public class Notification {
    private int id;
    private String message;
    private int reservationId;
    private boolean read;
    private LocalDateTime timestamp;

    public Notification(int id, String message, int reservationId, boolean read) {
        this.id = id;
        this.message = message;
        this.reservationId = reservationId;
        this.read = read;
        this.timestamp = LocalDateTime.now();
    }

    // Getters et setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}