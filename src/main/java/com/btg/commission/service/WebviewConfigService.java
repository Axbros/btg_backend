package com.btg.commission.service;

import com.btg.commission.dto.v1.WebviewConfigUpdateDTO;
import com.btg.commission.vo.WebviewBootstrapVO;
import com.btg.commission.vo.WebviewConfigDetailVO;

public interface WebviewConfigService {

    /** 安卓冷启动拉取；无配置行时返回安全默认值，不抛错 */
    WebviewBootstrapVO getBootstrapConfig();

    /** 管理端查看当前唯一配置；无行时返回与默认一致的详情（id 为 null） */
    WebviewConfigDetailVO getConfigForAdmin();

    /** 更新或插入唯一配置；校验 webUrl / 注入内容长度 */
    void updateConfig(WebviewConfigUpdateDTO dto, Long operatorUserId);
}
