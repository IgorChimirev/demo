package com.example.demo.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private Long amount;
    private String userIdFrom;
    private String userIdTo;
    private String orderNumber;
    private String date;
}