package org.example.campusLink.services;

import org.example.campusLink.entities.Invoices;
import org.example.campusLink.entities.Payments;
import org.example.campusLink.enumeration.Method;
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

        testPayment = new Payments();
        testPayment.setReservationId(1); // must exist in DB
        testPayment.setAmount(150.0f);
        testPayment.setMethod(Method.PHYSICAL);
        testPayment.setMeetingLat(null);
        testPayment.setMeetingLng(null);
        testPayment.setMeetingAddress(null);

        paymentService.ajouter(testPayment);
        testPayment.setId(paymentService.getLastInsertedPaymentId());

        testInvoice = new Invoices();
        testInvoice.setPaymentId(testPayment.getId());
        testInvoice.setDate(new Timestamp(System.currentTimeMillis()));
        testInvoice.setDetails("JUnit test invoice");
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
