package com.example.demo.service;

import com.example.demo.entity.ChatSession;
import com.example.demo.TelegramBot.TelegramBotService;
import com.example.demo.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatSessionRepository chatSessionRepository;
    private final TelegramBotService telegramBotService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String USER_CURRENT_SESSION_KEY = "user:current:session:";
    private static final String USER_ACTIVE_SESSIONS_KEY = "user:active:sessions:";

    public void sendMessage(String sessionId, Long fromUserId, String message, String fileId, String fileType, String caption) {
        if (fileId != null && !fileId.trim().isEmpty()) {
            // Если есть файл, отправляем файл с подписью
            sendFile(sessionId, fromUserId, fileId, fileType,
                    (message != null && !message.trim().isEmpty()) ? message : caption);
        } else if (message != null && !message.trim().isEmpty()) {
            // Если есть только текст, отправляем текст
            sendTextMessage(sessionId, fromUserId, message);
        }
    }

    public void sendFile(String sessionId, Long fromUserId, String fileId, String fileType, String caption) {
        log.debug("Sending file in session {} from user {}: {} (type: {})",
                sessionId, fromUserId, fileId, fileType);

        try {
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
            if (sessionOpt.isEmpty()) {
                throw new RuntimeException("Session not found: " + sessionId);
            }

            ChatSession session = sessionOpt.get();

            if (!"ACTIVE".equals(session.getStatus())) {
                throw new RuntimeException("Chat session is not active. Current status: " + session.getStatus());
            }

            if (!isUserInSession(session, fromUserId)) {
                throw new RuntimeException("User " + fromUserId + " is not a participant of session " + sessionId);
            }

            Long toUserId = session.getOtherUserId(fromUserId);
            String fromTempId = session.getUserTempId(fromUserId);
            String toTempId = session.getUserTempId(toUserId);

            if (toUserId == null) {
                throw new RuntimeException("Could not find recipient user in session");
            }

            // Отправка файла получателю
            String messagePrefix = getFileTypePrefix(fileType) + " от " + fromTempId;
            if (caption != null && !caption.trim().isEmpty()) {
                messagePrefix += ":\n" + caption;
            }

            telegramBotService.sendFileAsync(toUserId, fileId, fileType, messagePrefix);

            // Подтверждение отправителю
            String confirmationMessage = "✅ " + getFileTypeConfirmation(fileType) + " доставлено в чат с " + toTempId;
            telegramBotService.sendMessageAsync(fromUserId, confirmationMessage);

            // Обновление активности
            session.setLastActivity(LocalDateTime.now());
            chatSessionRepository.save(session);

            log.info("File delivered successfully in session {} from {} to {}",
                    sessionId, fromUserId, toUserId);

        } catch (Exception e) {
            log.error("Error sending file in session {} from user {}: {}",
                    sessionId, fromUserId, e.getMessage(), e);
            throw new RuntimeException("Failed to send file: " + e.getMessage());
        }
    }

    private String getFileTypePrefix(String fileType) {
        switch (fileType) {
            case "photo": return "📷 Фото";
            case "document": return "📎 Файл";
            case "voice": return "🎤 Голосовое сообщение";
            case "video": return "🎥 Видео";
            case "audio": return "🎵 Аудио";
            default: return "📁 Файл";
        }
    }

    private String getFileTypeConfirmation(String fileType) {
        switch (fileType) {
            case "photo": return "Фото";
            case "document": return "Файл";
            case "voice": return "Голосовое сообщение";
            case "video": return "Видео";
            case "audio": return "Аудио";
            default: return "Файл";
        }
    }

    private void sendTextMessage(String sessionId, Long fromUserId, String message) {
        log.debug("Sending message in session {} from user {}: {}", sessionId, fromUserId, message);

        try {
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
            if (sessionOpt.isEmpty()) {
                throw new RuntimeException("Session not found: " + sessionId);
            }

            ChatSession session = sessionOpt.get();

            if (!"ACTIVE".equals(session.getStatus())) {
                throw new RuntimeException("Chat session is not active. Current status: " + session.getStatus());
            }

            if (!isUserInSession(session, fromUserId)) {
                throw new RuntimeException("User " + fromUserId + " is not a participant of session " + sessionId);
            }

            Long toUserId = session.getOtherUserId(fromUserId);
            String fromTempId = session.getUserTempId(fromUserId);
            String toTempId = session.getUserTempId(toUserId);

            if (toUserId == null) {
                throw new RuntimeException("Could not find recipient user in session");
            }

            String messageToRecipient = "💬 Сообщение от " + fromTempId + ":\n" + message;
            telegramBotService.sendMessageAsync(toUserId, messageToRecipient);

            String confirmationMessage = "✅ Сообщение доставлено в чат с " + toTempId;
            telegramBotService.sendMessageAsync(fromUserId, confirmationMessage);

            session.setLastActivity(LocalDateTime.now());
            chatSessionRepository.save(session);

            log.info("Message delivered successfully in session {} from {} to {}",
                    sessionId, fromUserId, toUserId);

        } catch (Exception e) {
            log.error("Error sending message in session {} from user {}: {}",
                    sessionId, fromUserId, e.getMessage(), e);
            throw new RuntimeException("Failed to send message: " + e.getMessage());
        }
    }

    public ChatSession createChatSession(Long user1Id, Long user2Id, Long orderId) {
        log.info("Creating chat session between user1: {} and user2: {}", user1Id, user2Id);

        try {
            Optional<ChatSession> existingSession = findActiveSessionBetweenUsers(user1Id, user2Id, orderId);
            if (existingSession.isPresent()) {
                log.info("Active session already exists: {}", existingSession.get().getSessionId());
                return existingSession.get();
            }

            ChatSession session = new ChatSession();
            session.setSessionId(generateSessionId());
            session.setUser1Id(user1Id);
            session.setUser2Id(user2Id);
            session.setOrderId(orderId);
            session.setUser1TempId(generateTempId("user"));
            session.setUser2TempId(generateTempId("user"));
            session.setStatus("ACTIVE");
            session.setCloseApprovals(new HashSet<>());
            session.setCompletionApprovals(new HashSet<>());
            session.setPaid(false);

            chatSessionRepository.save(session);
            log.info("Chat session created successfully: {}", session.getSessionId());

            setUserCurrentSession(user1Id, session.getSessionId());
            setUserCurrentSession(user2Id, session.getSessionId());

            addToUserActiveSessions(user1Id, session.getSessionId());
            addToUserActiveSessions(user2Id, session.getSessionId());

            sendWelcomeMessages(session);

            return session;

        } catch (Exception e) {
            log.error("Error creating chat session between {} and {}: {}", user1Id, user2Id, e.getMessage(), e);
            throw new RuntimeException("Failed to create chat session: " + e.getMessage());
        }
    }

    public boolean handlePayCommand(String sessionId, Long userId, Long orderId) {
        log.info("Processing pay command for session {} by user {}", sessionId, userId);

        try {
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
            if (sessionOpt.isEmpty()) {
                throw new RuntimeException("Session not found: " + sessionId);
            }

            ChatSession session = sessionOpt.get();

            if (!isUserInSession(session, userId)) {
                throw new RuntimeException("User is not a participant of this session");
            }

            if (session.getPaid()) {
                log.info("Session {} already paid", sessionId);
                return false;
            }

            session.setPaid(true);
            session.setLastActivity(LocalDateTime.now());
            chatSessionRepository.save(session);

            String payerTempId = session.getUserTempId(userId);
            String otherUserTempId = session.getOtherUserTempId(userId);
            Long otherUserId = session.getOtherUserId(userId);

            String payerMessage = "✅ Вы подтвердили оплату заказа. Ожидайте подтверждения выполнения условий от исполнителя.";
            String otherUserMessage = "✅ Заказчик " + payerTempId + " подтвердил оплату.\n\n" +
                    "После выполнения работы подтвердите выполнение условий командой:\n" +
                    "/confirm_completion\n\n" +
                    "После этого заказчик также должен подтвердить выполнение условий, и тогда чат можно будет закрыть.";

            telegramBotService.sendMessageAsync(userId, payerMessage);
            telegramBotService.sendMessageAsync(otherUserId, otherUserMessage);

            log.info("Payment confirmed for session {} by user {}", sessionId, userId);
            return true;

        } catch (Exception e) {
            log.error("Error processing pay command for session {} by user {}: {}", sessionId, userId, e.getMessage(), e);
            throw new RuntimeException("Failed to process payment: " + e.getMessage());
        }
    }

    public boolean handleConfirmCompletion(String sessionId, Long userId) {
        log.info("Processing completion confirmation for session {} by user {}", sessionId, userId);

        try {
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
            if (sessionOpt.isEmpty()) {
                throw new RuntimeException("Session not found: " + sessionId);
            }

            ChatSession session = sessionOpt.get();

            if (!isUserInSession(session, userId)) {
                throw new RuntimeException("User is not a participant of this session");
            }

            if (!session.getPaid()) {
                String errorMessage = "❌ Подтверждение выполнения условий возможно только после оплаты заказа.\n\n" +
                        "Сначала должна быть произведена оплата с помощью команды /pay";
                telegramBotService.sendMessageAsync(userId, errorMessage);
                return false;
            }

            if (session.getCompletionApprovals() == null) {
                session.setCompletionApprovals(new HashSet<>());
            }

            if (!session.getCompletionApprovals().contains(userId)) {
                session.getCompletionApprovals().add(userId);
            }

            session.setLastActivity(LocalDateTime.now());
            chatSessionRepository.save(session);

            if (session.getCompletionApprovals().size() >= 2) {
                String completionMessage = "✅ Обе стороны подтвердили выполнение условий заказа. Теперь чат можно закрыть командой /close_chat";
                telegramBotService.sendMessageAsync(session.getUser1Id(), completionMessage);
                telegramBotService.sendMessageAsync(session.getUser2Id(), completionMessage);

                log.info("Both users confirmed completion for session {}", sessionId);
                return true;
            } else {
                Long otherUserId = session.getOtherUserId(userId);
                String userTempId = session.getUserTempId(userId);

                String waitingMessage = "✅ " + userTempId + " подтвердил выполнение условий. " +
                        "Ожидаем подтверждение от второй стороны.\n\n" +
                        "После подтверждения обеими сторонами чат можно будет закрыть.";

                telegramBotService.sendMessageAsync(otherUserId, waitingMessage);
                telegramBotService.sendMessageAsync(userId,
                        "✅ Вы подтвердили выполнение условий. Ожидаем подтверждение от второй стороны.");

                log.info("Completion confirmation received for session {}, waiting for second user", sessionId);
                return true;
            }

        } catch (Exception e) {
            log.error("Error processing completion confirmation for session {} by user {}: {}",
                    sessionId, userId, e.getMessage(), e);
            return false;
        }
    }

    public boolean initiateCloseChat(String sessionId, Long userId) {
        log.info("Initiating chat close for session {} by user {}", sessionId, userId);

        try {
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
            if (sessionOpt.isEmpty()) {
                throw new RuntimeException("Session not found: " + sessionId);
            }

            ChatSession session = sessionOpt.get();

            if (!isUserInSession(session, userId)) {
                throw new RuntimeException("User is not a participant of this session");
            }

            if (session.getPaid()) {
                if (session.getCompletionApprovals() == null || session.getCompletionApprovals().size() < 2) {
                    String errorMessage = "❌ Нельзя закрыть чат до подтверждения выполнения условий обеими сторонами.\n\n" +
                            "Используйте команду /confirm_completion после выполнения работы.\n" +
                            "Текущий статус подтверждений: " +
                            (session.getCompletionApprovals() != null ? session.getCompletionApprovals().size() : 0) + "/2";
                    telegramBotService.sendMessageAsync(userId, errorMessage);
                    return false;
                }
            }

            if (session.getCloseApprovals() == null) {
                session.setCloseApprovals(new HashSet<>());
            }

            if (!session.getCloseApprovals().contains(userId)) {
                session.getCloseApprovals().add(userId);
            }

            chatSessionRepository.save(session);

            Long otherUserId = session.getOtherUserId(userId);
            String userTempId = session.getUserTempId(userId);

            String notificationMessage = "⚠️ Пользователь " + userTempId +
                    " предложил закрыть чат.\nДля подтверждения отправьте команду: /approve_close";

            telegramBotService.sendMessageAsync(otherUserId, notificationMessage);

            telegramBotService.sendMessageAsync(userId,
                    "✅ Запрос на закрытие чата отправлен. Ожидайте подтверждения от второго участника.");

            log.info("Close chat initiated successfully for session {}", sessionId);
            return true;

        } catch (Exception e) {
            log.error("Error initiating chat close for session {} by user {}: {}",
                    sessionId, userId, e.getMessage(), e);
            return false;
        }
    }

    public boolean approveCloseChat(String sessionId, Long userId) {
        log.info("Approving chat close for session {} by user {}", sessionId, userId);

        try {
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
            if (sessionOpt.isEmpty()) {
                throw new RuntimeException("Session not found: " + sessionId);
            }

            ChatSession session = sessionOpt.get();

            if (!isUserInSession(session, userId)) {
                throw new RuntimeException("User is not a participant of this session");
            }

            if (session.getPaid()) {
                if (session.getCompletionApprovals() == null || session.getCompletionApprovals().size() < 2) {
                    String errorMessage = "❌ Нельзя закрыть чат до подтверждения выполнения условий обеими сторонами.\n\n" +
                            "Используйте команду /confirm_completion после выполнения работы.\n" +
                            "Текущий статус подтверждений: " +
                            (session.getCompletionApprovals() != null ? session.getCompletionApprovals().size() : 0) + "/2";
                    telegramBotService.sendMessageAsync(userId, errorMessage);
                    return false;
                }
            }

            if (session.getCloseApprovals() == null) {
                session.setCloseApprovals(new HashSet<>());
            }

            if (!session.getCloseApprovals().contains(userId)) {
                session.getCloseApprovals().add(userId);
            }

            if (session.getCloseApprovals().size() >= 2) {
                session.setStatus("CLOSED");
                session.setLastActivity(LocalDateTime.now());

                String closeMessage = "❌ Чат закрыт по взаимному согласию.";
                telegramBotService.sendMessageAsync(session.getUser1Id(), closeMessage);
                telegramBotService.sendMessageAsync(session.getUser2Id(), closeMessage);

                removeUserCurrentSession(session.getUser1Id());
                removeUserCurrentSession(session.getUser2Id());

                removeFromUserActiveSessions(session.getUser1Id(), sessionId);
                removeFromUserActiveSessions(session.getUser2Id(), sessionId);

                log.info("Chat session {} closed successfully", sessionId);
            } else {
                Long otherUserId = session.getOtherUserId(userId);
                String userTempId = session.getUserTempId(userId);

                String waitingMessage = "✅ " + userTempId + " подтвердил закрытие чата. " +
                        "Ожидаем подтверждение от второго участника.";

                telegramBotService.sendMessageAsync(otherUserId, waitingMessage);
                telegramBotService.sendMessageAsync(userId,
                        "✅ Вы подтвердили закрытие чата. Ожидаем подтверждение от второго участника.");

                log.info("Close approval received for session {}, waiting for second user", sessionId);
            }

            chatSessionRepository.save(session);
            return true;

        } catch (Exception e) {
            log.error("Error approving chat close for session {} by user {}: {}",
                    sessionId, userId, e.getMessage(), e);
            return false;
        }
    }

    public String getChatStatus(String sessionId, Long userId) {
        try {
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
            if (sessionOpt.isEmpty()) {
                return "Сессия не найдена";
            }

            ChatSession session = sessionOpt.get();
            if (!isUserInSession(session, userId)) {
                return "Вы не являетесь участником этого чата";
            }

            StringBuilder status = new StringBuilder();
            status.append("📊 Статус чата:\n\n");

            if (session.getPaid()) {
                status.append("💰 Оплата: ✅ подтверждена\n");
                int completions = session.getCompletionApprovals() != null ? session.getCompletionApprovals().size() : 0;
                status.append("✅ Подтверждения выполнения: ").append(completions).append("/2\n");

                if (completions < 2) {
                    status.append("\nОжидается подтверждение выполнения условий от ");
                    if (completions == 0) {
                        status.append("обеих сторон");
                    } else {
                        Set<Long> approved = session.getCompletionApprovals();
                        if (approved.contains(session.getUser1Id()) && !approved.contains(session.getUser2Id())) {
                            status.append(session.getUser2TempId());
                        } else if (!approved.contains(session.getUser1Id()) && approved.contains(session.getUser2Id())) {
                            status.append(session.getUser1TempId());
                        }
                    }
                } else {
                    status.append("\n✅ Все условия выполнены. Чат можно закрыть.");
                }
            } else {
                status.append("💰 Оплата: ❌ не подтверждена\n");
                status.append("\nДля использования команды /confirm_completion сначала должна быть произведена оплата.");
            }

            return status.toString();

        } catch (Exception e) {
            log.error("Error getting chat status for session {}: {}", sessionId, e.getMessage());
            return "Ошибка при получении статуса чата";
        }
    }

    public void switchUserSession(Long userId, String sessionId) {
        log.debug("Switching user {} to session {}", userId, sessionId);

        try {
            Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
            if (sessionOpt.isEmpty()) {
                throw new RuntimeException("Session not found: " + sessionId);
            }

            ChatSession session = sessionOpt.get();

            if (!isUserInSession(session, userId)) {
                throw new RuntimeException("User is not a participant of this session");
            }

            if (!"ACTIVE".equals(session.getStatus())) {
                throw new RuntimeException("Cannot switch to inactive session");
            }

            setUserCurrentSession(userId, sessionId);

            log.info("User {} switched to session {}", userId, sessionId);

        } catch (Exception e) {
            log.error("Error switching user {} to session {}: {}",
                    userId, sessionId, e.getMessage(), e);
            throw new RuntimeException("Failed to switch session: " + e.getMessage());
        }
    }

    public String getUserCurrentSession(Long userId) {
        try {
            String sessionId = (String) redisTemplate.opsForValue().get(USER_CURRENT_SESSION_KEY + userId);
            if (sessionId != null) {
                Optional<ChatSession> session = chatSessionRepository.findById(sessionId);
                if (session.isPresent() && "ACTIVE".equals(session.get().getStatus())) {
                    return sessionId;
                } else {
                    removeUserCurrentSession(userId);
                    return null;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Error getting current session for user {}: {}", userId, e.getMessage(), e);
            return null;
        }
    }

    public List<ChatSession> getUserActiveSessions(Long userId) {
        try {
            List<Object> sessionIdObjects = redisTemplate.opsForList().range(USER_ACTIVE_SESSIONS_KEY + userId, 0, -1);

            if (sessionIdObjects == null || sessionIdObjects.isEmpty()) {
                return List.of();
            }

            List<String> sessionIds = new ArrayList<>();
            for (Object obj : sessionIdObjects) {
                if (obj instanceof String) {
                    sessionIds.add((String) obj);
                }
            }

            if (sessionIds.isEmpty()) {
                return List.of();
            }

            List<ChatSession> activeSessions = new ArrayList<>();
            for (String sessionId : sessionIds) {
                Optional<ChatSession> sessionOpt = chatSessionRepository.findById(sessionId);
                if (sessionOpt.isPresent() && "ACTIVE".equals(sessionOpt.get().getStatus())) {
                    activeSessions.add(sessionOpt.get());
                }
            }

            return activeSessions;

        } catch (Exception e) {
            log.error("Error getting active sessions for user {}: {}", userId, e.getMessage(), e);
            return List.of();
        }
    }

    public Long getOrderId(String sessionId) {
        Long orderId = null;
        try {
            Optional<ChatSession> session = chatSessionRepository.findById(sessionId);
            if (session.isPresent()) {
                orderId = session.get().getOrderId();
            }
        } catch (Exception e) {
            log.error("Error getting order id for session {}: {}", sessionId, e.getMessage());
        }
        log.debug("Order ID for session {}: {}", sessionId, orderId);
        return orderId;
    }

    public Long getSecondUserId(String sessionId) {
        Long secondUserId = null;
        try {
            Optional<ChatSession> session = chatSessionRepository.findById(sessionId);
            if (session.isPresent()) {
                secondUserId = session.get().getUser2Id();
            }
        } catch (Exception e) {
            log.error("Error getting secondUserId for session {}: {}", sessionId, e.getMessage());
        }
        return secondUserId;
    }

    public Long getFirstUserId(String sessionId) {
        Long firstUserId = null;
        try {
            Optional<ChatSession> session = chatSessionRepository.findById(sessionId);
            if (session.isPresent()) {
                firstUserId = session.get().getUser1Id();
            }
        } catch (Exception e) {
            log.error("Error getting first UserId for session {}: {}", sessionId, e.getMessage());
        }
        return firstUserId;
    }

    private Optional<ChatSession> findActiveSessionBetweenUsers(Long user1Id, Long user2Id, Long orderId) {
        List<ChatSession> user1Sessions = getUserActiveSessions(user1Id);
        return user1Sessions.stream()
                .filter(session -> isUserInSession(session, user2Id) &&
                        orderId.equals(session.getOrderId()))
                .findFirst();
    }

    private boolean isUserInSession(ChatSession session, Long userId) {
        return userId.equals(session.getUser1Id()) || userId.equals(session.getUser2Id());
    }

    private void setUserCurrentSession(Long userId, String sessionId) {
        try {
            redisTemplate.opsForValue().set(
                    USER_CURRENT_SESSION_KEY + userId,
                    sessionId,
                    7, TimeUnit.DAYS
            );
            log.debug("Set current session for user {}: {}", userId, sessionId);
        } catch (Exception e) {
            log.error("Error setting current session for user {}: {}", userId, e.getMessage(), e);
        }
    }

    private void removeUserCurrentSession(Long userId) {
        try {
            redisTemplate.delete(USER_CURRENT_SESSION_KEY + userId);
            log.debug("Removed current session for user {}", userId);
        } catch (Exception e) {
            log.error("Error removing current session for user {}: {}", userId, e.getMessage(), e);
        }
    }

    private void addToUserActiveSessions(Long userId, String sessionId) {
        try {
            String key = USER_ACTIVE_SESSIONS_KEY + userId;

            redisTemplate.opsForList().remove(key, 0, sessionId);

            redisTemplate.opsForList().rightPush(key, sessionId);

            redisTemplate.expire(key, 7, TimeUnit.DAYS);
            log.debug("Added session {} to active sessions for user {}", sessionId, userId);
        } catch (Exception e) {
            log.error("Error adding session to active sessions for user {}: {}", userId, e.getMessage(), e);
        }
    }

    private void removeFromUserActiveSessions(Long userId, String sessionId) {
        try {
            redisTemplate.opsForList().remove(USER_ACTIVE_SESSIONS_KEY + userId, 0, sessionId);
            log.debug("Removed session {} from active sessions for user {}", sessionId, userId);
        } catch (Exception e) {
            log.error("Error removing session from active sessions for user {}: {}", userId, e.getMessage(), e);
        }
    }

    private void sendWelcomeMessages(ChatSession session) {
        try {
            String messageToUser1 = "🎉 Чат создан! Вы общаетесь с: " + session.getUser2TempId() +
                    "\n\nТеперь вы можете отправлять:\n" +
                    "💬 Текстовые сообщения\n" +
                    "📷 Фотографии\n" +
                    "📎 Файлы и документы\n" +
                    "🎤 Голосовые сообщения\n\n" +
                    "Просто отправляйте сообщения или файлы, и они будут доставлены анонимно." +
                    "\n\nДля управления чатом используйте команды:" +
                    "\n/sessions - список ваших чатов" +
                    "\n/pay - подтвердить оплату заказа" +
                    "\n/confirm_completion - подтвердить выполнение условий (после оплаты)" +
                    "\n/status - показать статус текущего чата" +
                    "\n/close_chat - предложить закрыть чат";

            String messageToUser2 = "🎉 Чат создан! Вы общаетесь с: " + session.getUser1TempId() +
                    "\n\nТеперь вы можете отправлять:\n" +
                    "💬 Текстовые сообщения\n" +
                    "📷 Фотографии\n" +
                    "📎 Файлы и документы\n" +
                    "🎤 Голосовые сообщения\n\n" +
                    "Просто отправляйте сообщения или файлы, и они будут доставлены анонимно." +
                    "\n\nДля управления чатом используйте команды:" +
                    "\n/sessions - список ваших чатов" +
                    "\n/pay - подтвердить оплату заказа" +
                    "\n/confirm_completion - подтвердить выполнение условий (после оплаты)" +
                    "\n/status - показать статус текущего чата" +
                    "\n/close_chat - предложить закрыть чат";

            telegramBotService.sendMessageAsync(session.getUser1Id(), messageToUser1);
            telegramBotService.sendMessageAsync(session.getUser2Id(), messageToUser2);

            log.info("Welcome messages sent for session {}", session.getSessionId());

        } catch (Exception e) {
            log.error("Error sending welcome messages for session {}: {}",
                    session.getSessionId(), e.getMessage(), e);
        }
    }

    private String generateSessionId() {
        return "session_" + UUID.randomUUID().toString();
    }

    private String generateTempId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}