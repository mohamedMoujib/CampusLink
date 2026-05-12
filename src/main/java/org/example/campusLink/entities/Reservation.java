package org.example.campusLink.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Reservation {
    private int id;
    private int studentId;
    private int serviceId;
    private LocalDateTime date;
    private ReservationStatus status;
    private String localisation;      // correspond à la colonne "localisation"
    private BigDecimal price;          // prix au moment de la réservation


    private String providerName;

    private int ProviderId ;

    // Champs supplémentaires pour les JOINs
    private String serviceTitle;
    private BigDecimal servicePrice;   // prix actuel du service (optionnel)
    private String studentName;

    // Constructeurs
    public Reservation() {}

    // Getters et setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public int getServiceId() { return serviceId; }
    public void setServiceId(int serviceId) { this.serviceId = serviceId; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }


    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getServiceTitle() { return serviceTitle; }
    public void setServiceTitle(String serviceTitle) { this.serviceTitle = serviceTitle; }

    public BigDecimal getServicePrice() { return servicePrice; }
    public void setServicePrice(BigDecimal servicePrice) { this.servicePrice = servicePrice; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public int getProviderId() {
        return ProviderId;
    }

    public void setProviderId(int providerId) {
        ProviderId = providerId;
    }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

}