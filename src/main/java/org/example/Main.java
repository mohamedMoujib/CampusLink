package org.example;


import org.example.campusLink.entities.Reviews;
import org.example.campusLink.Services.ReviewsService;


public class Main {

    public static void main(String[] args) {

        ReviewsService reviewsService = new ReviewsService();

        // 1️⃣ AJOUT AVIS
        Reviews review = new Reviews(
                2, // studentId
                1, // prestataireId
                2, // reservationId
                3, // rating (+3)
                "très Bon service"
        );

        reviewsService.addReview(review);
        System.out.println("Avis ajouté");

        // 2️⃣ MODIFICATION AVIS
        reviewsService.updateReview(
                6, // reviewId
                -2,
                "Excellent service"
        );
        System.out.println("Avis modifié");

        // 3️⃣ AFFICHER AVIS
        Reviews r = reviewsService.getReview(4);
        System.out.println(
                "Note: " + r.getRating() + " | " + r.getComment()
        );

        // 4️⃣ SUPPRESSION AVIS (-5)
        reviewsService.deleteReview(1);
        System.out.println("Avis supprimé");
    }
}
