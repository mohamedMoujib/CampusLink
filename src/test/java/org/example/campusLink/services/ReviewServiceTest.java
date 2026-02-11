package org.example.campusLink.services;

import org.example.campusLink.Services.ReviewsService;
import org.example.campusLink.entities.Reviews;
import org.example.campusLink.units.MyDatabase;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.*;

class ReviewServiceTest {

    ReviewsService reviewsService;

    int studentId = 1;
    int prestataireId = 1;

    int reservationId1 = 10;
    int reservationId2 = 11;
    int reservationId3 = 12;

    @BeforeEach
    void setup() {

        // 🔥 Nettoyer la table reviews avant chaque test
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM reviews")) {

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }

        reviewsService = new ReviewsService();
        System.out.println("\n=== Nouveau test ===");
    }

    // ==================== AJOUT ====================

    @Test
    void testAddReview() {

        System.out.println(">>> TEST ADD START");

        Reviews review = new Reviews(
                studentId,
                prestataireId,
                reservationId1,
                4,
                "Très bon service"
        );

        reviewsService.addReview(review);

        assertTrue(review.getId() > 0);

        Reviews saved = reviewsService.getReview(review.getId());

        assertNotNull(saved);
        assertEquals(4, saved.getRating());

        System.out.println("✅ Review ajoutée ID = " + review.getId());
    }

    // ==================== UPDATE ====================

    @Test
    void testUpdateReview() {

        System.out.println(">>> TEST UPDATE START");

        Reviews review = new Reviews(
                studentId,
                prestataireId,
                reservationId2,
                3,
                "Initial"
        );

        reviewsService.addReview(review);
        int id = review.getId();

        reviewsService.updateReview(id, 5, "Updated");

        Reviews updated = reviewsService.getReview(id);

        assertNotNull(updated);
        assertEquals(5, updated.getRating());
        assertEquals("Updated", updated.getComment());

        System.out.println("✅ Review modifiée");
    }

    // ==================== DELETE ====================

    @Test
    void testDeleteReview() {

        System.out.println(">>> TEST DELETE START");

        Reviews review = new Reviews(
                studentId,
                prestataireId,
                reservationId3,
                3,
                "A supprimer"
        );

        reviewsService.addReview(review);
        int id = review.getId();

        reviewsService.deleteReview(id);

        Reviews deleted = reviewsService.getReview(id);

        assertNull(deleted);

        System.out.println("✅ Review supprimée");
    }
}
