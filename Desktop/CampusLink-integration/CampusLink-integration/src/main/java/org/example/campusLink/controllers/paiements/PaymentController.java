package org.example.campusLink.controllers.paiements;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.example.campusLink.entities.Payments;
import org.example.campusLink.entities.Method;
import org.example.campusLink.services.paiements.ServicesPayments;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.example.campusLink.entities.Invoices;
import org.example.campusLink.services.paiements.ServiceInvoices;

public class PaymentController {

    private static final String OPENCAGE_API_KEY = "294699815f1b4034839f3e125ca32ca9";
    private static final String OPENCAGE_URL = "https://api.opencagedata.com/geocode/v1/json";

    private static final int DUMMY_RESERVATION_ID = 1;
    private static final float DUMMY_AMOUNT = 99.99f;

    private static final String DUMMY_USER1 = "Ahmed Ben Ali";
    private static final String DUMMY_USER2 = "Marie Dubois";
    private static final String DUMMY_SERVICE = "Cours de mathématiques";
    private static final String DUMMY_RESERVATION_DATE = "28 février 2026 à 14h00";

    private ServicesPayments paymentService;
    private ServiceInvoices invoiceService;
    private WebEngine engine;
    private Double meetingLat;
    private Double meetingLng;
    private String meetingAddress = "";


    @FXML private TextField providerNameField;
    @FXML private TextField amountField;
    @FXML private RadioButton cashRadio;
    @FXML private RadioButton d17Radio;
    private ToggleGroup methodGroup;
    @FXML private StackPane mapContainer;
    @FXML private Button submitButton;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        paymentService = new ServicesPayments();
        invoiceService = new ServiceInvoices();
        methodGroup = new ToggleGroup();
        cashRadio.setToggleGroup(methodGroup);
        d17Radio.setToggleGroup(methodGroup);
        cashRadio.setSelected(true);
        providerNameField.setPromptText("Nom du prestataire");
        amountField.setPromptText("Montant (ex: 99.99)");
        amountField.setText(String.valueOf(DUMMY_AMOUNT));

        initMap();
        submitButton.setOnAction(e -> handleSubmit());
    }

    private void initMap() {
        WebView webView = new WebView();
        engine = webView.getEngine();
        webView.setContextMenuEnabled(false);
        webView.setMinSize(0, 0);
        webView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        webView.prefWidthProperty().bind(mapContainer.widthProperty());
        webView.prefHeightProperty().bind(mapContainer.heightProperty());
        mapContainer.getChildren().add(webView);

        engine.getLoadWorker().stateProperty().addListener((obs, o, n) -> {
            if (n == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", this);
            }
        });

        engine.load(getClass().getResource("/map/map.html").toExternalForm());
    }

    public void saveLocation(double lat, double lng) {
        meetingLat = lat;
        meetingLng = lng;
        Platform.runLater(() -> showMessage("Recherche du lieu...", "#3b82f6"));

        Thread thread = new Thread(() -> {
            meetingAddress = getPlaceName(lat, lng);
            Platform.runLater(() -> {
                showMessage("📍 " + meetingAddress, "#3b82f6");
                String safe = meetingAddress.replace("\\", "\\\\").replace("'", "\\'");
                engine.executeScript("updateInfoBox('" + safe + "', " + lat + ", " + lng + ");");
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    private String getPlaceName(double lat, double lng) {
        try {
            String urlStr = OPENCAGE_URL
                    + "?q=" + lat + "%2C" + lng
                    + "&key=" + OPENCAGE_API_KEY
                    + "&language=fr"
                    + "&limit=1"
                    + "&no_annotations=1"
                    + "&pretty=0";

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "CampusLink/1.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            if (conn.getResponseCode() != 200) return String.format("%.5f, %.5f", lat, lng);

            String json = readResponse(conn);
            String key = "\"formatted\":\"";
            int start = json.indexOf(key);
            if (start != -1) {
                start += key.length();
                int end = json.indexOf("\"", start);
                if (end != -1) {
                    String value = decodeUnicode(json.substring(start, end));
                    if (!value.isEmpty()) return value;
                }
            }
        } catch (Exception e) {
            System.err.println("OpenCage error: " + e.getMessage());
        }
        return String.format("%.5f, %.5f", lat, lng);
    }

    private String decodeUnicode(String input) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            if (i + 5 < input.length() && input.charAt(i) == '\\' && input.charAt(i + 1) == 'u') {
                try {
                    int code = Integer.parseInt(input.substring(i + 2, i + 6), 16);
                    sb.append((char) code);
                    i += 6;
                } catch (NumberFormatException e) {
                    sb.append(input.charAt(i++));
                }
            } else {
                sb.append(input.charAt(i++));
            }
        }
        return sb.toString();
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    private void handleSubmit() {
        try {
            if (meetingLat == null || meetingLng == null) {
                showMessage("Veuillez choisir un lieu de rencontre ❌", "#ef4444");
                return;
            }

            int reservationId = DUMMY_RESERVATION_ID;

            float amount = amountField.getText().trim().isEmpty()
                    ? DUMMY_AMOUNT
                    : Float.parseFloat(amountField.getText().trim());

            Payments payment = new Payments();
            payment.setReservationId(reservationId);
            payment.setAmount(amount);
            String selectedMethod = d17Radio.isSelected() ? "VIRTUAL" : "PHYSICAL";
            payment.setMethod(Method.valueOf(selectedMethod));
            payment.setMeetingLat(meetingLat);
            payment.setMeetingLng(meetingLng);
            payment.setMeetingAddress(meetingAddress);

            paymentService.ajouter(payment);

            int paymentId = paymentService.getLastInsertedPaymentId();

            String details = "L'utilisateur " + DUMMY_USER1
                    + " a sollicité l'utilisateur " + DUMMY_USER2
                    + " pour le service \"" + DUMMY_SERVICE + "\""
                    + " lors de la réservation du " + DUMMY_RESERVATION_DATE
                    + " au lieu suivant : " + meetingAddress + ".";

            Invoices invoice = new Invoices();
            invoice.setPaymentId(paymentId);
            invoice.setInvoiceDate(Timestamp.valueOf(LocalDateTime.now()));
            invoice.setDetails(details);

            invoiceService.ajouter(invoice);

            showMessage("Engagement enregistré ✅ — Facture générée", "#22c55e");

            Parent root = FXMLLoader.load(getClass().getResource("/View/InvoiceView.fxml"));
            Stage stage = (Stage) submitButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (NumberFormatException | SQLException e) {
            showMessage("Erreur lors de l'enregistrement ❌ : " + e.getMessage(), "#ef4444");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void showMessage(String text, String color) {
        messageLabel.setText(text);
        messageLabel.setStyle("-fx-text-fill:" + color + ";");
    }
}