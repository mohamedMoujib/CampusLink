package org.example.campusLink.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {
    private final String URL = "jdbc:mysql://localhost:3306/campusLink";
    private final String USER = "root";
    private final String PASSWORD = "";
    private Connection connection;
    private static MyDatabase instance;

    private MyDatabase() {
        try {
            System.out.println("🔌 Tentative de connexion à la base de données...");
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connexion à la base de données établie!");
        } catch (SQLException e) {
            System.err.println("❌ ERREUR: Impossible de se connecter à la base de données");
            System.err.println("   Message: " + e.getMessage());
            System.err.println("   URL: " + URL);
            System.err.println("\n⚠️ Vérifiez que:");
            System.err.println("   1. MySQL est démarré");
            System.err.println("   2. La base 'campusLink' existe");
            System.err.println("   3. L'utilisateur 'root' avec mot de passe vide est configuré");
            System.err.println("\n⚠️ L'application continuera mais les fonctionnalités nécessitant la BD seront désactivées.\n");

            // NE PAS lancer de RuntimeException, juste mettre connection à null
            this.connection = null;
        }
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

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}