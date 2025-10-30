package com.example.demo.TelegramBot;

import com.example.demo.dto.PaymentRequest;
import com.example.demo.dto.TinkoffInitResponse;
import com.example.demo.entity.ChatSession;
import com.example.demo.service.ChatService;
import com.example.demo.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.springframework.context.annotation.Lazy;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class TelegramBotService extends TelegramLongPollingBot {

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Value("${telegram.bot.username:}")
    private String botUsername;

    private final ChatService chatService;
    private final OrderService orderService;
    private final ExecutorService messageExecutor = Executors.newFixedThreadPool(8);

    public TelegramBotService(@Lazy ChatService chatService, @Lazy OrderService orderService, @Value("${telegram.bot.token}") String botToken) {
        super(botToken);
        this.chatService = chatService;
        this.orderService = orderService;
    }

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        CompletableFuture.runAsync(() -> processUpdate(update), messageExecutor);
    }

    private void processUpdate(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }

        if (update.hasMessage()) {
            Long chatId = update.getMessage().getChatId();

            // Обработка текстовых сообщений
            if (update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                log.debug("Received message from {}: {}", chatId, messageText);
                processTextMessage(chatId, messageText);
            }
            // Обработка фото
            else if (update.getMessage().hasPhoto()) {
                handlePhotoMessage(update.getMessage());
            }
            // Обработка документов
            else if (update.getMessage().hasDocument()) {
                handleDocumentMessage(update.getMessage());
            }
            // Обработка голосовых сообщений
            else if (update.getMessage().hasVoice()) {
                handleVoiceMessage(update.getMessage());
            }
            // Обработка видео
            else if (update.getMessage().hasVideo()) {
                handleVideoMessage(update.getMessage());
            }
            // Обработка аудио
            else if (update.getMessage().hasAudio()) {
                handleAudioMessage(update.getMessage());
            }
        }
    }

    private void processTextMessage(Long chatId, String messageText) {
        if (messageText.startsWith("/start")) {
            sendWelcomeMessage(chatId);
        } else if (messageText.startsWith("/close_chat")) {
            handleCloseChat(chatId);
        } else if (messageText.startsWith("/approve_close")) {
            handleApproveClose(chatId);
        } else if (messageText.startsWith("/sessions")) {
            handleListSessions(chatId);
        } else if (messageText.startsWith("/switch_")) {
            handleSwitchSession(chatId, messageText);
        } else if (messageText.startsWith("/pay")) {
            handlePayCommand(chatId);
        } else if (messageText.startsWith("/confirm_completion")) {
            handleConfirmCompletion(chatId);
        } else if (messageText.startsWith("/status")) {
            handleStatusCommand(chatId);
        } else {
            handleChatMessage(chatId, messageText, null, null, null);
        }
    }

    private void handlePhotoMessage(org.telegram.telegrambots.meta.api.objects.Message message) {
        Long chatId = message.getChatId();
        // Берем самое большое фото (последний элемент в списке)
        String fileId = message.getPhoto().get(message.getPhoto().size() - 1).getFileId();
        String caption = message.getCaption();

        handleChatMessage(chatId, null, fileId, "photo", caption);
    }

    private void handleDocumentMessage(org.telegram.telegrambots.meta.api.objects.Message message) {
        Long chatId = message.getChatId();
        String fileId = message.getDocument().getFileId();
        String caption = message.getCaption();
        String fileName = message.getDocument().getFileName();

        // Можно добавить информацию о имени файла в caption
        String fullCaption = (caption != null ? caption + "\n" : "") + "📎 " + fileName;

        handleChatMessage(chatId, null, fileId, "document", fullCaption);
    }

    private void handleVoiceMessage(org.telegram.telegrambots.meta.api.objects.Message message) {
        Long chatId = message.getChatId();
        String fileId = message.getVoice().getFileId();
        String caption = message.getCaption();

        handleChatMessage(chatId, null, fileId, "voice", caption);
    }

    private void handleVideoMessage(org.telegram.telegrambots.meta.api.objects.Message message) {
        Long chatId = message.getChatId();
        String fileId = message.getVideo().getFileId();
        String caption = message.getCaption();

        handleChatMessage(chatId, null, fileId, "video", caption);
    }

    private void handleAudioMessage(org.telegram.telegrambots.meta.api.objects.Message message) {
        Long chatId = message.getChatId();
        String fileId = message.getAudio().getFileId();
        String caption = message.getCaption();

        handleChatMessage(chatId, null, fileId, "audio", caption);
    }

    private void handleStatusCommand(Long userId) {
        String currentSessionId = chatService.getUserCurrentSession(userId);
        if (currentSessionId != null) {
            try {
                String status = chatService.getChatStatus(currentSessionId, userId);
                sendMessageAsync(userId, status);
            } catch (Exception e) {
                sendMessageAsync(userId, "❌ Ошибка при получении статуса чата: " + e.getMessage());
            }
        } else {
            sendMessageAsync(userId, "У вас нет активного чата.");
        }
    }

    private void sendWelcomeMessage(Long chatId) {
        String welcomeText = """
            👋 Добро пожаловать в анонимный чат!
            
            Теперь вы можете отправлять:
            💬 Текстовые сообщения
            📷 Фотографии
            📎 Файлы и документы
            🎤 Голосовые сообщения
            🎥 Видео
            🎵 Аудио
            
            Доступные команды:
            /sessions - список активных чатов
            /pay - подтвердить оплату заказа
            /confirm_completion - подтвердить выполнение условий (после оплаты)
            /status - показать статус текущего чата
            /close_chat - предложить закрыть текущий чат
            /approve_close - подтвердить закрытие чата
            
            Просто отправляйте сообщения или файлы, и они будут доставлены анонимно.
            """;

        sendMessageAsync(chatId, welcomeText);
    }

    private void handlePayCommand(Long userId) {
        String currentSessionId = chatService.getUserCurrentSession(userId);
        Long SecondUserId = chatService.getSecondUserId(currentSessionId);
        Long FirstUserId = chatService.getFirstUserId(currentSessionId);
        if (currentSessionId != null) {
            try {
                Long orderId = chatService.getOrderId(currentSessionId);
                String amount = orderService.getAmountByOrderId(orderId);
                boolean success = chatService.handlePayCommand(String.valueOf(currentSessionId), userId, orderId);
                if (!success) {
                    sendMessageAsync(userId, "❌ Оплата уже была подтверждена ранее.");
                } else {
                    PaymentRequest paymentRequest = new PaymentRequest();
                    paymentRequest.setAmount(Long.valueOf(amount) * 1000);
                    paymentRequest.setUserIdFrom(String.valueOf(FirstUserId));
                    paymentRequest.setUserIdTo(String.valueOf(SecondUserId));
                    System.out.println(orderId);
                    paymentRequest.setOrderNumber(String.valueOf(orderId));
                    paymentRequest.setDate(LocalDate.now().toString());

                    ResponseEntity<TinkoffInitResponse> response = restTemplate.postForEntity(
                            "http://localhost:80/api/payment/create-payment-link",
                            paymentRequest,
                            TinkoffInitResponse.class
                    );
                    if (response.getBody().getSuccess().equals("true")) {
                        SendMessage message = new SendMessage();
                        message.setChatId(userId.toString());
                        message.setText("Ссылка на оплату вашего заказа в Т-банке");

                        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
                        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                        List<InlineKeyboardButton> row = new ArrayList<>();

                        InlineKeyboardButton urlButton = new InlineKeyboardButton();
                        urlButton.setText("Перейти для оплаты заказа");
                        urlButton.setUrl(response.getBody().getPaymentURL());

                        row.add(urlButton);
                        rows.add(row);
                        keyboard.setKeyboard(rows);
                        message.setReplyMarkup(keyboard);

                        try {
                            execute(message);
                        } catch (TelegramApiException e) {
                            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
                        }
                    } else {
                        sendMessageAsync(userId, response.getBody().getMessage());
                    }
                }

            } catch (Exception e) {
                sendMessageAsync(userId, "❌ Ошибка при обработке команды оплаты: " + e.getMessage());
            }
        } else {
            sendMessageAsync(userId, "У вас нет активного чата.");
        }
    }

    private void handleConfirmCompletion(Long userId) {
        String currentSessionId = chatService.getUserCurrentSession(userId);
        if (currentSessionId != null) {
            try {
                boolean success = chatService.handleConfirmCompletion(currentSessionId, userId);
                if (!success) {
                    sendMessageAsync(userId, "❌ Ошибка при подтверждении выполнения условий.");
                }
            } catch (Exception e) {
                sendMessageAsync(userId, "❌ Ошибка при подтверждении выполнения условий: " + e.getMessage());
            }
        } else {
            sendMessageAsync(userId, "У вас нет активного чата.");
        }
    }

    private void handleCloseChat(Long userId) {
        String currentSessionId = chatService.getUserCurrentSession(userId);
        if (currentSessionId != null) {
            chatService.initiateCloseChat(currentSessionId, userId);
        } else {
            sendMessageAsync(userId, "У вас нет активного чата для закрытия.");
        }
    }

    private void handleApproveClose(Long userId) {
        String currentSessionId = chatService.getUserCurrentSession(userId);
        if (currentSessionId != null) {
            chatService.approveCloseChat(currentSessionId, userId);
        } else {
            sendMessageAsync(userId, "У вас нет активного чата.");
        }
    }

    private void handleListSessions(Long userId) {
        var sessions = chatService.getUserActiveSessions(userId);
        String currentSessionId = chatService.getUserCurrentSession(userId);

        if (sessions.isEmpty()) {
            sendMessageAsync(userId, "У вас нет активных чатов.");
            return;
        }

        String messageText = buildSessionsText(userId, sessions, currentSessionId);
        InlineKeyboardMarkup keyboard = createSessionsKeyboard(userId, sessions, currentSessionId);

        sendMessageWithKeyboard(userId, messageText, keyboard);
    }

    private void handleSwitchSession(Long userId, String messageText) {
        try {
            String[] parts = messageText.split("_");
            if (parts.length >= 2) {
                int sessionIndex = Integer.parseInt(parts[1]) - 1;
                var sessions = chatService.getUserActiveSessions(userId);

                if (sessionIndex >= 0 && sessionIndex < sessions.size()) {
                    String sessionId = sessions.get(sessionIndex).getSessionId();
                    chatService.switchUserSession(userId, sessionId);

                    handleListSessions(userId);
                } else {
                    sendMessageAsync(userId, "Неверный номер чата.");
                }
            }
        } catch (NumberFormatException e) {
            sendMessageAsync(userId, "Неверный формат команды. Используйте: /switch_1");
        }
    }

    // Обновите handleChatMessage для поддержки файлов
    private void handleChatMessage(Long userId, String message, String fileId, String fileType, String caption) {
        String currentSessionId = chatService.getUserCurrentSession(userId);
        if (currentSessionId != null) {
            chatService.sendMessage(currentSessionId, userId, message, fileId, fileType, caption);
        } else {
            sendMessageAsync(userId, "У вас нет активного чата. Создайте чат через веб-интерфейс.");
        }
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        Long userId = callbackQuery.getFrom().getId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        log.debug("Callback received from {}: {}", userId, callbackData);

        try {
            if (callbackData.startsWith("switch_")) {
                String sessionId = callbackData.substring(7);
                handleSwitchSessionCallback(userId, sessionId, messageId);
            } else if (callbackData.startsWith("close_")) {
                String sessionId = callbackData.substring(6);
                handleCloseChatCallback(userId, sessionId, messageId);
            } else if (callbackData.equals("refresh_sessions")) {
                handleRefreshSessionsCallback(userId, messageId);
            }

            answerCallbackQuery(callbackQuery.getId(), "Обрабатываю...");

        } catch (Exception e) {
            log.error("Error handling callback: {}", e.getMessage());
            answerCallbackQuery(callbackQuery.getId(), "❌ Ошибка: " + e.getMessage());
        }
    }

    private void handleSwitchSessionCallback(Long userId, String sessionId, Integer messageId) {
        try {
            chatService.switchUserSession(userId, sessionId);

            var sessions = chatService.getUserActiveSessions(userId);
            String currentSessionId = chatService.getUserCurrentSession(userId);

            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(userId.toString());
            editMessage.setMessageId(messageId);
            editMessage.setText("✅ Успешно переключены!\n\n" + buildSessionsText(userId, sessions, currentSessionId));
            editMessage.setReplyMarkup(createSessionsKeyboard(userId, sessions, currentSessionId));

            execute(editMessage);

        } catch (Exception e) {
            log.error("Error switching session via callback: {}", e.getMessage());
            sendMessageAsync(userId, "❌ Ошибка переключения: " + e.getMessage());
        }
    }

    private void handleCloseChatCallback(Long userId, String sessionId, Integer messageId) {
        try {
            boolean initiated = chatService.initiateCloseChat(sessionId, userId);
            if (initiated) {
                var sessions = chatService.getUserActiveSessions(userId);
                String currentSessionId = chatService.getUserCurrentSession(userId);

                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(userId.toString());
                editMessage.setMessageId(messageId);
                editMessage.setText("✅ Запрос на закрытие чата отправлен!\n\n" + buildSessionsText(userId, sessions, currentSessionId));
                editMessage.setReplyMarkup(createSessionsKeyboard(userId, sessions, currentSessionId));

                execute(editMessage);
            }
        } catch (Exception e) {
            log.error("Error closing chat via callback: {}", e.getMessage());
            sendMessageAsync(userId, "❌ Ошибка закрытия чата: " + e.getMessage());
        }
    }

    private void handleRefreshSessionsCallback(Long userId, Integer messageId) {
        try {
            var sessions = chatService.getUserActiveSessions(userId);
            String currentSessionId = chatService.getUserCurrentSession(userId);

            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(userId.toString());
            editMessage.setMessageId(messageId);
            editMessage.setText(buildSessionsText(userId, sessions, currentSessionId));
            editMessage.setReplyMarkup(createSessionsKeyboard(userId, sessions, currentSessionId));

            execute(editMessage);

        } catch (Exception e) {
            log.error("Error refreshing sessions: {}", e.getMessage());
        }
    }

    private String buildSessionsText(Long userId, List<ChatSession> sessions, String currentSessionId) {
        StringBuilder message = new StringBuilder("📋 Ваши активные чаты:\n\n");

        for (int i = 0; i < sessions.size(); i++) {
            ChatSession session = sessions.get(i);
            String otherUserTempId = session.getOtherUserTempId(userId);
            String statusIcon = session.getSessionId().equals(currentSessionId) ? "✅ " : "● ";

            message.append(statusIcon).append("Чат с: ").append(otherUserTempId)
                    .append("\nСтатус: ").append(session.getStatus())
                    .append("\nСоздан: ").append(session.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM HH:mm")))
                    .append("\n\n");
        }

        message.append("✅ - текущий активный чат\n");
        message.append("Нажмите на кнопку ниже чтобы переключиться на чат");

        return message.toString();
    }

    private InlineKeyboardMarkup createSessionsKeyboard(Long userId, List<ChatSession> sessions, String currentSessionId) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (ChatSession session : sessions) {
            String otherUserTempId = session.getOtherUserTempId(userId);
            String buttonText = session.getSessionId().equals(currentSessionId) ?
                    "✅ " + otherUserTempId : "💬 " + otherUserTempId;

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(buttonText);
            button.setCallbackData("switch_" + session.getSessionId());

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(button);

            InlineKeyboardButton closeButton = new InlineKeyboardButton();
            closeButton.setText("❌");
            closeButton.setCallbackData("close_" + session.getSessionId());
            row.add(closeButton);

            rows.add(row);
        }

        List<InlineKeyboardButton> actionRow = new ArrayList<>();
        InlineKeyboardButton refreshButton = new InlineKeyboardButton();
        refreshButton.setText("🔄 Обновить");
        refreshButton.setCallbackData("refresh_sessions");
        actionRow.add(refreshButton);

        rows.add(actionRow);

        keyboardMarkup.setKeyboard(rows);
        return keyboardMarkup;
    }

    private void answerCallbackQuery(String callbackQueryId, String text) {
        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQueryId);
            answer.setText(text);
            execute(answer);
        } catch (TelegramApiException e) {
            log.error("Error answering callback query: {}", e.getMessage());
        }
    }

    private void sendMessageWithKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        CompletableFuture.runAsync(() -> {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(text);
            message.setReplyMarkup(keyboard);

            try {
                execute(message);
                log.debug("Message with keyboard sent to {}", chatId);
            } catch (TelegramApiException e) {
                log.error("Error sending message with keyboard to {}: {}", chatId, e.getMessage());
            }
        }, messageExecutor);
    }

    // Добавьте методы для отправки файлов
    public CompletableFuture<Void> sendFileAsync(Long chatId, String fileId, String fileType, String caption) {
        return CompletableFuture.runAsync(() -> {
            try {
                switch (fileType) {
                    case "photo":
                        sendPhoto(chatId, fileId, caption);
                        break;
                    case "document":
                        sendDocument(chatId, fileId, caption);
                        break;
                    case "voice":
                        sendVoice(chatId, fileId, caption);
                        break;
                    case "video":
                        sendVideo(chatId, fileId, caption);
                        break;
                    case "audio":
                        sendAudio(chatId, fileId, caption);
                        break;
                    default:
                        sendDocument(chatId, fileId, caption);
                        break;
                }
                log.debug("File sent to {}: type {}", chatId, fileType);
            } catch (Exception e) {
                log.error("Error sending file to {}: {}", chatId, e.getMessage());
            }
        }, messageExecutor);
    }

    private void sendPhoto(Long chatId, String fileId, String caption) throws TelegramApiException {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId.toString());
        sendPhoto.setPhoto(new InputFile(fileId));
        if (caption != null && !caption.trim().isEmpty()) {
            sendPhoto.setCaption(caption);
        }
        execute(sendPhoto);
    }

    private void sendDocument(Long chatId, String fileId, String caption) throws TelegramApiException {
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId.toString());
        sendDocument.setDocument(new InputFile(fileId));
        if (caption != null && !caption.trim().isEmpty()) {
            sendDocument.setCaption(caption);
        }
        execute(sendDocument);
    }

    private void sendVoice(Long chatId, String fileId, String caption) throws TelegramApiException {
        SendVoice sendVoice = new SendVoice();
        sendVoice.setChatId(chatId.toString());
        sendVoice.setVoice(new InputFile(fileId));
        if (caption != null && !caption.trim().isEmpty()) {
            sendVoice.setCaption(caption);
        }
        execute(sendVoice);
    }

    private void sendVideo(Long chatId, String fileId, String caption) throws TelegramApiException {
        SendVideo sendVideo = new SendVideo();
        sendVideo.setChatId(chatId.toString());
        sendVideo.setVideo(new InputFile(fileId));
        if (caption != null && !caption.trim().isEmpty()) {
            sendVideo.setCaption(caption);
        }
        execute(sendVideo);
    }

    private void sendAudio(Long chatId, String fileId, String caption) throws TelegramApiException {
        SendAudio sendAudio = new SendAudio();
        sendAudio.setChatId(chatId.toString());
        sendAudio.setAudio(new InputFile(fileId));
        if (caption != null && !caption.trim().isEmpty()) {
            sendAudio.setCaption(caption);
        }
        execute(sendAudio);
    }

    public void sendAcceptOrderMessage(Long chatId, Long orderId, String messageText) {
        CompletableFuture.runAsync(() -> {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("🎯 Новый заказ!\n\n" + messageText);

            try {
                execute(message);
                log.info("Order notification sent to {}", chatId);
            } catch (TelegramApiException e) {
                log.error("Error sending order notification to {}: {}", chatId, e.getMessage());
            }
        }, messageExecutor);
    }

    public CompletableFuture<Void> sendMessageWithRetry(Long chatId, String text) {
        return CompletableFuture.runAsync(() -> {
            int maxRetries = 3;
            int retryCount = 0;

            while (retryCount < maxRetries) {
                try {
                    SendMessage message = new SendMessage();
                    message.setChatId(chatId.toString());
                    message.setText(text);

                    execute(message);
                    log.debug("Message sent to {}: {}", chatId, text.substring(0, Math.min(50, text.length())) + "...");
                    return;

                } catch (TelegramApiException e) {
                    retryCount++;
                    log.warn("Failed to send message to {} (attempt {}/{}): {}",
                            chatId, retryCount, maxRetries, e.getMessage());

                    if (retryCount >= maxRetries) {
                        log.error("Failed to send message to {} after {} attempts", chatId, maxRetries);
                        break;
                    }

                    try {
                        Thread.sleep(2000 * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, messageExecutor);
    }

    public CompletableFuture<Void> sendMessageAsync(Long chatId, String text) {
        return CompletableFuture.runAsync(() -> {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(text);

            try {
                execute(message);
                log.debug("Message sent to {}: {}", chatId, text.substring(0, Math.min(50, text.length())) + "...");
            } catch (TelegramApiException e) {
                log.error("Error sending message to {}: {}", chatId, e.getMessage());
            }
        }, messageExecutor);
    }

    public void sendMessage(Long chatId, String text) {
        sendMessageAsync(chatId, text);
    }
}