package org.example.campusLink.services.paiements;

import java.sql.SQLException;
import java.util.List;

public interface IServices <T>{
    void ajouter (T t) throws SQLException;
    void modifier (T t) throws SQLException;
    void supprimer (T t) throws SQLException;
    List<T> recuperer() throws SQLException;
}
