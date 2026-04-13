package com.btg.commission.service;

import com.btg.commission.vo.BitgetAssetSummaryVO;

public interface BitgetApiService {

    /**
     * 使用用户资料中保存的 Bitget 密钥拉取全账户 USDT 余额（GET /api/v2/account/all-account-balance，无查询参数）。
     *
     * @see <a href="https://www.bitget.com/zh-CN/api-doc/common/account/All-Account-Balance">全账户余额</a>
     */
    BitgetAssetSummaryVO queryCurrentUserAssets(Long userId);
}
