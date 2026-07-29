package top.egon.cola.component.accessguard.policy;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.policy.allow.AllowListPolicy;
import top.egon.cola.component.accessguard.policy.deny.DenyListPolicy;
import top.egon.cola.component.accessguard.policy.penalty.PenaltyBoxPolicy;
import top.egon.cola.component.accessguard.policy.ratelimit.RateLimitPolicy;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AdmissionOrderContractTest {

    @Test
    void builtInOrderIsFixedAndComplete() {
        DenyListPolicy deny = new DenyListPolicy((rule, version, hash) -> false);
        AllowListPolicy allow = new AllowListPolicy((rule, version, hash) -> false);
        PenaltyBoxPolicy penalty = new PenaltyBoxPolicy(key -> Optional.empty());
        RateLimitPolicy rate = new RateLimitPolicy(request ->
                new top.egon.cola.component.accessguard.store.RateLimitDecision(true, 1, java.time.Duration.ZERO));

        assertThat(AdmissionPolicies.builtIns(deny, allow, penalty, rate))
                .extracting(GuardPolicy::id)
                .containsExactly("deny-list", "allow-list", "penalty-box", "rate-limit");
    }
}
