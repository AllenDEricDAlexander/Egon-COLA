package top.egon.cola.component.accessguard.policy.deny;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.policy.allow.AllowListMode;
import top.egon.cola.component.accessguard.policy.GuardContext;
import top.egon.cola.component.accessguard.store.StoreOperationException;
import top.egon.cola.component.accessguard.store.local.LocalDenyListStore;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DenyListPolicyTest {

    @Test
    void denyListHitIsARealTerminalDecision() {
        LocalDenyListStore store = new LocalDenyListStore(Clock.systemUTC(), 10);
        store.add("draw", "data-v1", hash(), Duration.ZERO);
        DenyListPolicy policy = new DenyListPolicy(store);

        assertThat(policy.evaluate(
                GuardContext.forPolicy("draw", 1L, "state-v1", hash()),
                admission(new AdmissionConfig.DenyListConfig(true, "data-v1"))).decision())
                .isEqualTo(GuardDecision.DENY_LIST_HIT);
    }

    @Test
    void disabledDenyListDoesNotReadAStoreFailure() {
        DenyListPolicy policy = new DenyListPolicy((ruleId, dataVersion, keyHash) -> {
            throw new StoreOperationException("DENY_LIST_UNAVAILABLE");
        });

        assertThat(policy.evaluate(
                GuardContext.forPolicy("draw", 1L, "state-v1", hash()),
                admission(new AdmissionConfig.DenyListConfig(false, "data-v1"))).decision())
                .isEqualTo(GuardDecision.PASS);
    }

    @Test
    void storeFailurePropagatesInsteadOfBecomingAllow() {
        DenyListPolicy policy = new DenyListPolicy((ruleId, dataVersion, keyHash) -> {
            throw new StoreOperationException("DENY_LIST_UNAVAILABLE");
        });

        assertThatThrownBy(() -> policy.evaluate(
                GuardContext.forPolicy("draw", 1L, "state-v1", hash()),
                admission(new AdmissionConfig.DenyListConfig(true, "data-v1"))))
                .isInstanceOf(StoreOperationException.class);
    }

    private static AdmissionConfig admission(AdmissionConfig.DenyListConfig denyList) {
        return new AdmissionConfig(
                denyList,
                new AdmissionConfig.AllowListConfig(false, AllowListMode.GATE),
                new AdmissionConfig.PenaltyBoxConfig(
                        false, 3, Duration.ofMinutes(1), Duration.ofMinutes(10)),
                new AdmissionConfig.RateLimitConfig(
                        false, AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET,
                        10, 10, Duration.ofSeconds(1), 1));
    }

    private static String hash() {
        return "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    }
}
