package presentation;

import dao.IDao;
import metier.IMetier;
import metier.MetierIMP;

import java.io.File;
import java.util.Scanner;

public class PresentationDynamique {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(new File("src/main/resources/config.txt"));

        String daoClassName = scanner.nextLine();
        Class cDao = Class.forName(daoClassName);
        IDao dao = (IDao) cDao.newInstance();

        String metierClassName = scanner.nextLine();
        Class cMetier = Class.forName(metierClassName);



        IMetier metier = (IMetier) cMetier.getDeclaredConstructor().newInstance();

        // Injection
        ((MetierIMP) metier).setDao(dao);


        System.out.println("Résultat : " + metier.calcul());
    }
}