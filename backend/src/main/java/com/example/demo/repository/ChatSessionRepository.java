package com.example.demo.repository;

import com.example.demo.entity.ChatSession;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends CrudRepository<ChatSession, String> {
    List<ChatSession> findByUser1IdOrUser2Id(Long user1Id, Long user2Id);

    default List<ChatSession> findByUser1IdOrUser2IdAndStatus(Long user1Id, Long user2Id, String status) {
        List<ChatSession> sessions = findByUser1IdOrUser2Id(user1Id, user2Id);
        return sessions.stream()
                .filter(session -> status.equals(session.getStatus()))
                .toList();
    }
    ChatSession findByOrderId(String sessionId);
}