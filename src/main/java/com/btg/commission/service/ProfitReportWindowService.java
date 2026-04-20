package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.ProfitReportWindow;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.mapper.ProfitReportWindowMapper;
import com.btg.commission.vo.ProfitReportWindowTodayVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ProfitReportWindowService {

    public static final String MSG_NOT_IN_ALLOWED_TIME = "不在规定时间内，无法进行利润上报";

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final ProfitReportWindowMapper profitReportWindowMapper;
    private final BtgUserMapper btgUserMapper;

    /**
     * 仅当存在「business_date = 当日（上海）」且已开始、未结束的窗口行时允许上报。
     */
    public void requireOpenForProfitReport() {
        LocalDate today = LocalDate.now(ZONE);
        ProfitReportWindow w = profitReportWindowMapper.selectOne(new LambdaQueryWrapper<ProfitReportWindow>()
                .eq(ProfitReportWindow::getBusinessDate, today)
                .isNotNull(ProfitReportWindow::getOpenedAt)
                .isNull(ProfitReportWindow::getClosedAt));
        if (w == null) {
            throw new BizException(ResultCode.FORBIDDEN, MSG_NOT_IN_ALLOWED_TIME);
        }
    }

    public ProfitReportWindowTodayVO getTodayWindow(Long operatorUserId) {
        assertOperatorRoot(operatorUserId);
        LocalDate today = LocalDate.now(ZONE);
        ProfitReportWindow w = profitReportWindowMapper.selectOne(new LambdaQueryWrapper<ProfitReportWindow>()
                .eq(ProfitReportWindow::getBusinessDate, today));
        if (w == null) {
            return ProfitReportWindowTodayVO.builder()
                    .businessDate(today)
                    .recordExists(false)
                    .acceptingProfitReport(false)
                    .openedAt(null)
                    .closedAt(null)
                    .build();
        }
        boolean accepting = w.getOpenedAt() != null && w.getClosedAt() == null;
        return ProfitReportWindowTodayVO.builder()
                .businessDate(today)
                .recordExists(true)
                .acceptingProfitReport(accepting)
                .openedAt(w.getOpenedAt())
                .closedAt(w.getClosedAt())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void start(Long operatorUserId) {
        assertOperatorRoot(operatorUserId);
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime now = LocalDateTime.now(ZONE);

        ProfitReportWindow w = profitReportWindowMapper.selectByBusinessDateForUpdate(today);
        if (w == null) {
            ProfitReportWindow insert = new ProfitReportWindow();
            insert.setBusinessDate(today);
            insert.setOpenedAt(now);
            insert.setOpenedByUserId(operatorUserId);
            try {
                profitReportWindowMapper.insert(insert);
                return;
            } catch (RuntimeException e) {
                if (!isMysqlDuplicateKey(e)) {
                    throw e;
                }
                w = profitReportWindowMapper.selectByBusinessDateForUpdate(today);
            }
        }
        if (w == null) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "利润上报窗口初始化失败");
        }
        if (w.getOpenedAt() != null) {
            throw new BizException(ResultCode.CONFLICT, "今日已开始结算，请勿重复操作");
        }
        profitReportWindowMapper.update(
                null,
                new LambdaUpdateWrapper<ProfitReportWindow>()
                        .set(ProfitReportWindow::getOpenedAt, now)
                        .set(ProfitReportWindow::getOpenedByUserId, operatorUserId)
                        .eq(ProfitReportWindow::getId, w.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void stop(Long operatorUserId) {
        assertOperatorRoot(operatorUserId);
        LocalDate today = LocalDate.now(ZONE);
        LocalDateTime now = LocalDateTime.now(ZONE);
        ProfitReportWindow w = profitReportWindowMapper.selectByBusinessDateForUpdate(today);
        if (w == null || w.getOpenedAt() == null) {
            throw new BizException(ResultCode.CONFLICT, "今日尚未开始结算，无法结束");
        }
        if (w.getClosedAt() != null) {
            throw new BizException(ResultCode.CONFLICT, "今日已结束结算，请勿重复操作");
        }
        profitReportWindowMapper.update(
                null,
                new LambdaUpdateWrapper<ProfitReportWindow>()
                        .set(ProfitReportWindow::getClosedAt, now)
                        .set(ProfitReportWindow::getClosedByUserId, operatorUserId)
                        .eq(ProfitReportWindow::getId, w.getId()));
    }

    private static boolean isMysqlDuplicateKey(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof java.sql.SQLIntegrityConstraintViolationException sqlEx) {
                return sqlEx.getErrorCode() == 1062;
            }
        }
        return false;
    }

    private void assertOperatorRoot(Long operatorUserId) {
        if (operatorUserId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "未登录");
        }
        BtgUser op = btgUserMapper.selectById(operatorUserId);
        if (op == null || !Boolean.TRUE.equals(op.getIsRoot())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅根用户（系统管理员）可操作结算时间窗口");
        }
    }
}
