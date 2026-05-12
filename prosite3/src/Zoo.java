public class Zoo {
    public Animal[] animals;
    public String name;
    public String city;
    public final int nbrCages = 25;
    private int nbrAnimaux;

    public Zoo() {
        this.animals = new Animal[nbrCages];
        this.nbrAnimaux = 0;
        this.name = "";
        this.city = "";
    }

    public Zoo(String name, String city) {
        this();
        this.name = name;
        this.city = city;
    }

    public Zoo(Animal[] tab, String name, String city) {
        this(name, city);
        for (Animal a : tab) {
            addAnimal(a);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("nom: ").append(this.name).append("\n");
        sb.append("city: ").append(this.city).append("\n");
        for (int i = 0; i < nbrAnimaux; i++) {
            sb.append("Animals: ").append(animals[i].toString()).append("\n");
        }
        sb.append("nbrCages: ").append(this.nbrCages).append("\n");
        return sb.toString();
    }

    public void DisplayZoo() {
        System.out.println("Nom: " + this.name);
        System.out.println("Ville: " + this.city);
        System.out.println("Nombre de cage: " + this.nbrCages);
        System.out.println("Animals:");
        for (int i = 0; i < nbrAnimaux; i++) {
            System.out.println("  " + animals[i]);
        }
    }

    public boolean addAnimal(Animal a) {
        if (searchAnimal(a) != -1) {
            return false;
        }else{
        animals[nbrAnimaux] = a;
        nbrAnimaux++;}
        return true;
    }

    public int searchAnimal(Animal a) {
        for (int i = 0; i < nbrAnimaux; i++) {
            if (animals[i].name.equals(a.name)) {
                return i;
            }
        }
        return -1;
    }

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

    public boolean isZooFull() {
        return nbrAnimaux >= nbrCages;
    }

    public static Zoo comparerZoo(Zoo z1, Zoo z2) {
        if (z1.nbrAnimaux >= z2.nbrAnimaux) {
            return z1;
        } else {
            return z2;
        }
    }
}