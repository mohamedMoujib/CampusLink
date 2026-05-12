package org.example.campusLink.entities;

import java.util.Objects;

/**
 * Entity class representing a Category
 * Used for categorizing Services and Publications
 *
 * Database table: categories
 * Columns: id, name, description
 */
public class Categorie {

    private int id;
    private String name;
    private String description;

    // ==================== CONSTRUCTORS ====================

    /**
     * Default constructor
     */
    public Categorie() {}

    /**
     * Constructor with name only
     */
    public Categorie(String name) {
        this.name = name;
    }

    /**
     * Full constructor
     */
    public Categorie(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    /**
     * Constructor without ID (for creating new categories)
     */
    public Categorie(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // ==================== GETTERS & SETTERS ====================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // ==================== VALIDATION ====================

    /**
     * Check if the category has valid data
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty();
    }

    /**
     * Get validation error message
     */
    public String getValidationError() {
        if (name == null || name.trim().isEmpty()) {
            return "Le nom de la catégorie est obligatoire";
        }
        if (name.length() > 100) {
            return "Le nom ne peut pas dépasser 100 caractères";
        }
        if (description != null && description.length() > 500) {
            return "La description ne peut pas dépasser 500 caractères";
        }
        return null;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get display name (name in title case)
     */
    public String getDisplayName() {
        if (name == null || name.isEmpty()) {
            return "Sans catégorie";
        }
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    /**
     * Get short description (first 50 chars)
     */
    public String getShortDescription() {
        if (description == null || description.isEmpty()) {
            return "Aucune description";
        }
        if (description.length() <= 50) {
            return description;
        }
        return description.substring(0, 47) + "...";
    }

    /**
     * Check if category has description
     */
    public boolean hasDescription() {
        return description != null && !description.trim().isEmpty();
    }

    /**
     * Get category icon based on name (for UI display)
     */
    public String getIcon() {
        if (name == null) return "📚";

        String lowerName = name.toLowerCase();

        if (lowerName.contains("programmation") || lowerName.contains("informatique") || lowerName.contains("code")) {
            return "💻";
        } else if (lowerName.contains("mathématiques") || lowerName.contains("maths")) {
            return "🔢";
        } else if (lowerName.contains("physique")) {
            return "⚛️";
        } else if (lowerName.contains("chimie")) {
            return "🧪";
        } else if (lowerName.contains("langue") || lowerName.contains("anglais") || lowerName.contains("français")) {
            return "🗣️";
        } else if (lowerName.contains("histoire") || lowerName.contains("géographie")) {
            return "🌍";
        } else if (lowerName.contains("biologie") || lowerName.contains("svt")) {
            return "🧬";
        } else if (lowerName.contains("économie") || lowerName.contains("gestion")) {
            return "💼";
        } else if (lowerName.contains("droit")) {
            return "⚖️";
        } else if (lowerName.contains("art") || lowerName.contains("design")) {
            return "🎨";
        } else if (lowerName.contains("musique")) {
            return "🎵";
        } else if (lowerName.contains("sport")) {
            return "⚽";
        } else {
            return "📚";
        }
    }

    // ==================== OBJECT OVERRIDES ====================

    @Override
    public String toString() {
        return "Categorie{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + (description != null ? getShortDescription() : "null") + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Categorie categorie = (Categorie) o;
        return id == categorie.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * For use in ComboBox and other UI components
     */
    public String toComboBoxString() {
        return getIcon() + " " + name;
    }
}