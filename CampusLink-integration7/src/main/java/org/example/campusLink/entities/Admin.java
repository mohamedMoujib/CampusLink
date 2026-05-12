package org.example.campusLink.entities;

import java.time.LocalDate;


public class Admin extends User {

    // Constructeur vide
    public Admin() {
        super();
    }

    // Constructeur complet
    public Admin(int id, String name, String email, String password, String phone,
                 LocalDate dateNaissance, String gender, String profilePicture,
                 String address, String status, String department) {
        super(id, name, email, password, phone, dateNaissance, gender, profilePicture, address, status);
    }

    @Override
    public String getUserType() {
        return "ADMIN";
    }



    @Override
    public String toString() {
        return "Admin{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}