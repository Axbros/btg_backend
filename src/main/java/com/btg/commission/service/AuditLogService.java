package com.btg.commission.service;

import com.btg.commission.entity.AuditLog;
import com.btg.commission.enums.AuditAction;
import com.btg.commission.enums.AuditBusinessType;
import com.btg.commission.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public void log(AuditBusinessType businessType, Long businessId, AuditAction action, Long operatorUserId, String remark) {
        AuditLog log = new AuditLog();
        log.setBusinessType(businessType.getCode());
        log.setBusinessId(businessId);
        log.setAction(action.getCode());
        log.setOperatorUserId(operatorUserId);
        log.setRemark(remark);
        auditLogMapper.insert(log);
    }
}
