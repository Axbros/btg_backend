-- 执行一次：为 btg_user_profile 增加券商名称、钱包地址（与 Java 实体 walletName / walletAddress 对应列 wallet_name / wallet_address）
ALTER TABLE btg_user_profile
    ADD COLUMN wallet_name    VARCHAR(255) NULL COMMENT '券商名称' AFTER exchange_uid,
    ADD COLUMN wallet_address VARCHAR(512) NULL COMMENT '钱包地址' AFTER wallet_name;
