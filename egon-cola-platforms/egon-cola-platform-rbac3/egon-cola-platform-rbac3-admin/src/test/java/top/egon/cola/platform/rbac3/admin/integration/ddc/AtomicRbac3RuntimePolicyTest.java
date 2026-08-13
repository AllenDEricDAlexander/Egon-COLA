package top.egon.cola.platform.rbac3.admin.integration.ddc;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.runtime.repository.Rbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.config.properties.Rbac3AdminProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.Rbac3RuntimePolicySnapshotVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.ApplyFailureVO;
import top.egon.cola.platform.rbac3.admin.runtime.repository.ddc.AtomicRbac3RuntimePolicy;

class AtomicRbac3RuntimePolicyTest {

    @Test
    void startsWithCompleteImmutableAdministrativeDefaults() {
        AtomicRbac3RuntimePolicy policy = policyWithDefaults();

        Rbac3RuntimePolicySnapshotVO snapshot = policy.current();

        assertThat(snapshot.accessTokenTtl()).isEqualTo(Duration.ofSeconds(900));
        assertThat(snapshot.refreshTokenTtl()).isEqualTo(Duration.ofSeconds(604_800));
        assertThat(snapshot.sessionIdleTimeout()).isEqualTo(Duration.ofSeconds(1_800));
        assertThat(snapshot.sessionAbsoluteTimeout()).isEqualTo(Duration.ofSeconds(43_200));
        assertThat(snapshot.maximumActiveRoots()).isEqualTo(16);
        assertThat(snapshot.configVersions()).containsExactlyInAnyOrderEntriesOf(Map.of(
                AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, 0L,
                AtomicRbac3RuntimePolicy.REFRESH_TOKEN_TTL_KEY, 0L,
                AtomicRbac3RuntimePolicy.SESSION_IDLE_TIMEOUT_KEY, 0L,
                AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY, 0L,
                AtomicRbac3RuntimePolicy.MAXIMUM_ACTIVE_ROOTS_KEY, 0L));
        assertThatThrownBy(() -> snapshot.configVersions().put("other", 1L))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void publishesOneNewSnapshotAfterAValidUpdate() {
        AtomicRbac3RuntimePolicy policy = policyWithDefaults();
        Rbac3RuntimePolicySnapshotVO before = policy.current();

        policy.apply(AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, "1200", 7L);

        assertThat(policy.current()).isNotSameAs(before);
        assertThat(policy.current().accessTokenTtl()).isEqualTo(Duration.ofSeconds(1_200));
        assertThat(policy.current().configVersions())
                .containsEntry(AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, 7L);
        assertThat(before.accessTokenTtl()).isEqualTo(Duration.ofSeconds(900));
        assertThat(before.configVersions())
                .containsEntry(AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, 0L);
    }

    @Test
    void rejectsMalformedUnknownAndOverflowValuesWithoutChangingTheSnapshot() {
        for (String rawValue : new String[]{null, "", " 900", "900 ", "1.5", "+900",
                "900s", "999999999999999999999999"}) {
            AtomicRbac3RuntimePolicy policy = policyWithDefaults();
            Rbac3RuntimePolicySnapshotVO before = policy.current();

            var failure = assertThatThrownBy(() -> policy.apply(
                    AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, rawValue, 3L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("INVALID_INTEGER");
            if (rawValue == null || !rawValue.isEmpty()) {
                failure.hasMessageNotContaining(String.valueOf(rawValue));
            }
            assertThat(policy.current()).isSameAs(before);
            assertThat(policy.lastApplyFailure()).get()
                    .extracting(ApplyFailureVO::errorCode)
                    .isEqualTo("INVALID_INTEGER");
        }

        AtomicRbac3RuntimePolicy policy = policyWithDefaults();
        Rbac3RuntimePolicySnapshotVO before = policy.current();
        assertThatThrownBy(() -> policy.apply("rbac3.unknown", "1", 4L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNKNOWN_KEY");
        assertThat(policy.current()).isSameAs(before);
    }

    @Test
    void enforcesEveryRangeAndCrossFieldRelationship() {
        assertRejected(AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, "299");
        assertRejected(AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, "1801");
        assertRejected(AtomicRbac3RuntimePolicy.REFRESH_TOKEN_TTL_KEY, "86399");
        assertRejected(AtomicRbac3RuntimePolicy.REFRESH_TOKEN_TTL_KEY, "2592001");
        assertRejected(AtomicRbac3RuntimePolicy.SESSION_IDLE_TIMEOUT_KEY, "299");
        assertRejected(AtomicRbac3RuntimePolicy.SESSION_IDLE_TIMEOUT_KEY, "28801");
        assertRejected(AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY, "3599");
        assertRejected(AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY, "86401");
        assertRejected(AtomicRbac3RuntimePolicy.MAXIMUM_ACTIVE_ROOTS_KEY, "0");
        assertRejected(AtomicRbac3RuntimePolicy.MAXIMUM_ACTIVE_ROOTS_KEY, "33");

        AtomicRbac3RuntimePolicy idlePolicy = policyWithDefaults();
        idlePolicy.apply(AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY, "3600", 1L);
        assertThatThrownBy(() -> idlePolicy.apply(
                AtomicRbac3RuntimePolicy.SESSION_IDLE_TIMEOUT_KEY, "3601", 2L))
                .hasMessageContaining("IDLE_EXCEEDS_ABSOLUTE");

        AtomicRbac3RuntimePolicy refreshPolicy = policyWithDefaults();
        assertThatThrownBy(() -> refreshPolicy.apply(
                AtomicRbac3RuntimePolicy.REFRESH_TOKEN_TTL_KEY, "43199", 2L))
                .hasMessageContaining("REFRESH_BELOW_ABSOLUTE");
    }

    @Test
    void keepsLastFailureUntilTheSameKeyAppliesSuccessfully() {
        AtomicRbac3RuntimePolicy policy = policyWithDefaults();

        assertThatThrownBy(() -> policy.apply(
                AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, "299", 2L))
                .isInstanceOf(IllegalArgumentException.class);
        policy.apply(AtomicRbac3RuntimePolicy.MAXIMUM_ACTIVE_ROOTS_KEY, "8", 3L);
        assertThat(policy.lastApplyFailure()).get()
                .extracting(ApplyFailureVO::key)
                .isEqualTo(AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY);

        policy.apply(AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, "1200", 4L);
        assertThat(policy.lastApplyFailure()).isEmpty();
    }

    @Test
    void snapshotValidationCannotBeBypassedByAnotherPolicyImplementation() {
        Map<String, Long> versions = new HashMap<>();

        assertThatThrownBy(() -> new Rbac3RuntimePolicySnapshotVO(
                Duration.ofSeconds(299),
                Duration.ofDays(7),
                Duration.ofMinutes(30),
                Duration.ofHours(12),
                16,
                versions))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACCESS_TOKEN_TTL_OUT_OF_RANGE");
    }

    private void assertRejected(String key, String rawValue) {
        AtomicRbac3RuntimePolicy policy = policyWithDefaults();
        Rbac3RuntimePolicySnapshotVO before = policy.current();

        assertThatThrownBy(() -> policy.apply(key, rawValue, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(policy.current()).isSameAs(before);
        assertThat(policy.current().configVersions()).containsEntry(key, 0L);
    }

    private AtomicRbac3RuntimePolicy policyWithDefaults() {
        return new AtomicRbac3RuntimePolicy(new Rbac3AdminProperties());
    }
}
