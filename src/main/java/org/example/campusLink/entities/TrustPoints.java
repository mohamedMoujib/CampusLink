package org.example.campusLink.entities;

import java.time.LocalDateTime;

public class TrustPoints {

    private int id;
    private int prestataireId;
    private int pointsAdded;
    private String reason; // RESERVATION_COMPLETED / REVIEW_RATING
    private LocalDateTime date;

    public TrustPoints() {}

    public TrustPoints(int prestataireId, int pointsAdded, String reason) {
        this.prestataireId = prestataireId;
        this.pointsAdded = pointsAdded;
        this.reason = reason;
        this.date = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPrestataireId() {
        return prestataireId;
    }

    public void setPrestataireId(int prestataireId) {
        this.prestataireId = prestataireId;
    }

    public int getPointsAdded() {
        return pointsAdded;
    }

    public void setPointsAdded(int pointsAdded) {
        this.pointsAdded = pointsAdded;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
