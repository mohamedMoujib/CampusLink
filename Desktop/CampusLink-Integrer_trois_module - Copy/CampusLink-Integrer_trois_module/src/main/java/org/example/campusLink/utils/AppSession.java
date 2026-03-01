package org.example.campusLink.utils;

import org.example.campusLink.entities.User;

/**
 * Session utilisateur courante (après login).
 * Permet à toutes les vues (Student, Publication, etc.) d'accéder à l'utilisateur connecté.
 */
public final class AppSession {

    private static User currentUser;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
