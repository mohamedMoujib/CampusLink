package org.example.campusLink.entities;

public class Role {
    private int id;
    private String name; // "ETUDIANT", "PRESTATAIRE", "ADMIN"

    // Constructors
    public Role() {}

    public Role(int id, String name) {
        this.id = id;
        this.name = name;
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

    public void setName(String name) {
        this.name = name;
    }
}

