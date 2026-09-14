package com.starlight.reviewservice.repository;

import com.starlight.reviewservice.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRoomIdOrderByCreatedAtDesc(Long roomId);
    List<Review> findAllByOrderByCreatedAtDesc();
}
