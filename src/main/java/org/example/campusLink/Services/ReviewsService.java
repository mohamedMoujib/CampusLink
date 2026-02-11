package org.example.campusLink.Services;

import org.example.campusLink.entities.Reviews;

public class ReviewsService {

    private ReviewsDAO reviewsDAO = new ReviewsDAO();
    private TrustPointsService trustService = new TrustPointsService();

    // CREATE
    public void addReview(Reviews r) {

        if (reviewsDAO.existsByStudentAndService(
                r.getStudentId(), r.getReservationId())) {
            throw new IllegalStateException("Avis déjà existant");
        }

        reviewsDAO.save(r);

        // + points selon la note
        trustService.applyPoints(
                r.getPrestataireId(),
                r.getRating()
        );
    }

    // READ
    public Reviews getReview(int id) {
        return reviewsDAO.findById(id);
    }

    // UPDATE (SANS TEMPS)
    public void updateReview(int id, int newRating, String newComment) {

        Reviews old = reviewsDAO.findById(id);
        if (old == null) {
            throw new IllegalStateException("Avis introuvable");
        }

        int diff = newRating - old.getRating();

        old.setRating(newRating);
        old.setComment(newComment);
        reviewsDAO.update(old);

        // appliquer la différence
        trustService.applyPoints(
                old.getPrestataireId(),
                diff
        );
    }

    // DELETE (SANS TEMPS)
    public void deleteReview(int id) {

        Reviews r = reviewsDAO.findById(id);
        if (r == null) {
            throw new IllegalStateException("Avis introuvable");
        }

        reviewsDAO.delete(id);

        // retirer les points
        trustService.applyPoints(
                r.getPrestataireId(),
                -r.getRating()
        );
    }
}
