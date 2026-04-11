package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btg.commission.entity.CommissionRecord;
import com.btg.commission.entity.CommissionStrategy;
import com.btg.commission.entity.ProfitRecord;
import com.btg.commission.entity.SysUser;
import com.btg.commission.mapper.CommissionRecordMapper;
import com.btg.commission.mapper.CommissionStrategyMapper;
import com.btg.commission.mapper.ProfitRecordMapper;
import com.btg.commission.mapper.SysUserMapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.vo.CommissionMineListItemVo;
import com.btg.commission.vo.CommissionRecordVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommissionQueryService {

    private final CommissionRecordMapper commissionRecordMapper;
    private final ProfitRecordMapper profitRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final CommissionStrategyMapper commissionStrategyMapper;

    public Page<CommissionMineListItemVo> pageReceived(Long userId, long page, long size) {
        Page<CommissionRecord> mp = new Page<>(page, size);
        commissionRecordMapper.selectPage(mp, new LambdaQueryWrapper<CommissionRecord>()
                .eq(CommissionRecord::getToUserId, userId)
                .orderByDesc(CommissionRecord::getConfirmedTime));
        List<CommissionRecord> rows = mp.getRecords();
        Map<Long, String> profitNoById = loadProfitRecordNos(rows);
        Page<CommissionMineListItemVo> out = new Page<>(mp.getCurrent(), mp.getSize(), mp.getTotal());
        out.setRecords(rows.stream().map(c -> toListItem(c, profitNoById)).toList());
        return out;
    }

    /**
     * 当前用户作为收款方（toUser）的单条佣金流水详情；不存在返回 null（由调用方 404），非本人流水抛出 403。
     */
    public CommissionRecordVo getReceivedDetail(Long userId, Long commissionRecordId) {
        CommissionRecord c = commissionRecordMapper.selectById(commissionRecordId);
        if (c == null) {
            return null;
        }
        if (!userId.equals(c.getToUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "无权查看该佣金流水");
        }
        List<CommissionRecord> single = List.of(c);
        Map<Long, String> profitNoById = loadProfitRecordNos(single);
        Map<Long, SysUser> userById = loadUsers(single);
        Map<Long, String> strategyNameById = loadStrategyNames(single);
        return toVo(c, profitNoById, userById, strategyNameById);
    }

    private CommissionMineListItemVo toListItem(CommissionRecord c, Map<Long, String> profitNoById) {
        return CommissionMineListItemVo.builder()
                .id(c.getId())
                .profitRecordNo(c.getProfitRecordId() != null ? profitNoById.get(c.getProfitRecordId()) : null)
                .profitAmount(c.getProfitAmount())
                .commissionAmount(c.getCommissionAmount())
                .commissionRate(c.getCommissionRate())
                .status(c.getStatus())
                .build();
    }

    private Map<Long, String> loadProfitRecordNos(List<CommissionRecord> rows) {
        List<Long> ids = rows.stream()
                .map(CommissionRecord::getProfitRecordId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return profitRecordMapper.selectList(new LambdaQueryWrapper<ProfitRecord>().in(ProfitRecord::getId, ids))
                .stream()
                .collect(Collectors.toMap(ProfitRecord::getId, ProfitRecord::getRecordNo, (a, b) -> a));
    }

    private Map<Long, SysUser> loadUsers(List<CommissionRecord> rows) {
        List<Long> ids = rows.stream()
                .map(CommissionRecord::getFromUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, ids))
                .stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
    }

    private Map<Long, String> loadStrategyNames(List<CommissionRecord> rows) {
        List<Long> ids = rows.stream()
                .map(CommissionRecord::getStrategyId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return commissionStrategyMapper.selectList(new LambdaQueryWrapper<CommissionStrategy>().in(CommissionStrategy::getId, ids))
                .stream()
                .collect(Collectors.toMap(CommissionStrategy::getId, CommissionStrategy::getStrategyName, (a, b) -> a));
    }

    private CommissionRecordVo toVo(
            CommissionRecord c,
            Map<Long, String> profitNoById,
            Map<Long, SysUser> userById,
            Map<Long, String> strategyNameById) {
        SysUser from = c.getFromUserId() != null ? userById.get(c.getFromUserId()) : null;
        return CommissionRecordVo.builder()
                .id(c.getId())
                .profitRecordId(c.getProfitRecordId())
                .fromUserId(c.getFromUserId())
                .toUserId(c.getToUserId())
                .strategyId(c.getStrategyId())
                .commissionRate(c.getCommissionRate())
                .profitAmount(c.getProfitAmount())
                .commissionAmount(c.getCommissionAmount())
                .status(c.getStatus())
                .confirmedTime(c.getConfirmedTime())
                .profitRecordNo(c.getProfitRecordId() != null ? profitNoById.get(c.getProfitRecordId()) : null)
                .fromNickname(from != null ? from.getNickname() : null)
                .fromMobile(from != null ? from.getMobile() : null)
                .strategyName(c.getStrategyId() != null ? strategyNameById.get(c.getStrategyId()) : null)
                .build();
    }
}
