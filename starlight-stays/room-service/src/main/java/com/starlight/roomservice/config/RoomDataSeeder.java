package com.starlight.roomservice.config;

import com.starlight.roomservice.model.Room;
import com.starlight.roomservice.repository.RoomRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class RoomDataSeeder {

    @Bean
    CommandLineRunner seedRooms(RoomRepository roomRepository) {
        return args -> {
            if (roomRepository.count() == 0) {
                Room r1 = new Room();
                r1.setPropertyName("The Celestial Penthouse");
                r1.setRoomType("Penthouse");
                r1.setNightlyRate(new BigDecimal("850.00"));
                r1.setAvailable(true);
                roomRepository.save(r1);

                Room r2 = new Room();
                r2.setPropertyName("Nebula Horizon Villa");
                r2.setRoomType("Villa");
                r2.setNightlyRate(new BigDecimal("620.00"));
                r2.setAvailable(true);
                roomRepository.save(r2);

                Room r3 = new Room();
                r3.setPropertyName("Aurora Sky Chalet");
                r3.setRoomType("Chalet");
                r3.setNightlyRate(new BigDecimal("490.00"));
                r3.setAvailable(true);
                roomRepository.save(r3);

                Room r4 = new Room();
                r4.setPropertyName("Astral Balcony Suite");
                r4.setRoomType("Studio");
                r4.setNightlyRate(new BigDecimal("280.00"));
                r4.setAvailable(true);
                roomRepository.save(r4);

                Room r5 = new Room();
                r5.setPropertyName("Waterfront Starlight Bungalow");
                r5.setRoomType("Bungalow");
                r5.setNightlyRate(new BigDecimal("540.00"));
                r5.setAvailable(true);
                roomRepository.save(r5);

                Room r6 = new Room();
                r6.setPropertyName("Supernova Presidential Estate");
                r6.setRoomType("Penthouse");
                r6.setNightlyRate(new BigDecimal("1200.00"));
                r6.setAvailable(true);
                roomRepository.save(r6);
            }
        };
    }
}
