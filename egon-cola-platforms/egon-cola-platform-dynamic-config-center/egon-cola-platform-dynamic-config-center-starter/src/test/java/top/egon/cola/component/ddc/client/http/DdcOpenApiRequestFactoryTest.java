package top.egon.cola.component.ddc.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import top.egon.cola.component.ddc.error.http.DdcOpenApiRequestException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcOpenApiRequestFactoryTest {

    private static final Instant NOW = Instant.parse("2026-08-09T01:00:00Z");

    @Test
    void createsImmutableSignedRequestWithCanonicalTargetAndTraceHeaders() {
        DdcOpenApiRequestFactory factory = factory(true);

        DdcOpenApiRequestFactory.Request request = factory.create(
                HttpMethod.POST,
                "/api/v1/ddc/openapi/test",
                Map.of("z", List.of("last"), "a", List.of("a/b")),
                Map.of("enabled", true)
        );
        byte[] body = request.body();
        DdcCanonicalRequest canonical = new DdcCanonicalRequest(
                "POST",
                request.target().getRawPath(),
                Map.of("z", List.of("last"), "a", List.of("a/b")),
                NOW.toEpochMilli(),
                "nonce-1",
                body
        );

        assertThat(request.target().getRawQuery()).isEqualTo("a=a%2Fb&z=last");
        assertThat(request.headers().getFirst(DdcRequestSigner.SIGNATURE_HEADER))
                .isEqualTo(new DdcRequestSigner().sign(canonical, "sk"));
        assertThat(request.headers().getFirst("traceparent"))
                .matches("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}");
        assertThat(request.hasBody()).isTrue();
        body[0] = 0;
        assertThat(request.body()[0]).isNotZero();
        assertThatThrownBy(() -> request.headers().set("X-Test", "changed"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void createsUnsignedRequestWithoutAuthenticationHeaders() {
        DdcOpenApiRequestFactory.Request request = factory(false).create(
                HttpMethod.GET,
                "/api/v1/ddc/openapi/test",
                Map.of(),
                null
        );

        assertThat(request.target().toString())
                .isEqualTo("/api/v1/ddc/openapi/test");
        assertThat(request.headers()).doesNotContainKeys(
                DdcRequestSigner.ACCESS_KEY_HEADER,
                DdcRequestSigner.TIMESTAMP_HEADER,
                DdcRequestSigner.NONCE_HEADER,
                DdcRequestSigner.CONTENT_SHA256_HEADER,
                DdcRequestSigner.SIGNATURE_HEADER
        );
        assertThat(request.headers().getFirst("traceparent")).isNotBlank();
        assertThat(request.body()).isEmpty();
        assertThat(request.hasBody()).isFalse();
    }

    @Test
    void reportsSerializationFailureThroughTransportException() {
        SelfReference request = new SelfReference();
        request.value = request;

        assertThatThrownBy(() -> factory(true).create(
                HttpMethod.POST,
                "/api/v1/ddc/openapi/test",
                Map.of(),
                request
        )).isInstanceOf(DdcOpenApiRequestException.class)
                .hasMessage("DDC OpenAPI request serialization failed");
    }

    private DdcOpenApiRequestFactory factory(boolean signatureEnabled) {
        return new DdcOpenApiRequestFactory(
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> "nonce-1",
                signatureEnabled,
                "ak",
                "sk"
        );
    }

    private static final class SelfReference {

        public SelfReference value;
    }
}
