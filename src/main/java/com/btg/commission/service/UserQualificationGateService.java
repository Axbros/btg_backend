package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.enums.QualificationStatusEnum;
import com.btg.commission.mapper.BtgUserMapper;
import com.btg.commission.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 正式业务入口统一校验：系统管理员资格审核须为 {@link QualificationStatusEnum#APPROVED}。
 */
@Service
@RequiredArgsConstructor
public class UserQualificationGateService {

    private static final String MSG = "当前资格审核未通过，请先完成资格审核";

    private final BtgUserMapper btgUserMapper;
    private final UserProfileMapper userProfileMapper;

    public void requireApprovedForFormalBusiness(Long userId) {
        if (userId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, MSG);
        }
        BtgUser u = btgUserMapper.selectById(userId);
        if (u != null && Boolean.TRUE.equals(u.getIsRoot())) {
            return;
        }
        UserProfile p = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
        if (p == null || p.getQualificationStatus() != QualificationStatusEnum.APPROVED) {
            throw new BizException(ResultCode.FORBIDDEN, MSG);
        }
    }
}
