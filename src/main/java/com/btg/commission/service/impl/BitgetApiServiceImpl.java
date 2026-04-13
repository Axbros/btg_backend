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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BitgetApiServiceImpl implements BitgetApiService {

    /** Bitget 全账户余额，见 https://www.bitget.com/zh-CN/api-doc/common/account/All-Account-Balance */
    private static final String ALL_ACCOUNT_BALANCE_PATH = "/api/v2/account/all-account-balance";
    private static final String API_LABEL = "ALL_ACCOUNT_BALANCE";
    private static final DateTimeFormatter SYNC_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserProfileMapper userProfileMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final BitgetProperties bitgetProperties;

    @Override
    public BitgetAssetSummaryVO queryCurrentUserAssets(Long userId) {
        String lastSync = SYNC_TIME_FMT.format(LocalDateTime.now());
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
                    .productType(API_LABEL)
                    .accounts(Collections.emptyList())
                    .totalUsdtBalance(null)
                    .lastSyncTime(lastSync)
                    .build();
        }
        String accessKey = profile.getBitgetAccessKey().trim();
        String secretKey = profile.getBitgetSecretKey().trim();
        String passphrase = profile.getBitgetPassphrase().trim();
        try {
            String ts = String.valueOf(System.currentTimeMillis());
            String sign = BitgetSignUtil.sign(ts, "GET", ALL_ACCOUNT_BALANCE_PATH, "", "", secretKey);
            String url = bitgetProperties.getBaseUrl() + ALL_ACCOUNT_BALANCE_PATH;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ACCESS-KEY", accessKey);
            headers.set("ACCESS-SIGN", sign);
            headers.set("ACCESS-TIMESTAMP", ts);
            headers.set("ACCESS-PASSPHRASE", passphrase);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return parseAllAccountBalance(resp.getBody(), lastSync);
        } catch (RestClientResponseException e) {
            int sc = e.getStatusCode().value();
            log.warn("Bitget all-account-balance HTTP error, userId={}, status={}", userId, sc);
            String hint = extractBitgetMsg(e.getResponseBodyAsString());
            String msg = hint != null
                    ? "资产查询失败：" + shortenSafeMessage(hint)
                    : "资产查询失败：HTTP " + sc;
            return failSummary(msg, lastSync);
        } catch (Exception e) {
            log.warn("Bitget all-account-balance failed, userId={}, type={}", userId, e.getClass().getSimpleName());
            return failSummary("资产查询失败：" + shortenSafeMessage(e.getMessage()), lastSync);
        }
    }

    private static BitgetAssetSummaryVO failSummary(String message, String lastSync) {
        return BitgetAssetSummaryVO.builder()
                .success(false)
                .message(message)
                .productType(API_LABEL)
                .accounts(Collections.emptyList())
                .totalUsdtBalance(null)
                .lastSyncTime(lastSync)
                .build();
    }

    private BitgetAssetSummaryVO parseAllAccountBalance(String body, String lastSync) {
        if (!StringUtils.hasText(body)) {
            return failSummary("资产查询失败：空响应", lastSync);
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            String code = root.path("code").asText("");
            if (!"00000".equals(code)) {
                String msg = root.path("msg").asText("接口返回错误");
                return failSummary("资产查询失败：" + shortenSafeMessage(msg), lastSync);
            }
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) {
                return BitgetAssetSummaryVO.builder()
                        .success(true)
                        .message("ok")
                        .productType(API_LABEL)
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
                String accountType = firstTextOrNumberAsText(row, "accountType", "account_type");
                String bal = firstNumericString(row, "usdtBalance", "USDTBalance", "balance");
                BigDecimal v = parseDecimalLoose(bal);
                if (v != null) {
                    total = total.add(v);
                }
                accounts.add(BitgetAssetAccountVO.builder()
                        .coin(accountType)
                        .accountType(accountType)
                        .usdtBalance(bal)
                        .usdtAvailable(null)
                        .usdtFrozen(null)
                        .build());
            }
            return BitgetAssetSummaryVO.builder()
                    .success(true)
                    .message("ok")
                    .productType(API_LABEL)
                    .accounts(accounts)
                    .totalUsdtBalance(MoneyUtil.money(total).toPlainString())
                    .lastSyncTime(lastSync)
                    .build();
        } catch (Exception e) {
            log.warn("Bitget all-account-balance parse failed, type={}", e.getClass().getSimpleName());
            return failSummary("资产查询失败：响应解析异常", lastSync);
        }
    }

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
