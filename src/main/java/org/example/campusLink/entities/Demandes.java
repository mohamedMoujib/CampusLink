package org.example.campusLink.entities;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Entity class for Demandes table
 * Represents student requests for services
 */
public class Demandes {

    // Database fields
    private int id;
    private int studentId;
    private int serviceId;
    private int prestataireId;
    private String message;
    private Timestamp requestedDate;
    private BigDecimal proposedPrice;
    private String status; // EN_ATTENTE, CONFIRMEE, REFUSEE, TERMINEE
    private Timestamp createdAt;

    // Additional fields for UI display (from JOINs)
    private String serviceName;
    private String serviceDescription;
    private String studentName;
    private String prestataireName;
    private BigDecimal servicePrice;
    private String categoryName;

    // Constructors
    public Demandes() {
        this.status = "EN_ATTENTE";
    }

    public Demandes(int studentId, int serviceId, int prestataireId, String message) {
        this.studentId = studentId;
        this.serviceId = serviceId;
        this.prestataireId = prestataireId;
        this.message = message;
        this.status = "EN_ATTENTE";
    }

    public Demandes(int id, int studentId, int serviceId, int prestataireId,
                    String message, Timestamp requestedDate, BigDecimal proposedPrice,
                    String status, Timestamp createdAt) {
        this.id = id;
        this.studentId = studentId;
        this.serviceId = serviceId;
        this.prestataireId = prestataireId;
        this.message = message;
        this.requestedDate = requestedDate;
        this.proposedPrice = proposedPrice;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getServiceId() {
        return serviceId;
    }

    public void setServiceId(int serviceId) {
        this.serviceId = serviceId;
    }

    public int getPrestataireId() {
        return prestataireId;
    }

    public void setPrestataireId(int prestataireId) {
        this.prestataireId = prestataireId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Timestamp getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(Timestamp requestedDate) {
        this.requestedDate = requestedDate;
    }

    public BigDecimal getProposedPrice() {
        return proposedPrice;
    }

    public void setProposedPrice(BigDecimal proposedPrice) {
        this.proposedPrice = proposedPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // UI Display Fields
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getPrestataireName() {
        return prestataireName;
    }

    public void setPrestataireName(String prestataireName) {
        this.prestataireName = prestataireName;
    }

    public BigDecimal getServicePrice() {
        return servicePrice;
    }

    public void setServicePrice(BigDecimal servicePrice) {
        this.servicePrice = servicePrice;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    // Helper methods
    public String getFormattedPrice() {
        if (proposedPrice != null) {
            return String.format("%.2f€", proposedPrice);
        } else if (servicePrice != null) {
            return String.format("%.2f€", servicePrice);
        }
        return "0.00€";
    }

    public String getStatusLabel() {
        return switch (status) {
            case "EN_ATTENTE" -> "En attente";
            case "CONFIRMEE" -> "Confirmée";
            case "REFUSEE" -> "Refusée";
            case "TERMINEE" -> "Terminée";
            default -> status;
        };
    }

    public String getFormattedDate() {
        if (requestedDate != null) {
            return requestedDate.toString();
        } else if (createdAt != null) {
            return createdAt.toString();
        }
        return "Non défini";
    }

    @Override
    public String toString() {
        return "Demandes{" +
                "id=" + id +
                ", studentId=" + studentId +
                ", serviceId=" + serviceId +
                ", prestataireId=" + prestataireId +
                ", message='" + message + '\'' +
                ", requestedDate=" + requestedDate +
                ", proposedPrice=" + proposedPrice +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", serviceName='" + serviceName + '\'' +
                ", studentName='" + studentName + '\'' +
                '}';
    }
}