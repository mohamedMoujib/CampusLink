package org.example.campusLink;

import org.example.campusLink.entities.Admin;
import org.example.campusLink.services.UserService;
import org.example.campusLink.utils.MyDatabase;
import org.example.campusLink.utils.PasswordUtil;

import java.sql.Connection;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        Connection connection = null;

        try {
            // 1️⃣ Get database connection
            connection = MyDatabase.getInstance().getConnection();

            // 2️⃣ Initialize UserService
            UserService userService = new UserService(connection);

            // 3️⃣ Create new Admin
            Admin admin = new Admin();
            admin.setName("Admin");
            admin.setEmail("admin@campuslink.tn");

            // Hash the password
            String hashedPassword = PasswordUtil.hashPassword("00000000"); // replace with your password
            admin.setPassword(hashedPassword);

            admin.setPhone("12345678");
            admin.setAddress("Admin HQ");
            admin.setGender("Male");
            admin.setStatus("ACTIVE"); // Admin should be active by default

            // 4️⃣ Save admin to DB
            userService.ajouterAdmin(admin); // assuming UserService has ajouterAdmin(Admin a)
            System.out.println("✅ Admin created successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("❌ Error creating admin: " + e.getMessage());
        } finally {
            try {
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
