
package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User createOrUpdateUser(Long userId, String username) {
        Optional<User> existingUser = userRepository.findById(userId);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            user.setUsername(username);
        } else {
            user = new User();
            user.setId(userId);
            user.setUsername(username);
            user.setAcceptedOffer(false);
        }

        return userRepository.save(user);
    }

    public User acceptOffer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setAcceptedOffer(true);
        return userRepository.save(user);
    }

    public boolean hasAcceptedOffer(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.map(User::isAcceptedOffer).orElse(false);
    }

    public Optional<User> getUser(Long userId) {
        return userRepository.findById(userId);
    }
}