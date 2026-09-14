package com.starlight.bookingservice.client;

import com.starlight.bookingservice.dto.RoomDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RoomClient {

    private final RestTemplate restTemplate;

    public RoomClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @CircuitBreaker(name = "roomServiceCB", fallbackMethod = "roomFallback")
    public RoomDto getRoomById(Long roomId) {
        return restTemplate.getForObject("http://ROOM-SERVICE/api/rooms/" + roomId, RoomDto.class);
    }

    public RoomDto roomFallback(Long roomId, Throwable t) {
        RoomDto fallbackRoom = new RoomDto();
        fallbackRoom.setId(roomId);
        fallbackRoom.setPropertyName("Fallback Suite (Service Degraded)");
        fallbackRoom.setAvailable(true);
        return fallbackRoom;
    }
}
