package org.example.campusLink.entities;


import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Demande {
    private int id;
    private int studentId;
    private int serviceId;
    private int prestataireId;
    private String message;
    private LocalDateTime requestedDate; // nullable
    private BigDecimal proposedPrice;    // nullable
    private DemandeStatus status;
    private LocalDateTime createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public int getServiceId() { return serviceId; }
    public void setServiceId(int serviceId) { this.serviceId = serviceId; }

    public int getPrestataireId() { return prestataireId; }
    public void setPrestataireId(int prestataireId) { this.prestataireId = prestataireId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getRequestedDate() { return requestedDate; }
    public void setRequestedDate(LocalDateTime requestedDate) { this.requestedDate = requestedDate; }

    public BigDecimal getProposedPrice() { return proposedPrice; }
    public void setProposedPrice(BigDecimal proposedPrice) { this.proposedPrice = proposedPrice; }

    public DemandeStatus getStatus() { return status; }
    public void setStatus(DemandeStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
