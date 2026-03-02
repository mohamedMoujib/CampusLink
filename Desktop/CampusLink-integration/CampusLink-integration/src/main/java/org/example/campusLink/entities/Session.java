package org.example.campusLink.entities;

import org.example.campusLink.entities.User;

public class Session {

    private static User currentUser;

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static int getUserId() {
        return currentUser.getId();
    }
}