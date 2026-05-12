public class Zoo {
    public  Animale[] animals;
    public  String name;
    public String city;
    public  int nbrCages;


    public Zoo(Animale[] animals, String name, String city, int nbrCages) {
        this.animals = animals;
        this.name = name;
        this.city = city;
        this.nbrCages = nbrCages;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("nom: ").append(this.name).append("\n");
        sb.append("city: ").append(this.city).append("\n");
        for (Animale a : animals)
            sb.append("Animals: ").append(a.toString()).append("\n");

        sb.append("nbrCages: ").append(this.nbrCages).append("\n");
        return sb.toString();

    }
    public void DisplayZoo() {
        System.out.println("Animals: " + this.animals);
        System.out.println("Nom: " + this.name);
        System.out.println("Ville: " + this.city);
        System.out.println("Nombre de cage: " + this.nbrCages);
    }
}
