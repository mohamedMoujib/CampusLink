package org.example.campusLink.Services;

import org.example.campusLink.entities.*;
import org.example.campusLink.utils.MyDatabase;
import org.junit.jupiter.api.*;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReviewServiceTest {

    static Gestion_Categorie gc;
    static Gestion_Service gs;
    static Gestion_Demande gd;

    static int categoryId;
    static int serviceId;
    static int studentId;
    static int prestataireId;

    @BeforeAll
    static void setup() throws Exception {

        gc = new Gestion_Categorie();
        gs = new Gestion_Service();
        gd = new Gestion_Demande();

        Connection conn = MyDatabase.getInstance().getConnection();

        // Prestataire
        PreparedStatement ps1 = conn.prepareStatement(
                "INSERT INTO users(name,email,password,status) VALUES(?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS
        );
        ps1.setString(1, "Prestataire Test");
        ps1.setString(2, "prestataire_" + System.nanoTime() + "@mail.com");
        ps1.setString(3, "123");
        ps1.setString(4, "ACTIVE");
        ps1.executeUpdate();

        ResultSet rs1 = ps1.getGeneratedKeys();
        rs1.next();
        prestataireId = rs1.getInt(1);

        // Student
        PreparedStatement ps2 = conn.prepareStatement(
                "INSERT INTO users(name,email,password,status) VALUES(?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS
        );
        ps2.setString(1, "Student Test");
        ps2.setString(2, "student_" + System.nanoTime() + "@mail.com");
        ps2.setString(3, "123");
        ps2.setString(4, "ACTIVE");
        ps2.executeUpdate();

        ResultSet rs2 = ps2.getGeneratedKeys();
        rs2.next();
        studentId = rs2.getInt(1);

        // Catégorie
        Categorie c = new Categorie();
        c.setName("CatTest_" + System.nanoTime());
        c.setDescription("Test");
        gc.ajouterCategorie(c);

        categoryId = gc.afficherCategories()
                .get(gc.afficherCategories().size() - 1)
                .getId();

        // Service
        Services s = new Services();
        s.setTitle("ServiceTest_" + System.nanoTime());
        s.setDescription("Desc");
        s.setPrice(100);
        s.setPrestataireId(prestataireId);
        s.setCategoryId(categoryId);

        gs.ajouterService(s);

        serviceId = gs.afficherServices()
                .get(gs.afficherServices().size() - 1)
                .getId();
    }

    /* ================= TEST 1 ================= */

    @Test
    @Order(1)
    void testPrixNegatif() {

        Services s = new Services();
        s.setTitle("Invalid");
        s.setPrice(-10);
        s.setPrestataireId(prestataireId);
        s.setCategoryId(categoryId);

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class,
                        () -> gs.ajouterService(s));

        assertEquals("Le prix doit être positif", ex.getMessage());

        System.out.println("✔ Exception prix négatif validée");
    }

    /* ================= TEST 2 ================= */

    @Test
    @Order(2)
    void testDemandeEnDouble() throws Exception {

        Demandes d = new Demandes();
        d.setStudentId(studentId);
        d.setPrestataireId(prestataireId);
        d.setServiceId(serviceId);

        gd.ajouterDemande(d);

        IllegalStateException ex =
                assertThrows(IllegalStateException.class,
                        () -> gd.ajouterDemande(d));

        assertEquals("Vous avez déjà réservé ce service", ex.getMessage());

        System.out.println("✔ Exception doublon validée");
    }

    @Test
    @Order(3)
    void testSuppressionServiceReserve() {

        IllegalStateException ex =
                assertThrows(IllegalStateException.class,
                        () -> gs.supprimerService(serviceId));

        assertEquals(
                "Impossible de supprimer un service avec des réservations actives",
                ex.getMessage()
        );

        System.out.println("✔ Exception suppression validée");
    }


    @Test
    @Order(4)
    void testStatutInvalide() {

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class,
                        () -> gd.modifierStatut(9999, "INVALID"));

        assertEquals("Statut invalide", ex.getMessage());

        System.out.println(" Exception statut invalide validée");
    }
}
