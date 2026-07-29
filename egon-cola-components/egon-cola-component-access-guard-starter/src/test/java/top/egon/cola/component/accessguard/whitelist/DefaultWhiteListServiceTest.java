package top.egon.cola.component.accessguard.whitelist;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.annotation.WhiteListMode;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.config.AccessGuardRule;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static top.egon.cola.component.accessguard.annotation.FailStrategy.FAIL_OPEN;
import static top.egon.cola.component.accessguard.annotation.TimeoutExecutorType.THREAD_POOL;
import static top.egon.cola.component.accessguard.annotation.WhiteListMode.BYPASS_GUARD;
import static top.egon.cola.component.accessguard.annotation.WhiteListMode.GATEKEEPER;

class DefaultWhiteListServiceTest {

    @Test
    void shouldMatchAnnotationUsers() {
        WhiteListDecision decision = service().check(rule(true, List.of("hash001"), GATEKEEPER), "hash001");

        assertThat(decision.passed()).isTrue();
        assertThat(decision.bypassGuard()).isFalse();
    }

    @Test
    void shouldRejectWhenEnabledAndEmptyListStrategyDenyAll() {
        WhiteListDecision decision = service().check(rule(true, List.of(), GATEKEEPER), "hash001");

        assertThat(decision.passed()).isFalse();
        assertThat(decision.reason()).isEqualTo("white list is empty");
    }

    @Test
    void shouldPassWhenWhiteListIsDisabled() {
        WhiteListDecision decision = service().check(rule(false, List.of(), GATEKEEPER), "hash001");

        assertThat(decision.passed()).isTrue();
        assertThat(decision.reason()).isEqualTo("white list disabled");
    }

    @Test
    void shouldReturnBypassGuardWhenConfigured() {
        WhiteListDecision decision = service().check(rule(true, List.of("hash001"), BYPASS_GUARD), "hash001");

        assertThat(decision.passed()).isTrue();
        assertThat(decision.bypassGuard()).isTrue();
        assertThat(decision.mode()).isEqualTo(BYPASS_GUARD);
    }

    private DefaultWhiteListService service() {
        AccessGuardProperties properties = new AccessGuardProperties();
        properties.getWhiteList().setEmptyListStrategy(AccessGuardProperties.WhiteListEmptyListStrategy.DENY_ALL);
        return new DefaultWhiteListService(properties, (ruleName, accessKeyHash) -> false);
    }

    private AccessGuardRule rule(boolean enabled, List<String> users, WhiteListMode mode) {
        return new AccessGuardRule(
                "draw-api",
                "userId",
                "",
                enabled,
                users,
                mode,
                true,
                1L,
                1L,
                TimeUnit.SECONDS,
                false,
                0L,
                Duration.ofHours(24),
                false,
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
