import java.util.Scanner;

public class zooz {



    public static void main(String[] args) {
       // int nbrCAges=20;
       //String Zoozname="my zooz";
        String Zoozname;
        int nbrCAges;
        //System.out.println(Zoozname+" comporte "+nbrCAges+"cages");
        Scanner sc = new Scanner(System.in);
        System.out.println("Saisir le nom du votre zooz");
        Zoozname=sc.nextLine();
        Scanner scc = new Scanner(System.in);
        System.out.println("Saisir le nombre de cage");
        nbrCAges=sc.nextInt();
        if (nbrCAges>=0) {
            System.out.println("le nombre de cage est :"+nbrCAges);
        }else  {
            System.out.println("Try again");
        }
        if (Zoozname.isEmpty()) {
            System.out.println("Saisir le nom du votre zooz");
        }else   {
            System.out.println("le nom du votre zooz est :"+Zoozname);
        }

    }



}
