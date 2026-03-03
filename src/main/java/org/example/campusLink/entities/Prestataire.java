package org.example.campusLink.entities;

import java.time.LocalDate;


public class Prestataire extends User {
    private int trustPoints;
    private String universite;
    private String filiere;
    private String specialization;

    public Prestataire() {
        super();
        this.trustPoints = 0;
    }

    public Prestataire(int id, String name, String email, String password, String phone,
                       LocalDate dateNaissance, String gender, String profilePicture,
                       String address, String status,
                       String universite, String filiere,
                       String specialization, int trustPoints) {

        super(id, name, email, password, phone,
                dateNaissance, gender, profilePicture,
                address, status);

        this.universite = universite;
        this.filiere = filiere;
        this.specialization = specialization;
        this.trustPoints = trustPoints;
    }

    @Override
    public String getUserType() {
        return "PRESTATAIRE";
    }

    // Getters & Setters


    public String getFiliere() {
        return filiere;
    }
    public void setFiliere(String filiere) {
        this.filiere = filiere;
    }

    public String getSpecialization() {
        return specialization;
    }
    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getUniversite() {
        return universite;
    }
    public void setUniversite(String universite) {
        this.universite = universite;
    }

    public int getTrustPoints() {
        return trustPoints;
    }

    public void setTrustPoints(int trustPoints) {
        this.trustPoints = trustPoints;
    }

    public void addTrustPoints(int points) {
        this.trustPoints += points;
    }

    @Override
    public String toString() {
        return "Prestataire{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", trustPoints=" + trustPoints +
                '}';
    }
}