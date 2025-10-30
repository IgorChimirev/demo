
package com.example.demo.controller;

import com.example.demo.TelegramBot.TelegramBotService;
import com.example.demo.dto.*;
import com.example.demo.entity.Executor;
import com.example.demo.service.ExecutorService;
import com.example.demo.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {
    private final OrderService orderService;

    @org.springframework.beans.factory.annotation.Value("${tinkoff.terminal.key:}")
    private String terminalKey;

    @org.springframework.beans.factory.annotation.Value("${tinkoff.terminal.password:}")
    private String terminalPassword;

    private final TelegramBotService telegramBotService;

    @PostMapping("/create-payment-link")
    public ResponseEntity<?> createPaymentLink(@Valid @RequestBody PaymentRequest request) {
        log.info("Received payment request: {}", request);
        try{
            orderService.updateOrderStatus(Long.valueOf(request.getOrderNumber()),"получена ссылка на оплату");

            String orderId = String.format("%s-n%s-%s-%s",
                    request.getUserIdTo(),
                    request.getOrderNumber(),
                    request.getUserIdFrom(),
                    request.getDate()
            );

            TinkoffInitRequest tinkoffRequest = new TinkoffInitRequest();
            tinkoffRequest.setAmount(request.getAmount().toString());
            tinkoffRequest.setDescription("Оплата заказа на платформе");
            tinkoffRequest.setOrderId(orderId);
            tinkoffRequest.setTerminalKey(terminalKey);

            String token = generateToken(tinkoffRequest);
            tinkoffRequest.setToken(token);


            TinkoffInitResponse response = callTinkoffInit(tinkoffRequest);
            return ResponseEntity.ok(response);
        }catch (Exception e){
            log.error("Ошибка при создании платежной ссылки: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    private String generateToken(TinkoffInitRequest request) throws NoSuchAlgorithmException {
        Map<String, String> fields = new TreeMap<>();
        fields.put("TerminalKey", request.getTerminalKey());
        fields.put("Amount", request.getAmount());
        fields.put("OrderId", request.getOrderId());
        fields.put("Description", request.getDescription());
        fields.put("Password", terminalPassword);

        StringBuilder concatenated = new StringBuilder();
        for (String value : fields.values()) {
            if (value != null) {
                concatenated.append(value);
            }
        }

        log.info("String for token generation: {}", concatenated.toString());

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(concatenated.toString().getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = String.format("%02x", b);
            hexString.append(hex);
        }

        String token = hexString.toString();
        log.info("Generated token: {}", token);

        return token;
    }

    private TinkoffInitResponse callTinkoffInit(TinkoffInitRequest request) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<TinkoffInitRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<TinkoffInitResponse> response = restTemplate.postForEntity(
                "https://securepay.tinkoff.ru/v2/Init",
                entity,
                TinkoffInitResponse.class
        );

        System.out.println("Response status: " + terminalKey);
        System.out.println("Response status: " + response);
        System.out.println("Response status: " + response.getBody());
        System.out.println("Response JSON body: " + response.getBody());

        return response.getBody();
    }

    @PostMapping("/payment-status")
    public ResponseEntity<String> handlePaymentStatus(@RequestBody PaymentStatusRequest request) {
        System.out.println("Received Payment Status request: dknr3ioh4vio3");
        try {
            log.info("Получен статус платежа: {}", request.getStatus());
            log.info("Данные платежа: {}", request);

            String orderId = request.getOrderId();
            log.info("OrderId для обработки: {}", orderId);

            String[] orderParts = orderId.split("-n");

            if (orderParts.length < 2) {
                log.error("Некорректный формат OrderId: {}", orderId);
                return ResponseEntity.badRequest().body("Invalid OrderId format");
            }

            String userIdTo = orderParts[0];
            String remainingPart = orderParts[1];

            String[] remainingParts = remainingPart.split("-");

            if (remainingParts.length < 3) {
                log.error("Некорректный формат оставшейся части OrderId: {}", remainingPart);
                return ResponseEntity.badRequest().body("Invalid OrderId format - missing parts");
            }

            String orderNumberStr = remainingParts[0];
            String userIdFrom = remainingParts[1];

            try {
                Long orderNumber = Long.parseLong(orderNumberStr);
                Long userIdToNumber = Long.parseLong(userIdTo);
                Long userIdFromNumber = Long.parseLong(userIdFrom);

                log.info("Распарсенные данные: orderNumber={}, userIdTo={}, userIdFrom={}",
                        orderNumber, userIdToNumber, userIdFromNumber);

                switch (request.getStatus()) {
                    case "CONFIRMED":
                        handleConfirmedPayment(orderNumber, userIdToNumber);
                        break;
                }

                return ResponseEntity.ok("OK");

            } catch (NumberFormatException e) {
                log.error("Ошибка парсинга чисел из OrderId: {}", orderId, e);
                return ResponseEntity.badRequest().body("Invalid number format in OrderId");
            }

        } catch (Exception error) {
            log.error("Ошибка обработки статуса платежа:", error);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка сервера");
        }
    }

    private void handleConfirmedPayment(Long orderNumber, Long userIdToNumber) {
        System.out.println("Received Payment Status requvfelr4oigio54ioest: dknr3ioh4vio3");
        try{
            telegramBotService.sendMessage(Long.valueOf(userIdToNumber),"Успешная оплата заказа "+String.valueOf(orderNumber));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}