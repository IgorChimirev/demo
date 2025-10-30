
package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.dto.UserRequest;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<User> createOrUpdateUser(@RequestBody UserRequest request) {
        User user = userService.createOrUpdateUser(
                request.getId(),
                request.getUsername()
        );
        return ResponseEntity.ok(user);
    }

    @PostMapping("/{userId}/accept-offer")
    public ResponseEntity<User> acceptOffer(@PathVariable Long userId) {
        User user = userService.acceptOffer(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}/accepted-offer")
    public ResponseEntity<Boolean> hasAcceptedOffer(@PathVariable Long userId) {
        boolean accepted = userService.hasAcceptedOffer(userId);
        return ResponseEntity.ok(accepted);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable Long userId) {
        Optional<User> user = userService.getUser(userId);
        return user.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }


}