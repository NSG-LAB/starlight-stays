package com.starlight.bookingservice.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
@FeignClient(name = "room-service")
public interface RoomServiceClient {
    @GetMapping("/api/rooms/{id}")
    Object getRoom(@PathVariable("id") Long id);
}
