package org.example.campusLink.services;

import org.example.campusLink.entities.Etudiant;
import org.example.campusLink.entities.User;
import org.example.campusLink.services.users.AuthService;
import org.junit.jupiter.api.*;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthServiceTest {

    static AuthService authService;
    static Etudiant testUser;

    @BeforeAll
    static void setup() {
        authService = new AuthService();
        System.out.println("=== START AUTH TESTS ===\n");
    }

    @BeforeEach
    void initUser() {
        testUser = new Etudiant();
        testUser.setName("Auth Test");
        testUser.setEmail("auth.test@example.com");
        testUser.setPassword("Password123");
        testUser.setPhone("+21699111222");
        testUser.setGender("Male");
        testUser.setUniversite("ESPRIT");
        testUser.setFiliere("INFO");
        testUser.setSpecialization("GL");
        testUser.setAddress("Tunis");
    }

    // ==================== SIGNUP ====================

    @Test
    @Order(1)
    void testSignup_success() throws SQLException {

        authService.signupEtudiant(testUser);

        assertTrue(testUser.getId() > 0);
        assertEquals("INACTIVE", testUser.getStatus());
    }

    @Test
    @Order(2)
    void testSignup_duplicateEmail() {

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.signupEtudiant(testUser)
        );

        assertTrue(ex.getMessage().contains("déjà utilisé"));
    }

    // ==================== LOGIN ====================

    @Order(3)
    void testLogin_inactiveAccount() {

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(testUser.getEmail(), "Password123","ETUDIANT")
        );

        assertTrue(ex.getMessage().contains("inactif"));
    }

    @Order(4)
    void testLogin_success() throws SQLException {

        // Activate first
        authService.activateAccount(testUser);

        User loggedUser = authService.login(
                testUser.getEmail(),
                "Password123","ETUDIANT"
        );

        assertNotNull(loggedUser);
        assertEquals(testUser.getEmail(), loggedUser.getEmail());
    }

    @Test
    @Order(5)
    void testLogin_wrongPassword() {

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(testUser.getEmail(), "WrongPass","ETUDIANT")
        );

        assertTrue(ex.getMessage().contains("incorrect"));
    }

    @Test
    @Order(6)
    void testLogin_emailNotFound() {

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login("unknown@email.com", "Password123","ETUDIANT")
        );

        assertTrue(ex.getMessage().contains("Email non trouvé"));
    }

    // ==================== CHANGE PASSWORD ====================

    @Order(7)
    void testChangePassword_success() throws SQLException {



        // 2️⃣ Activate
        authService.activateAccount(testUser);

        // 3️⃣ Change password
        authService.changePassword(
                testUser,
                "Password123",
                "NewPassword123"
        );

        // 4️⃣ Test login with new password
        User logged = authService.login(
                testUser.getEmail(),
                "NewPassword123","ETUDIANT"
        );

        assertNotNull(logged);
    }


    @Order(8)
    void testChangePassword_wrongCurrent() throws SQLException {

        authService.activateAccount(testUser);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.changePassword(
                        testUser,
                        "WrongCurrent",
                        "AnotherPass123"
                )
        );

        assertTrue(ex.getMessage().contains("actuel incorrect"));
    }


    // ==================== BAN ACCOUNT ====================

    @Order(9)
    void testLogin_bannedUser() throws SQLException {

        authService.activateAccount(testUser);

        authService.banAccount(testUser);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(testUser.getEmail(), "Password123","ETUDIANT")
        );

        assertTrue(ex.getMessage().contains("banni"));
    }


    @AfterAll
    static void tearDown() {
        System.out.println("\n🎉 AUTH TESTS FINISHED");
    }
}
