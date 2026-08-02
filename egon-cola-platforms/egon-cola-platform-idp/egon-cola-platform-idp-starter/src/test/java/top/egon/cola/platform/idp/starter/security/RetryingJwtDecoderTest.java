package top.egon.cola.platform.idp.starter.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryingJwtDecoderTest {

    @Test
    void rebuildsDecoderAndRetriesExactlyOnce() {
        AtomicInteger builds = new AtomicInteger();
        RetryingJwtDecoder decoder = new RetryingJwtDecoder(() -> {
            int build = builds.incrementAndGet();
            return token -> {
                if (build == 1) {
                    throw new BadJwtException("unknown kid");
                }
                return jwt();
            };
        });

        assertThat(decoder.decode("token").getSubject()).isEqualTo("identity-1");
        assertThat(builds).hasValue(2);
    }

    @Test
    void doesNotRetryMoreThanOnce() {
        AtomicInteger builds = new AtomicInteger();
        RetryingJwtDecoder decoder = new RetryingJwtDecoder(() -> {
            builds.incrementAndGet();
            return token -> { throw new BadJwtException("invalid"); };
        });

        assertThatThrownBy(() -> decoder.decode("token"))
                .isInstanceOf(BadJwtException.class);
        assertThat(builds).hasValue(2);
    }

    private Jwt jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("identity-1")
                .issuedAt(Instant.EPOCH)
                .expiresAt(Instant.EPOCH.plusSeconds(60))
                .build();
    }
}
