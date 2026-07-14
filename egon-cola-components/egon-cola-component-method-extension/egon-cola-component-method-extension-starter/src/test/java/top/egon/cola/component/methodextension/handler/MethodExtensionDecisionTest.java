package top.egon.cola.component.methodextension.handler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class MethodExtensionDecisionTest {

    @Test
    void shouldCreateAllowDecision() {
        MethodExtensionDecision decision = MethodExtensionDecision.allow();

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.responseProvided()).isFalse();
        assertThat(decision.response()).isNull();
        assertThat(decision.reason()).isEmpty();
    }

    @Test
    void shouldCreateFallbackRejectionWithReason() {
        MethodExtensionDecision decision = MethodExtensionDecision.rejectWithReason("blacklist hit");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.responseProvided()).isFalse();
        assertThat(decision.response()).isNull();
        assertThat(decision.reason()).isEqualTo("blacklist hit");
    }

    @Test
    void shouldCreateDirectResponseRejection() {
        MethodExtensionDecision decision = MethodExtensionDecision.reject("blocked", "policy rejected");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.responseProvided()).isTrue();
        assertThat(decision.response()).isEqualTo("blocked");
        assertThat(decision.reason()).isEqualTo("policy rejected");
    }

    @Test
    void shouldRejectNullDirectResponse() {
        assertThatNullPointerException()
                .isThrownBy(() -> MethodExtensionDecision.reject(null))
                .withMessage("response must not be null");
    }
}
