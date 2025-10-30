package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TinkoffInitResponse {
    @JsonProperty("Success")
    private String success;

    @JsonProperty("ErrorCode")
    private String errorCode;

    @JsonProperty("Message")
    private String message;

    @JsonProperty("MessageDetails")
    private String messageDetails;

    @JsonProperty("PaymentId")
    private String paymentId;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("PaymentURL")
    private String paymentURL;
}