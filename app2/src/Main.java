//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        Animale lion=new Animale("aa","zoro",4,true);
        Animale turtul=new Animale("bb","sila",9,false);
        Animale monkey=new Animale("cc","apes",2,true);
        Animale snake=new Animale("dd","shino",1,false);

        Animale [] animals={lion,turtul,monkey,snake};

        Zoo z=new Zoo(animals,"belvidaire","tunis",25);

       z.DisplayZoo();
       System.out.println(z);
       System.out.println(lion);
    }
    }

