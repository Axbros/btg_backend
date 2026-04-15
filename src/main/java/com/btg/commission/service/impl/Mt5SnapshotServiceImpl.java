package com.btg.commission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.mt5.Mt5SnapshotReportDTO;
import com.btg.commission.entity.BtgMt5AccountSnapshot;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.mapper.BtgMt5AccountSnapshotMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.redis.Mt5SnapshotRedisKey;
import com.btg.commission.service.Mt5SnapshotService;
import com.btg.commission.util.BigDecimalCompare;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.vo.Mt5SnapshotCacheVO;
import com.btg.commission.vo.Mt5SnapshotVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class Mt5SnapshotServiceImpl implements Mt5SnapshotService {

    public static final String SOURCE_EA_PUSH = "EA_PUSH";

    private static final String UNBOUND_ACCOUNT_MESSAGE =
            "账户未在系统中登记（trading_account_id 无匹配），不允许上报";

    private final BtgMt5AccountSnapshotMapper snapshotMapper;
    private final UserProfileMapper userProfileMapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reportSnapshot(Mt5SnapshotReportDTO dto) {
        String accountId = dto.getAccountId().trim();
        Long userId = requireUserIdByTradingAccountId(accountId);

        BigDecimal balance = MoneyUtil.money(dto.getBalance());
        BigDecimal equity = MoneyUtil.money(dto.getEquity());
        BigDecimal profit = optionalMoney(dto.getProfit());
        BigDecimal marginAmount = optionalMoney(dto.getMarginAmount());
        BigDecimal freeMargin = optionalMoney(dto.getFreeMargin());
        BigDecimal marginLevel = optionalMoney(dto.getMarginLevel());

        Mt5SnapshotCacheVO cached = readLatestFromRedis(accountId);
        if (cached != null
                && keyMetricsUnchanged(
                        cached, balance, equity, profit, marginAmount, freeMargin, marginLevel)) {
            touchRedisTtl(accountId);
            return;
        }

        BtgMt5AccountSnapshot row = new BtgMt5AccountSnapshot();
        row.setUserId(userId);
        row.setAccountId(accountId);
        row.setServerName(dto.getServerName().trim());
        row.setBalance(balance);
        row.setEquity(equity);
        row.setLastBalance(MoneyUtil.money(dto.getLastBalance()));
        row.setLastEquity(MoneyUtil.money(dto.getLastEquity()));
        row.setProfit(profit);
        row.setMarginAmount(marginAmount);
        row.setFreeMargin(freeMargin);
        row.setMarginLevel(marginLevel);
        row.setSource(SOURCE_EA_PUSH);
        row.setSnapshotTime(dto.getSnapshotTime() != null ? dto.getSnapshotTime() : LocalDateTime.now());
        row.setRawPayload(serializeRawPayload(dto));

        snapshotMapper.insert(row);
        writeLatestToRedis(Mt5SnapshotCacheVO.fromEntity(row));
    }

    @Override
    public Mt5SnapshotVO getLatestSnapshotForUser(Long userId) {
        if (userId == null) {
            return null;
        }
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
        String accountId = profile == null ? null : profile.getTradingAccountId();
        if (StringUtils.hasText(accountId)) {
            String trimmed = accountId.trim();
            Mt5SnapshotCacheVO fromRedis = readLatestFromRedis(trimmed);
            if (fromRedis != null) {
                return fromRedis.toApiVo();
            }
            BtgMt5AccountSnapshot row = snapshotMapper.selectLatestByAccountId(trimmed);
            if (row != null) {
                Mt5SnapshotCacheVO vo = Mt5SnapshotCacheVO.fromEntity(row);
                writeLatestToRedis(vo);
                return vo.toApiVo();
            }
            return null;
        }
        BtgMt5AccountSnapshot row = snapshotMapper.selectLatestByUserId(userId);
        if (row != null && StringUtils.hasText(row.getAccountId())) {
            writeLatestToRedis(Mt5SnapshotCacheVO.fromEntity(row));
        }
        return row == null ? null : toVo(row);
    }

    private Long requireUserIdByTradingAccountId(String accountId) {
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getTradingAccountId, accountId)
                .last("LIMIT 1"));
        if (profile == null || profile.getUserId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, UNBOUND_ACCOUNT_MESSAGE);
        }
        return profile.getUserId();
    }

    private static boolean keyMetricsUnchanged(
            Mt5SnapshotCacheVO cached,
            BigDecimal balance,
            BigDecimal equity,
            BigDecimal profit,
            BigDecimal marginAmount,
            BigDecimal freeMargin,
            BigDecimal marginLevel) {
        return BigDecimalCompare.sameValue(cached.getBalance(), balance)
                && BigDecimalCompare.sameValue(cached.getEquity(), equity)
                && BigDecimalCompare.sameValue(cached.getProfit(), profit)
                && BigDecimalCompare.sameValue(cached.getMarginAmount(), marginAmount)
                && BigDecimalCompare.sameValue(cached.getFreeMargin(), freeMargin)
                && BigDecimalCompare.sameValue(cached.getMarginLevel(), marginLevel);
    }

    private static BigDecimal optionalMoney(BigDecimal v) {
        return v == null ? null : MoneyUtil.money(v);
    }

    private Mt5SnapshotCacheVO readLatestFromRedis(String accountId) {
        String key = Mt5SnapshotRedisKey.latest(accountId);
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(json)) {
                return null;
            }
            return objectMapper.readValue(json, Mt5SnapshotCacheVO.class);
        } catch (JsonProcessingException e) {
            log.warn("MT5 snapshot redis parse failed for {}: {}", accountId, e.getMessage());
            return null;
        }
    }

    private void writeLatestToRedis(Mt5SnapshotCacheVO vo) {
        if (vo == null || !StringUtils.hasText(vo.getAccountId())) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(vo);
            String key = Mt5SnapshotRedisKey.latest(vo.getAccountId().trim());
            stringRedisTemplate.opsForValue().set(key, json, Mt5SnapshotRedisKey.LATEST_TTL);
        } catch (JsonProcessingException e) {
            log.warn("MT5 snapshot redis write serialize failed: {}", e.getMessage());
        }
    }

    private void touchRedisTtl(String accountId) {
        String key = Mt5SnapshotRedisKey.latest(accountId);
        Boolean ok = stringRedisTemplate.expire(key, Mt5SnapshotRedisKey.LATEST_TTL);
        if (Boolean.FALSE.equals(ok)) {
            Mt5SnapshotCacheVO cached = readLatestFromRedis(accountId);
            if (cached != null) {
                writeLatestToRedis(cached);
            }
        }
    }

    private static Mt5SnapshotVO toVo(BtgMt5AccountSnapshot e) {
        return Mt5SnapshotCacheVO.fromEntity(e).toApiVo();
    }

    private String serializeRawPayload(Mt5SnapshotReportDTO dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.warn("MT5 snapshot raw_payload serialize failed: {}", e.getMessage());
            return null;
        }
    }
}
