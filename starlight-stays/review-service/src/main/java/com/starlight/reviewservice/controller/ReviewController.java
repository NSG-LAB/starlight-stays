package com.starlight.reviewservice.controller;

import com.starlight.reviewservice.model.Review;
import com.starlight.reviewservice.repository.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@SuppressWarnings("null")
public class ReviewController {

    private final ReviewRepository reviewRepository;

    public ReviewController(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @GetMapping
    public List<Review> getAllReviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/room/{roomId}")
    public List<Review> getReviewsByRoomId(@PathVariable Long roomId) {
        return reviewRepository.findByRoomIdOrderByCreatedAtDesc(roomId);
    }

    @GetMapping("/stats/{roomId}")
    public Map<String, Object> getRoomReviewStats(@PathVariable Long roomId) {
        List<Review> reviews = reviewRepository.findByRoomIdOrderByCreatedAtDesc(roomId);
        Map<String, Object> stats = new HashMap<>();
        stats.put("roomId", roomId);
        stats.put("count", reviews.size());
        
        if (reviews.isEmpty()) {
            stats.put("averageRating", 5.0);
        } else {
            double avg = reviews.stream()
                    .filter(r -> r != null && r.getRating() != null)
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(5.0);
            stats.put("averageRating", Math.round(avg * 10.0) / 10.0);
        }
        return stats;
    }

    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody Review review) {
        if (review.getRoomId() == null) {
            return ResponseEntity.badRequest().body("Room ID is required.");
        }
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            return ResponseEntity.badRequest().body("Rating must be between 1 and 5 stars.");
        }
        if (review.getGuestName() == null || review.getGuestName().trim().isEmpty()) {
            review.setGuestName("Anonymous Resident");
        }
        if (review.getComment() == null || review.getComment().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Review comment is required.");
        }

        Review saved = reviewRepository.save(review);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
