package org.example;


import org.example.campusLink.entities.Reviews;
import org.example.campusLink.Services.ReviewsService;


public class Main {

    public static void main(String[] args) {

        ReviewsService reviewsService = new ReviewsService();

        // 1️⃣ AJOUT AVIS
        Reviews review = new Reviews(
                1, // studentId
                2, // prestataireId
                7, // reservationId
                1, // rating (+3)
                "Médicore service"
        );

        reviewsService.addReview(review);
        System.out.println("Avis ajouté");

        // 2️⃣ MODIFICATION AVIS
        reviewsService.updateReview(
                34, // reviewId
                1,
                "Excellent service"
        );
        System.out.println("Avis modifié");

        // 3️⃣ AFFICHER AVIS
        Reviews r = reviewsService.getReview(34);
        System.out.println(
                "Note: " + r.getRating() + " | " + r.getComment()
        );

        // 4️⃣ SUPPRESSION AVIS (-5)
        reviewsService.deleteReview(33);
        System.out.println("Avis supprimé");
    }
}
