package com.btg.commission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.entity.BtgBusinessFlowLog;
import com.btg.commission.enums.BusinessFlowType;
import com.btg.commission.enums.FlowAction;
import com.btg.commission.enums.FlowNodeRole;
import com.btg.commission.mapper.BtgBusinessFlowLogMapper;
import com.btg.commission.service.BusinessFlowLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessFlowLogServiceImpl implements BusinessFlowLogService {

    private final BtgBusinessFlowLogMapper btgBusinessFlowLogMapper;

    @Override
    public void append(
            BusinessFlowType businessType,
            Long businessId,
            Long rootBusinessId,
            Long nodeUserId,
            FlowNodeRole nodeRole,
            FlowAction action,
            String statusAfterAction,
            int versionNo,
            String remark,
            Long operatorUserId) {
        BtgBusinessFlowLog row = new BtgBusinessFlowLog();
        row.setBusinessType(businessType.name());
        row.setBusinessId(businessId);
        row.setRootBusinessId(rootBusinessId);
        row.setNodeUserId(nodeUserId);
        row.setNodeRole(nodeRole == null ? null : nodeRole.name());
        row.setAction(action.name());
        row.setStatusAfterAction(statusAfterAction);
        row.setVersionNo(versionNo);
        row.setRemark(remark);
        row.setOperatorUserId(operatorUserId);
        btgBusinessFlowLogMapper.insert(row);
    }

    @Override
    public List<BtgBusinessFlowLog> listForBusiness(BusinessFlowType businessType, Long businessId) {
        return btgBusinessFlowLogMapper.selectList(new LambdaQueryWrapper<BtgBusinessFlowLog>()
                .eq(BtgBusinessFlowLog::getBusinessType, businessType.name())
                .eq(BtgBusinessFlowLog::getBusinessId, businessId)
                .orderByAsc(BtgBusinessFlowLog::getCreatedAt));
    }
}
