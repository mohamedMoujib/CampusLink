package org.example.campusLink.Iservices;

import org.example.campusLink.entities.Demandes;
import java.sql.SQLException;
import java.util.List;

public interface Idemande {

    // CREATE : envoyer une demande de service
    void ajouterDemande(Demandes demande) throws SQLException;

    // READ : afficher toutes les demandes
    List<Demandes> afficherDemandes() throws SQLException;

    // UPDATE : modifier le statut de la demande
    void modifierStatut(int id, String status) throws SQLException;

    // DELETE : supprimer une demande
    void supprimerDemande(int id) throws SQLException;
}
