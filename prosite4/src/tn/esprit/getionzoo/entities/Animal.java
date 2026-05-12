package tn.esprit.getionzoo.entities;

public class Animal {
    private String name;
    private String family;
    private int age;
    private boolean mammal;

    public Animal() {
    }

    public Animal(String name, String family, int age, boolean mammal) {
        this.name = name;
        this.family = family;
        this.age = age;
        this.mammal = mammal;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Animal name cannot be empty");
        }
        this.name = name;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
        this.age = age;
    }

    public boolean isMammal() {
        return mammal;
    }

    public void setMammal(boolean mammal) {
        this.mammal = mammal;
    }

    @Override
    public String toString() {
        return "Name: " + name +
                ", Family: " + family +
                ", Age: " + age +
                ", Mammal: " + mammal;
    }
}
