package org.example.campusLink.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private static MyDatabase instance;
    private Connection connection;

    private final String URL = "jdbc:mysql://localhost:3306/campusLink";
    private final String USER = "root";
    private final String PASSWORD = "";

    private MyDatabase() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connection to database established");
        } catch (SQLException e) {
            throw new RuntimeException("Error connecting to database", e);
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
