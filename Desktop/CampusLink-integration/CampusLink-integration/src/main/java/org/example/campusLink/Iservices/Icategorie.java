package org.example.campusLink.Iservices;

import org.example.campusLink.entities.Categorie;
import java.sql.SQLException;
import java.util.List;

public interface Icategorie {

    void ajouterCategorie(Categorie categorie) throws SQLException;

    List<Categorie> afficherCategories() throws SQLException;

    void modifierCategorie(Categorie categorie) throws SQLException;

    void supprimerCategorie(int id) throws SQLException;
}
