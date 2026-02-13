package org.example.campusLink.entities;

public class Reviews {

    private int id;
    private int studentId;
    private int prestataireId;
    private int reservationId;
    private int rating;
    private String comment;

    // 🔥 NOUVEAUX CHAMPS
    private String serviceTitle;
    private String prestataireName;
    private String studentName;

    public Reviews() {}

    public Reviews(int studentId, int prestataireId,
                   int reservationId, int rating, String comment) {
        this.studentId = studentId;
        this.prestataireId = prestataireId;
        this.reservationId = reservationId;
        this.rating = rating;
        this.comment = comment;
    }

    // ===== GETTERS / SETTERS =====

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public int getPrestataireId() { return prestataireId; }
    public void setPrestataireId(int prestataireId) { this.prestataireId = prestataireId; }

    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    // 🔥 NOUVEAUX
    public String getServiceTitle() { return serviceTitle; }
    public void setServiceTitle(String serviceTitle) { this.serviceTitle = serviceTitle; }

    public String getPrestataireName() { return prestataireName; }
    public void setPrestataireName(String prestataireName) { this.prestataireName = prestataireName; }
    public String getStudentName() {return studentName;}
    public void setStudentName(String studentName) {this.studentName = studentName;}
}
