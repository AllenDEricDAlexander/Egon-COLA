package top.egon.cola.platform.rbac3.starter.client;

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

class HttpTenantServiceTokenSupplierTest {

    private static final Instant NOW = Instant.parse("2026-08-11T10:00:00Z");

    @Test
    void issuesAndCachesServiceTokensIndependentlyForExactTenants() {
        List<HttpTenantServiceTokenSupplier.TokenRequest> requests =
                new ArrayList<>();
        AtomicInteger assertionSequence = new AtomicInteger();
        HttpTenantServiceTokenSupplier supplier = supplier(
                () -> "assertion-" + assertionSequence.incrementAndGet(),
                request -> {
                    requests.add(request);
                    return new HttpTenantServiceTokenSupplier.TokenResponse(
                            "token-for-" + request.tenantId(),
                            "Bearer",
                            300L
                    );
                }
        );

        assertThat(supplier.apply("tenant-a")).isEqualTo("token-for-tenant-a");
        assertThat(supplier.apply("tenant-b")).isEqualTo("token-for-tenant-b");
        assertThat(supplier.apply("tenant-a")).isEqualTo("token-for-tenant-a");

        assertThat(requests).extracting(
                HttpTenantServiceTokenSupplier.TokenRequest::tenantId
        ).containsExactly("tenant-a", "tenant-b");
        assertThat(requests.getFirst().clientId()).isEqualTo("rbac3-service");
        assertThat(requests.getFirst().assertion()).isEqualTo("assertion-1");
        assertThat(requests.getFirst().resourceUri()).isEqualTo(
                URI.create("https://api.egon.internal/permission/rbac3")
        );
        assertThat(requests.getFirst().scopes()).containsExactlyInAnyOrder(
                "service:authorization:snapshot",
                "service:authorization:decide"
        );
    }

    @Test
    void rejectsAResponseThatIsNotABearerToken() {
        HttpTenantServiceTokenSupplier supplier = supplier(
                () -> "assertion",
                request -> new HttpTenantServiceTokenSupplier.TokenResponse(
                        "do-not-use", "MAC", 300L
                )
        );

        assertThatThrownBy(() -> supplier.apply("tenant-a"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("IDP_SERVICE_TOKEN_RESPONSE_INVALID")
                .hasMessageNotContaining("do-not-use");
    }

    private HttpTenantServiceTokenSupplier supplier(
            java.util.function.Supplier<String> assertions,
            HttpTenantServiceTokenSupplier.TokenEndpoint endpoint
    ) {
        return new HttpTenantServiceTokenSupplier(
                "rbac3-service",
                assertions,
                URI.create("https://api.egon.internal/permission/rbac3"),
                Set.of(
                        "service:authorization:snapshot",
                        "service:authorization:decide"
                ),
                Duration.ofSeconds(30),
                Clock.fixed(NOW, ZoneOffset.UTC),
                endpoint
        );
    }
}
