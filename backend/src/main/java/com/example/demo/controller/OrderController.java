
package com.example.demo.controller;

import com.example.demo.dto.OrderRequest;
import com.example.demo.entity.Order;
import com.example.demo.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/request")
    public ResponseEntity<Order> createOrder(/*@Valid*/ @RequestBody OrderRequest request) {
        System.out.println("=== CREATE ORDER REQUEST ===");
        System.out.println("Telegram User ID: " + request.getTelegramUserId());
        System.out.println("Telegram Username: " + request.getTelegramUsername());
        System.out.println("University: " + request.getUniversity());
        System.out.println("Subject: " + request.getSubject());
        System.out.println("Description: " + request.getDescription());
        System.out.println("Price: " + request.getPrice());

        try {
            Order order = orderService.createOrder(request);
            System.out.println(" Order created successfully with ID: " + order.getId());
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            System.err.println(" Error creating order: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{orderId}/accept")
    public ResponseEntity<Order> acceptOrder(@PathVariable Long orderId, @RequestParam Long executorId) {

        try {
            Order order = orderService.acceptOrder(orderId, executorId);
            return ResponseEntity.ok(order);
        }catch (Exception e) {
            System.err.println(" Error accepting order: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user/{telegramUserId}")
    public ResponseEntity<List<Order>> getOrdersByUser(@PathVariable Long telegramUserId) {
        List<Order> orders = orderService.getOrdersByUser(telegramUserId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/user/{telegramUserId}/active")
    public ResponseEntity<List<Order>> getActiveOrdersByUser(@PathVariable Long telegramUserId) {
        List<Order> orders = orderService.getActiveOrdersByUser(telegramUserId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/user/{telegramUserId}/completed")
    public ResponseEntity<List<Order>> getCompletedOrdersByUser(@PathVariable Long telegramUserId) {
        List<Order> orders = orderService.getCompletedOrdersByUser(telegramUserId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Order>> getAllActiveOrders() {
        List<Order> orders = orderService.getAllActiveOrders();
        return ResponseEntity.ok(orders);
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {
        Order order = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(order);
    }



    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long orderId) {
        System.out.println("=== DELETE ORDER REQUEST ===");
        System.out.println("Received DELETE request for order ID: " + orderId);

        try {
            orderService.deleteOrder(orderId);
            System.out.println("Order deleted successfully: " + orderId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Error deleting order " + orderId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/active/category/{category}/{telegramUserId}")
    public ResponseEntity<List<Order>> getActiveOrdersByCategory(@PathVariable String category,@PathVariable Long telegramUserId) {
        System.out.println("=== GET ACTIVE ORDERS BY CATEGORY ===");
        System.out.println("Category: " + category);

        try {
            List<Order> orders = orderService.getActiveOrdersByCategory(category,telegramUserId);
            System.out.println("Found " + orders.size() + " orders for category: " + category);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            System.err.println("Error getting orders by category: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}