package com.btg.commission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.config.BitgetProperties;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.service.BitgetApiService;
import com.btg.commission.util.BitgetSignUtil;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.vo.BitgetAssetAccountVO;
import com.btg.commission.vo.BitgetAssetSummaryVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BitgetApiServiceImpl implements BitgetApiService {

    private static final String MIX_ACCOUNTS_PATH = "/api/v2/mix/account/accounts";
    private static final String DEFAULT_PRODUCT_TYPE = "USDT-FUTURES";
    private static final DateTimeFormatter SYNC_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserProfileMapper userProfileMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final BitgetProperties bitgetProperties;

    @Override
    public BitgetAssetSummaryVO queryCurrentUserAssets(Long userId, String productType) {
        String lastSync = SYNC_TIME_FMT.format(LocalDateTime.now());
        String pt = resolveProductType(productType);
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
        if (profile == null
                || !StringUtils.hasText(profile.getBitgetAccessKey())
                || !StringUtils.hasText(profile.getBitgetSecretKey())
                || !StringUtils.hasText(profile.getBitgetPassphrase())) {
            return BitgetAssetSummaryVO.builder()
                    .success(false)
                    .message("未配置Bitget API信息")
                    .productType(pt)
                    .accounts(Collections.emptyList())
                    .totalUsdtBalance(null)
                    .lastSyncTime(lastSync)
                    .build();
        }
        String accessKey = profile.getBitgetAccessKey().trim();
        String secretKey = profile.getBitgetSecretKey().trim();
        String passphrase = profile.getBitgetPassphrase().trim();
        try {
            String queryString = "productType=" + URLEncoder.encode(pt, StandardCharsets.UTF_8);
            String ts = String.valueOf(System.currentTimeMillis());
            String sign = BitgetSignUtil.sign(ts, "GET", MIX_ACCOUNTS_PATH, queryString, "", secretKey);
            String url = bitgetProperties.getBaseUrl() + MIX_ACCOUNTS_PATH + "?" + queryString;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ACCESS-KEY", accessKey);
            headers.set("ACCESS-SIGN", sign);
            headers.set("ACCESS-TIMESTAMP", ts);
            headers.set("ACCESS-PASSPHRASE", passphrase);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return parseMixAccounts(resp.getBody(), lastSync, pt);
        } catch (RestClientResponseException e) {
            int sc = e.getStatusCode().value();
            log.warn("Bitget mix accounts HTTP error, userId={}, status={}", userId, sc);
            String hint = extractBitgetMsg(e.getResponseBodyAsString());
            String msg = hint != null
                    ? "资产查询失败：" + shortenSafeMessage(hint)
                    : "资产查询失败：HTTP " + sc;
            return failSummary(msg, lastSync, pt);
        } catch (Exception e) {
            log.warn("Bitget mix accounts failed, userId={}, type={}", userId, e.getClass().getSimpleName());
            return failSummary("资产查询失败：" + shortenSafeMessage(e.getMessage()), lastSync, pt);
        }
    }

    private static String resolveProductType(String productType) {
        if (!StringUtils.hasText(productType)) {
            return DEFAULT_PRODUCT_TYPE;
        }
        return productType.trim();
    }

    private static BitgetAssetSummaryVO failSummary(String message, String lastSync, String productType) {
        return BitgetAssetSummaryVO.builder()
                .success(false)
                .message(message)
                .productType(productType)
                .accounts(Collections.emptyList())
                .totalUsdtBalance(null)
                .lastSyncTime(lastSync)
                .build();
    }

    private BitgetAssetSummaryVO parseMixAccounts(String body, String lastSync, String productType) {
        if (!StringUtils.hasText(body)) {
            return failSummary("资产查询失败：空响应", lastSync, productType);
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            String code = root.path("code").asText("");
            if (!"00000".equals(code)) {
                String msg = root.path("msg").asText("接口返回错误");
                return failSummary("资产查询失败：" + shortenSafeMessage(msg), lastSync, productType);
            }
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) {
                return BitgetAssetSummaryVO.builder()
                        .success(true)
                        .message("ok")
                        .productType(productType)
                        .accounts(Collections.emptyList())
                        .totalUsdtBalance(MoneyUtil.money(BigDecimal.ZERO).toPlainString())
                        .lastSyncTime(lastSync)
                        .build();
            }
            BigDecimal total = MoneyUtil.money(BigDecimal.ZERO);
            List<BitgetAssetAccountVO> accounts = new ArrayList<>();
            for (JsonNode row : data) {
                if (row == null || !row.isObject()) {
                    continue;
                }
                String marginCoin = firstTextOrNumberAsText(row, "marginCoin", "margin_coin");
                String available = firstNumericString(row, "available", "availableBalance");
                String lockedRaw = firstNumericString(row, "locked", "frozen");
                String locked = StringUtils.hasText(lockedRaw) ? lockedRaw : "0";
                String usdtEq = firstNumericString(row, "usdtEquity", "usdt_equity", "accountEquity");
                BigDecimal val = parseDecimalLoose(usdtEq);
                if (val != null) {
                    total = total.add(val);
                }
                accounts.add(BitgetAssetAccountVO.builder()
                        .coin(marginCoin)
                        .accountType(productType)
                        .usdtBalance(usdtEq)
                        .usdtAvailable(available)
                        .usdtFrozen(locked)
                        .build());
            }
            return BitgetAssetSummaryVO.builder()
                    .success(true)
                    .message("ok")
                    .productType(productType)
                    .accounts(accounts)
                    .totalUsdtBalance(MoneyUtil.money(total).toPlainString())
                    .lastSyncTime(lastSync)
                    .build();
        } catch (Exception e) {
            log.warn("Bitget mix accounts parse failed, type={}", e.getClass().getSimpleName());
            return failSummary("资产查询失败：响应解析异常", lastSync, productType);
        }
    }

    /** marginCoin 等字段在部分版本可能为文本或数字 */
    private static String firstTextOrNumberAsText(JsonNode row, String... keys) {
        for (String k : keys) {
            if (!row.has(k) || row.get(k).isNull()) {
                continue;
            }
            JsonNode n = row.get(k);
            if (n.isTextual()) {
                String t = n.asText();
                if (StringUtils.hasText(t)) {
                    return t.trim();
                }
            }
            if (n.isNumber()) {
                return n.asText();
            }
        }
        return null;
    }

    private static String firstNumericString(JsonNode row, String... keys) {
        for (String k : keys) {
            if (!row.has(k) || row.get(k).isNull()) {
                continue;
            }
            JsonNode n = row.get(k);
            if (n.isNumber()) {
                return n.asText();
            }
            if (n.isTextual()) {
                String t = n.asText().trim();
                if (StringUtils.hasText(t)) {
                    return t;
                }
            }
        }
        return null;
    }

    private static BigDecimal parseDecimalLoose(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return MoneyUtil.money(new BigDecimal(s.trim()));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractBitgetMsg(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            JsonNode r = objectMapper.readTree(body);
            String msg = r.path("msg").asText(null);
            return StringUtils.hasText(msg) ? msg.trim() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String shortenSafeMessage(String m) {
        if (!StringUtils.hasText(m)) {
            return "未知错误";
        }
        String t = m.replace('\n', ' ').trim();
        if (t.length() > 120) {
            return t.substring(0, 120) + "…";
        }
        return t;
    }
}
