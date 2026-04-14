package com.btg.commission.util;

import com.btg.commission.entity.BtgBusinessFlowLog;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.ProfitReport;
import com.btg.commission.entity.SettlementOrder;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.service.UserService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 利润单 / 结算链「可见用户范围」：申报人看全链；上级只看「申报人 → 自己」路径上的节点；
 * 其他有权限用户（如链上参与但非邀请上级）仅看本人子树与链的交集。
 */
public final class ProfitFlowScope {

    private ProfitFlowScope() {
    }

    public static Set<Long> chainParticipantIds(ProfitReport report, List<SettlementOrder> orders) {
        Set<Long> ids = new LinkedHashSet<>();
        if (report.getReportUserId() != null) {
            ids.add(report.getReportUserId());
        }
        if (report.getDirectParentUserId() != null) {
            ids.add(report.getDirectParentUserId());
        }
        for (SettlementOrder o : orders) {
            if (o.getFromUserId() != null) {
                ids.add(o.getFromUserId());
            }
            if (o.getToUserId() != null) {
                ids.add(o.getToUserId());
            }
        }
        return ids;
    }

    /**
     * 当前查看者可看到的用户 id 集合（用于过滤结算边与流转日志中的 node/operator）。
     */
    public static Set<Long> visibleUserIds(
            ProfitReport report,
            List<SettlementOrder> orders,
            Long viewerUserId,
            BtgUserMapper btgUserMapper,
            UserService userService) {
        Objects.requireNonNull(report, "report");
        Long r = report.getReportUserId();
        if (r == null || viewerUserId == null) {
            return Set.of();
        }
        Set<Long> chain = chainParticipantIds(report, orders);
        if (viewerUserId.equals(r)) {
            return chain;
        }
        BtgUser viewer = btgUserMapper.selectById(viewerUserId);
        if (viewer != null && Boolean.TRUE.equals(viewer.getIsRoot())) {
            return chain;
        }
        if (userService.isUpstreamOf(viewerUserId, r)) {
            Set<Long> path = pathFromReporterUpToAncestor(r, viewerUserId, btgUserMapper);
            if (!path.isEmpty()) {
                return path;
            }
        }
        Set<Long> subtree = new HashSet<>();
        subtree.add(viewerUserId);
        subtree.addAll(userService.listDescendantUserIds(viewerUserId));
        return chain.stream().filter(subtree::contains).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 从申报人沿 {@code referrer_user_id} 向上直到 ancestor（含两端）。若 ancestor 不是申报人上级则返回空集。
     */
    public static Set<Long> pathFromReporterUpToAncestor(Long reporterUserId, Long ancestorUserId, BtgUserMapper btgUserMapper) {
        Set<Long> out = new LinkedHashSet<>();
        Long cur = reporterUserId;
        while (cur != null) {
            out.add(cur);
            if (cur.equals(ancestorUserId)) {
                return out;
            }
            BtgUser u = btgUserMapper.selectById(cur);
            cur = u == null ? null : u.getReferrerUserId();
        }
        return new LinkedHashSet<>();
    }

    /**
     * 从申报人沿邀请链向上、且仅包含 {@code U} 中的用户，用于展示顺序。
     */
    public static List<Long> upwardOrderWithinScope(Long reporterUserId, Set<Long> u, BtgUserMapper btgUserMapper) {
        List<Long> out = new ArrayList<>();
        if (reporterUserId == null || u == null || u.isEmpty()) {
            return out;
        }
        Long cur = reporterUserId;
        int guard = 0;
        while (cur != null && u.contains(cur) && guard++ < 128) {
            out.add(cur);
            BtgUser user = btgUserMapper.selectById(cur);
            Long p = user == null ? null : user.getReferrerUserId();
            if (p == null || !u.contains(p)) {
                break;
            }
            cur = p;
        }
        return out;
    }

    public static List<SettlementOrder> filterSettlements(List<SettlementOrder> orders, Set<Long> visibleUserIds) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }
        List<SettlementOrder> out = new ArrayList<>();
        for (SettlementOrder o : orders) {
            Long from = o.getFromUserId();
            Long to = o.getToUserId();
            if (from != null && to != null && visibleUserIds.contains(from) && visibleUserIds.contains(to)) {
                out.add(o);
            }
        }
        return out;
    }

    public static List<BtgBusinessFlowLog> filterFlowLogs(List<BtgBusinessFlowLog> logs, Set<Long> visibleUserIds) {
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        List<BtgBusinessFlowLog> out = new ArrayList<>();
        for (BtgBusinessFlowLog log : logs) {
            Long node = log.getNodeUserId();
            Long op = log.getOperatorUserId();
            if (node != null && !visibleUserIds.contains(node)) {
                continue;
            }
            if (op != null && !visibleUserIds.contains(op)) {
                continue;
            }
            out.add(log);
        }
        return out;
    }
}
