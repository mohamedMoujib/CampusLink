package org.example.campusLink.services;

import org.example.campusLink.entities.User;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceTest {

    static UserService userService;
    static User testUser;
    static int savedUserId; // Pour garder l'ID entre les tests

    @BeforeAll
    static void setup() {
        userService = new UserService();
        System.out.println("DÉBUT DES TESTS USERSERVICE \n");
    }

    @BeforeEach
    void initUser() {
        testUser = new User();
        testUser.setName("Ahmed Test");
        testUser.setEmail("ahmed.test@example.com");
        testUser.setPassword("Password123");
        testUser.setPhone("+21699112248");
        testUser.setDateNaissance(Timestamp.valueOf("2000-01-01 00:00:00"));
        testUser.setGender("Male");
        testUser.setUniversite("ESPRIT");
        testUser.setFiliere("INFO");
        testUser.setSpecialization("GL");
        testUser.setAddress("Tunis");
        testUser.setProfilePicture("test.png");
    }

    // ==================== TESTS AJOUTER ====================

    @Test
    @Order(1)
    @DisplayName("✅ Ajouter un utilisateur valide")
    void testAjouterUser_Success() throws SQLException {
        System.out.println("Test 1: Ajouter un utilisateur valide...");

        userService.ajouter(testUser);
        savedUserId = testUser.getId();

        assertTrue(testUser.getId() > 0, "L'ID devrait être généré");
        System.out.println("✅ User créé avec ID: " + testUser.getId());
    }

    @Test
    @Order(2)
    @DisplayName("❌ Ajouter avec email invalide")
    void testAjouterUser_InvalidEmail() {
        System.out.println("Test 2: Email invalide...");

        testUser.setEmail("aaaaaaaa");

        SQLException exception = assertThrows(
                SQLException.class,
                () -> userService.ajouter(testUser)
        );

        assertTrue(exception.getMessage().contains("Email invalide"));
        System.out.println("✅ Validation email fonctionne!");
    }

    @Test
    @Order(3)
    @DisplayName("❌ Ajouter avec email vide")
    void testAjouterUser_EmptyEmail() {
        System.out.println("Test 3: Email vide...");

        testUser.setEmail("");

        SQLException exception = assertThrows(
                SQLException.class,
                () -> userService.ajouter(testUser)
        );

        assertTrue(exception.getMessage().contains("Email invalide"));
        System.out.println("✅ Validation email vide fonctionne!");
    }



    @Test
    @Order(4)
    @DisplayName("❌ Ajouter avec mot de passe trop court")
    void testAjouterUser_PasswordTooShort() {
        System.out.println("Test 5: Mot de passe trop court...");

        testUser.setEmail("unique2@test.com");
        testUser.setPassword("12345"); // Moins de 6 caractères

        SQLException exception = assertThrows(
                SQLException.class,
                () -> userService.ajouter(testUser)
        );

        assertTrue(exception.getMessage().contains("Mot de passe invalide"));
        System.out.println("✅ Validation mot de passe fonctionne!");
    }

    @Test
    @Order(5)
    @DisplayName("❌ Ajouter avec téléphone invalide")
    void testAjouterUser_InvalidPhone() {
        System.out.println("Test 6: Téléphone invalide...");

        testUser.setEmail("unique3@test.com");
        testUser.setPhone("ABC123XYZ");

        SQLException exception = assertThrows(
                SQLException.class,
                () -> userService.ajouter(testUser)
        );

        assertTrue(exception.getMessage().contains("téléphone invalide"));
        System.out.println("✅ Validation téléphone fonctionne!");
    }



    @Test
    @Order(6)
    @DisplayName("❌ Ajouter avec email en double")
    void testAjouterUser_DuplicateEmail() {
        System.out.println("Test 8: Email en double...");

        // Utiliser le même email que le test 1
        testUser.setEmail("ahmed.test@example.com");

        SQLException exception = assertThrows(
                SQLException.class,
                () -> userService.ajouter(testUser)
        );

        assertTrue(exception.getMessage().contains("email existe déjà"));
        System.out.println("✅ Validation email unique fonctionne!");
    }

    // ==================== TESTS GETBYID ====================

    @Test
    @Order(7)
    @DisplayName("✅ Récupérer par ID valide")
    void testGetById_Success() throws SQLException {
        System.out.println("Test 9: Récupérer par ID valide...");

        User fetched = userService.getById(savedUserId);

        assertNotNull(fetched);
        assertEquals("ahmed.test@example.com", fetched.getEmail());
        System.out.println("✅ User récupéré: " + fetched.getName());
    }

    @Test
    @Order(8)
    @DisplayName("❌ Récupérer avec ID invalide (négatif)")
    void testGetById_InvalidId() {
        System.out.println("Test 10: ID négatif...");

        SQLException exception = assertThrows(
                SQLException.class,
                () -> userService.getById(-1)
        );

        assertTrue(exception.getMessage().contains("ID invalide"));
        System.out.println("✅ Validation ID négatif fonctionne!");
    }



    @Test
    @Order(9)
    @DisplayName("✅ Récupérer par ID inexistant retourne null")
    void testGetById_NonExistent() throws SQLException {
        System.out.println("Test 12: ID inexistant...");

        User fetched = userService.getById(99999);

        assertNull(fetched);
        System.out.println("✅ ID inexistant retourne null!");
    }

    // ==================== TESTS RECUPERER ====================

    @Test
    @Order(10)
    @DisplayName("✅ Récupérer tous les utilisateurs")
    void testRecuperer_Success() throws SQLException {
        System.out.println("Test 13: Récupérer tous les utilisateurs...");

        List<User> users = userService.recuperer();

        assertNotNull(users);
        assertTrue(users.size() > 0, "Il devrait y avoir au moins 1 utilisateur");
        System.out.println("✅ " + users.size() + " utilisateur(s) trouvé(s)");
    }

    // ==================== TESTS MODIFIER ====================

    @Test
    @Order(11)
    @DisplayName("✅ Modifier un utilisateur valide")
    void testModifier_Success() throws SQLException {
        System.out.println("Test 14: Modifier un utilisateur...");

        User user = userService.getById(savedUserId);
        user.setName("Ahmed Modified");
        user.setStatus("ACTIVE");
        user.setTrustPoints(100);

        userService.modifier(user);

        User updated = userService.getById(savedUserId);
        assertEquals("Ahmed Modified", updated.getName());
        assertEquals("ACTIVE", updated.getStatus());
        assertEquals(100, updated.getTrustPoints());
        System.out.println("✅ User modifié avec succès!");
    }

    @Test
    @Order(12)
    @DisplayName("❌ Modifier avec email invalide")
    void testModifier_InvalidEmail() throws SQLException {
        System.out.println("Test 15: Modifier avec email invalide...");

        User user = userService.getById(savedUserId);
        user.setEmail("email-invalide");

        SQLException exception = assertThrows(
                SQLException.class,
                () -> userService.modifier(user)
        );

        assertTrue(exception.getMessage().contains("Email invalide"));
        System.out.println("✅ Validation email lors de modification fonctionne!");
    }

    @Test
    @Order(13)
    @DisplayName("❌ Modifier avec téléphone invalide")
    void testModifier_InvalidPhone() throws SQLException {
        System.out.println("Test 16: Modifier avec téléphone invalide...");

        User user = userService.getById(savedUserId);
        user.setPhone("INVALID");

        SQLException exception = assertThrows(
                SQLException.class,
                () -> userService.modifier(user)
        );

        assertTrue(exception.getMessage().contains("téléphone invalide"));
        System.out.println("✅ Validation téléphone lors de modification fonctionne!");
    }





    // ==================== TESTS SUPPRIMER ====================

    @Test
    @Order(14)
    @DisplayName("❌ Supprimer avec ID invalide")
    void testSupprimer_InvalidId() {
        System.out.println("Test 19: Supprimer avec ID invalide...");

        User user = new User();
        user.setId(-1);

        SQLException exception = assertThrows(
                SQLException.class,
                () -> userService.supprimer(user)
        );

        assertTrue(exception.getMessage().contains("ID invalide"));
        System.out.println("✅ Validation ID lors de suppression fonctionne!");
    }

    @Test
    @Order(15)
    @DisplayName("✅ Supprimer un utilisateur existant")
    void testSupprimer_Success() throws SQLException {
        System.out.println("Test 20: Supprimer un utilisateur...");

        User user = userService.getById(savedUserId);
        assertNotNull(user, "L'utilisateur devrait exister avant suppression");

        userService.supprimer(user);

        User deleted = userService.getById(savedUserId);
        assertNull(deleted, "L'utilisateur devrait être supprimé");
        System.out.println("✅ User supprimé avec succès!");
    }

    @AfterAll
    static void tearDown() {
        System.out.println("\n🎉 === TOUS LES TESTS TERMINÉS ===");
    }
}