package org.example.campusLink.controllers.reservations;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.campusLink.entities.Reservation;
import org.example.campusLink.entities.ReservationStatus;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class ReservationCardCell extends ListCell<Reservation> {

    private final Consumer<Reservation> onEdit;
    private final Consumer<Reservation> onCancel;
    private final Consumer<Reservation> onContact;

    private final DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy · HH:mm");

    public ReservationCardCell(
            Consumer<Reservation> onEdit,
            Consumer<Reservation> onCancel,
            Consumer<Reservation> onContact) {
        this.onEdit    = onEdit;
        this.onCancel  = onCancel;
        this.onContact = onContact;
    }

    @Override
    protected void updateItem(Reservation r, boolean empty) {
        super.updateItem(r, empty);

        if (empty || r == null) { setText(null); setGraphic(null); return; }

        ReservationStatus status = r.getStatus() != null
                ? r.getStatus() : ReservationStatus.PENDING;

        // ── TOP ROW ───────────────────────────────────────────────────────────
        Label title = new Label(r.getServiceTitle() != null
                ? r.getServiceTitle() : "Service #" + r.getServiceId());
        title.setStyle("-fx-font-size:16px; -fx-font-weight:800; -fx-text-fill:#111827;");

        String providerText = r.getProviderName() != null
                ? "avec " + r.getProviderName() : "";
        Label provider = new Label(providerText);
        provider.setStyle("-fx-text-fill:#6b7280; -fx-font-size:12px;");

        VBox leftTop = new VBox(2, title, provider);

        Label badge = new Label(statusLabel(status));
        badge.setStyle(statusStyle(status));

        Region spacerTop = new Region();
        HBox.setHgrow(spacerTop, Priority.ALWAYS);
        HBox topRow = new HBox(leftTop, spacerTop, new HBox(badge));
        topRow.setAlignment(Pos.CENTER_LEFT);

        // ── MIDDLE ROW ────────────────────────────────────────────────────────
        Label date = new Label("📅 " + (r.getDate() != null
                ? r.getDate().format(fmt) : "—"));
        date.setStyle("-fx-text-fill:#6b7280; -fx-font-size:12px;");

        String locText = (r.getLocalisation() != null && !r.getLocalisation().isBlank())
                ? "📍 " + r.getLocalisation() : "";
        Label location = new Label(locText);
        location.setStyle("-fx-text-fill:#6b7280; -fx-font-size:12px;");

        HBox midRow = new HBox(18, date, location);
        midRow.setAlignment(Pos.CENTER_LEFT);

        // ── BOTTOM ROW ────────────────────────────────────────────────────────
        String priceText = r.getPrice() != null
                ? String.format("%.2f DT", r.getPrice()) : "—";
        Label price = new Label(priceText);
        price.setStyle("-fx-font-weight:bold; -fx-font-size:15px; -fx-text-fill:#4f46e5;");

        Region spacerBottom = new Region();
        HBox.setHgrow(spacerBottom, Priority.ALWAYS);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        switch (status) {
            case PENDING -> {
                Button btnEdit = new Button("✎ Modifier");
                btnEdit.setStyle(
                        "-fx-background-color:#f3f4f6; -fx-text-fill:#374151;" +
                                "-fx-background-radius:8; -fx-padding:6 14; -fx-cursor:hand;");
                btnEdit.setOnAction(e -> onEdit.accept(r));

                Button btnCancel = new Button("✕ Annuler");
                btnCancel.setStyle(
                        "-fx-background-color:#fee2e2; -fx-text-fill:#dc2626;" +
                                "-fx-background-radius:8; -fx-padding:6 14; -fx-cursor:hand;");
                btnCancel.setOnAction(e -> onCancel.accept(r));

                actions.getChildren().addAll(btnEdit, btnCancel);
            }
            case CONFIRMED -> {
                Button btnContact = new Button("💬 Contacter");
                btnContact.setStyle(
                        "-fx-background-color:#6366f1; -fx-text-fill:white;" +
                                "-fx-background-radius:8; -fx-padding:6 14; -fx-cursor:hand;");
                btnContact.setOnAction(e -> onContact.accept(r));

                Button btnCancel = new Button("✕ Annuler");
                btnCancel.setStyle(
                        "-fx-background-color:#fee2e2; -fx-text-fill:#dc2626;" +
                                "-fx-background-radius:8; -fx-padding:6 14; -fx-cursor:hand;");
                btnCancel.setOnAction(e -> onCancel.accept(r));

                actions.getChildren().addAll(btnContact, btnCancel);
            }
            default -> {
                Label done = new Label(status == ReservationStatus.CANCELLED
                        ? "Réservation annulée" : "Terminée");
                done.setStyle("-fx-text-fill:#9ca3af; -fx-font-size:12px;");
                actions.getChildren().add(done);
            }
        }

        HBox bottomRow = new HBox(price, spacerBottom, actions);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, topRow, midRow, new Separator(), bottomRow);
        card.setStyle("-fx-background-color:white; -fx-background-radius:12;" +
                "-fx-border-color:#e5e7eb; -fx-border-radius:12; -fx-border-width:1;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.04),6,0,0,2);");
        card.setPadding(new Insets(16));

        setText(null);
        setGraphic(card);
        setStyle("-fx-background-color:transparent; -fx-padding:4 0;");
    }

    private String statusLabel(ReservationStatus s) {
        return switch (s) {
            case PENDING   -> "⏳ En attente";
            case CONFIRMED -> "✅ Confirmée";
            case CANCELLED -> "❌ Annulée";
        };
    }

    private String statusStyle(ReservationStatus s) {
        String base = "-fx-background-radius:20; -fx-padding:4 12;" +
                "-fx-font-size:11px; -fx-font-weight:bold;";
        return base + switch (s) {
            case PENDING   -> "-fx-background-color:#fef9c3; -fx-text-fill:#b45309;";
            case CONFIRMED -> "-fx-background-color:#dcfce7; -fx-text-fill:#16a34a;";
            case CANCELLED -> "-fx-background-color:#fee2e2; -fx-text-fill:#dc2626;";
        };
    }
}