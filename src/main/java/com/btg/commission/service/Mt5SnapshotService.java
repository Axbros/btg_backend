package com.btg.commission.service;

import com.btg.commission.dto.mt5.Mt5SnapshotReportDTO;
import com.btg.commission.vo.Mt5SnapshotVO;

public interface Mt5SnapshotService {

    void reportSnapshot(Mt5SnapshotReportDTO dto);

    /** 当前登录用户（JWT userId）名下最新一条快照 */
    Mt5SnapshotVO getLatestSnapshotForUser(Long userId);
}
