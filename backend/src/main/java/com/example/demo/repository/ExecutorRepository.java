package com.example.demo.repository;

import com.example.demo.entity.Executor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ExecutorRepository extends JpaRepository<Executor, Long> {
    List<Executor> findByTelegramUserId(Long telegramUserId);
    Optional<Executor> findByIdAndTelegramUserId(Long id, Long telegramUserId);
    List<Executor> findByCategory(String category);
    boolean existsByTelegramUserIdAndCategory(Long telegramUserId, String category);
}

