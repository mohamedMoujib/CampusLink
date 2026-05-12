package org.example.campusLink.services.reservations;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;

public class SmsService {

    private static final String ACCOUNT_SID = "AC8dc3c5ad42dff0a88fa0ce8a03d6787f";
    private static final String AUTH_TOKEN = "cd9f4a9229a828ae001eaa46c38b7d06";
    private static final String FROM_PHONE = "+16812928031"; // ton numéro Twilio

    static {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    }

    public static void sendSms(String to, String body) {

        try {

            Message.creator(
                    new com.twilio.type.PhoneNumber(to),
                    new com.twilio.type.PhoneNumber(FROM_PHONE),
                    body
            ).create();

            System.out.println("SMS envoyé réellement à " + to);

        } catch (Exception e) {
            System.out.println("Erreur SMS : " + e.getMessage());
        }
    }
}