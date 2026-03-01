package org.example.campusLink.Services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour EmailService_publication :
 * - génération du code de vérification
 * - envoi email "publication compatible" (comportement avec/sans mot de passe app)
 */
@DisplayName("EmailService_publication")
class EmailServicePublicationTest {

    private EmailService_publication emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService_publication();
    }

    @Test
    @DisplayName("generateVerificationCode retourne un code à 6 chiffres")
    void generateVerificationCode_format() {
        for (int i = 0; i < 20; i++) {
            String code = emailService.generateVerificationCode();
            assertNotNull(code);
            assertTrue(code.matches("\\d{6}"), "Code doit être 6 chiffres: " + code);
            int value = Integer.parseInt(code);
            assertTrue(value >= 100_000 && value <= 999_999, "Code entre 100000 et 999999: " + code);
        }
    }

    @Test
    @DisplayName("sendCompatiblePublicationEmail ne lève pas d'exception")
    void sendCompatiblePublicationEmail_noException_withValidParams() {
        assertDoesNotThrow(() -> emailService.sendCompatiblePublicationEmail(
                "test@example.com",
                "Tuteur Test",
                "Cours Java",
                "Étudiant Demo",
                "Besoin aide Java",
                "Je cherche du soutien en POO.",
                50.0,
                85.0
        ));
    }

    @Test
    @DisplayName("sendCompatiblePublicationEmail accepte des valeurs null pour affichage")
    void sendCompatiblePublicationEmail_acceptsNulls() {
        // Vérifie que les null sont gérés (pas de NPE)
        boolean sent = emailService.sendCompatiblePublicationEmail(
                "null-test@example.com",
                null,
                null,
                null,
                null,
                null,
                0.0,
                0.0
        );
        assertFalse(sent);
    }
}
