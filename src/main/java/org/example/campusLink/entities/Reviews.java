package org.example.campusLink.entities;



public class Reviews {

    private int id;
    private int studentId;
    private int prestataireId;
    private int reservationId;
    private int rating; // 1 à 5
    private String comment;

    public Reviews() {}

    public Reviews(int studentId, int prestataireId, int reservationId, int rating, String comment) {
        this.studentId = studentId;
        this.prestataireId = prestataireId;
        this.reservationId = reservationId;
        this.rating = rating;
        this.comment = comment;
    }

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
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("La note doit être entre 1 et 5");
        }
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
