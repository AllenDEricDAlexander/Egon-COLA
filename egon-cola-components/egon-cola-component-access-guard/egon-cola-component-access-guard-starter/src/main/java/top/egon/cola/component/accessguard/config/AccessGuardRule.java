package top.egon.cola.component.accessguard.config;

import top.egon.cola.component.accessguard.annotation.FailStrategy;
import top.egon.cola.component.accessguard.annotation.TimeoutExecutorType;
import top.egon.cola.component.accessguard.annotation.WhiteListMode;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public record AccessGuardRule(
        String name,
        String key,
        String keyExpression,
        boolean whiteListEnabled,
        List<String> whiteListUsers,
        WhiteListMode whiteListMode,
        boolean rateLimiterEnabled,
        long permits,
        long interval,
        TimeUnit intervalUnit,
        boolean blacklistEnabled,
        long blacklistCount,
        Duration blacklistTimeout,
        boolean enableBlacklistForAllKey,
        boolean timeoutEnabled,
        Duration timeout,
        TimeoutExecutorType timeoutExecutor,
        boolean fallbackOnException,
        boolean cancelRunningTask,
        String fallbackMethod,
        String returnJson,
        FailStrategy failStrategy
) {
}
