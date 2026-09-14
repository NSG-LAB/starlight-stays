package com.starlight.roomservice.repository;
import com.starlight.roomservice.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByPropertyNameAndIsAvailableTrue(String propertyName);
}
