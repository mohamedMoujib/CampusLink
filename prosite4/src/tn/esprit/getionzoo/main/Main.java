package tn.esprit.getionzoo.main;

import tn.esprit.getionzoo.entities.Animal;
import tn.esprit.getionzoo.entities.Zoo;

public class Main {
    public static void main(String[] args) {
        Zoo z1 = new Zoo("belvidaire", "tunis");

        Animal lion = new Animal("zoro", "lion", 4, true);
        Animal cat = new Animal("shino", "cat", 1, false);

        // Add animals
        System.out.println("Added lion: " + z1.addAnimal(lion));
        System.out.println("Added cat: " + z1.addAnimal(cat));

        // Try adding duplicate
        System.out.println("Added cat again: " + z1.addAnimal(cat));

        // Display zoo
        z1.displayZoo();

        // Search
        System.out.println("Search index for cat: " + z1.searchAnimal(cat));

        // Remove animal
        System.out.println("Removed cat: " + z1.removeAnimal(cat));

        // Display again
        z1.displayZoo();

        // Another zoo
        Zoo z2 = new Zoo("friguia", "nabeul");
        z2.addAnimal(new Animal("tiger", "tiger", 5, true));

        // Compare zoos
        Zoo bigger = Zoo.comparerZoo(z1, z2);
        System.out.println("Zoo with more animals: " + bigger.getName());
    }
}
