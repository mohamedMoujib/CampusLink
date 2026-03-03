package org.example.campusLink.entities;

import com.mysql.cj.protocol.x.FetchDoneEntity;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    DONE;
    public String labelFr() {
        return switch (this) {
            case PENDING -> "En attente";
            case CONFIRMED -> "Confirmée";
            case CANCELLED -> "Annulée";
            case DONE -> "Terminée";
        };
    }
}
