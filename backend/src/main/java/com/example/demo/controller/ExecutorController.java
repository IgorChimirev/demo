
package com.example.demo.controller;

import com.example.demo.dto.ExecutorRequest;
import com.example.demo.entity.Executor;
import com.example.demo.service.ExecutorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/executors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExecutorController {
    private final ExecutorService executorService;

    @PostMapping
    public ResponseEntity<Executor> createExecutor(@Valid @RequestBody ExecutorRequest request) {
        Executor executor = executorService.createExecutor(request);
        return ResponseEntity.ok(executor);
    }

    @GetMapping("/user/{telegramUserId}")
    public ResponseEntity<List<Executor>> getExecutorsByUser(@PathVariable Long telegramUserId) {
        List<Executor> executors = executorService.getExecutorsByUser(telegramUserId);
        return ResponseEntity.ok(executors);
    }

    @GetMapping
    public ResponseEntity<List<Executor>> getAllExecutors() {
        List<Executor> executors = executorService.getAllExecutors();
        return ResponseEntity.ok(executors);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Executor>> getExecutorsByCategory(@PathVariable String category) {
        List<Executor> executors = executorService.getExecutorsByCategory(category);
        return ResponseEntity.ok(executors);
    }
    @PutMapping("/{id}")
    public ResponseEntity<Executor> updateExecutor(
            @PathVariable Long id,
            @Valid @RequestBody ExecutorRequest request) {
        try {
            Executor executor = executorService.updateExecutor(id, request);
            return ResponseEntity.ok(executor);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}