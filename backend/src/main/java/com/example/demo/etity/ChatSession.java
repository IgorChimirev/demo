package com.example.demo.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@RedisHash(value = "ChatSession", timeToLive = 604800) // 7 дней TTL
public class ChatSession implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String sessionId;

    @Indexed
    private Long user1Id;

    @Indexed
    private Long user2Id;

    @Indexed
    private Long orderId;

    private String user1TempId;
    private String user2TempId;

    private String status = "ACTIVE";

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastActivity;

    private Set<Long> closeApprovals = new HashSet<>();

    
    private Boolean paid = false;
    private Set<Long> completionApprovals = new HashSet<>();

    public ChatSession() {
        this.createdAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }

    public String getOtherUserTempId(Long userId) {
        return userId.equals(user1Id) ? user2TempId : user1TempId;
    }

    public Long getOtherUserId(Long userId) {
        return userId.equals(user1Id) ? user2Id : user1Id;
    }

    public String getUserTempId(Long userId) {
        return userId.equals(user1Id) ? user1TempId : user2TempId;
    }
}