package com.example.demo_3001.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MomoResponse {
    private String requestId;
    private Integer resultCode;
    private int errorCode;
    private String orderId;
    private String message;
    private String localMessage;
    private String requestType;
    private String payUrl;
    private String signature;
    private String qrCodeUrl;
    private String deeplink;
    private String deeplinkWebInApp;
}
