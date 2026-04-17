package com.btg.commission.service;

import com.btg.commission.dto.v1.ReplenishmentCapitalSubmitRequest;

public interface ReplenishmentWorkflowService {

    void approveByAdmin(Long applyId, Long adminUserId, String remark);

    void rejectByAdmin(Long applyId, Long adminUserId, String remark);

    void assignCapital(Long applyId, Long adminUserId, Long capitalUserId, String remark);

    void capitalSubmit(Long applyId, Long capitalUserId, ReplenishmentCapitalSubmitRequest dto);

    void confirmArrival(Long applyId, Long applicantUserId, String remark);

    void rejectArrival(Long applyId, Long applicantUserId, String remark);
}
