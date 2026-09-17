package com.starlight.reviewservice.config;

import com.starlight.reviewservice.model.Review;
import com.starlight.reviewservice.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@SuppressWarnings("null")
public class ReviewDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ReviewDataSeeder.class);
    private final ReviewRepository reviewRepository;

    public ReviewDataSeeder(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Override
    public void run(String... args) {
        if (reviewRepository.count() == 0) {
            log.info("🌟 Seeding initial luxury reviews for Starlight Stays suites...");

            List<Review> initialReviews = Arrays.asList(
                new Review(1L, "Seraphina Vance", 5, "The celestial observatory and private heated plunge pool exceeded all expectations. Incredible concierge service!"),
                new Review(1L, "Julian Thorne", 5, "Unmatched skyline vistas. The Starlink connectivity and personal butler made this an unforgettable stay."),
                new Review(2L, "Marcus Aurelius B.", 5, "The bioluminescent garden and glass infinity pool are pure architectural wizardry. Will definitely return."),
                new Review(2L, "Elena Rostova", 4, "Extremely serene retreat. Loved the sauna & spa. Heli-pad transfers were seamless."),
                new Review(3L, "Kaelen Drake", 5, "Watching the aurora borealis through the heated glass dome from the cedar hot tub was magical!"),
                new Review(4L, "Aria Montgomery", 5, "Perfect balance of ultra-modern automation, soundproof acoustics, and panoramic city views."),
                new Review(5L, "David Kim", 5, "The overwater glass-floor deck overlooking the azure lagoon was surreal. Private yacht dock was brilliant."),
                new Review(6L, "Hélène de Belmont", 5, "The presidential villa offered supreme privacy, exceptional gastronomy from our private chef, and world-class luxury.")
            );

            reviewRepository.saveAll(initialReviews);
            log.info("✓ Seeded {} luxury reviews.", initialReviews.size());
        }
    }
}
