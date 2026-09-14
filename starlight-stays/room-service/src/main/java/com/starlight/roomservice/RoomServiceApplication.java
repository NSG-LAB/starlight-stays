package com.starlight.roomservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class RoomServiceApplication {
    public static void main(String[] args) { SpringApplication.run(RoomServiceApplication.class, args); }
}
