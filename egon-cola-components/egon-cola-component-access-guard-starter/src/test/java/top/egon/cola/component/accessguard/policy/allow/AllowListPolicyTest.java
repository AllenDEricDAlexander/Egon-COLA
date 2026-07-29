package top.egon.cola.component.accessguard.policy.allow;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.policy.GuardContext;
import top.egon.cola.component.accessguard.policy.PolicyResult;
import top.egon.cola.component.accessguard.store.StoreOperationException;
import top.egon.cola.component.accessguard.store.local.LocalAllowListStore;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllowListPolicyTest {

    @Test
    void allowListBypassNeverIncludesDenyList() {
        LocalAllowListStore store = new LocalAllowListStore(Clock.systemUTC(), 10);
        store.add("draw", "data-v1", hash(), Duration.ZERO);
        AllowListPolicy policy = new AllowListPolicy(store);

        PolicyResult result = policy.evaluate(
                GuardContext.forPolicy("draw", 1L, "state-v1", hash()),
                new AdmissionConfig.AllowListConfig(
                        true,
                        AllowListMode.BYPASS_RATE_LIMIT_AND_PENALTY,
                        "data-v1"));

        assertThat(result.bypassedPolicies())
                .containsExactlyInAnyOrder("penalty-box", "rate-limit")
                .doesNotContain("deny-list");
    }

    @Test
    void gateRejectsMissAndAllowsHit() {
        LocalAllowListStore store = new LocalAllowListStore(Clock.systemUTC(), 10);
        AllowListPolicy policy = new AllowListPolicy(store);
        AdmissionConfig.AllowListConfig config = new AdmissionConfig.AllowListConfig(
                true, AllowListMode.GATE, "data-v1");
        GuardContext context = GuardContext.forPolicy("draw", 1L, "state-v1", hash());

        assertThat(policy.evaluate(context, config).decision()).isEqualTo(GuardDecision.ALLOW_LIST_MISS);
        store.add("draw", "data-v1", hash(), Duration.ZERO);
        assertThat(policy.evaluate(context, config).decision()).isEqualTo(GuardDecision.PASS);
    }

    @Test
    void storeFailurePropagatesInsteadOfBecomingAListDecision() {
        AllowListPolicy policy = new AllowListPolicy((ruleId, dataVersion, keyHash) -> {
            throw new StoreOperationException("ALLOW_LIST_UNAVAILABLE");
        });

        assertThatThrownBy(() -> policy.evaluate(
                GuardContext.forPolicy("draw", 1L, "state-v1", hash()),
                new AdmissionConfig.AllowListConfig(true, AllowListMode.GATE, "data-v1")))
                .isInstanceOf(StoreOperationException.class)
                .hasMessageContaining("ALLOW_LIST_UNAVAILABLE");
    }

    private static String hash() {
        return "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    }
}
