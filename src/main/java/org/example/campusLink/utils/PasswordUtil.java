package org.example.campusLink.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (hashedPassword == null || plainPassword == null) {
            return false;
        }

        // Normalize PHP's $2y$ prefix to $2a$ which jBCrypt understands
        String normalizedHash = hashedPassword.replace("$2y$", "$2a$");

        return BCrypt.checkpw(plainPassword, normalizedHash);
    }
}