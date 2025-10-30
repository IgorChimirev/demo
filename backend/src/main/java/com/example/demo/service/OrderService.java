package com.example.demo.service;

import com.example.demo.TelegramBot.TelegramBotService;
import com.example.demo.dto.OrderRequest;
import com.example.demo.entity.Order;
import com.example.demo.entity.Executor;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ExecutorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final TelegramBotService telegramBotService;
    private final ExecutorRepository executorRepository;
    private final ChatService chatService;

    public Order createOrder(OrderRequest request) {
        log.info("🔄 Creating order for user: {}", request.getTelegramUserId());

        Order order = new Order();
        order.setTelegramUserId(request.getTelegramUserId());
        order.setTelegramUsername(request.getTelegramUsername());
        order.setUniversity(request.getUniversity());
        order.setSubject(request.getSubject());
        order.setCategory(request.getCategory());
        order.setDescription(request.getDescription());
        order.setPrice(request.getPrice());
        order.setStatus("в поиске");
        order.setCreatedAt(LocalDateTime.now());

        log.debug("Order entity prepared - University: {}, Subject: {}, Category: {}",
                order.getUniversity(), order.getSubject(), order.getCategory());

        try {
            Order savedOrder = orderRepository.save(order);
            log.info("Order saved to database with ID: {}", savedOrder.getId());

            // Асинхронное уведомление исполнителей
            notifyExecutorsAboutNewOrderAsync(savedOrder);
            return savedOrder;
        } catch (Exception e) {
            log.error("Error saving order to database: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Async
    public CompletableFuture<Void> notifyExecutorsAboutNewOrderAsync(Order order) {
        return CompletableFuture.runAsync(() -> {
            notifyExecutorsAboutNewOrder(order);
        });
    }

    private void notifyExecutorsAboutNewOrder(Order order) {
        try {
            List<Executor> executors = executorRepository.findByCategory(order.getCategory());
            log.debug("Found {} executors for category: {}", executors.size(), order.getCategory());

            // Убираем дубликаты по telegramUserId
            Map<Long, Executor> uniqueExecutors = executors.stream()
                    .collect(Collectors.toMap(
                            Executor::getTelegramUserId,
                            Function.identity(),
                            (existing, replacement) -> existing // При дубликатах берем существующую запись
                    ));

            log.debug("After deduplication: {} unique executors", uniqueExecutors.size());

            for (Executor executor : uniqueExecutors.values()) {
                try {
                    String availableOrdersLink = generateAvailableOrdersLink();

                    telegramBotService.sendAcceptOrderMessage(
                            executor.getTelegramUserId(),
                            order.getId(),
                            "🎯 Появился новый заказ по вашей специальности!\n\n" +
                                    "📚 Предмет: " + order.getSubject() + "\n" +
                                    "📚 Категория: " + order.getCategory() + "\n" +
                                    "🏫 ВУЗ: " + order.getUniversity() + "\n" +
                                    "📝 Описание: " + order.getDescription() + "\n" +
                                    "💰 Цена: " + order.getPrice() + "\n"+
                                    "💼 [Перейти к доступным заказам](" + availableOrdersLink + ")"
                    );

                    log.debug("Notification sent to executor: {}", executor.getTelegramUserId());
                } catch (Exception e) {
                    log.warn("Failed to send notification to executor {}: {}",
                            executor.getTelegramUserId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error notifying executors: {}", e.getMessage(), e);
        }
    }

    private String generateAvailableOrdersLink() {
        return "https://t.me/studentsbotmainbot/student?startapp=available_orders";

    }

    public List<Order> getOrdersByUser(Long telegramUserId) {
        return orderRepository.findByTelegramUserIdOrderByCreatedAtDesc(telegramUserId);
    }

    public Order acceptOrder(Long orderId, Long executorId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        order.setStatus("принят исполнителем");
        order.setExecutorId(executorId);

        CompletableFuture.runAsync(() -> {
            try {

                chatService.createChatSession(order.getTelegramUserId(), executorId,order.getId());

                log.info("Chat session created for order {} between {} and {}",
                        orderId, order.getTelegramUserId(), executorId);


                CompletableFuture.allOf(
                        telegramBotService.sendMessageAsync(
                                order.getTelegramUserId(),
                                "✅ Ваш заказ принят!\n\n" +
                                        "Исполнитель готов приступить к работе. " +
                                        "Для общения используйте анонимный чат с ботом @" +
                                        telegramBotService.getBotUsername() + "\n\n" +
                                        "Команды чата:\n" +
                                        "/sessions - список ваших чатов\n" +
                                        "/close_chat - предложить закрыть чат"
                        ),
                        telegramBotService.sendMessageAsync(
                                executorId,
                                "✅ Вы приняли заказ!\n\n" +
                                        "Для общения с заказчиком используйте анонимный чат с ботом @" +
                                        telegramBotService.getBotUsername() + "\n\n" +
                                        "Команды чата:\n" +
                                        "/sessions - список ваших чатов\n" +
                                        "/close_chat - предложить закрыть чат"
                        )
                ).exceptionally(ex -> {
                    log.warn("Error sending acceptance notifications: {}", ex.getMessage());
                    return null;
                });

            } catch (Exception e) {
                log.error("Error creating chat session for order {}: {}", orderId, e.getMessage(), e);

                // Отправляем уведомление об ошибке
                telegramBotService.sendMessageAsync(
                        executorId,
                        "⚠️ Чат не был создан автоматически. " +
                                "Вы можете начать общение через веб-интерфейс."
                );
            }
        });

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} accepted by executor {}", orderId, executorId);
        return savedOrder;
    }

    public List<Order> getActiveOrdersByUser(Long telegramUserId) {
        return orderRepository.findByTelegramUserIdAndStatus(telegramUserId, "в поиске");
    }

    public List<Order> getCompletedOrdersByUser(Long telegramUserId) {
        return orderRepository.findByTelegramUserIdAndStatus(telegramUserId, "завершен");
    }

    public List<Order> getAllActiveOrders() {
        return orderRepository.findByStatus("в поиске");
    }

    public Order updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        String oldStatus = order.getStatus();
        order.setStatus(status);

        if ("завершен".equals(status)) {
            order.setCompletedAt(LocalDateTime.now());

            CompletableFuture.runAsync(() -> {
                String completionMessage = "🏁 Заказ завершен. Спасибо за использование сервиса!";

                if (order.getExecutorId() != null) {
                    telegramBotService.sendMessageAsync(order.getExecutorId(), completionMessage);
                }
                telegramBotService.sendMessageAsync(order.getTelegramUserId(), completionMessage);
            });
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} status changed from {} to {}", orderId, oldStatus, status);
        return savedOrder;
    }

    public String getAmountByOrderId(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        return order.getPrice();
    }

    public List<Order> getActiveOrdersByCategory(String category, Long telegramUserId) {
        log.debug("Getting active orders by category: {} for user: {}", category, telegramUserId);

        List<Order> orders = orderRepository.findByTelegramUserIdNotAndStatusAndCategory(
                telegramUserId, "в поиске", category);

        log.debug("Found {} orders for category: {}", orders.size(), category);
        return orders;
    }

    public void deleteOrder(Long orderId) {
        log.info("Deleting order with ID: {}", orderId);

        try {
            boolean exists = orderRepository.existsById(orderId);
            if (!exists) {
                log.warn("Order not found with id: {}", orderId);
                throw new RuntimeException("Order not found with id: " + orderId);
            }

            orderRepository.deleteById(orderId);
            log.info("Order successfully deleted: {}", orderId);

        } catch (Exception e) {
            log.error("Error during order deletion {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }
}