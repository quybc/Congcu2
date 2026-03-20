package com.example.demo_3001.service;

import com.example.demo_3001.config.MomoConfig;
import com.example.demo_3001.dto.MomoResponse;
import com.example.demo_3001.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MomoService {

    private final MomoConfig momoConfig;

    public MomoResponse createPayment(Order order) {
        String internalOrderId = String.valueOf(order.getId());
        String requestId = System.currentTimeMillis() + "-" + UUID.randomUUID();
        String orderId = internalOrderId + "-" + requestId;
        String amount = String.valueOf((long) order.getTotalPrice());
        String orderInfo = "Thanh toan don hang #" + internalOrderId;
        String extraData = "";
        String partnerName = momoConfig.getPartnerName() == null ? "Demo3001" : momoConfig.getPartnerName();
        String storeId = momoConfig.getStoreId() == null ? "Demo3001Store" : momoConfig.getStoreId();

        // Standard MoMo signature format (alphabetical order)
        String rawData = "accessKey=" + momoConfig.getAccessKey() +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&ipnUrl=" + momoConfig.getNotifyUrl() +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + momoConfig.getPartnerCode() +
                "&redirectUrl=" + momoConfig.getReturnUrl() +
                "&requestId=" + requestId +
                "&requestType=" + momoConfig.getRequestType();

        String signature = computeHmacSha256(rawData, momoConfig.getSecretKey());

        Map<String, String> requestBody = new LinkedHashMap<>();
        requestBody.put("partnerCode", momoConfig.getPartnerCode());
        requestBody.put("partnerName", partnerName);
        requestBody.put("storeId", storeId);
        requestBody.put("requestId", requestId);
        requestBody.put("amount", amount);
        requestBody.put("orderId", orderId);
        requestBody.put("orderInfo", orderInfo);
        requestBody.put("redirectUrl", momoConfig.getReturnUrl());
        requestBody.put("ipnUrl", momoConfig.getNotifyUrl());
        requestBody.put("lang", "vi");
        requestBody.put("extraData", extraData);
        requestBody.put("requestType", momoConfig.getRequestType());
        requestBody.put("signature", signature);
        requestBody.put("accessKey", momoConfig.getAccessKey());

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            return restTemplate.postForObject(momoConfig.getMomoApiUrl(), request, MomoResponse.class);
        } catch (Exception e) {
            MomoResponse failedResponse = new MomoResponse();
            failedResponse.setErrorCode(-1);
            failedResponse.setMessage(e.getMessage());
            failedResponse.setLocalMessage("Không thể kết nối MoMo Sandbox.");
            return failedResponse;
        }
    }

    private String computeHmacSha256(String message, String secretKey) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] bytes = sha256_HMAC.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error computing HMAC SHA256", e);
        }
    }
}
