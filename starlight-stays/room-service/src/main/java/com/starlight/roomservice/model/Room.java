package com.starlight.roomservice.model;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
public class Room implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String propertyName; private String roomType; private BigDecimal nightlyRate; private boolean isAvailable = true;
    public Room() {}
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getPropertyName() { return propertyName; } public void setPropertyName(String propertyName) { this.propertyName = propertyName; }
    public String getRoomType() { return roomType; } public void setRoomType(String roomType) { this.roomType = roomType; }
    public BigDecimal getNightlyRate() { return nightlyRate; } public void setNightlyRate(BigDecimal nightlyRate) { this.nightlyRate = nightlyRate; }
    public boolean isAvailable() { return isAvailable; } public void setAvailable(boolean available) { isAvailable = available; }
}
