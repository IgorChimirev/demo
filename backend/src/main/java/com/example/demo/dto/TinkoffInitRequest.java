package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TinkoffInitRequest {
    @JsonProperty("Amount")
    private String amount;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("OrderId")
    private String orderId;

    @JsonProperty("TerminalKey")
    private String terminalKey;

    @JsonProperty("Token")
    private String token;

}