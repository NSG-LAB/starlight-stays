package com.starlight.roomservice.controller;
import com.starlight.roomservice.model.Room;
import com.starlight.roomservice.repository.RoomRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@SuppressWarnings("null")
@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Cacheable(value = "rooms", key = "'all_' + (#propertyName != null ? #propertyName : 'all')")
    @GetMapping
    public List<Room> getAllRooms(@RequestParam(required = false) String propertyName) {
        if (propertyName != null && !propertyName.trim().isEmpty()) {
            return roomRepository.findByPropertyNameAndIsAvailableTrue(propertyName);
        }
        return roomRepository.findAll();
    }

    @Cacheable(value = "rooms", key = "'avail_' + #propertyName")
    @GetMapping("/{propertyName}/available")
    public List<Room> getAvailableRooms(@PathVariable String propertyName) {
        return roomRepository.findByPropertyNameAndIsAvailableTrue(propertyName);
    }
    
    @Cacheable(value = "rooms", key = "'room_' + #id")
    @GetMapping("/{id}")
    public Room getRoom(@PathVariable Long id) {
        return roomRepository.findById(id).orElseThrow();
    }

    @CacheEvict(value = "rooms", allEntries = true)
    @PostMapping
    public Room addRoom(@RequestBody Room room) {
        return roomRepository.save(room);
    }
}
