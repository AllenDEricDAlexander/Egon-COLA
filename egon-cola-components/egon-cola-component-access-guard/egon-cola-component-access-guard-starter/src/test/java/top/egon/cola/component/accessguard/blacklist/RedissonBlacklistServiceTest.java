package top.egon.cola.component.accessguard.blacklist;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.context.AccessGuardContext;
import top.egon.cola.component.accessguard.support.AccessGuardRedisKeys;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static top.egon.cola.component.accessguard.annotation.FailStrategy.FAIL_OPEN;
import static top.egon.cola.component.accessguard.annotation.TimeoutExecutorType.THREAD_POOL;
import static top.egon.cola.component.accessguard.annotation.WhiteListMode.GATEKEEPER;

class RedissonBlacklistServiceTest {

    @Test
    void shouldRefuseAutomaticBlacklistForAllKeyWhenDisabled() {
        RedissonBlacklistService service = new RedissonBlacklistService(
                null,
                new AccessGuardRedisKeys("egon:access-guard", "draw", "prod")
        );

        BlacklistStatus status = service.incrementRejectAndMaybeBlacklist(rule(false), context("all"));

        assertThat(status.blacklisted()).isFalse();
        assertThat(status.reason()).isEqualTo("blacklist for all key disabled");
    }

    private AccessGuardContext context(String accessKeyHash) {
        AccessGuardContext context = new AccessGuardContext();
        context.setRuleName("draw-api");
        context.setAccessKeyHash(accessKeyHash);
        return context;
    }

    private AccessGuardRule rule(boolean enableBlacklistForAllKey) {
        return new AccessGuardRule(
                "draw-api",
                "all",
                "",
                false,
                List.of(),
                GATEKEEPER,
                true,
                1L,
                1L,
                TimeUnit.SECONDS,
                true,
                3L,
                Duration.ofHours(24),
                enableBlacklistForAllKey,
                false,
                Duration.ofMillis(350),
                THREAD_POOL,
                false,
                true,
                "",
                "",
                FAIL_OPEN
        );
    }
}
