package org.example.campusLink.controllers.paiements;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.example.campusLink.entities.Invoices;
import org.example.campusLink.entities.Method;
import org.example.campusLink.entities.Payments;
import org.example.campusLink.entities.Reservation;
import org.example.campusLink.entities.User;
import org.example.campusLink.services.paiements.ServiceInvoices;
import org.example.campusLink.services.paiements.ServicesPayments;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PaymentController {

    private static final String OPENCAGE_API_KEY = "294699815f1b4034839f3e125ca32ca9";
    private static final String OPENCAGE_URL =
            "https://api.opencagedata.com/geocode/v1/json";

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private Label    lblProviderName;
    @FXML private Label    lblServiceName;
    @FXML private Label    lblDate;
    @FXML private Label    lblAmount;
    @FXML private Label    lblServiceBadge;
    @FXML private Label    lblStatusBadge;
    @FXML private RadioButton cashRadio;
    @FXML private RadioButton d17Radio;
    @FXML private StackPane   mapContainer;
    @FXML private Button      submitButton;
    @FXML private Label       messageLabel;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ServicesPayments paymentService = new ServicesPayments();
    private final ServiceInvoices  invoiceService = new ServiceInvoices();

    private WebEngine engine;
    private Double    meetingLat;
    private Double    meetingLng;
    private String    meetingAddress = "";

    private Reservation currentReservation;
    private User        currentUser;

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        ToggleGroup methodGroup = new ToggleGroup();
        cashRadio.setToggleGroup(methodGroup);
        d17Radio.setToggleGroup(methodGroup);
        cashRadio.setSelected(true);
        initMap();
    }

    /**
     * Called by EtudiantReservationsController after FXML load.
     */
    public void initData(Reservation reservation, User user) {
        this.currentReservation = reservation;
        this.currentUser        = user;

        // Fill summary card
        lblProviderName.setText(reservation.getProviderName() != null
                ? reservation.getProviderName() : "—");
        lblServiceName.setText(reservation.getServiceTitle() != null
                ? reservation.getServiceTitle() : "—");
        lblDate.setText(reservation.getDate() != null
                ? reservation.getDate().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "—");
        lblAmount.setText(reservation.getPrice() != null
                ? String.format("%.2f DT", reservation.getPrice()) : "—");

        // Header badges
        lblServiceBadge.setText(reservation.getServiceTitle() != null
                ? reservation.getServiceTitle() : "");
    }

    // ── MAP ───────────────────────────────────────────────────────────────────

    private void initMap() {
        WebView webView = new WebView();
        engine = webView.getEngine();
        webView.setContextMenuEnabled(false);
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

        Thread t = new Thread(() -> {
            meetingAddress = getPlaceName(lat, lng);
            Platform.runLater(() -> {
                showMessage("📍 " + meetingAddress, "#3b82f6");
                String safe = meetingAddress
                        .replace("\\", "\\\\").replace("'", "\\'");
                engine.executeScript(
                        "updateInfoBox('" + safe + "'," + lat + "," + lng + ");");
            });
        });
        t.setDaemon(true);
        t.start();
    }

    // ── SUBMIT ────────────────────────────────────────────────────────────────
    // Add this field at the top:
    private Runnable onSuccess;

    // Add this setter:
    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    // Replace handleSubmit() success block:
    @FXML
    private void handleSubmit() {
        if (meetingLat == null || meetingLng == null) {
            showMessage("Veuillez choisir un lieu de rencontre ❌", "#ef4444");
            return;
        }

        try {
            int   reservationId = currentReservation != null
                    ? currentReservation.getId() : 1;
            float amount = currentReservation != null
                    && currentReservation.getPrice() != null
                    ? currentReservation.getPrice().floatValue() : 0f;

            Payments payment = new Payments();
            payment.setReservationId(reservationId);
            payment.setAmount(amount);
            payment.setMethod(Method.valueOf(
                    d17Radio.isSelected() ? "VIRTUAL" : "PHYSICAL"));
            payment.setMeetingLat(meetingLat);
            payment.setMeetingLng(meetingLng);
            payment.setMeetingAddress(meetingAddress);
            paymentService.ajouter(payment);

            int paymentId = paymentService.getLastInsertedPaymentId();

            String studentName  = currentUser != null
                    ? currentUser.getName() : "Étudiant";
            String providerName = currentReservation != null
                    && currentReservation.getProviderName() != null
                    ? currentReservation.getProviderName() : "Prestataire";
            String service = currentReservation != null
                    && currentReservation.getServiceTitle() != null
                    ? currentReservation.getServiceTitle() : "Service";
            String dateStr = currentReservation != null
                    && currentReservation.getDate() != null
                    ? currentReservation.getDate().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))
                    : "—";

            String details = "L'utilisateur " + studentName
                    + " a sollicité " + providerName
                    + " pour le service \"" + service + "\""
                    + " lors de la réservation du " + dateStr
                    + " au lieu suivant : " + meetingAddress + ".";

            Invoices invoice = new Invoices();
            invoice.setPaymentId(paymentId);
            invoice.setInvoiceDate(Timestamp.valueOf(LocalDateTime.now()));
            invoice.setDetails(details);
            invoiceService.ajouter(invoice);

            showMessage("Engagement enregistré ✅  —  Facture générée !", "#22c55e");

            // ✅ Close modal then navigate to factures
            javafx.animation.PauseTransition pause =
                    new javafx.animation.PauseTransition(
                            javafx.util.Duration.seconds(1.8));
            pause.setOnFinished(e -> {
                onClose();
                // ✅ Trigger navigation to factures after modal closes
                if (onSuccess != null) onSuccess.run();
            });
            pause.play();

        } catch (SQLException e) {
            showMessage("Erreur lors de l'enregistrement ❌ : " + e.getMessage(),
                    "#ef4444");
            e.printStackTrace();
        }
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) submitButton.getScene().getWindow();
        stage.close();
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private void showMessage(String text, String color) {
        messageLabel.setText(text);
        messageLabel.setStyle("-fx-text-fill:" + color + "; -fx-font-size:13px;" +
                "-fx-font-weight:bold;");
    }

    private String getPlaceName(double lat, double lng) {
        try {
            String urlStr = OPENCAGE_URL
                    + "?q=" + lat + "%2C" + lng
                    + "&key=" + OPENCAGE_API_KEY
                    + "&language=fr&limit=1&no_annotations=1&pretty=0";

            HttpURLConnection conn =
                    (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "CampusLink/1.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            if (conn.getResponseCode() != 200)
                return String.format("%.5f, %.5f", lat, lng);

            String json = readResponse(conn);
            String key  = "\"formatted\":\"";
            int start   = json.indexOf(key);
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

    private String readResponse(HttpURLConnection conn) throws Exception {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    private String decodeUnicode(String input) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            if (i + 5 < input.length()
                    && input.charAt(i) == '\\'
                    && input.charAt(i + 1) == 'u') {
                try {
                    int code = Integer.parseInt(
                            input.substring(i + 2, i + 6), 16);
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
}