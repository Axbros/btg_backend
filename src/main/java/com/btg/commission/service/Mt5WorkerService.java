package com.btg.commission.service;

import com.btg.commission.dto.mt5.Mt5WorkerHeartbeatDTO;
import com.btg.commission.vo.mt5.AssignedMt5AccountVO;

import java.util.List;

public interface Mt5WorkerService {

    void heartbeat(Mt5WorkerHeartbeatDTO dto);

    List<AssignedMt5AccountVO> listAssignedAccounts(String workerCode);

    /**
     * 为用户资料分配可用 Worker，返回 {@code btg_mt5_worker.id}。
     * 若已分配则返回既有 id。
     */
    Long allocateWorkerForUserProfile(Long userId);
}
