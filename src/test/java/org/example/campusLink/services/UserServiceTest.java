package org.example.campusLink.services;

import org.example.campusLink.entities.Etudiant;
import org.example.campusLink.entities.User;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceTest {

    static UserService userService;
    static Etudiant testUser;
    static int savedUserId;

    @BeforeAll
    static void setup() {
        userService = new UserService();
        System.out.println("=== DÉBUT TEST USERSERVICE ===\n");
    }

    @BeforeEach
    void initUser() {
        testUser = new Etudiant();
        testUser.setName("admin ");
        testUser.setEmail("admin@campuslink.tn");
        testUser.setPassword("00000000");
        testUser.setPhone("+21699115248");
        testUser.setGender("Male");
        //testUser.setUniversite("ESPRIT");
        //testUser.setFiliere("INFO");
        //testUser.setSpecialization("GL");
        //testUser.setAddress("Tunis");
        testUser.setStatus("ACTIVE");
    }

    // ==================== AJOUTER ====================

    @Test
    @Order(1)
    void testAjouterUser_Success() throws SQLException {

        userService.ajouter(testUser);
        savedUserId = testUser.getId();

        assertTrue(savedUserId > 0);
    }

    @Order(2)
    void testAjouterUser_InvalidEmail() {

        testUser.setEmail("invalid");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.ajouter(testUser)
        );

        assertTrue(ex.getMessage().contains("Email invalide"));
    }

    @Order(3)
    void testAjouterUser_InvalidPassword() {

        testUser.setEmail("unique@email.com");
        testUser.setPassword("123");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.ajouter(testUser)
        );

        assertTrue(ex.getMessage().contains("Mot de passe invalide"));
    }

    // ==================== GET BY ID ====================

    @Order(4)
    void testGetById_Success() throws SQLException {

        User fetched = userService.getById(savedUserId);

        assertNotNull(fetched);
        assertEquals("ahmed.test@example.com", fetched.getEmail());
    }

    @Order(5)
    void testGetById_NonExistent() throws SQLException {

        User fetched = userService.getById(999999);
        assertNull(fetched);
    }

    // ==================== RECUPERER ====================

    @Order(6)
    void testRecuperer() throws SQLException {

        List<User> users = userService.recuperer();
        assertNotNull(users);
        assertTrue(users.size() > 0);
    }

    // ==================== MODIFIER ====================

    @Order(7)
    void testModifier_Success() throws SQLException {

        Etudiant user = (Etudiant) userService.getById(savedUserId);
        user.setName("Ahmed Modified");
        user.setStatus("ACTIVE");

        userService.modifier(user);

        User updated = userService.getById(savedUserId);
        assertEquals("Ahmed Modified", updated.getName());
        assertEquals("ACTIVE", updated.getStatus());
    }

    @Order(8)
    void testModifier_InvalidEmail() throws SQLException {

        Etudiant user = (Etudiant) userService.getById(savedUserId);
        user.setEmail("invalid");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.modifier(user)
        );

        assertTrue(ex.getMessage().contains("Email invalide"));
    }

    // ==================== SUPPRIMER ====================


    @Order(9)
    void testSupprimer_Success() throws SQLException {

        User user = userService.getById(savedUserId);
        assertNotNull(user);

        userService.supprimer(user);

        User deleted = userService.getById(savedUserId);
        assertNull(deleted);
    }

    @AfterAll
    static void tearDown() {
        System.out.println("\n🎉 === TOUS LES TESTS TERMINÉS ===");
    }
}
