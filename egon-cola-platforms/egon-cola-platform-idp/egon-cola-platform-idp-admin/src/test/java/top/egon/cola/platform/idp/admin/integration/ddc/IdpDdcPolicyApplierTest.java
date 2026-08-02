package top.egon.cola.platform.idp.admin.integration.ddc;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdpDdcPolicyApplierTest {

    @Test
    void validUpdateAtomicallyReplacesOneVersionedPolicyValue() {
        AtomicIdpRuntimePolicy policy = new AtomicIdpRuntimePolicy();
        IdpDdcPolicyApplier applier = new IdpDdcPolicyApplier(
                AtomicIdpRuntimePolicy.ACCESS_TOKEN_TTL_KEY,
                0,
                policy
        );

        applier.apply(
                AtomicIdpRuntimePolicy.ACCESS_TOKEN_TTL_KEY,
                "1200",
                2L
        );

        assertEquals(Duration.ofMinutes(20),
                policy.current().accessTokenTtl());
        assertEquals(2L, policy.current().configVersions().get(
                AtomicIdpRuntimePolicy.ACCESS_TOKEN_TTL_KEY
        ));
    }

    @Test
    void invalidAccessTtlAndStaleVersionKeepLastKnownGoodSnapshot() {
        AtomicIdpRuntimePolicy policy = new AtomicIdpRuntimePolicy();
        IdpDdcPolicyApplier applier = new IdpDdcPolicyApplier(
                AtomicIdpRuntimePolicy.ACCESS_TOKEN_TTL_KEY,
                0,
                policy
        );
        IdpRuntimePolicy.Snapshot before = policy.current();

        assertThrows(IllegalArgumentException.class, () -> applier.apply(
                AtomicIdpRuntimePolicy.ACCESS_TOKEN_TTL_KEY,
                "1",
                2L
        ));
        assertEquals(before, policy.current());

        applier.apply(
                AtomicIdpRuntimePolicy.ACCESS_TOKEN_TTL_KEY,
                "900",
                3L
        );
        IdpRuntimePolicy.Snapshot versionThree = policy.current();
        assertThrows(IllegalArgumentException.class, () -> applier.apply(
                AtomicIdpRuntimePolicy.ACCESS_TOKEN_TTL_KEY,
                "1200",
                2L
        ));
        assertEquals(versionThree, policy.current());
    }

    @Test
    void secretLikeValueIsNeverReflectedInFailureMessage() {
        AtomicIdpRuntimePolicy policy = new AtomicIdpRuntimePolicy();
        IdpDdcPolicyApplier applier = new IdpDdcPolicyApplier(
                AtomicIdpRuntimePolicy.REFRESH_TOKEN_TTL_KEY,
                10,
                policy
        );
        String secretLikeValue = "secret-value-must-not-be-logged";

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> applier.apply(
                        AtomicIdpRuntimePolicy.REFRESH_TOKEN_TTL_KEY,
                        secretLikeValue,
                        1L
                )
        );

        org.assertj.core.api.Assertions.assertThat(failure)
                .hasMessageNotContaining(secretLikeValue);
    }
}
