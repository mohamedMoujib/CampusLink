package org.example.campusLink.services;

import org.example.campusLink.Services.TrustPointsService;
import org.example.campusLink.units.MyDatabase;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

class TrustPointsServiceTest {

    TrustPointsService trustService;

    int testUserId = 3; // ⚠️ mets un prestataire existant

    @BeforeEach
    void setup() {

        trustService = new TrustPointsService();

        try (Connection conn = MyDatabase.getInstance().getConnection()) {

            // Reset trust_points
            try (PreparedStatement ps =
                         conn.prepareStatement(
                                 "UPDATE users SET trust_points = 0 WHERE id = ?")) {

                ps.setInt(1, testUserId);
                ps.executeUpdate();
            }

            // Nettoyer historique
            try (PreparedStatement ps =
                         conn.prepareStatement(
                                 "DELETE FROM trust_point_history WHERE prestataire_id = ?")) {

                ps.setInt(1, testUserId);
                ps.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\n=== Nouveau test TrustPoints ===");
    }

    @Test
    void testApplyPoints() {

        System.out.println(">>> TEST APPLY POINTS");

        trustService.applyPoints(testUserId, 2);

        try (Connection conn = MyDatabase.getInstance().getConnection()) {

            // Vérifier trust_points
            try (PreparedStatement ps =
                         conn.prepareStatement(
                                 "SELECT trust_points FROM users WHERE id = ?")) {

                ps.setInt(1, testUserId);
                ResultSet rs = ps.executeQuery();

                assertTrue(rs.next());
                int points = rs.getInt("trust_points");

                assertEquals(2, points);
            }

            // Vérifier historique
            try (PreparedStatement ps =
                         conn.prepareStatement(
                                 "SELECT COUNT(*) FROM trust_point_history WHERE prestataire_id = ?")) {

                ps.setInt(1, testUserId);
                ResultSet rs = ps.executeQuery();

                assertTrue(rs.next());
                int count = rs.getInt(1);

                assertEquals(1, count);
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail("Erreur base de données");
        }

        System.out.println("✅ Points appliqués correctement");
    }
}
