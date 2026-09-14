package com.starlight.conciergeservice.controller;

import com.starlight.conciergeservice.model.RecommendationRequest;
import com.starlight.conciergeservice.model.RecommendationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/concierge")
public class ConciergeController {

    @GetMapping("/styles")
    public List<Map<String, String>> getTravelStyles() {
        List<Map<String, String>> styles = new ArrayList<>();
        styles.add(Map.of("id", "CELESTIAL", "name", "✨ Celestial Stargazing & High Glamour", "description", "Observatory domes, heated plunge pools, and personal butler service."));
        styles.add(Map.of("id", "OCEANIC", "name", "🌊 Azure Lagoon & Overwater Escape", "description", "Glass-bottom villas, private yacht docks, and tranquil marine vistas."));
        styles.add(Map.of("id", "ALPINE", "name", "🏔️ Aurora & Nordic Mountain Wellness", "description", "Cedar hot tubs, northern lights glass roofs, and wood-fired saunas."));
        styles.add(Map.of("id", "ESTATE", "name", "👑 Grand Presidential VIP Estate", "description", "Supreme privacy, private executive chef, and multiple master wings."));
        styles.add(Map.of("id", "ARCHITECTURAL", "name", "🌿 Architectural Bioluminescent Sanctuary", "description", "Infinity pools, glowing gardens, and private heli-pad transfers."));
        styles.add(Map.of("id", "EXECUTIVE", "name", "💼 Skyline Executive & Smart Automation", "description", "Acoustic soundproof studio, high-speed Starlink, and skyline espresso bar."));
        return styles;
    }

    @PostMapping("/recommend")
    public ResponseEntity<RecommendationResponse> recommendSuite(@RequestBody RecommendationRequest request) {
        String style = request.getTravelStyle() != null ? request.getTravelStyle().toUpperCase() : "CELESTIAL";
        int guests = request.getGuestCount() != null ? request.getGuestCount() : 2;

        RecommendationResponse resp = new RecommendationResponse();

        if (guests >= 4 || style.contains("ESTATE") || style.contains("FAMILY")) {
            resp.setRoomId(6L);
            resp.setSuiteName("Starlight Grand Presidential Estate");
            resp.setSuiteType("Presidential Villa");
            resp.setNightlyRate(1200.0);
            resp.setConfidenceScore(99);
            resp.setMatchRationale("Matched for multi-guest party seeking supreme privacy, dedicated gastronomy, and generous space.");
            resp.setBespokeAmenities(List.of("3 Master Suites", "Dedicated Private Chef", "Cinema Screening Room", "Chauffeured Limousine"));
            resp.setCuratedThreeDayItinerary(List.of(
                "Day 1: Private heli-transfer arrival, chef's bespoke 7-course tasting menu paired with rare vintages.",
                "Day 2: Morning wellness yoga on the starlight terrace, afternoon cinema screening, and private fireside lounge.",
                "Day 3: Sunrise hot-air balloon excursion followed by a personalized champagne brunch."
            ));
            resp.setConciergeSignoff("Curated exclusively by Master Sommelier & Estate Butler for your distinguished party.");
        } else if (style.contains("OCEAN") || style.contains("LAGOON") || style.contains("WATER")) {
            resp.setRoomId(5L);
            resp.setSuiteName("Oceanic Horizon Overwater Villa");
            resp.setSuiteType("Water Villa");
            resp.setNightlyRate(850.0);
            resp.setConfidenceScore(98);
            resp.setMatchRationale("Selected for supreme oceanic immersion, glass-floor marine viewing, and yachting privileges.");
            resp.setBespokeAmenities(List.of("Overwater Deck", "Glass Floor Lagoon", "Private Yacht Dock", "Sunken Hammock Net"));
            resp.setCuratedThreeDayItinerary(List.of(
                "Day 1: Twilight speedboat transfer, private overwater welcome toast, and bioluminescent night snorkel.",
                "Day 2: Private catamaran yacht cruise, coral reef exploration, and sunset deck dining.",
                "Day 3: In-villa oceanic massage followed by private floating breakfast in the lagoon."
            ));
            resp.setConciergeSignoff("Handpicked by the Starlight Marine Concierge Team.");
        } else if (style.contains("ALPINE") || style.contains("AURORA") || style.contains("MOUNTAIN")) {
            resp.setRoomId(3L);
            resp.setSuiteName("Aurora Borealis Sky Chalet");
            resp.setSuiteType("Chalet");
            resp.setNightlyRate(450.0);
            resp.setConfidenceScore(97);
            resp.setMatchRationale("Tailored for crisp alpine romance, northern lights watching, and wood-fired relaxation.");
            resp.setBespokeAmenities(List.of("Northern Lights Heated Glass Roof", "Cedar Hot Tub", "Nordic Herbal Sauna", "Artisan Fireplace"));
            resp.setCuratedThreeDayItinerary(List.of(
                "Day 1: Snowcat escort to the chalet, hot spiced glühwein by the artisan hearth, and aurora watch.",
                "Day 2: Guided alpine snowshoe trek, cedar hot tub relaxation, and Nordic charcoal grill dinner.",
                "Day 3: Panoramic breakfast under the heated glass dome with fresh berry preserves."
            ));
            resp.setConciergeSignoff("Arranged with warm alpine hospitality by Starlight Nordic Concierge.");
        } else if (style.contains("EXECUTIVE") || style.contains("WORK") || style.contains("TECH")) {
            resp.setRoomId(4L);
            resp.setSuiteName("Skyline Executive High-Tech Loft");
            resp.setSuiteType("Loft");
            resp.setNightlyRate(380.0);
            resp.setConfidenceScore(96);
            resp.setMatchRationale("Optimized for modern executives requiring whisper-quiet acoustics, ultra-fast fiber, and city views.");
            resp.setBespokeAmenities(List.of("Smart Voice Automation", "Soundproof Audio/Podcast Studio", "High-Speed Starlink", "Italian Espresso Bar"));
            resp.setCuratedThreeDayItinerary(List.of(
                "Day 1: Seamless biometric keyless check-in, barista tasting flight, and twilight skyline terrace relaxation.",
                "Day 2: High-productivity studio session, private executive lunch catering, and evening jazz club reservations.",
                "Day 3: Sunrise balcony espresso and complimentary town car transfer to the terminal."
            ));
            resp.setConciergeSignoff("Orchestrated by Starlight Corporate & Executive Services.");
        } else if (style.contains("ARCHITECTURAL") || style.contains("GARDEN") || style.contains("VILLA")) {
            resp.setRoomId(2L);
            resp.setSuiteName("Nebula Architectural Glass Villa");
            resp.setSuiteType("Villa");
            resp.setNightlyRate(620.0);
            resp.setConfidenceScore(98);
            resp.setMatchRationale("Designed for architecture connoisseurs seeking infinity pool serenity and private botanical gardens.");
            resp.setBespokeAmenities(List.of("Glass Infinity Plunge Pool", "Bioluminescent Flora Garden", "Private Heli-Pad", "Aromatherapy Spa"));
            resp.setCuratedThreeDayItinerary(List.of(
                "Day 1: Direct heli-pad touchdown, chilled champagne in the infinity pool, and stroll through illuminated gardens.",
                "Day 2: In-villa botanical spa therapy, afternoon tea, and sunset architectural photo tour.",
                "Day 3: Outdoor pergola breakfast surrounded by cascading water sculptures."
            ));
            resp.setConciergeSignoff("Presented by Starlight Design & Wellness Curators.");
        } else {
            // Default: Signature Celestial Penthouse (Room 1)
            resp.setRoomId(1L);
            resp.setSuiteName("The Celestial Penthouse Suite");
            resp.setSuiteType("Penthouse");
            resp.setNightlyRate(750.0);
            resp.setConfidenceScore(99);
            resp.setMatchRationale("Our flagship signature suite with 360-degree celestial observatory dome and dedicated butler service.");
            resp.setBespokeAmenities(List.of("Sky Observatory Telescope", "Heated Plunge Pool", "24/7 Personal Butler", "High-Speed Starlink"));
            resp.setCuratedThreeDayItinerary(List.of(
                "Day 1: VIP penthouse check-in, chilled vintage Dom Pérignon, and guided telescope tour of constellations.",
                "Day 2: Rooftop plunge pool morning swim, bespoke afternoon high-tea, and candlelight balcony dinner.",
                "Day 3: Gourmet breakfast served in bed beneath the celestial skylight."
            ));
            resp.setConciergeSignoff("Crafted with passion by the Chief Starlight Ambassador.");
        }

        return ResponseEntity.ok(resp);
    }
}
