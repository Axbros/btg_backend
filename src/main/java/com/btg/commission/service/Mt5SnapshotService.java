package com.btg.commission.service;

import com.btg.commission.dto.mt5.Mt5SnapshotReportDTO;
import com.btg.commission.vo.Mt5SnapshotVO;

public interface Mt5SnapshotService {

    void reportSnapshot(Mt5SnapshotReportDTO dto);

    Mt5SnapshotVO getLatestByAccountId(String accountId);
}
