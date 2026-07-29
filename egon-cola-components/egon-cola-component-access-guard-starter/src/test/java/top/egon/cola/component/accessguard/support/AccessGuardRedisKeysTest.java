package top.egon.cola.component.accessguard.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccessGuardRedisKeysTest {

    private final AccessGuardRedisKeys keys = new AccessGuardRedisKeys("egon:access-guard", "draw", "prod");

    @Test
    void shouldBuildWhiteListKey() {
        assertThat(keys.whiteList("draw-api", "hash001"))
                .isEqualTo("egon:access-guard:draw:prod:draw-api:hash001:white-list");
    }

    @Test
    void shouldBuildLimiterKey() {
        assertThat(keys.limiter("draw-api", "hash001"))
                .isEqualTo("egon:access-guard:draw:prod:draw-api:hash001:limiter");
    }

    @Test
    void shouldBuildConfigVersionKey() {
        assertThat(keys.configVersion("draw-api"))
                .isEqualTo("egon:access-guard:draw:prod:draw-api:all:config-version");
    }
}
