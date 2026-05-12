package org.example.campusLink.entities;

import java.time.LocalDateTime;

public class Reviews {

    private int id;
    private int studentId;
    private int prestataireId;
    private int reservationId;
    private int rating;
    private String comment;

    // 🔥 Nouveaux champs pour le signalement
    private boolean isReported;
    private String reportReason;
    private LocalDateTime reportedAt;

    // Champs supplémentaires (non en BD, mais récupérés via JOIN)
    private String serviceTitle;
    private String prestataireName;
    private String studentName;

    // ===================== CONSTRUCTEURS =====================

    public Reviews() {
    }

    public Reviews(int studentId, int prestataireId, int reservationId, int rating, String comment) {
        this.studentId = studentId;
        this.prestataireId = prestataireId;
        this.reservationId = reservationId;
        this.rating = rating;
        this.comment = comment;
        this.isReported = false;
    }

    // ===================== GETTERS & SETTERS =====================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getPrestataireId() {
        return prestataireId;
    }

    public void setPrestataireId(int prestataireId) {
        this.prestataireId = prestataireId;
    }

    public int getReservationId() {
        return reservationId;
    }

    public void setReservationId(int reservationId) {
        this.reservationId = reservationId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    // 🔥 GETTERS & SETTERS pour le signalement

    public boolean isReported() {
        return isReported;
    }

    public void setReported(boolean reported) {
        isReported = reported;
    }

    public String getReportReason() {
        return reportReason;
    }

    public void setReportReason(String reportReason) {
        this.reportReason = reportReason;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    // Champs supplémentaires

    public String getServiceTitle() {
        return serviceTitle;
    }

    public void setServiceTitle(String serviceTitle) {
        this.serviceTitle = serviceTitle;
    }

    public String getPrestataireName() {
        return prestataireName;
    }

    public void setPrestataireName(String prestataireName) {
        this.prestataireName = prestataireName;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    @Override
    public String toString() {
        return "Reviews{" +
                "id=" + id +
                ", studentId=" + studentId +
                ", prestataireId=" + prestataireId +
                ", rating=" + rating +
                ", isReported=" + isReported +
                '}';
    }
}