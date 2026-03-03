package org.example.campusLink.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private static MyDatabase instance;
    private Connection connection;

    private static final String URL = "jdbc:mysql://localhost:3306/campuslink?serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private MyDatabase() {
        try {
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connection established");
        } catch (SQLException e) {
            throw new RuntimeException("Error connecting to database", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (connection != null && !connection.isClosed()) connection.close();
            } catch (SQLException ignored) {}
        }));
    }

    public static MyDatabase getInstance() {
        if (instance == null) instance = new MyDatabase();
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) {}
    }
}
