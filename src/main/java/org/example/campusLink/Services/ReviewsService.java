package org.example.campusLink.Services;

import org.example.campusLink.entities.Reviews;

public class ReviewsService {

    private ReviewsDAO reviewsDAO = new ReviewsDAO();
    private TrustPointsService trustService = new TrustPointsService();

    // ===================== CREATE =====================

    public void addReview(Reviews r) {

        if (reviewsDAO.existsByStudentAndService(
                r.getStudentId(), r.getReservationId())) {
            throw new IllegalStateException("Avis déjà existant");
        }

        // 1️⃣ Sauvegarde review
        reviewsDAO.save(r);

        // 2️⃣ Appliquer points (sécurisé)
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
}
