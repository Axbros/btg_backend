package com.btg.commission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.v1.WebviewConfigUpdateDTO;
import com.btg.commission.entity.BtgWebviewConfig;
import com.btg.commission.enums.AuditAction;
import com.btg.commission.enums.AuditBusinessType;
import com.btg.commission.mapper.BtgWebviewConfigMapper;
import com.btg.commission.service.AuditLogService;
import com.btg.commission.service.WebviewConfigService;
import com.btg.commission.vo.WebviewBootstrapVO;
import com.btg.commission.vo.WebviewConfigDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WebviewConfigServiceImpl implements WebviewConfigService {

    private static final int MAX_INJECT_LEN = 20_000;

    private final BtgWebviewConfigMapper webviewConfigMapper;
    private final AuditLogService auditLogService;

    @Override
    public WebviewBootstrapVO getBootstrapConfig() {
        BtgWebviewConfig row = loadOneOrNull();
        if (row == null) {
            return defaultBootstrap();
        }
        return toBootstrap(row);
    }

    @Override
    public WebviewConfigDetailVO getConfigForAdmin() {
        BtgWebviewConfig row = loadOneOrNull();
        if (row == null) {
            return emptyDetail();
        }
        return toDetail(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(WebviewConfigUpdateDTO dto, Long operatorUserId) {
        if (dto == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "请求体不能为空");
        }
        if (operatorUserId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "未登录");
        }
        validateWebUrl(dto.getWebUrl());
        assertInjectLength(dto.getInjectJs(), "injectJs");
        assertInjectLength(dto.getInjectCss(), "injectCss");

        BtgWebviewConfig row = loadOneOrNull();
        if (row == null) {
            BtgWebviewConfig insert = new BtgWebviewConfig();
            applyDto(insert, dto);
            webviewConfigMapper.insert(insert);
            auditLogService.log(
                    AuditBusinessType.WEBVIEW_CONFIG,
                    insert.getId(),
                    AuditAction.UPDATE,
                    operatorUserId,
                    trimOrNull(dto.getRemark()));
            return;
        }
        applyDto(row, dto);
        webviewConfigMapper.updateById(row);
        auditLogService.log(
                AuditBusinessType.WEBVIEW_CONFIG,
                row.getId(),
                AuditAction.UPDATE,
                operatorUserId,
                trimOrNull(dto.getRemark()));
    }

    private BtgWebviewConfig loadOneOrNull() {
        return webviewConfigMapper.selectOne(new LambdaQueryWrapper<BtgWebviewConfig>()
                .orderByAsc(BtgWebviewConfig::getId)
                .last("LIMIT 1"));
    }

    private static void applyDto(BtgWebviewConfig entity, WebviewConfigUpdateDTO dto) {
        entity.setEnabled(Boolean.TRUE.equals(dto.getEnabled()));
        entity.setWebUrl(dto.getWebUrl().trim());
        entity.setInjectJs(blankToNull(dto.getInjectJs()));
        entity.setInjectCss(blankToNull(dto.getInjectCss()));
        entity.setShowSplash(Boolean.TRUE.equals(dto.getShowSplash()));
        entity.setSplashDurationMs(dto.getSplashDurationMs());
        entity.setVersion(dto.getVersion());
        entity.setRemark(trimOrNull(dto.getRemark()));
    }

    private static String blankToNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        return s.trim();
    }

    private static String trimOrNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        return s.trim();
    }

    private void validateWebUrl(String webUrl) {
        if (!StringUtils.hasText(webUrl)) {
            throw new BizException(ResultCode.BAD_REQUEST, "webUrl 不能为空");
        }
        String u = webUrl.trim();
        String lower = u.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("https://")) {
            throw new BizException(ResultCode.BAD_REQUEST, "webUrl 必须以 https:// 开头");
        }
        if (lower.startsWith("http://")) {
            throw new BizException(ResultCode.BAD_REQUEST, "不允许使用 http://");
        }
        if (lower.startsWith("javascript:") || lower.startsWith("file:") || lower.startsWith("data:")) {
            throw new BizException(ResultCode.BAD_REQUEST, "不允许 javascript: / file: / data: 等协议");
        }
    }

    private static void assertInjectLength(String content, String fieldName) {
        if (content == null) {
            return;
        }
        if (content.length() > MAX_INJECT_LEN) {
            throw new BizException(ResultCode.BAD_REQUEST, fieldName + " 长度不能超过 " + MAX_INJECT_LEN);
        }
    }

    private static WebviewBootstrapVO defaultBootstrap() {
        return WebviewBootstrapVO.builder()
                .enabled(false)
                .webUrl("")
                .injectJs("")
                .injectCss("")
                .showSplash(false)
                .splashDurationMs(0)
                .version(0L)
                .build();
    }

    private static WebviewBootstrapVO toBootstrap(BtgWebviewConfig row) {
        return WebviewBootstrapVO.builder()
                .enabled(Boolean.TRUE.equals(row.getEnabled()))
                .webUrl(row.getWebUrl() != null ? row.getWebUrl() : "")
                .injectJs(emptyIfNull(row.getInjectJs()))
                .injectCss(emptyIfNull(row.getInjectCss()))
                .showSplash(Boolean.TRUE.equals(row.getShowSplash()))
                .splashDurationMs(row.getSplashDurationMs() != null ? row.getSplashDurationMs() : 0)
                .version(row.getVersion() != null ? row.getVersion() : 0L)
                .build();
    }

    private static WebviewConfigDetailVO emptyDetail() {
        return WebviewConfigDetailVO.builder()
                .id(null)
                .enabled(false)
                .webUrl("")
                .injectJs("")
                .injectCss("")
                .showSplash(false)
                .splashDurationMs(0)
                .version(0L)
                .remark(null)
                .createdAt(null)
                .updatedAt(null)
                .build();
    }

    private static WebviewConfigDetailVO toDetail(BtgWebviewConfig row) {
        return WebviewConfigDetailVO.builder()
                .id(row.getId())
                .enabled(Boolean.TRUE.equals(row.getEnabled()))
                .webUrl(row.getWebUrl() != null ? row.getWebUrl() : "")
                .injectJs(emptyIfNull(row.getInjectJs()))
                .injectCss(emptyIfNull(row.getInjectCss()))
                .showSplash(Boolean.TRUE.equals(row.getShowSplash()))
                .splashDurationMs(row.getSplashDurationMs() != null ? row.getSplashDurationMs() : 0)
                .version(row.getVersion() != null ? row.getVersion() : 0L)
                .remark(row.getRemark())
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .build();
    }

    private static String emptyIfNull(String s) {
        return s == null ? "" : s;
    }
}
