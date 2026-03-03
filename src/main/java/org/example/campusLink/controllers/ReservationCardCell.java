package org.example.campusLink.controllers;

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

    private final Consumer<Reservation> onEdit;
    private final Consumer<Reservation> onDelete;
    private final Consumer<Reservation> onContact;
    private final Consumer<Reservation> onCancel;
    private final BiConsumer<Reservation, ReservationStatus> onUpdateStatus;

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy HH:mm");

    public ReservationCardCell(
            Consumer<Reservation> onEdit,
            Consumer<Reservation> onDelete,
            Consumer<Reservation> onContact,
            Consumer<Reservation> onCancel,
            BiConsumer<Reservation, ReservationStatus> onUpdateStatus
    ) {
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        this.onContact = onContact;
        this.onCancel = onCancel;
        this.onUpdateStatus = onUpdateStatus;
    }

    @Override
    protected void updateItem(Reservation r, boolean empty) {
        super.updateItem(r, empty);

        if (empty || r == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        // ---- header
        Label title = new Label(r.getServiceTitle() != null ? r.getServiceTitle() : ("Service #" + r.getServiceId()));
        title.setStyle("-fx-font-size:16px; -fx-font-weight:800;");

        Label subtitle = new Label("pour " + (r.getStudentName() != null ? r.getStudentName() : ("Étudiant #" + r.getStudentId())));
        subtitle.getStyleClass().add("muted");

        VBox leftTop = new VBox(2, title, subtitle);

        ReservationStatus status = r.getStatus() != null ? r.getStatus() : ReservationStatus.PENDING;

        Label badge = new Label(statusLabel(status));
        badge.getStyleClass().addAll("badge", statusBadgeClass(status));

        Button btnEdit = new Button("✎");
        btnEdit.getStyleClass().add("icon-btn");
        btnEdit.setOnAction(e -> onEdit.accept(r));

        Button btnDelete = new Button("🗑");
        btnDelete.getStyleClass().add("icon-btn-danger");
        btnDelete.setOnAction(e -> onDelete.accept(r));

        HBox rightTop = new HBox(8, badge, btnEdit, btnDelete);
        rightTop.setAlignment(Pos.CENTER_RIGHT);

        Region spacerTop = new Region();
        HBox.setHgrow(spacerTop, Priority.ALWAYS);

        HBox topRow = new HBox(leftTop, spacerTop, rightTop);

        // ---- middle
        Label date = new Label("📅 " + (r.getDate() != null ? r.getDate().format(fmt) : ""));
        Label location = new Label("📍 " + (r.getLocalisation() != null ? r.getLocalisation() : "Lieu : ..."));
        date.getStyleClass().add("muted");
        location.getStyleClass().add("muted");

        HBox midRow = new HBox(18, date, location);
        midRow.setAlignment(Pos.CENTER_LEFT);

        // ---- bottom
        String priceText = "";
        if (r.getPrice() != null) {
            priceText = String.format("%.0f DT", r.getPrice());
        }
        Label price = new Label(priceText);
        price.getStyleClass().add("price");

        Region spacerBottom = new Region();
        HBox.setHgrow(spacerBottom, Priority.ALWAYS);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        if (status == ReservationStatus.PENDING) {
            Button accept = new Button("Accepter");
            accept.getStyleClass().add("btn-success");
            accept.setOnAction(e -> onUpdateStatus.accept(r, ReservationStatus.CONFIRMED));

            Button refuse = new Button("Refuser");
            refuse.getStyleClass().add("btn-danger");
            refuse.setOnAction(e -> onUpdateStatus.accept(r, ReservationStatus.CANCELLED));

            actions.getChildren().addAll(accept, refuse);

        } else if (status == ReservationStatus.CONFIRMED) {
            Button contact = new Button("Contacter");
            contact.getStyleClass().add("primary-btn");
            contact.setOnAction(e -> onContact.accept(r));

            Button cancel = new Button("Annuler");
            cancel.getStyleClass().add("btn-danger-outline");
            cancel.setOnAction(e -> onCancel.accept(r));

            actions.getChildren().addAll(contact, cancel);

        } else {
            Button details = new Button("Voir les détails");
            details.getStyleClass().add("btn-outline");
            details.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION, "Détails réservation #" + r.getId()).showAndWait());
            actions.getChildren().add(details);
        }

        HBox bottomRow = new HBox(price, spacerBottom, actions);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, topRow, midRow, new Separator(), bottomRow);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));

        setText(null);
        setGraphic(card);
    }

    private String statusLabel(ReservationStatus s) {
        return switch (s) {
            case CONFIRMED -> "Confirmée";
            case PENDING -> "En attente";
            case DONE -> "Terminée";
            case CANCELLED -> "Annulée";
        };
    }

    private String statusBadgeClass(ReservationStatus s) {
        return switch (s) {
            case CONFIRMED -> "badge-green";
            case PENDING -> "badge-yellow";
            default -> "badge-blue";
        };
    }
}