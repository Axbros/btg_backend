package com.btg.commission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btg.commission.entity.BtgMt5Worker;
import com.btg.commission.vo.mt5.AssignedMt5AccountVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BtgMt5WorkerMapper extends BaseMapper<BtgMt5Worker> {

    /**
     * 在线、心跳有效、已分配有效账号数小于 {@code max_accounts} 的候选中择优一条。
     */
    BtgMt5Worker selectBestWorkerForAllocation(@Param("now") LocalDateTime now);

    int countValidAssignedAccounts(@Param("workerDbId") Long workerDbId);

    List<AssignedMt5AccountVO> listAssignedAccounts(@Param("workerDbId") Long workerDbId);
}
