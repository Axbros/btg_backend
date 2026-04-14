package com.btg.commission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btg.commission.common.api.ResultCode;
import com.btg.commission.common.exception.BizException;
import com.btg.commission.dto.mt5.Mt5WorkerHeartbeatDTO;
import com.btg.commission.entity.BtgMt5Worker;
import com.btg.commission.entity.UserProfile;
import com.btg.commission.mapper.BtgMt5WorkerMapper;
import com.btg.commission.mapper.UserProfileMapper;
import com.btg.commission.service.Mt5WorkerService;
import com.btg.commission.vo.mt5.AssignedMt5AccountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class Mt5WorkerServiceImpl implements Mt5WorkerService {

    private static final int DEFAULT_MAX_ACCOUNTS = 4;
    private static final int DEFAULT_HEARTBEAT_EXPIRE_SECONDS = 60;

    private final BtgMt5WorkerMapper btgMt5WorkerMapper;
    private final UserProfileMapper userProfileMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void heartbeat(Mt5WorkerHeartbeatDTO dto) {
        String workerCode = dto.getWorkerCode().trim();
        BtgMt5Worker worker = btgMt5WorkerMapper.selectOne(new LambdaQueryWrapper<BtgMt5Worker>()
                .eq(BtgMt5Worker::getWorkerCode, workerCode)
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (worker == null) {
            BtgMt5Worker created = new BtgMt5Worker();
            created.setWorkerCode(workerCode);
            created.setStatus(1);
            created.setMaxAccounts(DEFAULT_MAX_ACCOUNTS);
            created.setCurrentAccounts(0);
            created.setHeartbeatExpireSeconds(DEFAULT_HEARTBEAT_EXPIRE_SECONDS);
            created.setLastHeartbeatTime(now);
            applyHeartbeatPayload(created, dto);
            btgMt5WorkerMapper.insert(created);
            syncCurrentAccountsFromDb(created.getId());
            return;
        }
        if (worker.isDisabled()) {
            throw new BizException(ResultCode.FORBIDDEN, "MT5 worker 已禁用");
        }
        BtgMt5Worker patch = new BtgMt5Worker();
        patch.setId(worker.getId());
        patch.setStatus(1);
        patch.setLastHeartbeatTime(now);
        applyHeartbeatPayload(patch, dto);
        btgMt5WorkerMapper.updateById(patch);
        syncCurrentAccountsFromDb(worker.getId());
    }

    private void applyHeartbeatPayload(BtgMt5Worker target, Mt5WorkerHeartbeatDTO dto) {
        if (StringUtils.hasText(dto.getVersion())) {
            target.setVersion(dto.getVersion().trim());
        }
        if (StringUtils.hasText(dto.getHostName())) {
            target.setHostName(dto.getHostName().trim());
        }
        if (StringUtils.hasText(dto.getIpAddress())) {
            target.setIpAddress(dto.getIpAddress().trim());
        }
        if (StringUtils.hasText(dto.getRemark())) {
            target.setRemark(dto.getRemark().trim());
        }
        if (dto.getCurrentAccounts() != null) {
            target.setCurrentAccounts(Math.max(0, dto.getCurrentAccounts()));
        }
    }

    @Override
    public List<AssignedMt5AccountVO> listAssignedAccounts(String workerCode) {
        if (!StringUtils.hasText(workerCode)) {
            throw new BizException(ResultCode.BAD_REQUEST, "workerId 不能为空");
        }
        BtgMt5Worker worker = btgMt5WorkerMapper.selectOne(new LambdaQueryWrapper<BtgMt5Worker>()
                .eq(BtgMt5Worker::getWorkerCode, workerCode.trim())
                .last("LIMIT 1"));
        if (worker == null) {
            return Collections.emptyList();
        }
        return btgMt5WorkerMapper.listAssignedAccounts(worker.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long allocateWorkerForUserProfile(Long userId) {
        UserProfile profile = userProfileMapper.selectByUserIdForUpdate(userId);
        if (profile == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户资料不存在");
        }
        if (profile.getAssignedWorkerId() != null) {
            return profile.getAssignedWorkerId();
        }
        if (!StringUtils.hasText(profile.getTradingAccountId())
                || !StringUtils.hasText(profile.getTradingAccountId().trim())) {
            throw new BizException(ResultCode.BAD_REQUEST, "交易账号为空，无法分配 MT5 worker");
        }
        if (!StringUtils.hasText(profile.getServerName()) || !StringUtils.hasText(profile.getServerName().trim())) {
            throw new BizException(ResultCode.BAD_REQUEST, "服务器名称为空，无法分配 MT5 worker");
        }
        LocalDateTime now = LocalDateTime.now();
        BtgMt5Worker best = btgMt5WorkerMapper.selectBestWorkerForAllocation(now);
        if (best == null || !isWorkerOnline(best, now)) {
            throw new BizException(ResultCode.BAD_REQUEST, "暂无可用MT5 worker，请稍后重试");
        }
        UserProfile patch = new UserProfile();
        patch.setId(profile.getId());
        patch.setAssignedWorkerId(best.getId());
        userProfileMapper.updateById(patch);
        syncCurrentAccountsFromDb(best.getId());
        return best.getId();
    }

    /**
     * 在线：status=1、有心跳、且距上次心跳不超过 {@code heartbeatExpireSeconds}（缺省 60）。
     */
    public boolean isWorkerOnline(BtgMt5Worker worker) {
        return isWorkerOnline(worker, LocalDateTime.now());
    }

    private boolean isWorkerOnline(BtgMt5Worker worker, LocalDateTime now) {
        if (worker == null || worker.getStatus() == null || worker.getStatus() != 1) {
            return false;
        }
        LocalDateTime last = worker.getLastHeartbeatTime();
        if (last == null) {
            return false;
        }
        int expire = worker.getHeartbeatExpireSeconds() != null ? worker.getHeartbeatExpireSeconds()
                : DEFAULT_HEARTBEAT_EXPIRE_SECONDS;
        long seconds = ChronoUnit.SECONDS.between(last, now);
        return seconds >= 0 && seconds <= expire;
    }

    private void syncCurrentAccountsFromDb(Long workerDbId) {
        if (workerDbId == null) {
            return;
        }
        int cnt = btgMt5WorkerMapper.countValidAssignedAccounts(workerDbId);
        BtgMt5Worker patch = new BtgMt5Worker();
        patch.setId(workerDbId);
        patch.setCurrentAccounts(cnt);
        btgMt5WorkerMapper.updateById(patch);
    }
}
