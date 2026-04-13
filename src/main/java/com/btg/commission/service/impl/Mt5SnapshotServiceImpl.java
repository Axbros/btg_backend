package com.btg.commission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.dto.mt5.Mt5SnapshotReportDTO;
import com.btg.commission.entity.BtgMt5AccountSnapshot;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.mapper.BtgMt5AccountSnapshotMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.service.Mt5SnapshotService;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.vo.Mt5SnapshotVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class Mt5SnapshotServiceImpl implements Mt5SnapshotService {

    public static final String SOURCE_EA_PUSH = "EA_PUSH";

    private final BtgMt5AccountSnapshotMapper snapshotMapper;
    private final UserProfileMapper userProfileMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reportSnapshot(Mt5SnapshotReportDTO dto) {
        String accountId = dto.getAccountId().trim();
        Long userId = resolveUserIdByTradingAccountId(accountId);

        BtgMt5AccountSnapshot row = new BtgMt5AccountSnapshot();
        row.setUserId(userId);
        row.setAccountId(accountId);
        row.setServerName(dto.getServerName().trim());
        row.setBalance(MoneyUtil.money(dto.getBalance()));
        row.setEquity(MoneyUtil.money(dto.getEquity()));
        row.setLastBalance(MoneyUtil.money(dto.getLastBalance()));
        row.setLastEquity(MoneyUtil.money(dto.getLastEquity()));
        row.setProfit(dto.getProfit() == null ? null : MoneyUtil.money(dto.getProfit()));
        row.setMarginAmount(dto.getMarginAmount() == null ? null : MoneyUtil.money(dto.getMarginAmount()));
        row.setFreeMargin(dto.getFreeMargin() == null ? null : MoneyUtil.money(dto.getFreeMargin()));
        row.setMarginLevel(dto.getMarginLevel() == null ? null : MoneyUtil.money(dto.getMarginLevel()));
        row.setSource(SOURCE_EA_PUSH);
        row.setSnapshotTime(dto.getSnapshotTime() != null ? dto.getSnapshotTime() : LocalDateTime.now());
        row.setRawPayload(serializeRawPayload(dto));

        snapshotMapper.insert(row);
    }

    @Override
    public Mt5SnapshotVO getLatestByAccountId(String accountId) {
        if (!StringUtils.hasText(accountId)) {
            return null;
        }
        BtgMt5AccountSnapshot row = snapshotMapper.selectLatestByAccountId(accountId.trim());
        return row == null ? null : toVo(row);
    }

    private Long resolveUserIdByTradingAccountId(String accountId) {
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getTradingAccountId, accountId)
                .last("LIMIT 1"));
        return profile == null ? null : profile.getUserId();
    }

    private static Mt5SnapshotVO toVo(BtgMt5AccountSnapshot e) {
        return Mt5SnapshotVO.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .accountId(e.getAccountId())
                .serverName(e.getServerName())
                .balance(e.getBalance())
                .equity(e.getEquity())
                .lastBalance(e.getLastBalance())
                .lastEquity(e.getLastEquity())
                .profit(e.getProfit())
                .marginAmount(e.getMarginAmount())
                .freeMargin(e.getFreeMargin())
                .marginLevel(e.getMarginLevel())
                .snapshotTime(e.getSnapshotTime())
                .build();
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
