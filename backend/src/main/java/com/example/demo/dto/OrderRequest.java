
package com.example.demo.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class OrderRequest {
    private Long telegramUserId;
    private String telegramUsername;

    @NotBlank(message = "ВУЗ не может быть пустым")
    private String university;

    @NotBlank(message = "Предмет не может быть пустым")
    private String subject;

    @NotBlank(message = "Описание не может быть пустым")
    private String description;

    @NotBlank(message = "Категория не может быть пустой")
    private String category;


    private String price;

    public Long getTelegramUserId() { return telegramUserId; }
    public String getTelegramUsername() { return telegramUsername; }
    public String getUniversity() { return university; }
    public String getSubject() { return subject; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getPrice() { return price; }
}