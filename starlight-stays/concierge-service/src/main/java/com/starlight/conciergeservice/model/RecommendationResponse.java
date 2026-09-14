package com.starlight.conciergeservice.model;

import java.util.List;

public class RecommendationResponse {
    private Long roomId;
    private String suiteName;
    private String suiteType;
    private Double nightlyRate;
    private Integer confidenceScore; // e.g., 98
    private String matchRationale;
    private List<String> bespokeAmenities;
    private List<String> curatedThreeDayItinerary;
    private String conciergeSignoff;

    public RecommendationResponse() {}

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    public String getSuiteType() {
        return suiteType;
    }

    public void setSuiteType(String suiteType) {
        this.suiteType = suiteType;
    }

    public Double getNightlyRate() {
        return nightlyRate;
    }

    public void setNightlyRate(Double nightlyRate) {
        this.nightlyRate = nightlyRate;
    }

    public Integer getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Integer confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getMatchRationale() {
        return matchRationale;
    }

    public void setMatchRationale(String matchRationale) {
        this.matchRationale = matchRationale;
    }

    public List<String> getBespokeAmenities() {
        return bespokeAmenities;
    }

    public void setBespokeAmenities(List<String> bespokeAmenities) {
        this.bespokeAmenities = bespokeAmenities;
    }

    public List<String> getCuratedThreeDayItinerary() {
        return curatedThreeDayItinerary;
    }

    public void setCuratedThreeDayItinerary(List<String> curatedThreeDayItinerary) {
        this.curatedThreeDayItinerary = curatedThreeDayItinerary;
    }

    public String getConciergeSignoff() {
        return conciergeSignoff;
    }

    public void setConciergeSignoff(String conciergeSignoff) {
        this.conciergeSignoff = conciergeSignoff;
    }
}
