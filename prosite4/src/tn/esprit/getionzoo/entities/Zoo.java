package tn.esprit.getionzoo.entities;

public class Zoo {
    private Animal[] animals;
    private String name;
    private String city;
    private final int nbrCages = 25;
    private int nbrAnimaux;

    // Constructors
    public Zoo() {
        this.animals = new Animal[nbrCages];
        this.nbrAnimaux = 0;
        this.name = "";
        this.city = "";
    }

    public Zoo(String name, String city) {
        this();
        setName(name);
        this.city = city;
    }

    public Zoo(Animal[] tab, String name, String city) {
        this(name, city);
        for (Animal a : tab) {
            addAnimal(a);
        }
    }

    // Getters and Setters
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Zoo name cannot be empty");
        }
        this.name = name;
    }

    public String getCity() {
        return this.city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getNbrAnimaux() {
        return nbrAnimaux;
    }

    public int getNbrCages() {
        return nbrCages;
    }

    // Add an animal to the zoo
    public boolean addAnimal(Animal a) {
        if (isZooFull()) {
            System.out.println("Zoo is full!");
            return false;
        }
        if (searchAnimal(a) != -1) { // Already exists
            System.out.println("Animal already exists in the zoo!");
            return false;
        }
        animals[nbrAnimaux] = a;
        nbrAnimaux++;
        return true;
    }

    // Search for an animal by name
    public int searchAnimal(Animal a) {
        for (int i = 0; i < nbrAnimaux; i++) {
            if (animals[i].getName().equalsIgnoreCase(a.getName())) {
                return i;
            }
        }
        return -1;
    }

    // Remove an animal
    public boolean removeAnimal(Animal a) {
        int ind = searchAnimal(a);
        if (ind == -1) {
            return false;
        }
        for (int i = ind; i < nbrAnimaux - 1; i++) {
            animals[i] = animals[i + 1];
        }
        animals[nbrAnimaux - 1] = null;
        nbrAnimaux--;
        return true;
    }

    // Check if zoo is full
    public boolean isZooFull() {
        return nbrAnimaux >= nbrCages;
    }

    // Compare two zoos
    public static Zoo comparerZoo(Zoo z1, Zoo z2) {
        return (z1.nbrAnimaux >= z2.nbrAnimaux) ? z1 : z2;
    }

    // Display zoo information
    public void displayZoo() {
        System.out.println("Nom: " + this.name);
        System.out.println("Ville: " + this.city);
        System.out.println("Nombre de cages: " + this.nbrCages);
        System.out.println("Animaux (" + nbrAnimaux + "):");
        for (int i = 0; i < nbrAnimaux; i++) {
            System.out.println("  " + animals[i]);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("nom: ").append(this.name).append("\n");
        sb.append("city: ").append(this.city).append("\n");
        for (int i = 0; i < nbrAnimaux; i++) {
            sb.append("Animal: ").append(animals[i].toString()).append("\n");
        }
        sb.append("nbrCages: ").append(this.nbrCages).append("\n");
        return sb.toString();
    }
}
