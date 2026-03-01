package org.example.campusLink.entities;

import java.time.LocalDate;


public class Etudiant extends User {
    private String universite;
    private String filiere;
    private String specialization;

    // Constructeur vide
    public Etudiant() {
        super();
    }

    // Constructeur complet
    public Etudiant(int id, String name, String email, String password, String phone,
                    LocalDate dateNaissance, String gender, String profilePicture,
                    String address, String status, String universite, String filiere,
                    String specialization) {
        super(id, name, email, password, phone, dateNaissance, gender, profilePicture, address, status);
        this.universite = universite;
        this.filiere = filiere;
        this.specialization = specialization;
    }

    @Override
    public String getUserType() {
        return "ETUDIANT";
    }

    // Getters & Setters
    public String getUniversite() {
        return universite;
    }

    public void setUniversite(String universite) {
        this.universite = universite;
    }

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

    @Override
    public String toString() {
        return "Etudiant{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", universite='" + universite + '\'' +
                ", filiere='" + filiere + '\'' +
                ", specialization='" + specialization + '\'' +
                '}';
    }
}