package com.starlight.addonservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "experience_addons")
public class ExperienceAddon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String title;

    private String category;
    private Double price;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String icon;
    private Boolean active = true;

    public ExperienceAddon() {}

    public ExperienceAddon(String code, String title, String category, Double price, String description, String icon) {
        this.code = code;
        this.title = title;
        this.category = category;
        this.price = price;
        this.description = description;
        this.icon = icon;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
