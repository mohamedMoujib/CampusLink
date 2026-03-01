package org.example.campusLink.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private static MyDatabase instance;
    private volatile Connection connection;

    private final String URL = "jdbc:mysql://localhost:3306/campusLink";
    private final String USER = "root";
    private final String PASSWORD = "";

    private MyDatabase() {
    }

    public static synchronized MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    /**
     * Retourne une connexion valide. Si la connexion existante est fermée
     * (ex. après un try-with-resources dans un service), une nouvelle est créée.
     * Ne pas fermer cette connexion dans les appels — elle est partagée.
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connection to database established");
        }
        return connection;
    }
}
