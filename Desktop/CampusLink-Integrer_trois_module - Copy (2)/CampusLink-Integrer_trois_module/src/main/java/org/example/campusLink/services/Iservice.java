package org.example.campusLink.services;

import java.sql.SQLException;
import java.util.List;

public interface Iservice <T>{
    void ajouter(T entity) throws SQLException;
    T getById(int id) throws SQLException;
    List<T> recuperer() throws SQLException;
    void modifier(T entity)throws SQLException;
    void supprimer(T entity)  throws SQLException;
}