package org.example.campusLink.entities;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

public class User {

    private int id;
    private String name;
    private String email;
    private String password;
    private String phone;
    private Timestamp dateNaissance;
    private String gender;
    private String universite;
    private String filiere;
    private String specialization;
    private int trustPoints;
    private String profilePicture;
    private String address;
    private String status; // "ACTIVE", "INACTIVE", "BANNED"
    private List<Role> roles; //

    public User() {}

    public User( String name, String email, String password, String phone,
                Timestamp dateNaissance, String gender, String universite,
                String filiere, String specialization, int trustPoints,
                String profilePicture, String address,
                String status, List<Role> roles) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.dateNaissance = dateNaissance;
        this.gender = gender;
        this.universite = universite;
        this.filiere = filiere;
        this.specialization = specialization;
        this.trustPoints = trustPoints;
        this.profilePicture = profilePicture;
        this.address = address;
        this.status = status;
        this.roles = roles;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getPhone() {
        return phone;
    }

    public Timestamp getDateNaissance() {
        return dateNaissance;
    }

    public String getGender() {
        return gender;
    }

    public String getUniversite() {
        return universite;
    }

    public String getFiliere() {
        return filiere;
    }

    public String getSpecialization() {
        return specialization;
    }

    public int getTrustPoints() {
        return trustPoints;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public String getAddress() {
        return address;
    }

    public String getStatus() {
        return status;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public void setTrustPoints(int trustPoints) {
        this.trustPoints = trustPoints;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public void setFiliere(String filiere) {
        this.filiere = filiere;
    }

    public void setUniversite(String universite) {
        this.universite = universite;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setDateNaissance(Timestamp dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }
}
