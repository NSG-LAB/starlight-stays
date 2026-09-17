package com.starlight.chatservice.controller;

import com.starlight.chatservice.model.ChatMessage;
import com.starlight.chatservice.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/chat")
@SuppressWarnings("null")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatMessageRepository chatMessageRepository, SimpMessagingTemplate messagingTemplate) {
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/history/{channelId}")
    public List<ChatMessage> getChatHistory(@PathVariable String channelId) {
        return chatMessageRepository.findByChannelIdOrderByCreatedAtAsc(channelId);
    }

    @PostMapping("/send")
    public ResponseEntity<ChatMessage> sendRestMessage(@RequestBody ChatMessage message) {
        processAndBroadcastMessage(message);
        return ResponseEntity.ok(message);
    }

    @MessageMapping("/chat.send")
    public void receiveWsMessage(@Payload ChatMessage message) {
        processAndBroadcastMessage(message);
    }

    private void processAndBroadcastMessage(ChatMessage message) {
        if (message.getSenderRole() == null || message.getSenderRole().isEmpty()) {
            message.setSenderRole("GUEST");
        }
        
        ChatMessage saved = chatMessageRepository.save(message);
        String destination = "/topic/channel/" + saved.getChannelId();
        messagingTemplate.convertAndSend(destination, saved);
        log.info("💬 Broadcast message from [{}] to channel [{}]", saved.getSender(), destination);

        // Auto-reply with VIP Concierge Butler if sender is GUEST
        if ("GUEST".equalsIgnoreCase(saved.getSenderRole())) {
            triggerButlerAutoResponse(saved);
        }
    }

    private void triggerButlerAutoResponse(ChatMessage guestMsg) {
        CompletableFuture.delayedExecutor(600, TimeUnit.MILLISECONDS).execute(() -> {
            try {
                String replyContent = generateButlerReply(guestMsg.getContent(), guestMsg.getSender());
                ChatMessage butlerReply = new ChatMessage(
                        guestMsg.getChannelId(),
                        "Lord Alistair (Head Butler)",
                        "BUTLER_BOT",
                        replyContent
                );
                ChatMessage savedReply = chatMessageRepository.save(butlerReply);
                String destination = "/topic/channel/" + guestMsg.getChannelId();
                messagingTemplate.convertAndSend(destination, savedReply);
                log.info("🤖 VIP Butler bot replied to channel [{}]", destination);
            } catch (Exception e) {
                log.error("Failed to generate butler auto-response", e);
            }
        });
    }

    private String generateButlerReply(String content, String guestName) {
        String lower = content != null ? content.toLowerCase(Locale.ROOT) : "";
        if (lower.contains("wine") || lower.contains("champagne") || lower.contains("drink")) {
            return "Good evening, " + guestName + ". Our master sommelier has chilled a vintage bottle of Dom Pérignon. Shall I have our steward present it to your suite?";
        } else if (lower.contains("dining") || lower.contains("dinner") || lower.contains("food") || lower.contains("chef")) {
            return "It would be an honor to arrange a private culinary showcase by Chef Antoine on your suite's private terrace. What time suits your party?";
        } else if (lower.contains("transfer") || lower.contains("heli") || lower.contains("airport") || lower.contains("car")) {
            return "Your Rolls-Royce Phantom and helipad transfers are on standby with our concierge dispatch. Please advise your flight coordinates.";
        } else if (lower.contains("spa") || lower.contains("massage") || lower.contains("relax")) {
            return "Our Celestial Spa offers private en-suite aromatherapy and Himalayan hot stone treatments. I have reserved an appointment slot for you.";
        } else if (lower.contains("wifi") || lower.contains("internet") || lower.contains("password")) {
            return "High-speed Starlink Quantum connectivity is active across all residences: Network 'Starlight-VIP-Resident' (autologin enabled).";
        } else {
            return "Greetings, " + guestName + ". I am Lord Alistair, your personal head butler. How may our concierge team elevate your luxury stay at Starlight Stays today?";
        }
    }
}
