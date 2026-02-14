package org.example.campusLink.services;


import org.example.campusLink.entities.Role;
import org.example.campusLink.entities.User;
import org.example.campusLink.utils.MyDatabase;
import org.example.campusLink.utils.PasswordUtil;

import java.sql.Connection;
import java.sql.SQLException;

public class AuthService {

    private final Connection connection;
    private final UserService userService;
    private final UserRoleService userRoleService;
    private final RoleService roleService;

    public AuthService() {
        this.connection = MyDatabase.getInstance().getConnection();
        this.userService = new UserService(this.connection);
        this.userRoleService = new UserRoleService(this.connection);
        this.roleService = new RoleService(this.connection);
    }


    public void signUp(User user, String roleName) throws SQLException {

        try {
            connection.setAutoCommit(false);

            userService.ajouter(user);

            if (user.getId() <= 0) {
                throw new SQLException("User ID not generated");
            }

            Role role = roleService.getRoleByName(roleName);

            if (role == null) {
                throw new SQLException("Role not found: " + roleName);
            }

            userRoleService.ajouter(user.getId(), role.getId());

            connection.commit();

        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Sign up failed", e);
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public User login(String email, String password, String roleName) throws SQLException {

        User user = userService.getByEmail(email);
        if (user == null) {
            throw new SQLException("User not found with email: " + email);
        }

        if (!PasswordUtil.checkPassword(password, user.getPassword())) {
            throw new SQLException("Invalid password");
        }

        Role role = roleService.getRoleByName(roleName);
        if (role == null) {
            throw new SQLException("Role not found: " + roleName);
        }

        boolean hasRole = userRoleService.userHasRole(user.getId(), role.getId());
        if (!hasRole) {
            throw new SQLException("User does not have role: " + roleName);
        }

        return user;
    }
}

