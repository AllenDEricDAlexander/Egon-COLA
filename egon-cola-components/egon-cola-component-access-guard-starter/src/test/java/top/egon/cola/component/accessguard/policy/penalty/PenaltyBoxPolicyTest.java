package top.egon.cola.component.accessguard.policy.penalty;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.accessguard.core.GuardDecision;
import top.egon.cola.component.accessguard.core.plan.AdmissionConfig;
import top.egon.cola.component.accessguard.policy.allow.AllowListMode;
import top.egon.cola.component.accessguard.policy.GuardContext;
import top.egon.cola.component.accessguard.store.PenaltyKey;
import top.egon.cola.component.accessguard.store.local.LocalPenaltyStore;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PenaltyBoxPolicyTest {

    @Test
    void activePenaltyIsARealRejectionSeparateFromDenyList() {
        LocalPenaltyStore store = new LocalPenaltyStore(Clock.systemUTC(), 10);
        GuardContext context = GuardContext.forPolicy("draw", 1L, "state-v1", hash());
        AdmissionConfig.PenaltyBoxConfig config = new AdmissionConfig.PenaltyBoxConfig(
                true, 1, Duration.ofMinutes(1), Duration.ofMinutes(10));
        store.recordViolation(new PenaltyKey("draw", "state-v1", hash()), 1,
                Duration.ofMinutes(1), Duration.ofMinutes(10));

        assertThat(new PenaltyBoxPolicy(store).evaluate(context, admission(config)).decision())
                .isEqualTo(GuardDecision.PENALTY_ACTIVE);
    }

    private static AdmissionConfig admission(AdmissionConfig.PenaltyBoxConfig penaltyBox) {
        return new AdmissionConfig(
                new AdmissionConfig.DenyListConfig(false),
                new AdmissionConfig.AllowListConfig(false, AllowListMode.GATE),
                penaltyBox,
                new AdmissionConfig.RateLimitConfig(
                        false, AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET,
                        10, 10, Duration.ofSeconds(1), 1));
    }

    private static String hash() {
        return "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd";
    }
}
