package org.example.campusLink.services;

import org.example.campusLink.entities.Reviews;

import java.util.List;

public class ReviewsService {

    private ReviewsDAO reviewsDAO = new ReviewsDAO();
    private TrustPointsService trustService = new TrustPointsService();

    // ===================== CREATE =====================

    public void addReview(Reviews r) {

        if (reviewsDAO.existsByStudentAndService(
                r.getStudentId(), r.getReservationId())) {
            throw new IllegalStateException("Avis déjà existant");
        }

        reviewsDAO.save(r);

        try {
            trustService.applyPoints(
                    r.getPrestataireId(),
                    r.getRating()
            );
        } catch (Exception e) {
            System.out.println("⚠️ Erreur TrustPoints ignorée: " + e.getMessage());
        }
    }

    // ===================== READ =====================

    public Reviews getReview(int id) {
        return reviewsDAO.findById(id);
    }

    public List<Reviews> getReviewsByStudentWithDetails(int studentId) {
        return reviewsDAO.findByStudentWithDetails(studentId);
    }

    public List<Reviews> getReviewsByTutor(int tutorId) {
        return reviewsDAO.findByTutorWithDetails(tutorId);
    }

    public List<Reviews> getAllReviewsWithDetails() {
        return reviewsDAO.findAllWithDetails();
    }

    // ===================== 🔥 REPORT MANAGEMENT =====================

    public void reportReview(int reviewId, String reason) {
        reviewsDAO.reportReview(reviewId, reason);
    }

    public void unreportReview(int reviewId) {
        reviewsDAO.unreportReview(reviewId);
    }

    public int getReportedReviewsCount() {
        return reviewsDAO.getReportedReviewsCount();
    }

    // ===================== UPDATE =====================

    public void updateReview(int id, int newRating, String newComment) {

        Reviews old = reviewsDAO.findById(id);
        if (old == null) {
            throw new IllegalStateException("Avis introuvable");
        }

        int diff = newRating - old.getRating();

        old.setRating(newRating);
        old.setComment(newComment);
        reviewsDAO.update(old);

        try {
            trustService.applyPoints(
                    old.getPrestataireId(),
                    diff
            );
        } catch (Exception e) {
            System.out.println("⚠️ Erreur TrustPoints ignorée: " + e.getMessage());
        }
    }

    // ===================== DELETE =====================

    public void deleteReview(int id) {

        Reviews r = reviewsDAO.findById(id);
        if (r == null) {
            throw new IllegalStateException("Avis introuvable");
        }

        reviewsDAO.delete(id);

        try {
            trustService.applyPoints(
                    r.getPrestataireId(),
                    -r.getRating()
            );
        } catch (Exception e) {
            System.out.println("⚠️ Erreur TrustPoints ignorée: " + e.getMessage());
        }
    }

    // ===================== GET TRUST POINTS =====================

    public int getTrustPoints(int userId) {
        return reviewsDAO.getTrustPointsByUserId(userId);
    }
}