package com.starlight.addonservice.repository;

import com.starlight.addonservice.model.ExperienceAddon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@SuppressWarnings("null")
public interface ExperienceAddonRepository extends JpaRepository<ExperienceAddon, Long> {
    List<ExperienceAddon> findByActiveTrueOrderByPriceAsc();
    Optional<ExperienceAddon> findByCode(String code);
}
