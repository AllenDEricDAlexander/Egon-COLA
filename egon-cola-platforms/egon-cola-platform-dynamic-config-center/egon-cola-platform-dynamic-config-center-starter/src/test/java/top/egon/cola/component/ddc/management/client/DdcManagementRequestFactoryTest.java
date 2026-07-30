package top.egon.cola.component.ddc.management.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import top.egon.cola.component.ddc.security.DdcCanonicalRequest;
import top.egon.cola.component.ddc.security.DdcRequestSigner;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcManagementRequestFactoryTest {

    @Test
    void createsImmutableRequestWithCanonicalTargetAndSignature() {
        Instant now = Instant.parse("2026-07-25T02:00:00Z");
        DdcManagementRequestFactory factory = new DdcManagementRequestFactory(
                new DdcManagementClientProperties(
                        "http://ddc.test",
                        "ak",
                        "sk",
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(2)
                ),
                new ObjectMapper(),
                Clock.fixed(now, ZoneOffset.UTC),
                () -> "nonce-1"
        );

        DdcManagementRequestFactory.SignedRequest request = factory.create(
                HttpMethod.POST,
                "/api/v1/ddc/openapi/management/test",
                Map.of("z", List.of("last"), "a", List.of("a/b")),
                Map.of("enabled", true)
        );
        byte[] body = request.body();
        DdcCanonicalRequest canonical = new DdcCanonicalRequest(
                "POST",
                request.target().getRawPath(),
                Map.of("z", List.of("last"), "a", List.of("a/b")),
                now.toEpochMilli(),
                "nonce-1",
                body
        );

        assertThat(request.target().getRawQuery()).isEqualTo("a=a%2Fb&z=last");
        assertThat(request.headers().getFirst(DdcRequestSigner.SIGNATURE_HEADER))
                .isEqualTo(new DdcRequestSigner().sign(canonical, "sk"));
        body[0] = 0;
        assertThat(request.body()[0]).isNotZero();
        assertThatThrownBy(() -> request.headers().set("X-Test", "changed"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
