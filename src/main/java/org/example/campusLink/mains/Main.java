package org.example.campusLink.mains;

import org.example.campusLink.entities.Reservation;
import org.example.campusLink.entities.ReservationStatus;
import org.example.campusLink.services.ReservationService;

import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("=== TEST BACKEND ===");

            ReservationService service = new ReservationService();

            Reservation r = new Reservation();
            r.setStudentId(1);
            r.setServiceId(1);
            r.setDate(LocalDateTime.now().plusDays(1));
            r.setStatus(ReservationStatus.PENDING);

            int id = service.add(r);
            System.out.println("Reservation created ID = " + id);

            service.updateStatus(id, ReservationStatus.CONFIRMED);
            System.out.println("Reservation confirmed.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
