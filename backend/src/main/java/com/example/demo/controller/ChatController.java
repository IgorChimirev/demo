package com.example.demo.controller;

import com.example.demo.dto.ChatRequest;
import com.example.demo.entity.ChatSession;
import com.example.demo.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {
    private final ChatService chatService;



    @PostMapping("/message")


    public ResponseEntity<Void> sendMessage(@RequestBody ChatRequest request) {
        try {
            chatService.sendMessage(
                    request.getSessionId(),
                    request.getUserId(),
                    request.getMessage(),
                    request.getFileId(),
                    request.getFileType(),
                    request.getCaption()
            );
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error sending message", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/close")
    public ResponseEntity<Void> initiateCloseChat(@RequestBody ChatRequest request) {
        try {
            boolean success = chatService.initiateCloseChat(request.getSessionId(), request.getUserId());
            return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error initiating chat close", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/approve-close")
    public ResponseEntity<Void> approveCloseChat(@RequestBody ChatRequest request) {
        try {
            boolean success = chatService.approveCloseChat(request.getSessionId(), request.getUserId());
            return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error approving chat close", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/switch")
    public ResponseEntity<Void> switchSession(@RequestBody ChatRequest request) {
        try {
            chatService.switchUserSession(request.getUserId(), request.getSessionId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error switching session", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/sessions/{userId}")
    public ResponseEntity<List<ChatSession>> getUserSessions(@PathVariable Long userId) {
        try {
            List<ChatSession> sessions = chatService.getUserActiveSessions(userId);
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            log.error("Error getting user sessions", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/current-session/{userId}")
    public ResponseEntity<Map<String, String>> getCurrentSession(@PathVariable Long userId) {
        try {
            String sessionId = chatService.getUserCurrentSession(userId);
            return ResponseEntity.ok(Map.of("sessionId", sessionId != null ? sessionId : ""));
        } catch (Exception e) {
            log.error("Error getting current session", e);
            return ResponseEntity.badRequest().build();
        }
    }

}