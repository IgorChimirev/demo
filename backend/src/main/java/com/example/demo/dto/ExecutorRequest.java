
package com.example.demo.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class ExecutorRequest {
    private Long telegramUserId;
    private String telegramUsername;

    @NotBlank(message = "Имя не может быть пустым")
    private String name;

    @NotBlank(message = "Категория не может быть пустой")
    private String category;

    private String description;
    private String price;
    private String experience;
    private String contacts;
}