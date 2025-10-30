
package com.example.demo.repository;

import com.example.demo.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByTelegramUserId(Long telegramUserId);
    List<Order> findByTelegramUserIdAndStatus(Long telegramUserId, String status);
    List<Order> findByStatus(String status);
    List<Order> findByStatusAndCategory(String status, String category);
    List<Order> findByStatusOrderByCreatedAtDesc(String status);
    List<Order> findByTelegramUserIdOrderByCreatedAtDesc(Long telegramUserId);
    List<Order> findByTelegramUserIdNotAndStatusAndCategory(Long telegramUserId, String status, String category);

}
