package org.example.campusLink.units;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private static final String URL =
            "jdbc:mysql://localhost:3306/campusLink?serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // 👈 XAMPP

    private static MyDatabase instance;
    private Connection connection;

    private MyDatabase() {
        try {
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connection established");
        } catch (SQLException e) {
            throw new RuntimeException("❌ Error connecting to database", e);
        }
    }

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
