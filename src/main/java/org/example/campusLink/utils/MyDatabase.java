package org.example.campusLink.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private static final String URL =
            "jdbc:mysql://localhost:3306/campusLinkmerge?serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static MyDatabase instance;

    private MyDatabase() {
        System.out.println("🔥 MyDatabase READY (connection per call)");
    }

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    // ✅ NOUVELLE connexion à chaque appel
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}