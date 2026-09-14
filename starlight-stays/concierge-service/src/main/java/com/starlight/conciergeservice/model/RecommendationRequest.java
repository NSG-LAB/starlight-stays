package com.starlight.conciergeservice.model;

import java.util.List;

public class RecommendationRequest {
    private String travelStyle; // e.g., ROMANCE, FAMILY, EXECUTIVE, ALPINE, OCEANIC, CELESTIAL
    private Integer guestCount;
    private List<String> preferredAmenities;
    private String budgetPreference; // MODERATE, PREMIUM, ULTRA_LUXURY

    public RecommendationRequest() {}

    public String getTravelStyle() {
        return travelStyle;
    }

    public void setTravelStyle(String travelStyle) {
        this.travelStyle = travelStyle;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public List<String> getPreferredAmenities() {
        return preferredAmenities;
    }

    public void setPreferredAmenities(List<String> preferredAmenities) {
        this.preferredAmenities = preferredAmenities;
    }

    public String getBudgetPreference() {
        return budgetPreference;
    }

    public void setBudgetPreference(String budgetPreference) {
        this.budgetPreference = budgetPreference;
    }
}
