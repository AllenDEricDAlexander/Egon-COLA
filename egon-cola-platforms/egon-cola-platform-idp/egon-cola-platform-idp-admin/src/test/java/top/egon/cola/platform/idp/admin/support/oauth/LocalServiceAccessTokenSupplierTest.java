package top.egon.cola.platform.idp.admin.support.oauth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.platform.idp.admin.oauth.service.impl.PrivateKeyJwtAuthenticator;
import top.egon.cola.platform.idp.admin.token.service.impl.ClientCredentialsTokenService;
import top.egon.cola.platform.idp.core.oauth.ClientAssertionAuthentication;
import top.egon.cola.platform.idp.core.token.ServiceAccessToken;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalServiceAccessTokenSupplierTest {

    @TempDir
    private Path tempDirectory;

    @Test
    void obtainsThroughSharedAuthenticationAndSigningPathAndRenewsBySkew() {
        MutableClock clock = new MutableClock(
                Instant.parse("2026-08-10T00:00:00Z")
        );
        PrivateKeyJwtAuthenticator authenticator =
                mock(PrivateKeyJwtAuthenticator.class);
        ClientCredentialsTokenService tokens =
                mock(ClientCredentialsTokenService.class);
        AtomicInteger assertions = new AtomicInteger();
        when(authenticator.authenticate(
                eq(PrivateKeyJwtAuthenticator.ASSERTION_TYPE),
                eq("idp-service"),
                any()
        )).thenReturn(new ClientAssertionAuthentication(
                "idp-service",
                "idp-service-key-1",
                "assertion-1",
                clock.instant(),
                clock.instant().plusSeconds(60)
        ));
        when(tokens.issue(any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> new ServiceAccessToken(
                        "service-token-" + assertions.get(),
                        "Bearer",
                        clock.instant().plusSeconds(300),
                        Set.of("rbac3:policy:read")
                ));
        LocalServiceAccessTokenSupplier supplier =
                new LocalServiceAccessTokenSupplier(
                        authenticator,
                        tokens,
                        "idp-service",
                        URI.create("https://api.egon.internal/prod/permission/rbac3"),
                        "tenant-001",
                        Set.of("rbac3:policy:read"),
                        Duration.ofMinutes(5),
                        Duration.ofSeconds(30),
                        clock,
                        () -> "assertion-" + assertions.incrementAndGet()
                );

        assertThat(supplier.get()).isEqualTo("Bearer service-token-1");
        assertThat(supplier.get()).isEqualTo("Bearer service-token-1");

        clock.advance(Duration.ofSeconds(271));
        assertThat(supplier.get()).isEqualTo("Bearer service-token-2");

        verify(authenticator, times(2)).authenticate(
                eq(PrivateKeyJwtAuthenticator.ASSERTION_TYPE),
                eq("idp-service"),
                any()
        );
        verify(tokens, times(2)).issue(
                any(),
                any(),
                eq("tenant-001"),
                eq(Set.of("rbac3:policy:read")),
                eq(Duration.ofMinutes(5))
        );
    }

    @Test
    void cachesServiceTokensIndependentlyForEachExactTenant() {
        MutableClock clock = new MutableClock(
                Instant.parse("2026-08-10T00:00:00Z")
        );
        PrivateKeyJwtAuthenticator authenticator =
                mock(PrivateKeyJwtAuthenticator.class);
        ClientCredentialsTokenService tokens =
                mock(ClientCredentialsTokenService.class);
        AtomicInteger assertions = new AtomicInteger();
        when(authenticator.authenticate(
                eq(PrivateKeyJwtAuthenticator.ASSERTION_TYPE),
                eq("idp-service"),
                any()
        )).thenAnswer(invocation -> new ClientAssertionAuthentication(
                "idp-service",
                "idp-service-key-1",
                "assertion-" + assertions.get(),
                clock.instant(),
                clock.instant().plusSeconds(60)
        ));
        when(tokens.issue(any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> new ServiceAccessToken(
                        "service-token-" + invocation.getArgument(2),
                        "Bearer",
                        clock.instant().plusSeconds(300),
                        Set.of("service:authorization:decide")
                ));
        LocalServiceAccessTokenSupplier supplier =
                new LocalServiceAccessTokenSupplier(
                        authenticator,
                        tokens,
                        "idp-service",
                        URI.create("https://api.egon.internal/prod/permission/rbac3"),
                        "tenant-default",
                        Set.of("service:authorization:decide"),
                        Duration.ofMinutes(5),
                        Duration.ofSeconds(30),
                        clock,
                        () -> "assertion-" + assertions.incrementAndGet()
                );

        assertThat(supplier.get())
                .isEqualTo("Bearer service-token-tenant-default");
        assertThat(supplier.get("tenant-b"))
                .isEqualTo("Bearer service-token-tenant-b");
        assertThat(supplier.get("tenant-b"))
                .isEqualTo("Bearer service-token-tenant-b");
        assertThat(supplier.get())
                .isEqualTo("Bearer service-token-tenant-default");

        verify(tokens).issue(
                any(), any(), eq("tenant-default"),
                eq(Set.of("service:authorization:decide")),
                eq(Duration.ofMinutes(5))
        );
        verify(tokens).issue(
                any(), any(), eq("tenant-b"),
                eq(Set.of("service:authorization:decide")),
                eq(Duration.ofMinutes(5))
        );
        verify(authenticator, times(2)).authenticate(
                eq(PrivateKeyJwtAuthenticator.ASSERTION_TYPE),
                eq("idp-service"),
                any()
        );
    }

    @Test
    void requiresAbsoluteOwnerOnlyPrivateKeyFile() throws Exception {
        PrivateKeyJwtAuthenticator authenticator =
                mock(PrivateKeyJwtAuthenticator.class);
        ClientCredentialsTokenService tokens =
                mock(ClientCredentialsTokenService.class);
        Clock clock = Clock.systemUTC();

        assertThatThrownBy(() -> productionSupplier(
                authenticator,
                tokens,
                Path.of("relative-client-key.pem"),
                clock
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("absolute");

        Path insecureKey = tempDirectory.resolve("client-key.pem");
        Files.writeString(insecureKey, "invalid-key");
        Files.setPosixFilePermissions(insecureKey, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.GROUP_READ
        ));
        LocalServiceAccessTokenSupplier supplier = productionSupplier(
                authenticator,
                tokens,
                insecureKey,
                clock
        );

        assertThatThrownBy(supplier::get)
                .isInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("privateKeyFile must be owner-only");
    }

    private static LocalServiceAccessTokenSupplier productionSupplier(
            PrivateKeyJwtAuthenticator authenticator,
            ClientCredentialsTokenService tokens,
            Path privateKey,
            Clock clock
    ) {
        return new LocalServiceAccessTokenSupplier(
                authenticator,
                tokens,
                "idp-service",
                "idp-service-key-1",
                privateKey,
                URI.create("https://idp.example.test/oauth2/token"),
                URI.create("https://api.egon.internal/prod/permission/rbac3"),
                "tenant-001",
                Set.of("rbac3:policy:read"),
                Duration.ofMinutes(5),
                Duration.ofSeconds(30),
                clock,
                new SecureRandom()
        );
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
