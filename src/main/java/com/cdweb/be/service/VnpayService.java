package com.cdweb.be.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VnpayService {

    @Value("${vnpay.tmnCode}")
    private String tmnCode;

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    @Value("${vnpay.payUrl}")
    private String payUrl;

    @Value("${vnpay.returnUrl}")
    private String returnUrl;

    /**
     * Tạo URL thanh toán VNPAY
     */
    public String createPaymentUrl(Integer orderId, long amount, String orderInfo, String ipAddr) {
        String vnpVersion    = "2.1.0";
        String vnpCommand    = "pay";
        String vnpCurrCode   = "VND";
        String vnpLocale     = "vn";
        String vnpOrderType  = "other";
        String vnpTxnRef     = String.valueOf(orderId);
        String vnpCreateDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String vnpExpireDate = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(new Date(System.currentTimeMillis() + 15 * 60 * 1000)); // 15 phút

        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version",    vnpVersion);
        vnpParams.put("vnp_Command",    vnpCommand);
        vnpParams.put("vnp_TmnCode",    tmnCode);
        vnpParams.put("vnp_Amount",     String.valueOf(amount * 100)); // VNPAY tính đơn vị x100
        vnpParams.put("vnp_CurrCode",   vnpCurrCode);
        vnpParams.put("vnp_TxnRef",     vnpTxnRef);
        vnpParams.put("vnp_OrderInfo",  orderInfo);
        vnpParams.put("vnp_OrderType",  vnpOrderType);
        vnpParams.put("vnp_Locale",     vnpLocale);
        vnpParams.put("vnp_ReturnUrl",  returnUrl);
        vnpParams.put("vnp_IpAddr",     ipAddr);
        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        String hashData = buildHashData(vnpParams);
        String secureHash = hmacSHA512(hashSecret, hashData);

        StringBuilder queryUrl = new StringBuilder();
        vnpParams.forEach((k, v) -> {
            queryUrl.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(v, StandardCharsets.UTF_8))
                    .append("&");
        });
        queryUrl.append("vnp_SecureHash=").append(secureHash);

        return payUrl + "?" + queryUrl;
    }

    /**
     * Xác thực IPN callback từ VNPAY
     * Trả về true nếu hợp lệ
     */
    public boolean verifyIpn(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null) return false;

        Map<String, String> filteredParams = new TreeMap<>(params);
        filteredParams.remove("vnp_SecureHash");
        filteredParams.remove("vnp_SecureHashType");

        String hashData = buildHashData(filteredParams);
        String computedHash = hmacSHA512(hashSecret, hashData);
        return computedHash.equalsIgnoreCase(receivedHash);
    }

    /**
     * Xác thực Return URL sau khi user thanh toán xong
     */
    public boolean verifyReturnUrl(Map<String, String> params) {
        return verifyIpn(params); // Cùng logic
    }

    private String buildHashData(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .forEach(e -> {
                    if (sb.length() > 0) sb.append("&");
                    sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
                            .append("=")
                            .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
                });
        return sb.toString();
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("VNPAY HMAC error", e);
        }
    }
}