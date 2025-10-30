
package com.example.demo.service;

import com.example.demo.dto.ExecutorRequest;
import com.example.demo.entity.Executor;
import com.example.demo.repository.ExecutorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExecutorService {
    private final ExecutorRepository executorRepository;

    public Executor createExecutor(ExecutorRequest request) {
        Executor executor = new Executor();
        executor.setTelegramUserId(request.getTelegramUserId());
        executor.setTelegramUsername(request.getTelegramUsername());
        executor.setName(request.getName());
        executor.setCategory(request.getCategory());
        executor.setDescription(request.getDescription());
        executor.setPrice(request.getPrice());
        executor.setExperience(request.getExperience());
        executor.setContacts(request.getContacts());

        return executorRepository.save(executor);
    }

    public List<Executor> getExecutorsByUser(Long telegramUserId) {
        return executorRepository.findByTelegramUserId(telegramUserId);
    }

    public List<Executor> getAllExecutors() {
        return executorRepository.findAll();
    }

    public List<Executor> getExecutorsByCategory(String category) {
        return executorRepository.findByCategory(category);
    }
    public Executor updateExecutor(Long id, ExecutorRequest request) {
        Executor executor = executorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Executor not found"));

        executor.setName(request.getName());
        executor.setCategory(request.getCategory());
        executor.setDescription(request.getDescription());
        executor.setPrice(request.getPrice());
        executor.setExperience(request.getExperience());
        executor.setContacts(request.getContacts());

        return executorRepository.save(executor);
    }
}