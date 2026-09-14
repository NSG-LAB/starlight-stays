package com.starlight.bookingservice.dto;

public class RoomDto {
    private Long id;
    private String propertyName;
    private String roomType;
    private Double nightlyRate;
    private boolean available;

    public RoomDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public Double getNightlyRate() { return nightlyRate; }
    public void setNightlyRate(Double nightlyRate) { this.nightlyRate = nightlyRate; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
