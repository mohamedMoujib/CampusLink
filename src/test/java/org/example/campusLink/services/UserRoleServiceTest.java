package org.example.campusLink.services;

import org.example.campusLink.entities.Role;
import org.example.campusLink.entities.User;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserRoleServiceTest {

    static UserService userService;
    static RoleService roleService;
    static UserRoleService userRoleService;

    static User testUser;
    static int testUserId;
    static int etudiantRoleId;
    static int prestataireRoleId;
    static int adminRoleId;

    @BeforeAll
    static void setup() throws SQLException {
        userService = new UserService();
        roleService = new RoleService();
        userRoleService = new UserRoleService();

        System.out.println(" DÉBUT DES TESTS USERROLESERVICE ===\n");

        // Créer un utilisateur de test
        testUser = new User();
        testUser.setName("Test UserRole");
        testUser.setEmail("userrole.test2@example.com");
        testUser.setPassword("Password123");
        testUser.setPhone("+21699112258");
        testUser.setDateNaissance(Timestamp.valueOf("2000-01-01 00:00:00"));
        testUser.setGender("Male");
        testUser.setUniversite("ESPRIT");
        testUser.setFiliere("INFO");
        testUser.setSpecialization("GL");
        testUser.setAddress("Tunis");
        testUser.setProfilePicture("test.png");

        userService.ajouter(testUser);
        testUserId = testUser.getId();
        System.out.println("✅ Utilisateur de test créé avec ID: " + testUserId);

        // Récupérer les IDs des rôles
        Role etudiant = roleService.getRoleByName("ETUDIANT");
        Role prestataire = roleService.getRoleByName("PRESTATAIRE");
        Role admin = roleService.getRoleByName("ADMIN");

        etudiantRoleId = etudiant.getId();
        prestataireRoleId = prestataire.getId();
        adminRoleId = admin.getId();

        System.out.println("✅ Rôles récupérés:");
        System.out.println("   - ETUDIANT (ID: " + etudiantRoleId + ")");
        System.out.println("   - PRESTATAIRE (ID: " + prestataireRoleId + ")");
        System.out.println("   - ADMIN (ID: " + adminRoleId + ")\n");
    }

    // ==================== TESTS AJOUTER ====================

    @Test
    @Order(1)
    @DisplayName("✅ Ajouter un rôle ETUDIANT à un utilisateur")
    void testAjouter_RoleEtudiant() throws SQLException {
        System.out.println("Test 1: Ajouter rôle ETUDIANT...");

        userRoleService.ajouter(testUserId, etudiantRoleId);

        assertTrue(userRoleService.userHasRole(testUserId, etudiantRoleId));
        System.out.println("✅ Rôle ETUDIANT ajouté avec succès!");
    }



    @Test
    @Order(2)
    @DisplayName("✅ Ajouter un troisième rôle PRESTATAIRE")
    void testAjouter_RolePrestataire() throws SQLException {
        System.out.println("Test 3: Ajouter rôle PRESTATAIRE...");

        userRoleService.ajouter(testUserId, prestataireRoleId);

        assertTrue(userRoleService.userHasRole(testUserId, prestataireRoleId));
        System.out.println("✅ Rôle PRESTATAIRE ajouté avec succès!");
    }

    @Test
    @Order(3)
    @DisplayName("✅ Ajouter un rôle déjà existant (pas d'erreur)")
    void testAjouter_DuplicateRole() throws SQLException {
        System.out.println("Test 4: Ajouter rôle déjà existant...");

        // L'utilisateur a déjà le rôle ETUDIANT
        userRoleService.ajouter(testUserId, etudiantRoleId);

        // Ne devrait pas lancer d'exception
        assertTrue(userRoleService.userHasRole(testUserId, etudiantRoleId));
        System.out.println("✅ Ajout de rôle en double géré correctement!");
    }


    // ==================== TESTS RECUPERER ====================

    @Test
    @Order(4)
    @DisplayName("✅ Récupérer tous les rôles d'un utilisateur")
    void testRecuperer_AllRoles() throws SQLException {
        System.out.println("Test 4: Récupérer tous les rôles...");

        List<Role> roles = userRoleService.recuperer(testUserId);

        assertNotNull(roles);
        assertEquals(2, roles.size(), "L'utilisateur devrait avoir 2 rôles");

        // Vérifier que tous les rôles sont présents
        List<String> roleNames = roles.stream()
                .map(Role::getName)
                .toList();

        assertTrue(roleNames.contains("ETUDIANT"));
        assertTrue(roleNames.contains("PRESTATAIRE"));

        System.out.println("✅ " + roles.size() + " rôles récupérés:");
        for (Role role : roles) {
            System.out.println("   - " + role.getName());
        }
    }



    // ==================== TESTS SUPPRIMER ====================

    @Test
    @Order(5)
    @DisplayName("✅ Supprimer un rôle d'un utilisateur")
    void testSupprimer_ExistingRole() throws SQLException {
        System.out.println("Test 5: Supprimer un rôle...");

        // Vérifier que le rôle existe
        assertTrue(userRoleService.userHasRole(testUserId, etudiantRoleId));

        // Supprimer le rôle
        userRoleService.supprimer(testUserId, etudiantRoleId);

        // Vérifier que le rôle a été supprimé
        assertFalse(userRoleService.userHasRole(testUserId, etudiantRoleId));

        System.out.println("✅ Rôle ETUDIANT supprimé avec succès!");
    }


    @AfterAll
    static void tearDown() throws SQLException {
        // Nettoyer l'utilisateur de test
        if (testUser != null && testUser.getId() > 0) {
            userService.supprimer(testUser);
            System.out.println("\n🧹 Utilisateur de test supprimé");
        }

        System.out.println("\n🎉 === TOUS LES TESTS TERMINÉS ===");
    }


}
