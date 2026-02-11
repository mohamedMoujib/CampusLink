package org.example.campusLink.units;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private static final String URL =
            "jdbc:mysql://localhost:3306/campusLink?serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static MyDatabase instance;

    private MyDatabase() {
        System.out.println("✅ Database ready");
    }

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    // 🔥 Nouvelle connexion à chaque appel
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
