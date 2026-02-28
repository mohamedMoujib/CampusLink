package org.example.campusLink.services;

import org.example.campusLink.entities.Payments;
import org.example.campusLink.enumeration.Method;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServicesPaymentsTest {

    private ServicesPayments paymentService;
    private Payments testPayment;
    private final int validReservationId = 1;

    @BeforeEach
    void setUp() throws SQLException {
        paymentService = new ServicesPayments();
        testPayment = new Payments();
        testPayment.setReservationId(validReservationId);
        testPayment.setAmount(150.0f);
        testPayment.setMethod(Method.PHYSICAL);
        testPayment.setMeetingLat(48.8566);
        testPayment.setMeetingLng(2.3522);
        testPayment.setMeetingAddress("Paris, France");

        paymentService.ajouter(testPayment);
        testPayment.setId(paymentService.getLastInsertedPaymentId());
        System.out.println("Setup: Payment added with ID " + testPayment.getId());
    }

    @Test
    void testAjouter() throws SQLException {
        List<Payments> payments = paymentService.recuperer();

        boolean found = payments.stream().anyMatch(p ->
                p.getId() == testPayment.getId() &&
                        p.getReservationId() == testPayment.getReservationId() &&
                        p.getAmount() == testPayment.getAmount() &&
                        p.getMethod() == testPayment.getMethod() &&
                        (p.getMeetingLat() != null && p.getMeetingLat().equals(testPayment.getMeetingLat())) &&
                        (p.getMeetingLng() != null && p.getMeetingLng().equals(testPayment.getMeetingLng())) &&
                        (p.getMeetingAddress() != null && p.getMeetingAddress().equals(testPayment.getMeetingAddress()))
        );
        assertTrue(found, "Payment should be inserted in the database");
        System.out.println("Test Ajouter passed for payment ID " + testPayment.getId());
    }

    @Test
    void testRecuperer() throws SQLException {
        List<Payments> payments = paymentService.recuperer();

        assertNotNull(payments);
        assertFalse(payments.isEmpty(), "Payments list should not be empty");
        System.out.println("Test Recuperer passed: " + payments.size() + " payments found");
    }

    @Test
    void testSupprimer() throws SQLException {
        paymentService.supprimer(testPayment);

        List<Payments> payments = paymentService.recuperer();
        boolean found = payments.stream().anyMatch(p -> p.getId() == testPayment.getId());

        assertFalse(found, "Payment should be deleted");
        System.out.println("Test Supprimer passed for payment ID " + testPayment.getId());
    }

    @Test
    void testModifier() throws SQLException {
        testPayment.setAmount(200.0f);
        testPayment.setMeetingAddress("Lyon, France");

        paymentService.modifier(testPayment);

        List<Payments> payments = paymentService.recuperer();
        boolean updated = payments.stream().anyMatch(p ->
                p.getId() == testPayment.getId() &&
                        p.getAmount() == 200.0f &&
                        p.getMeetingAddress().equals("Lyon, France")
        );

        assertTrue(updated, "Payment should be updated");
        System.out.println("Test Modifier passed for payment ID " + testPayment.getId());
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (testPayment != null) {
            paymentService.supprimer(testPayment);
            System.out.println("CleanUp: Payment deleted for ID " + testPayment.getId());
        }
    }
}