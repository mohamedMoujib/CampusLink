package org.example.campusLink.services;

import org.example.campusLink.entities.Invoices;
import org.example.campusLink.entities.Payments;
import org.example.campusLink.enumeration.Method;
import org.example.campusLink.enumeration.Status;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceInvoicesTest {

    private ServiceInvoices invoiceService;
    private ServicesPayments paymentService;
    private Payments testPayment;
    private Invoices testInvoice;

    @BeforeEach
    void setUp() throws SQLException {
        invoiceService = new ServiceInvoices();
        paymentService = new ServicesPayments();

        testPayment = new Payments(
                0,
                1, // reservation_id MUST exist in DB
                150.0f,
                Method.PHYSICAL,
                Status.PAID
        );
        paymentService.ajouter(testPayment);
        testPayment.setId(paymentService.getLastInsertedPaymentId());
        testInvoice = new Invoices(
                0,
                testPayment.getId(), // use the real payment id
                new Timestamp(System.currentTimeMillis()),
                "JUnit test invoice"
        );
    }

    @Test
    void testAjouter() throws SQLException {
        invoiceService.ajouter(testInvoice);

        List<Invoices> invoices = invoiceService.recuperer();
        boolean found = invoices.stream()
                .anyMatch(i -> i.getDetails().equals("JUnit test invoice"));

        assertTrue(found, "Invoice should be inserted in database");
    }

    @Test
    void testRecuperer() throws SQLException {
        invoiceService.ajouter(testInvoice);

        List<Invoices> invoices = invoiceService.recuperer();

        assertNotNull(invoices);
        assertFalse(invoices.isEmpty());
    }

    @Test
    void testSupprimer() throws SQLException {
        invoiceService.ajouter(testInvoice);

        invoiceService.supprimer(testInvoice);

        List<Invoices> invoices = invoiceService.recuperer();
        boolean found = invoices.stream()
                .anyMatch(i -> i.getDetails().equals("JUnit test invoice"));

        assertFalse(found, "Invoice should be deleted");
    }

    @Test
    void testModifier() throws SQLException {
        invoiceService.ajouter(testInvoice);

        testInvoice.setDetails("Updated details");
        invoiceService.modifier(testInvoice);

        List<Invoices> invoices = invoiceService.recuperer();
        boolean updated = invoices.stream()
                .anyMatch(i -> i.getDetails().equals("Updated details"));

        assertTrue(updated);
    }

    @AfterEach
    void cleanUp() throws SQLException {
        if (testInvoice != null) {
            invoiceService.supprimer(testInvoice);
        }
        if (testPayment != null) {
            paymentService.supprimer(testPayment);
        }
    }
}
