package com.btg.commission.service;

import com.btg.commission.entity.UserAccountSummary;
import com.btg.commission.vo.AccountSummaryVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountOverviewService {

    private final UserAccountSummaryService userAccountSummaryService;

    public AccountSummaryVo summary(Long userId) {
        UserAccountSummary s = userAccountSummaryService.getOrEmpty(userId);
        return AccountSummaryVo.builder()
                .totalProfitAmount(s.getTotalProfitAmount())
                .totalCommissionOutAmount(s.getTotalCommissionOutAmount())
                .totalCommissionInAmount(s.getTotalCommissionInAmount())
                .pendingCommissionOutAmount(s.getPendingCommissionOutAmount())
                .pendingCommissionInAmount(s.getPendingCommissionInAmount())
                .build();
    }
}
