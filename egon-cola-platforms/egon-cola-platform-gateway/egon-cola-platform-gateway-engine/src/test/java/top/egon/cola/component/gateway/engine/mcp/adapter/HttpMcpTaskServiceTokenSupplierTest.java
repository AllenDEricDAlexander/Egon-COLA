package top.egon.cola.component.gateway.engine.mcp.adapter;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpMcpTaskServiceTokenSupplierTest {

    private static final Instant NOW = Instant.parse("2026-08-11T12:00:00Z");

    @Test
    void cachesTokensByExactTenantAndTargetResource() {
        List<HttpMcpTaskServiceTokenSupplier.TokenRequest> requests =
                new ArrayList<>();
        AtomicInteger assertions = new AtomicInteger();
        HttpMcpTaskServiceTokenSupplier supplier = supplier(
                () -> "assertion-" + assertions.incrementAndGet(),
                request -> {
                    requests.add(request);
                    return new HttpMcpTaskServiceTokenSupplier.TokenResponse(
                            "token-" + requests.size(),
                            "Bearer",
                            300L
                    );
                }
        );

        URI first = URI.create("https://api.egon.internal/identity/mcp-one");
        URI second = URI.create("https://api.egon.internal/identity/mcp-two");

        assertThat(supplier.issue("tenant-a", first)).isEqualTo("token-1");
        assertThat(supplier.issue("tenant-a", first)).isEqualTo("token-1");
        assertThat(supplier.issue("tenant-b", first)).isEqualTo("token-2");
        assertThat(supplier.issue("tenant-a", second)).isEqualTo("token-3");

        assertThat(requests).hasSize(3);
        assertThat(requests.getFirst().clientId())
                .isEqualTo("gateway-engine-service");
        assertThat(requests.getFirst().tenantId()).isEqualTo("tenant-a");
        assertThat(requests.getFirst().resourceUri()).isEqualTo(first);
        assertThat(requests.getFirst().assertion()).isEqualTo("assertion-1");
        assertThat(requests.getFirst().scopes())
                .containsExactly("mcp:operation:invoke");
    }

    @Test
    void rejectsInvalidTokenEndpointResponsesWithoutLeakingTheToken() {
        HttpMcpTaskServiceTokenSupplier supplier = supplier(
                () -> "assertion",
                request -> new HttpMcpTaskServiceTokenSupplier.TokenResponse(
                        "do-not-log", "MAC", 300L
                )
        );

        assertThatThrownBy(() -> supplier.issue(
                "tenant-a",
                URI.create("https://api.egon.internal/identity/mcp")
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("IDP_SERVICE_TOKEN_RESPONSE_INVALID")
                .hasMessageNotContaining("do-not-log");
    }

    private HttpMcpTaskServiceTokenSupplier supplier(
            java.util.function.Supplier<String> assertions,
            HttpMcpTaskServiceTokenSupplier.TokenEndpoint endpoint
    ) {
        return new HttpMcpTaskServiceTokenSupplier(
                "gateway-engine-service",
                assertions,
                Set.of("mcp:operation:invoke"),
                Duration.ofSeconds(30),
                Clock.fixed(NOW, ZoneOffset.UTC),
                endpoint
        );
    }
}
