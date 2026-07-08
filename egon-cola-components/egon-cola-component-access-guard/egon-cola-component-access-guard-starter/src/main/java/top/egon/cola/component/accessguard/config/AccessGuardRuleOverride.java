package top.egon.cola.component.accessguard.config;

import top.egon.cola.component.accessguard.annotation.FailStrategy;
import top.egon.cola.component.accessguard.annotation.TimeoutExecutorType;
import top.egon.cola.component.accessguard.annotation.WhiteListMode;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public record AccessGuardRuleOverride(
        Boolean enabled,
        String name,
        String key,
        String keyExpression,
        Boolean whiteListEnabled,
        List<String> whiteListUsers,
        WhiteListMode whiteListMode,
        Boolean rateLimiterEnabled,
        Long permits,
        Long interval,
        TimeUnit intervalUnit,
        Boolean blacklistEnabled,
        Long blacklistCount,
        Duration blacklistTimeout,
        Boolean enableBlacklistForAllKey,
        Boolean timeoutEnabled,
        Duration timeout,
        TimeoutExecutorType timeoutExecutor,
        Boolean fallbackOnException,
        Boolean cancelRunningTask,
        String fallbackMethod,
        String returnJson,
        FailStrategy failStrategy
) {
}
