package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.strategy.StrategySaveRequest;
import com.btg.commission.entity.CommissionStrategy;
import com.btg.commission.enums.StrategyStatus;
import com.btg.commission.mapper.CommissionStrategyMapper;
import com.btg.commission.util.MoneyUtil;
import com.btg.commission.vo.CommissionStrategyVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommissionStrategyService {

    private final CommissionStrategyMapper commissionStrategyMapper;

    public List<CommissionStrategyVo> listAllOrdered() {
        return commissionStrategyMapper.selectList(new LambdaQueryWrapper<CommissionStrategy>()
                        .orderByAsc(CommissionStrategy::getSortNo)
                        .orderByAsc(CommissionStrategy::getId))
                .stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    public List<CommissionStrategyVo> listEnabled() {
        return commissionStrategyMapper.selectList(new LambdaQueryWrapper<CommissionStrategy>()
                        .eq(CommissionStrategy::getStatus, StrategyStatus.ENABLED)
                        .orderByAsc(CommissionStrategy::getSortNo)
                        .orderByAsc(CommissionStrategy::getId))
                .stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    public CommissionStrategyVo get(Long id) {
        CommissionStrategy s = commissionStrategyMapper.selectById(id);
        if (s == null) {
            throw new BizException(ResultCode.NOT_FOUND, "strategy not found");
        }
        return toVo(s);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(StrategySaveRequest req) {
        Long dup = commissionStrategyMapper.selectCount(new LambdaQueryWrapper<CommissionStrategy>()
                .eq(CommissionStrategy::getStrategyCode, req.getStrategyCode()));
        if (dup != null && dup > 0) {
            throw new BizException(ResultCode.CONFLICT, "strategy code exists");
        }
        CommissionStrategy s = new CommissionStrategy();
        s.setStrategyCode(req.getStrategyCode());
        s.setStrategyName(req.getStrategyName());
        s.setCommissionRate(MoneyUtil.rate(req.getCommissionRate()));
        s.setDescription(req.getDescription());
        s.setStatus(req.getStatus());
        s.setSortNo(req.getSortNo());
        commissionStrategyMapper.insert(s);
        return s.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, StrategySaveRequest req) {
        CommissionStrategy existing = commissionStrategyMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ResultCode.NOT_FOUND, "strategy not found");
        }
        Long dup = commissionStrategyMapper.selectCount(new LambdaQueryWrapper<CommissionStrategy>()
                .eq(CommissionStrategy::getStrategyCode, req.getStrategyCode())
                .ne(CommissionStrategy::getId, id));
        if (dup != null && dup > 0) {
            throw new BizException(ResultCode.CONFLICT, "strategy code exists");
        }
        existing.setStrategyCode(req.getStrategyCode());
        existing.setStrategyName(req.getStrategyName());
        existing.setCommissionRate(MoneyUtil.rate(req.getCommissionRate()));
        existing.setDescription(req.getDescription());
        existing.setStatus(req.getStatus());
        existing.setSortNo(req.getSortNo());
        commissionStrategyMapper.updateById(existing);
    }

    private CommissionStrategyVo toVo(CommissionStrategy s) {
        return CommissionStrategyVo.builder()
                .id(s.getId())
                .strategyCode(s.getStrategyCode())
                .strategyName(s.getStrategyName())
                .commissionRate(s.getCommissionRate())
                .description(s.getDescription())
                .status(s.getStatus())
                .sortNo(s.getSortNo())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
