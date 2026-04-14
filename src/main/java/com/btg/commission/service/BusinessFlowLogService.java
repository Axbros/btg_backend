package com.btg.commission.service;

import com.btg.commission.entity.BtgBusinessFlowLog;
import com.btg.commission.enums.BusinessFlowType;
import com.btg.commission.enums.FlowAction;
import com.btg.commission.enums.FlowNodeRole;

import java.util.List;

public interface BusinessFlowLogService {

    void append(
            BusinessFlowType businessType,
            Long businessId,
            Long rootBusinessId,
            Long nodeUserId,
            FlowNodeRole nodeRole,
            FlowAction action,
            String statusAfterAction,
            int versionNo,
            String remark,
            Long operatorUserId);

    List<BtgBusinessFlowLog> listForBusiness(BusinessFlowType businessType, Long businessId);
}
