package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class PaymentStatusRequest {
    @JsonProperty("Status")
    private String status;

    @JsonProperty("OrderId")
    private String orderId;

    @JsonProperty("PaymentId")
    private String paymentId;

    @JsonProperty("Amount")
    private String amount;

    private Map<String, Object> otherData = new HashMap<>();
}