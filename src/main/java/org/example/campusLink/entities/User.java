package org.example.campusLink.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;


public abstract class User {
    protected int id;
    protected String name;
    protected String email;
    protected String password;
    protected String phone;
    protected LocalDate dateNaissance;
    protected String gender;
    protected String profilePicture;
    protected String address;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
    protected String status; // ACTIVE, INACTIVE, BANNED

    public User() {
    }

    public User(int id, String name, String email, String password, String phone,
                LocalDate dateNaissance, String gender, String profilePicture,
                String address, String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.dateNaissance = dateNaissance;
        this.gender = gender;
        this.profilePicture = profilePicture;
        this.address = address;
        this.status = status;
    }

    // Méthode abstraite pour obtenir le type d'utilisateur
    public abstract String getUserType();

    // Getters & Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", userType='" + getUserType() + '\'' +
                '}';
    }
}