package com.btg.commission.service;

import com.btg.commission.vo.BitgetAssetSummaryVO;

public interface BitgetApiService {

    /**
     * 使用用户资料中保存的 Bitget 密钥拉取合约账户列表（GET /api/v2/mix/account/accounts）。
     *
     * @param productType Bitget 必填参数，如 USDT-FUTURES、COIN-FUTURES、USDC-FUTURES；null 或空时默认 USDT-FUTURES
     * @see <a href="https://www.bitgetapp.com/zh-CN/api-doc/contract/account/Get-Account-List">获取账户信息列表</a>
     */
    BitgetAssetSummaryVO queryCurrentUserAssets(Long userId, String productType);
}
