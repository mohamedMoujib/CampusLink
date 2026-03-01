package org.example.campusLink.Iservices;


import org.example.campusLink.entities.Services;

import java.sql.SQLException;
import java.util.List;

public interface Iservice {

    // CREATE
    void ajouterService(Services service) throws SQLException;

    // READ
    List<Services> afficherServices() throws SQLException;

    // UPDATE
    void modifierService(Services service) throws SQLException;

    // DELETE
    void supprimerService(int id) throws SQLException;
}
