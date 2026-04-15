package com.btg.commission.redis;

import java.time.Duration;

/**
 * MT5 快照 Redis Key 约定。
 */
public final class Mt5SnapshotRedisKey {

    public static final String PREFIX_LATEST = "mt5:snapshot:latest:";

    /** 与 latest 同步刷新 */
    public static final Duration LATEST_TTL = Duration.ofHours(24);

    private Mt5SnapshotRedisKey() {
    }

    public static String latest(String accountId) {
        return PREFIX_LATEST + accountId;
    }
}
