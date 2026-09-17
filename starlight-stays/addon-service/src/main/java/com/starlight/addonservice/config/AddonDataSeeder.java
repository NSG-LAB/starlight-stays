package com.starlight.addonservice.config;

import com.starlight.addonservice.model.ExperienceAddon;
import com.starlight.addonservice.repository.ExperienceAddonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@SuppressWarnings("null")
public class AddonDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AddonDataSeeder.class);
    private final ExperienceAddonRepository addonRepository;

    public AddonDataSeeder(ExperienceAddonRepository addonRepository) {
        this.addonRepository = addonRepository;
    }

    @Override
    public void run(String... args) {
        if (addonRepository.count() == 0) {
            log.info("💎 Seeding signature luxury experiences & VIP add-ons...");

            List<ExperienceAddon> addons = Arrays.asList(
                new ExperienceAddon(
                    "HELI_TRANSFER",
                    "Heli-Pad VIP Airport Transfer",
                    "Transportation",
                    850.0,
                    "Direct helicopter transfer from international terminal straight to the private Starlight Sky-Helipad.",
                    "🚁"
                ),
                new ExperienceAddon(
                    "PRIVATE_CHEF",
                    "Private Rooftop Chef Gastronomy",
                    "Gastronomy",
                    450.0,
                    "A curated 5-course tasting menu with sommelier wine pairing prepared in your suite's open galley.",
                    "👨‍🍳"
                ),
                new ExperienceAddon(
                    "CHAMPAGNE_VIP",
                    "Dom Pérignon Vintage Welcome Suite Package",
                    "Beverage",
                    320.0,
                    "Chilled vintage Dom Pérignon, Belgian artisan truffles, and fresh strawberries awaiting your arrival.",
                    "🍾"
                ),
                new ExperienceAddon(
                    "NIGHT_DIVE",
                    "Bioluminescent Night Coral Reef Dive",
                    "Excursion",
                    290.0,
                    "Guided underwater night expedition exploring glowing marine flora under starlight with private instructor.",
                    "🤿"
                ),
                new ExperienceAddon(
                    "SPA_RITUAL",
                    "Celestial En-Suite Hot Stone Spa",
                    "Wellness",
                    220.0,
                    "60-minute Himalayan warm mineral stone therapy and organic aromatherapy in your suite's private bath.",
                    "✨"
                )
            );

            addonRepository.saveAll(addons);
            log.info("✓ Seeded {} luxury VIP add-on experiences.", addons.size());
        }
    }
}
