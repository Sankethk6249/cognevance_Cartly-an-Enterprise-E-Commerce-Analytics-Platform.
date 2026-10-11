package com.cognevance.ecommerce.service;

import com.cognevance.ecommerce.model.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class PaymentService {
    @Value("${payment.provider}") private String provider;
    @Value("${razorpay.key-id}") private String keyId;
    @Value("${razorpay.key-secret}") private String secret;
    private final RestTemplate rest = new RestTemplate();

    public boolean isRazorpay() { return "razorpay".equalsIgnoreCase(provider); }

    /** Creates a gateway order. In mock mode nothing leaves the server. */
    public Map<String, Object> createGatewayOrder(Order o) {
        long paise = o.getTotal().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
        if (!isRazorpay()) {
            return Map.of("provider", "mock", "gatewayOrderId", "mock_order_" + o.getId(), "amount", paise);
        }
        HttpHeaders h = new HttpHeaders();
        h.setBasicAuth(keyId, secret);
        h.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("amount", paise, "currency", "INR", "receipt", "order_" + o.getId());
        try {
            Map<?, ?> res = rest.postForObject("https://api.razorpay.com/v1/orders", new HttpEntity<>(body, h), Map.class);
            return Map.of("provider", "razorpay", "keyId", keyId, "gatewayOrderId", String.valueOf(res.get("id")), "amount", paise);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment gateway is unavailable");
        }
    }

    /** Verifies the Razorpay HMAC signature. Mock mode accepts any payment id (demo only). */
    public boolean verify(String gatewayOrderId, String paymentId, String signature) {
        if (!isRazorpay()) return true;
        if (signature == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal((gatewayOrderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return false;
        }
    }
}