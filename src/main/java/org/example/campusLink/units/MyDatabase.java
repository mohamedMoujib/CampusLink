package org.example.campusLink.units;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {
    private final String URL = "jdbc:mysql://localhost:3306/campuslink";
    private final String USER = "root";
    private final String PASSWORD = "";
    private Connection connection;
    private static MyDatabase instance;

    private MyDatabase() {
        try {
            this.connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/campuslink", "root", "");
            System.out.println("Connection established");
        } catch (SQLException e) {
            throw new RuntimeException("Error connecting to database", e);        }

    }

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }

        return instance;
    }

    public Connection getConnection() {
        return this.connection;
    }
}

