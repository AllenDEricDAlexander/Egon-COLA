package top.egon.cola.component.accessguard.store.redisson;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessGuardRedisKeyFactoryTest {

    private static final String HASH = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    private final AccessGuardRedisKeyFactory factory =
            new AccessGuardRedisKeyFactory("egon:access-guard", "draw-service");

    @Test
    void stateKeysContainNamespaceApplicationPolicyVersionAndOnlyTheHash() {
        String key = factory.rateLimit("draw", "state-v2", HASH);

        assertThat(key)
                .isEqualTo("egon:access-guard:draw-service:draw:rate-limit:state-v2:" + HASH)
                .doesNotContain("raw-user-id");
    }

    @Test
    void listKeysUseTheirIndependentDataVersion() {
        assertThat(factory.allowList("draw", "allow-v3"))
                .isEqualTo("egon:access-guard:draw-service:draw:allow-list:allow-v3");
        assertThat(factory.denyList("draw", "deny-v4"))
                .isEqualTo("egon:access-guard:draw-service:draw:deny-list:deny-v4");
    }

    @Test
    void rejectsUnsafeSegmentsAndNonHashedIdentity() {
        assertThatThrownBy(() -> factory.rateLimit("draw:other", "v1", HASH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ruleId");
        assertThatThrownBy(() -> factory.rateLimit("draw", "v1", "raw-user-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keyHash");
    }
}
