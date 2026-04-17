package com.btg.commission.util;

import com.btg.commission.entity.BtgBusinessFlowLog;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.enums.FlowAction;
import com.btg.commission.enums.FlowNodeDisplayStatus;
import com.btg.commission.vo.flow.BusinessFlowNodeVO;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class FlowLogViewUtil {

    private FlowLogViewUtil() {
    }

    public static List<BusinessFlowNodeVO> toFlowNodes(
            List<BtgBusinessFlowLog> logs,
            Function<Long, BtgUser> userLoader) {
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        List<BusinessFlowNodeVO> out = new ArrayList<>(logs.size());
        for (BtgBusinessFlowLog log : logs) {
            Long opId = log.getOperatorUserId();
            BtgUser op = opId == null ? null : userLoader.apply(opId);
            Long nu = log.getNodeUserId();
            BtgUser nodeUser = nu == null ? null : userLoader.apply(nu);
            out.add(BusinessFlowNodeVO.builder()
                    .nodeUserId(nu)
                    .nodeName(displayName(nodeUser))
                    .nodeRole(log.getNodeRole())
                    .action(log.getAction())
                    .displayStatus(displayStatusForAction(log.getAction()))
                    .remark(log.getRemark())
                    .operateTime(log.getCreatedAt())
                    .operatorUserId(opId)
                    .operatorName(displayName(op))
                    .versionNo(log.getVersionNo())
                    .build());
        }
        return out;
    }

    private static String displayName(BtgUser u) {
        if (u == null) {
            return null;
        }
        if (StringUtils.hasText(u.getNickname())) {
            return u.getNickname().trim();
        }
        if (StringUtils.hasText(u.getMobile())) {
            return u.getMobile().trim();
        }
        return null;
    }

    private static FlowNodeDisplayStatus displayStatusForAction(String action) {
        if (action == null) {
            return FlowNodeDisplayStatus.IN_PROGRESS;
        }
        try {
            FlowAction a = FlowAction.valueOf(action);
            return switch (a) {
                case SUBMIT, RESUBMIT, ASSIGN -> FlowNodeDisplayStatus.PENDING_REVIEW;
                case APPROVE, ADVANCE -> FlowNodeDisplayStatus.APPROVED;
                case REJECT -> FlowNodeDisplayStatus.REJECTED;
                case RETURN_TO_APPLICANT -> FlowNodeDisplayStatus.RETURNED_FOR_EDIT;
                case CANCEL -> FlowNodeDisplayStatus.REJECTED;
            };
        } catch (IllegalArgumentException e) {
            return FlowNodeDisplayStatus.IN_PROGRESS;
        }
    }
}
