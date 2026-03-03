package org.example.campusLink.controllers.reservations;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.example.campusLink.entities.Reservation;
import org.example.campusLink.entities.ReservationStatus;

import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ReservationCardCell extends ListCell<Reservation> {

    public enum Role { ETUDIANT, PRESTATAIRE }

    private final Role role;
    private final Consumer<Reservation> onEdit;
    private final Consumer<Reservation> onDelete;
    private final Consumer<Reservation> onContact;
    private final Consumer<Reservation> onCoordination;  // ✅ étudiant only
    private final BiConsumer<Reservation, ReservationStatus> onUpdateStatus; // prestataire only

    private final DateTimeFormatter fmt =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy · HH:mm");

    // ── PRESTATAIRE constructor (4 params) ────────────────────────────────────
    public ReservationCardCell(
            Consumer<Reservation> onEdit,
            Consumer<Reservation> onDelete,
            Consumer<Reservation> onContact,
            BiConsumer<Reservation, ReservationStatus> onUpdateStatus) {
        this.role           = Role.PRESTATAIRE;
        this.onEdit         = onEdit;
        this.onDelete       = onDelete;
        this.onContact      = onContact;
        this.onCoordination = null;
        this.onUpdateStatus = onUpdateStatus;
    }

    // ── ETUDIANT constructor (4 params: edit, cancel, contact, coordination) ──
    public ReservationCardCell(
            Consumer<Reservation> onEdit,
            Consumer<Reservation> onCancel,
            Consumer<Reservation> onContact,
            Consumer<Reservation> onCoordination) {
        this.role           = Role.ETUDIANT;
        this.onEdit         = onEdit;
        this.onDelete       = onCancel;
        this.onContact      = onContact;
        this.onCoordination = onCoordination;
        this.onUpdateStatus = null;
    }

    @Override
    protected void updateItem(Reservation r, boolean empty) {
        super.updateItem(r, empty);
        if (empty || r == null) { setText(null); setGraphic(null); return; }

        ReservationStatus status = r.getStatus() != null
                ? r.getStatus() : ReservationStatus.PENDING;

        // ── TOP ROW ───────────────────────────────────────────────────────────
        String subtitle = role == Role.PRESTATAIRE
                ? (r.getStudentName()  != null ? "par "  + r.getStudentName()  : "")
                : (r.getProviderName() != null ? "avec " + r.getProviderName() : "");

        Label title = new Label(r.getServiceTitle() != null
                ? r.getServiceTitle() : "Service #" + r.getServiceId());
        title.setStyle("-fx-font-size:16px; -fx-font-weight:800; -fx-text-fill:#111827;");

        Label sub = new Label(subtitle);
        sub.setStyle("-fx-text-fill:#6b7280; -fx-font-size:12px;");

        VBox leftTop = new VBox(2, title, sub);

        Label badge = new Label(statusLabel(status));
        badge.setStyle(statusStyle(status));

        Region spacerTop = new Region();
        HBox.setHgrow(spacerTop, Priority.ALWAYS);
        HBox topRow = new HBox(leftTop, spacerTop, badge);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // ── MIDDLE ROW ────────────────────────────────────────────────────────
        Label date = new Label("📅 " + (r.getDate() != null
                ? r.getDate().format(fmt) : "—"));
        date.setStyle("-fx-text-fill:#6b7280; -fx-font-size:12px;");

        Label location = new Label(
                r.getLocalisation() != null && !r.getLocalisation().isBlank()
                        ? "📍 " + r.getLocalisation() : "");
        location.setStyle("-fx-text-fill:#6b7280; -fx-font-size:12px;");

        HBox midRow = new HBox(18, date, location);
        midRow.setAlignment(Pos.CENTER_LEFT);

        // ── BOTTOM ROW ────────────────────────────────────────────────────────
        Label price = new Label(r.getPrice() != null
                ? String.format("%.2f DT", r.getPrice()) : "—");
        price.setStyle("-fx-font-weight:bold; -fx-font-size:15px; -fx-text-fill:#4f46e5;");

        Region spacerBottom = new Region();
        HBox.setHgrow(spacerBottom, Priority.ALWAYS);

        HBox actions = buildActions(r, status);

        HBox bottomRow = new HBox(price, spacerBottom, actions);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, topRow, midRow, new Separator(), bottomRow);
        card.setStyle(
                "-fx-background-color:white; -fx-background-radius:12;" +
                        "-fx-border-color:#e5e7eb; -fx-border-radius:12; -fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.04),6,0,0,2);");
        card.setPadding(new Insets(16));

        setText(null);
        setGraphic(card);
        setStyle("-fx-background-color:transparent; -fx-padding:4 0;");
    }

    private HBox buildActions(Reservation r, ReservationStatus status) {
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnMsg = new Button("💬 Message");
        btnMsg.setStyle(
                "-fx-background-color:#e0e7ff; -fx-text-fill:#4f46e5;" +
                        "-fx-font-weight:bold; -fx-background-radius:8;" +
                        "-fx-padding:6 14; -fx-cursor:hand;");
        btnMsg.setOnAction(e -> onContact.accept(r));

        if (role == Role.PRESTATAIRE) {
            buildPrestataireActions(r, status, actions, btnMsg);
        } else {
            buildEtudiantActions(r, status, actions, btnMsg);
        }
        return actions;
    }

    private void buildPrestataireActions(Reservation r, ReservationStatus status,
                                         HBox actions, Button btnMsg) {
        Button btnDelete = new Button("🗑");
        btnDelete.setStyle(
                "-fx-background-color:#f3f4f6; -fx-text-fill:#6b7280;" +
                        "-fx-background-radius:8; -fx-padding:6 10; -fx-cursor:hand;");
        btnDelete.setOnAction(e -> onDelete.accept(r));

        switch (status) {
            case PENDING -> {
                Button btnAccept = new Button("✅ Accepter");
                btnAccept.setStyle(
                        "-fx-background-color:#dcfce7; -fx-text-fill:#16a34a;" +
                                "-fx-font-weight:bold; -fx-background-radius:8;" +
                                "-fx-padding:6 14; -fx-cursor:hand;");
                btnAccept.setOnAction(e ->
                        onUpdateStatus.accept(r, ReservationStatus.CONFIRMED));

                Button btnRefuse = new Button("❌ Refuser");
                btnRefuse.setStyle(
                        "-fx-background-color:#fee2e2; -fx-text-fill:#dc2626;" +
                                "-fx-font-weight:bold; -fx-background-radius:8;" +
                                "-fx-padding:6 14; -fx-cursor:hand;");
                btnRefuse.setOnAction(e ->
                        onUpdateStatus.accept(r, ReservationStatus.CANCELLED));

                actions.getChildren().addAll(btnMsg, btnAccept, btnRefuse, btnDelete);
            }
            case CONFIRMED -> {
                Button btnCancel = new Button("Annuler");
                btnCancel.setStyle(
                        "-fx-background-color:#fee2e2; -fx-text-fill:#dc2626;" +
                                "-fx-background-radius:8; -fx-padding:6 14; -fx-cursor:hand;");
                btnCancel.setOnAction(e ->
                        onUpdateStatus.accept(r, ReservationStatus.CANCELLED));

                actions.getChildren().addAll(btnMsg, btnCancel, btnDelete);
            }
            case CANCELLED -> {
                Label done = new Label("Annulée");
                done.setStyle("-fx-text-fill:#9ca3af; -fx-font-size:12px;");
                actions.getChildren().addAll(done, btnDelete);
            }
        }
    }

    private void buildEtudiantActions(Reservation r, ReservationStatus status,
                                      HBox actions, Button btnMsg) {
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
                btnCancel.setOnAction(e -> onDelete.accept(r));

                actions.getChildren().addAll(btnMsg, btnEdit, btnCancel);
            }
            case CONFIRMED -> {
                // ✅ Coordination button — only on confirmed reservations
                Button btnCoord = new Button("📍 Coordination");
                btnCoord.setStyle(
                        "-fx-background-color:#d1fae5; -fx-text-fill:#065f46;" +
                                "-fx-font-weight:bold; -fx-background-radius:8;" +
                                "-fx-padding:6 14; -fx-cursor:hand;");
                btnCoord.setOnAction(e -> onCoordination.accept(r));

                Button btnCancel = new Button("✕ Annuler");
                btnCancel.setStyle(
                        "-fx-background-color:#fee2e2; -fx-text-fill:#dc2626;" +
                                "-fx-background-radius:8; -fx-padding:6 14; -fx-cursor:hand;");
                btnCancel.setOnAction(e -> onDelete.accept(r));

                actions.getChildren().addAll(btnMsg, btnCoord, btnCancel);
            }
            case CANCELLED -> {
                Label done = new Label("Réservation annulée");
                done.setStyle("-fx-text-fill:#9ca3af; -fx-font-size:12px;");
                actions.getChildren().add(done);
            }
        }
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