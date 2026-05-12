public class Main {
    public static void main(String[] args) {
        Animal lion = new Animal("aa", "zoro", 4, true);
        Animal turtul = new Animal("bb", "sila", 9, false);
        Animal monkey = new Animal("cc", "apes", 2, true);
        Animal snake = new Animal("dd", "shino", 1, false);

        Animal[] animals = {lion, turtul, monkey, snake};
        System.out.println(lion);
        Zoo z = new Zoo(animals, "belvidaire", "tunis");

        z.DisplayZoo();

        System.out.println(z);

        Animal cat = new Animal("cat", "shino", 1, true);
        boolean add = z.addAnimal(cat);
        System.out.println("l ajout d un animale : " + add);
        System.out.println(z);
        int index = z.searchAnimal(cat);
        System.out.println("Search index for cat: " + index);
    }
}