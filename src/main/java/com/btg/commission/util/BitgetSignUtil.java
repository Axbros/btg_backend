package com.btg.commission.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Bitget V2 私有接口签名：Base64(HMAC_SHA256(secretKey, prehash))。
 * prehash = timestamp + METHOD + requestPath + (queryString 非空则 ? + queryString) + body
 */
public final class BitgetSignUtil {

    private BitgetSignUtil() {
    }

    public static String sign(
            String timestamp,
            String method,
            String requestPath,
            String queryString,
            String body,
            String secretKey) throws Exception {
        String qs = (queryString == null || queryString.isEmpty()) ? "" : "?" + queryString;
        String b = body == null ? "" : body;
        String prehash = timestamp + method.toUpperCase() + requestPath + qs + b;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(prehash.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(raw);
    }
}
