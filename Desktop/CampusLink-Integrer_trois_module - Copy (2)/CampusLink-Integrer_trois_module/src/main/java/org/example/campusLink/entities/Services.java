package org.example.campusLink.entities;

import java.text.DecimalFormat;

/**
 * Services Entity - Represents a service offered by a provider
 * Enhanced with display methods for JavaFX UI
 */
public class Services {

    // ==================== CORE FIELDS ====================

    private int id;
    private String title;
    private String description;
    private String image;
    private double price;
    private int prestataireId;
    private int categoryId;
    private String status; // EN_ATTENTE, ACTIF, INACTIF, REJETE

    // ==================== DISPLAY FIELDS (from JOINs) ====================

    private String categoryName;      // From categories table
    private String prestataireName;   // From users table

    // ==================== FORMATTING ====================

    private static final DecimalFormat priceFormatter = new DecimalFormat("#,##0.00");

    // ==================== CONSTRUCTORS ====================

    public Services() {
    }

    public Services(int id, String title, String description, String image,
                    double price, int prestataireId, int categoryId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.image = image;
        this.price = price;
        this.prestataireId = prestataireId;
        this.categoryId = categoryId;
        this.status = "EN_ATTENTE";
    }

    // ==================== CORE GETTERS & SETTERS ====================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getPrestataireId() {
        return prestataireId;
    }

    public void setPrestataireId(int prestataireId) {
        this.prestataireId = prestataireId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // ==================== DISPLAY GETTERS & SETTERS ====================

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getPrestataireName() {
        return prestataireName;
    }

    public void setPrestataireName(String prestataireName) {
        this.prestataireName = prestataireName;
    }

    // ==================== DISPLAY METHODS ====================

    /**
     * ✅ Get formatted price with currency symbol
     * Example: "25,00 €" or "100,50 €"
     *
     * CRITICAL: This method is used by CreateDemande_controller
     */
    public String getFormattedPrice() {
        return priceFormatter.format(price) + " €";
    }

    /**
     * ✅ Get short description (max 100 chars)
     * Useful for cards and previews
     */
    public String getShortDescription() {
        if (description == null || description.isEmpty()) {
            return "Aucune description disponible";
        }

        if (description.length() <= 100) {
            return description;
        }

        return description.substring(0, 97) + "...";
    }

    /**
     * ✅ Get category display name with fallback
     * Returns category name or "Non catégorisé" if null
     */
    public String getCategoryDisplayName() {
        return categoryName != null && !categoryName.isBlank()
                ? categoryName
                : "Non catégorisé";
    }

    /**
     * ✅ Get prestataire display name with fallback
     * Returns prestataire name or "Prestataire #ID" if null
     */
    public String getPrestataireDisplayName() {
        return prestataireName != null && !prestataireName.isBlank()
                ? prestataireName
                : "Prestataire #" + prestataireId;
    }

    /**
     * ✅ Get status display label (French)
     */
    public String getStatusLabel() {
        return switch (status) {
            case "EN_ATTENTE" -> "En attente";
            case "ACTIF" -> "Actif";
            case "INACTIF" -> "Inactif";
            case "REJETE" -> "Rejeté";
            default -> status;
        };
    }

    /**
     * ✅ Get status color for UI
     * Returns hex color code
     */
    public String getStatusColor() {
        return switch (status) {
            case "EN_ATTENTE" -> "#f59e0b"; // Orange
            case "ACTIF" -> "#10b981";      // Green
            case "INACTIF" -> "#6b7280";    // Gray
            case "REJETE" -> "#ef4444";     // Red
            default -> "#9ca3af";           // Default gray
        };
    }

    /**
     * ✅ Check if service is active/available
     */
    public boolean isActive() {
        return "ACTIF".equals(status);
    }

    /**
     * ✅ Check if service is pending approval
     */
    public boolean isPending() {
        return "EN_ATTENTE".equals(status);
    }

    /**
     * ✅ Check if service has been rejected
     */
    public boolean isRejected() {
        return "REJETE".equals(status);
    }

    /**
     * ✅ Get price range category
     * Useful for filtering
     */
    public String getPriceRange() {
        if (price < 20) return "Moins de 20€";
        if (price < 50) return "20€ - 50€";
        if (price < 100) return "50€ - 100€";
        if (price < 200) return "100€ - 200€";
        return "Plus de 200€";
    }

    // ==================== VALIDATION METHODS ====================

    /**
     * ✅ Check if service is valid
     */
    public boolean isValid() {
        return title != null && !title.isBlank()
                && price > 0
                && prestataireId > 0
                && categoryId > 0;
    }

    /**
     * ✅ Validate service data and return error message
     */
    public String validate() {
        if (title == null || title.isBlank()) {
            return "Le titre est obligatoire";
        }

        if (title.length() < 3) {
            return "Le titre doit contenir au moins 3 caractères";
        }

        if (title.length() > 200) {
            return "Le titre ne peut pas dépasser 200 caractères";
        }

        if (description != null && description.length() > 1000) {
            return "La description ne peut pas dépasser 1000 caractères";
        }

        if (price <= 0) {
            return "Le prix doit être supérieur à 0";
        }

        if (price > 10000) {
            return "Le prix ne peut pas dépasser 10000€";
        }

        if (prestataireId <= 0) {
            return "Prestataire invalide";
        }

        if (categoryId <= 0) {
            return "Catégorie invalide";
        }

        return null; // Valid
    }

    // ==================== UTILITY METHODS ====================

    /**
     * ✅ Get truncated title for UI display
     */
    public String getTruncatedTitle(int maxLength) {
        if (title == null) return "";
        if (title.length() <= maxLength) return title;
        return title.substring(0, maxLength - 3) + "...";
    }

    /**
     * ✅ Clone service (useful for editing)
     */
    public Services clone() {
        Services cloned = new Services();
        cloned.setId(this.id);
        cloned.setTitle(this.title);
        cloned.setDescription(this.description);
        cloned.setImage(this.image);
        cloned.setPrice(this.price);
        cloned.setPrestataireId(this.prestataireId);
        cloned.setCategoryId(this.categoryId);
        cloned.setStatus(this.status);
        cloned.setCategoryName(this.categoryName);
        cloned.setPrestataireName(this.prestataireName);
        return cloned;
    }

    // ==================== OBJECT METHODS ====================

    @Override
    public String toString() {
        return "Services{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", price=" + price +
                ", prestataire=" + getPrestataireDisplayName() +
                ", category=" + getCategoryDisplayName() +
                ", status='" + status + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Services services = (Services) o;
        return id == services.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}