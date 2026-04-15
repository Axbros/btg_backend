package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.ProfitDistribution;
import com.btg.commission.entity.ProfitReport;
import com.btg.commission.entity.SettlementOrder;
import com.btg.commission.enums.ProfitReportStatus;
import com.btg.commission.enums.SettlementOrderStatus;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.mapper.ProfitDistributionMapper;
import com.btg.commission.mapper.ProfitReportMapper;
import com.btg.commission.mapper.SettlementOrderMapper;
import com.btg.commission.vo.flow.ProfitFlowDetailVO;
import com.btg.commission.vo.flow.ProfitFlowLayerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 利润分润链路详情：权限、总利润切片与结算状态组装。
 *
 * <p>规则摘要：
 * <ul>
 *   <li>可访问：申报人、申报人在邀请链上的任意祖先上级、根用户、或本单分润/结算链上的参与用户。</li>
 *   <li>根：返回全链路 layers 与全量金额。</li>
 *   <li>非根上级（邀请链祖先）：仅返回「本用户在分润链上的层级及以下」layers，金额对可见层全量展示。</li>
 *   <li>申报人：仅返回本人切片层 + {@code directParent*} 直属上级处理摘要；顶层流转文案对齐直属上级环节。</li>
 *   <li>其他链上参与方：自链上位置向下切片，之上不返回。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ProfitFlowDetailService implements ProfitFlowDetailQuery {

    private static final String SCOPE_FULL = "FULL_FINANCIAL";
    private static final String SCOPE_ANCESTOR_SUBCHAIN = "ANCESTOR_SUBCHAIN_FINANCIAL";
    private static final String SCOPE_REPORTER = "REPORTER_SUBCHAIN";
    private static final String SCOPE_CHAIN_PARTICIPANT = "CHAIN_PARTICIPANT_SUBCHAIN";

    private static final String PA_NONE = "NONE";
    private static final String PA_APPLICANT_RESUBMIT = "APPLICANT_RESUBMIT";
    private static final String PA_DIRECT_PARENT_REVIEW = "DIRECT_PARENT_REVIEW";
    private static final String PA_SUBMIT_TRANSFER_PROOF = "SUBMIT_TRANSFER_PROOF";
    private static final String PA_SETTLEMENT_REVIEW = "SETTLEMENT_REVIEW";
    private static final String PA_AWAIT_UPPER_LAYER = "AWAIT_UPPER_LAYER";

    private final ProfitReportMapper profitReportMapper;
    private final ProfitDistributionMapper profitDistributionMapper;
    private final SettlementOrderMapper settlementOrderMapper;
    private final BtgUserMapper btgUserMapper;
    private final UserService userService;

    /**
     * 按 root_report_id 查询利润分润链路详情（含切片与结算状态）；无权限则抛出 FORBIDDEN。
     */
    @Override
    public ProfitFlowDetailVO getProfitFlowDetailByRootReportId(Long rootReportId, Long currentUserId) {
        ProfitReport report = profitReportMapper.selectById(rootReportId);
        if (report == null) {
            throw new BizException(ResultCode.NOT_FOUND, "利润上报不存在");
        }
        if (!canAccessProfitFlow(report, currentUserId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权查看该利润分润链路");
        }

        List<ProfitDistribution> distributions = profitDistributionMapper.selectList(
                new LambdaQueryWrapper<ProfitDistribution>()
                        .eq(ProfitDistribution::getReportId, rootReportId)
                        .orderByAsc(ProfitDistribution::getLevelNo));

        List<SettlementOrder> settlements = settlementOrderMapper.selectList(
                new LambdaQueryWrapper<SettlementOrder>()
                        .eq(SettlementOrder::getRootReportId, rootReportId)
                        .orderByAsc(SettlementOrder::getLevelNo));

        String dataScope = resolveDataScope(report, currentUserId);
        boolean viewerRoot = isRoot(currentUserId);
        boolean viewerReporter = Objects.equals(currentUserId, report.getReportUserId());
        int minLayerIndex = resolveMinVisibleLayerIndex(report, currentUserId, viewerRoot, viewerReporter, distributions, settlements);
        int maskAboveExclusive = -1;

        List<Long> chainUserIds = chainUserIds(distributions);
        SettlementOrder bottleneck = findBottleneckSettlementScoped(settlements, chainUserIds, minLayerIndex);

        Map<Long, BtgUser> userCache = new HashMap<>();

        List<ProfitFlowLayerVO> layers = buildLayers(
                report,
                distributions,
                settlements,
                bottleneck,
                maskAboveExclusive,
                minLayerIndex,
                userCache);

        FlowSummary summary = buildFlowSummary(report, bottleneck, settlements, userCache, viewerReporter);

        BtgUser applicant = loadUser(userCache, report.getReportUserId());
        DirectParentSlice dp = viewerReporter ? buildDirectParentSlice(report, settlements, userCache) : null;
        Long handlerUserId = summary.effectiveHandlerUserId != null
                ? summary.effectiveHandlerUserId
                : report.getCurrentHandlerUserId();
        return ProfitFlowDetailVO.builder()
                .reportId(report.getId())
                .reportNo(report.getReportNo())
                .reportUserId(report.getReportUserId())
                .reportUserName(displayName(applicant))
                .profitAmount(report.getProfitAmount())
                .currentHandlerUserId(handlerUserId)
                .currentHandlerUserName(displayName(loadUser(userCache, handlerUserId)))
                .status(report.getStatus())
                .flowStatus(report.getFlowStatus())
                .submitVersion(report.getSubmitVersion() == null ? 1 : report.getSubmitVersion())
                .lastRejectReason(report.getLastRejectReason())
                .dataScope(dataScope)
                .currentFlowStatus(summary.flowStatusCode)
                .currentFlowStatusDesc(summary.flowStatusDesc)
                .pendingAction(summary.pendingAction)
                .pendingActorDisplayName(summary.pendingActorDisplayName)
                .directParentStatus(dp == null ? null : dp.directParentStatus())
                .directParentStatusDesc(dp == null ? null : dp.directParentStatusDesc())
                .directParentAction(dp == null ? null : dp.directParentAction())
                .directParentReviewerName(dp == null ? null : dp.directParentReviewerName())
                .directParentRemark(dp == null ? null : dp.directParentRemark())
                .directParentOperateTime(dp == null ? null : dp.directParentOperateTime())
                .layers(layers)
                .build();
    }

    private boolean isRoot(Long userId) {
        BtgUser u = btgUserMapper.selectById(userId);
        return u != null && Boolean.TRUE.equals(u.getIsRoot());
    }

    private static List<Long> chainUserIds(List<ProfitDistribution> distributions) {
        if (distributions == null) {
            return List.of();
        }
        return distributions.stream().map(ProfitDistribution::getBeneficiaryUserId).collect(Collectors.toList());
    }

    /**
     * 结算瓶颈：仅考虑「付款方在分润链上索引 &gt;= minFromIndex」的边（自下而上第一条非 APPROVED）。
     */
    private static SettlementOrder findBottleneckSettlementScoped(
            List<SettlementOrder> settlements, List<Long> chainUserIds, int minFromIndex) {
        if (settlements == null || settlements.isEmpty() || chainUserIds.isEmpty()) {
            return null;
        }
        List<SettlementOrder> sorted = new ArrayList<>(settlements);
        sorted.sort(Comparator.comparing(SettlementOrder::getLevelNo, Comparator.nullsLast(Integer::compareTo)));
        for (SettlementOrder o : sorted) {
            int fi = fromUserChainIndex(o, chainUserIds);
            if (fi < minFromIndex) {
                continue;
            }
            if (o.getStatus() != SettlementOrderStatus.APPROVED) {
                return o;
            }
        }
        return null;
    }

    private static int fromUserChainIndex(SettlementOrder o, List<Long> chainUserIds) {
        if (o == null || o.getFromUserId() == null) {
            return -1;
        }
        for (int i = 0; i < chainUserIds.size(); i++) {
            if (Objects.equals(chainUserIds.get(i), o.getFromUserId())) {
                return i;
            }
        }
        return -1;
    }

    private int resolveMinVisibleLayerIndex(
            ProfitReport report,
            Long viewerUserId,
            boolean viewerRoot,
            boolean viewerReporter,
            List<ProfitDistribution> distributions,
            List<SettlementOrder> settlements) {
        if (distributions == null || distributions.isEmpty()) {
            return 0;
        }
        int n = distributions.size() - 1;
        if (viewerRoot) {
            return 0;
        }
        if (viewerReporter) {
            return n;
        }
        int idx = chainIndexOfUser(distributions, settlements, viewerUserId);
        if (idx < 0) {
            return 0;
        }
        return idx;
    }

    private record DirectParentSlice(
            String directParentStatus,
            String directParentStatusDesc,
            String directParentAction,
            String directParentReviewerName,
            String directParentRemark,
            LocalDateTime directParentOperateTime) {
    }

    private DirectParentSlice buildDirectParentSlice(
            ProfitReport report,
            List<SettlementOrder> settlements,
            Map<Long, BtgUser> userCache) {
        Long parentId = report.getDirectParentUserId();
        Long reporterId = report.getReportUserId();
        if (parentId == null || reporterId == null) {
            return null;
        }
        BtgUser parent = loadUser(userCache, parentId);
        String parentName = displayName(parent);
        SettlementOrder edge = findReporterToParentEdge(settlements, reporterId, parentId);

        if (report.getStatus() == ProfitReportStatus.RETURNED_TO_APPLICANT) {
            return new DirectParentSlice(
                    "RETURNED_TO_APPLICANT",
                    "直属上级已退回，请修改后重提",
                    PA_APPLICANT_RESUBMIT,
                    parentName,
                    report.getLastRejectReason(),
                    report.getLastRejectTime() != null ? report.getLastRejectTime() : report.getAuditTime());
        }
        if (report.getStatus() == ProfitReportStatus.PENDING_DIRECT_REVIEW) {
            return new DirectParentSlice(
                    "PENDING_DIRECT_REVIEW",
                    "待直属上级审核利润",
                    PA_DIRECT_PARENT_REVIEW,
                    parentName,
                    null,
                    null);
        }
        if (report.getStatus() == ProfitReportStatus.REJECTED) {
            return new DirectParentSlice(
                    "PROFIT_REJECTED",
                    "直属上级已拒绝该利润上报",
                    PA_NONE,
                    parentName,
                    StringUtils.hasText(report.getLastRejectReason())
                            ? report.getLastRejectReason().trim()
                            : (StringUtils.hasText(report.getAuditRemark()) ? report.getAuditRemark().trim() : null),
                    report.getAuditTime());
        }
        if (edge != null && edge.getStatus() != null) {
            return switch (edge.getStatus()) {
                case INIT -> new DirectParentSlice(
                        "SETTLEMENT_INIT",
                        "待链上开启本层结算（请等待上一层通过）",
                        PA_AWAIT_UPPER_LAYER,
                        parentName,
                        null,
                        null);
                case PENDING_SUBMIT -> new DirectParentSlice(
                        "SETTLEMENT_PENDING_SUBMIT",
                        "待您提交给直属上级的转账凭证",
                        PA_SUBMIT_TRANSFER_PROOF,
                        parentName,
                        null,
                        edge.getSubmitTime());
                case PENDING_REVIEW -> new DirectParentSlice(
                        "SETTLEMENT_PENDING_REVIEW",
                        "待直属上级审核您的转账凭证",
                        PA_SETTLEMENT_REVIEW,
                        parentName,
                        null,
                        edge.getSubmitTime());
                case APPROVED -> new DirectParentSlice(
                        "SETTLEMENT_APPROVED",
                        "直属上级已通过本笔上缴/结算",
                        PA_NONE,
                        parentName,
                        StringUtils.hasText(edge.getAuditRemark()) ? edge.getAuditRemark().trim() : null,
                        edge.getAuditTime());
                case REJECTED -> new DirectParentSlice(
                        "SETTLEMENT_REJECTED",
                        "直属上级已拒绝本笔上缴/结算",
                        PA_NONE,
                        parentName,
                        StringUtils.hasText(edge.getAuditRemark()) ? edge.getAuditRemark().trim() : null,
                        edge.getAuditTime());
            };
        }
        if (report.getStatus() == ProfitReportStatus.IN_SETTLEMENT_CHAIN) {
            return new DirectParentSlice(
                    "IN_SETTLEMENT_CHAIN",
                    "直属相关环节已完成，后续由上级链路处理",
                    PA_NONE,
                    parentName,
                    null,
                    null);
        }
        if (report.getStatus() == ProfitReportStatus.ALL_COMPLETED) {
            return new DirectParentSlice(
                    "ALL_COMPLETED",
                    "全链路已完成",
                    PA_NONE,
                    parentName,
                    null,
                    null);
        }
        return new DirectParentSlice(
                report.getStatus() == null ? null : report.getStatus().name(),
                describeProfitStatus(report.getStatus()),
                PA_NONE,
                parentName,
                null,
                null);
    }

    private static SettlementOrder findReporterToParentEdge(
            List<SettlementOrder> settlements, Long reporterId, Long parentId) {
        if (settlements == null) {
            return null;
        }
        for (SettlementOrder o : settlements) {
            if (Objects.equals(o.getFromUserId(), reporterId) && Objects.equals(o.getToUserId(), parentId)) {
                return o;
            }
        }
        return null;
    }

    private boolean canAccessProfitFlow(ProfitReport report, Long viewerUserId) {
        if (viewerUserId == null) {
            return false;
        }
        if (Objects.equals(viewerUserId, report.getReportUserId())) {
            return true;
        }
        BtgUser viewer = btgUserMapper.selectById(viewerUserId);
        if (viewer != null && Boolean.TRUE.equals(viewer.getIsRoot())) {
            return true;
        }
        if (userService.isUpstreamOf(viewerUserId, report.getReportUserId())) {
            return true;
        }
        Long d = profitDistributionMapper.selectCount(new LambdaQueryWrapper<ProfitDistribution>()
                .eq(ProfitDistribution::getReportId, report.getId())
                .eq(ProfitDistribution::getBeneficiaryUserId, viewerUserId));
        if (d != null && d > 0) {
            return true;
        }
        Long s = settlementOrderMapper.selectCount(new LambdaQueryWrapper<SettlementOrder>()
                .eq(SettlementOrder::getRootReportId, report.getId())
                .and(w -> w.eq(SettlementOrder::getFromUserId, viewerUserId)
                        .or()
                        .eq(SettlementOrder::getToUserId, viewerUserId)));
        return s != null && s > 0;
    }

    /** 与 layers 裁剪策略对应的数据范围标签（前端展示用）。 */
    private String resolveDataScope(ProfitReport report, Long viewerUserId) {
        BtgUser viewer = btgUserMapper.selectById(viewerUserId);
        if (viewer != null && Boolean.TRUE.equals(viewer.getIsRoot())) {
            return SCOPE_FULL;
        }
        if (Objects.equals(viewerUserId, report.getReportUserId())) {
            return SCOPE_REPORTER;
        }
        if (userService.isUpstreamOf(viewerUserId, report.getReportUserId())) {
            return SCOPE_ANCESTOR_SUBCHAIN;
        }
        return SCOPE_CHAIN_PARTICIPANT;
    }

    private int chainIndexOfUser(
            List<ProfitDistribution> distributions,
            List<SettlementOrder> settlements,
            Long userId) {
        for (int j = 0; j < distributions.size(); j++) {
            if (Objects.equals(userId, distributions.get(j).getBeneficiaryUserId())) {
                return j;
            }
        }
        if (settlements != null) {
            for (SettlementOrder o : settlements) {
                if (Objects.equals(userId, o.getFromUserId())) {
                    return beneficiaryIndex(distributions, o.getFromUserId());
                }
                if (Objects.equals(userId, o.getToUserId())) {
                    return beneficiaryIndex(distributions, o.getToUserId());
                }
            }
        }
        return -1;
    }

    private static int beneficiaryIndex(List<ProfitDistribution> distributions, Long beneficiaryUserId) {
        for (int j = 0; j < distributions.size(); j++) {
            if (Objects.equals(beneficiaryUserId, distributions.get(j).getBeneficiaryUserId())) {
                return j;
            }
        }
        return -1;
    }

    private List<ProfitFlowLayerVO> buildLayers(
            ProfitReport report,
            List<ProfitDistribution> distributions,
            List<SettlementOrder> settlements,
            SettlementOrder bottleneck,
            int maskAboveExclusive,
            int minLayerIndex,
            Map<Long, BtgUser> userCache) {

        List<ProfitFlowLayerVO> out = new ArrayList<>();
        if (distributions == null || distributions.isEmpty()) {
            return out;
        }

        Map<Long, SettlementOrder> payEdgeByFrom = new HashMap<>();
        if (settlements != null) {
            for (SettlementOrder o : settlements) {
                if (o.getFromUserId() != null) {
                    payEdgeByFrom.put(o.getFromUserId(), o);
                }
            }
        }

        List<Long> chainUserIds = distributions.stream()
                .map(ProfitDistribution::getBeneficiaryUserId)
                .collect(Collectors.toList());

        int n = chainUserIds.size() - 1;

        for (int j = 0; j < distributions.size(); j++) {
            if (j < minLayerIndex) {
                continue;
            }
            ProfitDistribution d = distributions.get(j);
            Long uid = d.getBeneficiaryUserId();
            Long parentId = j > 0 ? chainUserIds.get(j - 1) : null;
            Long childId = j < n ? chainUserIds.get(j + 1) : null;

            SettlementOrder edge = uid != null ? payEdgeByFrom.get(uid) : null;

            boolean maskFinancial = maskAboveExclusive >= 0 && j < maskAboveExclusive;
            BigDecimal upper = maskFinancial ? null : d.getUpperRatio();
            BigDecimal lower = maskFinancial ? null : d.getLowerRatio();
            BigDecimal income = maskFinancial ? null : d.getIncomeAmount();
            BigDecimal payUp = null;
            String settlementStatus = null;
            Long layerHandlerId = null;
            String layerHandlerName = null;
            String remark = null;
            LocalDateTime operateTime = null;

            if (j > 0 && edge != null) {
                if (!maskFinancial) {
                    payUp = edge.getPayAmount();
                }
                settlementStatus = edge.getStatus() == null ? null : edge.getStatus().name();
                layerHandlerId = settlementPendingActorUserId(edge);
                layerHandlerName = displayName(loadUser(userCache, layerHandlerId));
                remark = pickLayerRemark(report, j, n, edge);
                operateTime = pickOperateTime(edge);
            } else if (j == 0) {
                settlementStatus = null;
            }

            boolean currentNode = resolveCurrentNode(report, j, n, bottleneck, edge);

            out.add(ProfitFlowLayerVO.builder()
                    .levelNo(d.getLevelNo())
                    .userId(uid)
                    .userName(displayName(loadUser(userCache, uid)))
                    .parentUserId(parentId)
                    .parentUserName(displayName(loadUser(userCache, parentId)))
                    .childUserId(childId)
                    .childUserName(displayName(loadUser(userCache, childId)))
                    .upperRatio(upper)
                    .lowerRatio(lower)
                    .incomeAmount(income)
                    .payAmountToParent(payUp)
                    .settlementStatus(settlementStatus)
                    .currentHandlerUserId(layerHandlerId)
                    .currentHandlerUserName(layerHandlerName)
                    .remark(remark)
                    .operateTime(operateTime)
                    .currentNode(currentNode)
                    .financialsMasked(maskFinancial)
                    .build());
        }
        return out;
    }

    private static boolean resolveCurrentNode(
            ProfitReport report,
            int j,
            int n,
            SettlementOrder bottleneck,
            SettlementOrder edgeAtJ) {
        if (report.getStatus() == ProfitReportStatus.RETURNED_TO_APPLICANT) {
            return j == n;
        }
        if (report.getStatus() == ProfitReportStatus.PENDING_DIRECT_REVIEW) {
            if (bottleneck != null && Objects.equals(report.getReportUserId(), bottleneck.getFromUserId())) {
                return j == n;
            }
            return j == n;
        }
        if (report.getStatus() == ProfitReportStatus.REJECTED && j == n) {
            return true;
        }
        if (bottleneck != null && edgeAtJ != null && Objects.equals(edgeAtJ.getId(), bottleneck.getId())) {
            return true;
        }
        return false;
    }

    private static String pickLayerRemark(ProfitReport report, int j, int n, SettlementOrder edge) {
        if (edge.getStatus() == SettlementOrderStatus.REJECTED && StringUtils.hasText(edge.getAuditRemark())) {
            return edge.getAuditRemark().trim();
        }
        if (j == n && report.getStatus() == ProfitReportStatus.REJECTED) {
            if (StringUtils.hasText(report.getLastRejectReason())) {
                return report.getLastRejectReason().trim();
            }
            if (StringUtils.hasText(report.getAuditRemark())) {
                return report.getAuditRemark().trim();
            }
        }
        return null;
    }

    private static LocalDateTime pickOperateTime(SettlementOrder edge) {
        if (edge.getAuditTime() != null) {
            return edge.getAuditTime();
        }
        return edge.getSubmitTime();
    }

    private static Long settlementPendingActorUserId(SettlementOrder o) {
        if (o == null || o.getStatus() == null) {
            return null;
        }
        return switch (o.getStatus()) {
            case INIT, APPROVED, REJECTED -> null;
            case PENDING_SUBMIT -> o.getFromUserId();
            case PENDING_REVIEW -> o.getToUserId();
        };
    }

    private FlowSummary buildFlowSummary(
            ProfitReport report,
            SettlementOrder bottleneck,
            List<SettlementOrder> settlements,
            Map<Long, BtgUser> userCache,
            boolean viewerReporter) {
        if (report.getStatus() == ProfitReportStatus.RETURNED_TO_APPLICANT) {
            Long applicantId = report.getReportUserId();
            return new FlowSummary(
                    "RETURNED_TO_APPLICANT",
                    "已退回申报人，待修改后重提",
                    PA_APPLICANT_RESUBMIT,
                    displayName(loadUser(userCache, applicantId)),
                    applicantId);
        }
        if (report.getStatus() == ProfitReportStatus.PENDING_DIRECT_REVIEW) {
            Long reviewer = report.getDirectParentUserId();
            return new FlowSummary(
                    "PENDING_DIRECT_REVIEW",
                    "待直属上级审核利润",
                    PA_DIRECT_PARENT_REVIEW,
                    displayName(loadUser(userCache, reviewer)),
                    reviewer);
        }
        if (report.getStatus() == ProfitReportStatus.REJECTED) {
            return new FlowSummary(
                    "PROFIT_REJECTED",
                    "利润单已拒绝",
                    PA_NONE,
                    null,
                    null);
        }
        if (bottleneck != null) {
            return summaryForSettlement(report, bottleneck, userCache);
        }
        if (report.getStatus() == ProfitReportStatus.ALL_COMPLETED) {
            return new FlowSummary(
                    "ALL_COMPLETED",
                    "全链路结算已完成",
                    PA_NONE,
                    null,
                    null);
        }
        if (report.getStatus() == ProfitReportStatus.IN_SETTLEMENT_CHAIN
                && settlements != null
                && !settlements.isEmpty()
                && settlements.stream().allMatch(s -> s.getStatus() == SettlementOrderStatus.APPROVED)) {
            return new FlowSummary(
                    "IN_SETTLEMENT_CHAIN",
                    "逐级结算进行中（当前无待处理节点）",
                    PA_NONE,
                    null,
                    null);
        }
        if (viewerReporter && report.getStatus() == ProfitReportStatus.IN_SETTLEMENT_CHAIN) {
            return new FlowSummary(
                    "IN_SETTLEMENT_DIRECT_SCOPE_IDLE",
                    "您与直属上级相关环节暂无待办，后续由上级链路处理",
                    PA_NONE,
                    null,
                    null);
        }
        String fs = StringUtils.hasText(report.getFlowStatus()) ? report.getFlowStatus() : String.valueOf(report.getStatus());
        return new FlowSummary(fs, describeProfitStatus(report.getStatus()), PA_NONE, null, null);
    }

    private FlowSummary summaryForSettlement(ProfitReport report, SettlementOrder o, Map<Long, BtgUser> userCache) {
        return switch (o.getStatus()) {
            case INIT -> new FlowSummary(
                    "SETTLEMENT_INIT",
                    "该层结算尚未开始，等待上一层通过",
                    PA_AWAIT_UPPER_LAYER,
                    null,
                    null);
            case PENDING_SUBMIT -> new FlowSummary(
                    "SETTLEMENT_PENDING_SUBMIT",
                    "待付款人提交转账凭证",
                    PA_SUBMIT_TRANSFER_PROOF,
                    displayName(loadUser(userCache, o.getFromUserId())),
                    o.getFromUserId());
            case PENDING_REVIEW -> new FlowSummary(
                    "SETTLEMENT_PENDING_REVIEW",
                    "待收款上级审核结算",
                    PA_SETTLEMENT_REVIEW,
                    displayName(loadUser(userCache, o.getToUserId())),
                    o.getToUserId());
            case REJECTED -> new FlowSummary(
                    "SETTLEMENT_REJECTED",
                    "该层结算已拒绝",
                    PA_NONE,
                    null,
                    null);
            case APPROVED -> new FlowSummary(
                    String.valueOf(report.getStatus()),
                    describeProfitStatus(report.getStatus()),
                    PA_NONE,
                    null,
                    null);
        };
    }

    private static String describeProfitStatus(ProfitReportStatus s) {
        if (s == null) {
            return "";
        }
        return switch (s) {
            case PENDING_DIRECT_REVIEW -> "待直属上级审核利润";
            case IN_SETTLEMENT_CHAIN -> "逐级结算进行中";
            case REJECTED -> "利润单已拒绝";
            case ALL_COMPLETED -> "全链路结算已完成";
            case RETURNED_TO_APPLICANT -> "已退回申报人待修改";
        };
    }

    private BtgUser loadUser(Map<Long, BtgUser> cache, Long id) {
        if (id == null) {
            return null;
        }
        return cache.computeIfAbsent(id, btgUserMapper::selectById);
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

    private record FlowSummary(
            String flowStatusCode,
            String flowStatusDesc,
            String pendingAction,
            String pendingActorDisplayName,
            Long effectiveHandlerUserId) {
    }
}
