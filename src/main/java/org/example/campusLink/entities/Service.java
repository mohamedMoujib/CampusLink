package org.example.campusLink.entities;

import java.math.BigDecimal;

public class Service {
    private int id;
    private String title;
    private String description;
    private BigDecimal price;
    private String image;
    private int prestataireId;       // correspond à provider_id
    private Integer categoryId;

    // Champ supplémentaire pour le nom du prestataire (issu d'un JOIN)
    private String providerName;

    // Getters et setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public int getPrestataireId() { return prestataireId; }
    public void setPrestataireId(int prestataireId) { this.prestataireId = prestataireId; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    @Override
    public String toString() {
        return title + " (" + price + " DT) - " + providerName;
    }
}