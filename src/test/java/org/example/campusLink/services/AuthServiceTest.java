package org.example.campusLink.services;

import org.example.campusLink.entities.User;
import org.example.campusLink.entities.Role;
import org.junit.jupiter.api.*;
import java.sql.SQLException;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthServiceTest {

    static AuthService authService;
    static User testUser;
    static final String TEST_ROLE = "ETUDIANT";

    @BeforeAll
    static void setup() throws SQLException {
        authService = new AuthService();

        // Make sure the role exists
        RoleService roleService = new RoleService();
        Role role = roleService.getRoleByName(TEST_ROLE);

    }

    @BeforeEach
    void initUser() {
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("testuser@example.com");
        testUser.setPassword("Password123");
        testUser.setPhone("+21612345678");
        testUser.setDateNaissance(Timestamp.valueOf("2000-01-01 00:00:00"));
        testUser.setGender("Male");
        testUser.setUniversite("ESPRIT");
        testUser.setFiliere("INFO");
        testUser.setSpecialization("GL");
        testUser.setAddress("Tunis");
        testUser.setProfilePicture("profile.png");
    }

    @Test
    @Order(1)
    void testSignUp_success() throws SQLException {
        authService.signUp(testUser, TEST_ROLE);
        assertTrue(testUser.getId() > 0, "User ID should be generated");
    }

    @Test
    @Order(2)
    void testSignUp_duplicateEmail() {
        SQLException exception = assertThrows(SQLException.class,
                () -> authService.signUp(testUser, TEST_ROLE));
        assertTrue(exception.getMessage().contains("Sign up failed"));
    }

    @Test
    @Order(3)
    void testLogin_success() throws SQLException {
        User loggedInUser = authService.login(testUser.getEmail(), "Password123", TEST_ROLE);
        assertNotNull(loggedInUser);
        assertEquals(testUser.getEmail(), loggedInUser.getEmail());
    }

    @Test
    @Order(4)
    void testLogin_invalidPassword() {
        SQLException exception = assertThrows(SQLException.class,
                () -> authService.login(testUser.getEmail(), "WrongPassword", TEST_ROLE));
        assertTrue(exception.getMessage().contains("Invalid password"));
    }

    @Test
    @Order(5)
    void testLogin_invalidRole() {
        SQLException exception = assertThrows(SQLException.class,
                () -> authService.login(testUser.getEmail(), "Password123", "ADMIN"));
        assertTrue(exception.getMessage().contains("User does not have role"));
    }


}

