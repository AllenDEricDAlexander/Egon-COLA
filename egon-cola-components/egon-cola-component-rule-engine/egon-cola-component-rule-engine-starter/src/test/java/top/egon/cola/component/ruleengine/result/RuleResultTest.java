package top.egon.cola.component.ruleengine.result;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleResultTest {

    @Test
    void shouldBuildSuccessStopAndFailureResultsWithIntCode() {
        RuleResult<String> success = RuleResult.success("ok");
        RuleResult<String> stopped = RuleResult.stop(600100, "blocked", "reject");
        IllegalStateException exception = new IllegalStateException("boom");
        RuleResult<String> failed = RuleResult.fail(500100, "failed", exception);

        assertThat(success.isSuccess()).isTrue();
        assertThat(success.getStatus()).isEqualTo(RuleStatus.SUCCESS);
        assertThat(success.getCode()).isZero();
        assertThat(success.getData()).isEqualTo("ok");

        assertThat(stopped.isSuccess()).isFalse();
        assertThat(stopped.getStatus()).isEqualTo(RuleStatus.STOPPED);
        assertThat(stopped.getCode()).isEqualTo(600100);
        assertThat(stopped.getData()).isEqualTo("reject");

        assertThat(failed.isSuccess()).isFalse();
        assertThat(failed.getStatus()).isEqualTo(RuleStatus.FAILED);
        assertThat(failed.getException()).isSameAs(exception);
    }
}
