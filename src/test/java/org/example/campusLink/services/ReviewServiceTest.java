package org.example.campusLink.Services;

import org.example.campusLink.Services.reviews.ReviewsService;
import org.example.campusLink.entities.Reviews;
import org.example.campusLink.utils.MyDatabase;
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
    int reservationId4 = 13;

    @BeforeEach
    void setup() {
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM reviews")) {
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        reviewsService = new ReviewsService();
        System.out.println("\n=== Nouveau test ===");
    }

    @Test
    void testAddPositiveReview() {
        System.out.println(">>> TEST ADD POSITIVE REVIEW");

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

        System.out.println("✅ Review positive ajoutée ID = " + review.getId());
    }

    @Test
    void testAddNegativeReview() {
        System.out.println(">>> TEST ADD NEGATIVE REVIEW");

        Reviews review = new Reviews(
                studentId,
                prestataireId,
                reservationId4,
                -3,
                "Mauvaise expérience"
        );

        reviewsService.addReview(review);

        assertTrue(review.getId() > 0);

        Reviews saved = reviewsService.getReview(review.getId());

        assertNotNull(saved);
        assertEquals(-3, saved.getRating());

        System.out.println("✅ Review négative ajoutée ID = " + review.getId());
    }

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

        reviewsService.updateReview(id, -2, "Updated to negative");

        Reviews updated = reviewsService.getReview(id);

        assertNotNull(updated);
        assertEquals(-2, updated.getRating());
        assertEquals("Updated to negative", updated.getComment());

        System.out.println("✅ Review modifiée de positive à négative");
    }

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