package org.example.campusLink.entities;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


public class Publications {

    // ==================== ENUMERATIONS ====================

    public enum TypePublication {
        DEMANDE_SERVICE("Demande de Service", "🔍"),
        VENTE_OBJET("Vente d'Objet", "🏷️");

        private final String label;
        private final String icon;

        TypePublication(String label, String icon) {
            this.label = label;
            this.icon = icon;
        }

        public String getLabel() {
            return label;
        }

        public String getIcon() {
            return icon;
        }
    }

    public enum StatusPublication {
        ACTIVE("Active", "✓"),
        EN_COURS("En Cours", "⏳"),
        TERMINEE("Terminée", "✅"),
        ANNULEE("Annulée", "✗");

        private final String label;
        private final String icon;

        StatusPublication(String label, String icon) {
            this.label = label;
            this.icon = icon;
        }

        public String getLabel() {
            return label;
        }

        public String getIcon() {
            return icon;
        }
    }

    // ==================== CHAMPS BASE DE DONNÉES ====================

    private int id;
    private int studentId;
    private TypePublication typePublication;
    private String titre;
    private String message;
    private String imageUrl;
    private String localisation;

    // Pour les demandes de service
    private Integer serviceId;
    private Integer prestataireId;
    private Timestamp requestedDate;
    private BigDecimal proposedPrice;

    // Pour les ventes
    private BigDecimal prixVente;

    // Métadonnées
    private StatusPublication status;
    private int vues;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // ==================== CHAMPS D'AFFICHAGE ====================

    private String studentName;
    private String serviceName;
    private String serviceDescription;
    private BigDecimal servicePrice;
    private String prestataireName;
    private String categoryName;

    // ==================== CONSTRUCTEURS ====================

    public Publications() {
        this.status = StatusPublication.ACTIVE;
        this.vues = 0;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    public Publications(int studentId, TypePublication type, String titre, String message) {
        this();
        this.studentId = studentId;
        this.typePublication = type;
        this.titre = titre;
        this.message = message;
    }

    // ==================== GETTERS & SETTERS ====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public TypePublication getTypePublication() { return typePublication; }
    public void setTypePublication(TypePublication typePublication) { this.typePublication = typePublication; }
    public void setTypePublicationFromString(String type) { this.typePublication = TypePublication.valueOf(type); }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean hasImage() { return imageUrl != null && !imageUrl.isEmpty(); }

    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    public Integer getServiceId() { return serviceId; }
    public void setServiceId(Integer serviceId) { this.serviceId = serviceId; }

    public Integer getPrestataireId() { return prestataireId; }
    public void setPrestataireId(Integer prestataireId) { this.prestataireId = prestataireId; }

    public Timestamp getRequestedDate() { return requestedDate; }
    public void setRequestedDate(Timestamp requestedDate) { this.requestedDate = requestedDate; }

    public BigDecimal getProposedPrice() { return proposedPrice; }
    public void setProposedPrice(BigDecimal proposedPrice) { this.proposedPrice = proposedPrice; }

    public BigDecimal getPrixVente() { return prixVente; }
    public void setPrixVente(BigDecimal prixVente) { this.prixVente = prixVente; }

    public StatusPublication getStatus() { return status; }
    public void setStatus(StatusPublication status) { this.status = status; }
    public void setStatusFromString(String status) { this.status = StatusPublication.valueOf(status); }

    public int getVues() { return vues; }
    public void setVues(int vues) { this.vues = vues; }
    public void incrementVues() { this.vues++; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // Display fields
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getServiceDescription() { return serviceDescription; }
    public void setServiceDescription(String serviceDescription) { this.serviceDescription = serviceDescription; }

    public BigDecimal getServicePrice() { return servicePrice; }
    public void setServicePrice(BigDecimal servicePrice) { this.servicePrice = servicePrice; }

    public String getPrestataireName() { return prestataireName; }
    public void setPrestataireName(String prestataireName) { this.prestataireName = prestataireName; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    // ==================== MÉTHODES UTILITAIRES ====================

    public String getFormattedPrice() {
        BigDecimal price = (typePublication == TypePublication.VENTE_OBJET)
                ? prixVente
                : (proposedPrice != null ? proposedPrice : servicePrice);

        if (price == null) return "Prix non spécifié";

        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("fr", "FR"));
        return formatter.format(price);
    }

    public String getFormattedDate() {
        if (createdAt == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        return createdAt.toLocalDateTime().format(formatter);
    }

    public String getRelativeTime() {
        if (createdAt == null) return "";

        long diff = System.currentTimeMillis() - createdAt.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return "il y a " + days + " jour" + (days > 1 ? "s" : "");
        else if (hours > 0) return "il y a " + hours + " heure" + (hours > 1 ? "s" : "");
        else if (minutes > 0) return "il y a " + minutes + " minute" + (minutes > 1 ? "s" : "");
        else return "à l'instant";
    }

    public String getMessagePreview(int maxLength) {
        if (message == null) return "";
        if (message.length() <= maxLength) return message;
        return message.substring(0, maxLength) + "...";
    }

    public boolean isDemandeService() {
        return typePublication == TypePublication.DEMANDE_SERVICE;
    }

    public boolean isVenteObjet() {
        return typePublication == TypePublication.VENTE_OBJET;
    }

    public String getTypeIcon() {
        return typePublication != null ? typePublication.getIcon() : "";
    }

    public String getStatusIcon() {
        return status != null ? status.getIcon() : "";
    }

    // ==================== VALIDATION ====================

    public boolean isValid() {
        if (titre == null || titre.trim().isEmpty()) return false;
        if (message == null || message.trim().isEmpty()) return false;
        if (typePublication == null) return false;

        if (typePublication == TypePublication.DEMANDE_SERVICE) {
            // serviceId peut être null pour une nouvelle demande (pas encore matchée)
            return serviceId == null || serviceId > 0;
        } else if (typePublication == TypePublication.VENTE_OBJET) {
            return prixVente != null && prixVente.compareTo(BigDecimal.ZERO) > 0;
        }

        return true;
    }

    @Override
    public String toString() {
        return "Publication{id=" + id + ", type=" + typePublication + ", titre='" + titre + "', status=" + status + "}";
    }
}