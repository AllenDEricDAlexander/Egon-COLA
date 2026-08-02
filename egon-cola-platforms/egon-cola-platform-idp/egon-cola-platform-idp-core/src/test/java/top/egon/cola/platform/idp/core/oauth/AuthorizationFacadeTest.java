package top.egon.cola.platform.idp.core.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.core.port.AuthorizationCodeStore;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationFacadeTest {

    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");
    private static final String CLIENT_ID = "gateway-admin-web";
    private static final String REDIRECT_URI =
            "https://gateway.example.test/oauth/callback";
    private static final String AUDIENCE = "gateway-admin";
    private static final String VERIFIER =
            "0123456789abcdefghijklmnopqrstuvwxyz-._~ABCDE";
    private static final String RAW_CODE =
            "authorization-code-secret-value-0123456789";

    private FakeOAuthClientStore clients;
    private FakeAuthorizationCodeStore codes;
    private FakeTenantMembershipPort memberships;
    private AuthorizationFacade facade;

    @BeforeEach
    void setUp() {
        clients = new FakeOAuthClientStore();
        clients.put(new OAuthClient(
                CLIENT_ID,
                OAuthClient.ClientType.PUBLIC,
                OAuthClient.Status.ACTIVE,
                true,
                List.of(REDIRECT_URI),
                List.of(AUDIENCE)
        ));
        codes = new FakeAuthorizationCodeStore();
        memberships = new FakeTenantMembershipPort();
        memberships.put(new TenantMembershipPort.TenantMembership(
                "alice-sub",
                "tenant-a",
                "tenant-user-a",
                "Tenant A",
                TenantMembershipPort.MembershipStatus.ACTIVE
        ));
        facade = new AuthorizationFacade(
                clients,
                codes,
                memberships,
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> RAW_CODE
        );
    }

    @Test
    void storesOnlyCodeDigestAndConsumesMatchingS256CodeOnce() {
        AuthorizationFacade.AuthorizationResult result = facade.authorize(
                validRequest(),
                "alice-sub"
        );

        assertEquals(RAW_CODE, result.code());
        assertEquals("state-123", result.state());
        assertEquals(NOW.plusSeconds(60), result.expiresAt());
        assertFalse(codes.values.containsKey(RAW_CODE));
        assertNotEquals(RAW_CODE, codes.singleDigest());
        assertEquals(Duration.ofSeconds(60), codes.singleTtl());

        AuthorizationCode consumed = facade.consume(
                result.code(),
                VERIFIER,
                REDIRECT_URI,
                CLIENT_ID
        );

        assertEquals("alice-sub", consumed.identitySub());
        assertEquals("tenant-a", consumed.tenantId());
        assertEquals("tenant-user-a", consumed.rbac3UserId());
        assertEquals(AUDIENCE, consumed.audience());
        assertThrows(OAuthException.class, () -> facade.consume(
                result.code(),
                VERIFIER,
                REDIRECT_URI,
                CLIENT_ID
        ));
    }

    @Test
    void refusesAuthorizationWhenRbac3DoesNotResolveMembership() {
        memberships.remove("alice-sub", "tenant-a", CLIENT_ID);

        OAuthException exception = assertThrows(
                OAuthException.class,
                () -> facade.authorize(validRequest(), "alice-sub")
        );

        assertEquals("access_denied", exception.oauthError());
        assertEquals(0, codes.values.size());
    }

    @Test
    void requiresExactRedirectRegisteredAudienceAndActiveClient() {
        assertOAuthError(
                "invalid_request",
                validRequest().withRedirectUri(REDIRECT_URI + "/")
        );
        assertOAuthError(
                "invalid_target",
                validRequest().withAudience("unknown-api")
        );
        clients.put(clients.get(CLIENT_ID).withStatus(
                OAuthClient.Status.DISABLED
        ));
        assertOAuthError("unauthorized_client", validRequest());
    }

    @Test
    void browserAuthorizationRequiresStateNonceAndS256() {
        assertOAuthError("invalid_request", validRequest().withState(""));
        assertOAuthError("invalid_request", validRequest().withNonce(""));
        assertOAuthError(
                "invalid_request",
                validRequest().withCodeChallengeMethod("plain")
        );
        assertOAuthError(
                "invalid_request",
                validRequest().withResponseType("token")
        );
    }

    @Test
    void wrongVerifierOrClientConsumesCodeAndCannotBeRetried() {
        String code = facade.authorize(validRequest(), "alice-sub").code();

        assertOAuthGrantFailure(() -> facade.consume(
                code,
                "wrong-verifier-value-that-is-long-enough-0123456789",
                REDIRECT_URI,
                CLIENT_ID
        ));
        assertOAuthGrantFailure(() -> facade.consume(
                code,
                VERIFIER,
                REDIRECT_URI,
                CLIENT_ID
        ));

        String second = facade.authorize(validRequest(), "alice-sub").code();
        assertOAuthGrantFailure(() -> facade.consume(
                second,
                VERIFIER,
                REDIRECT_URI,
                "ddc-admin-web"
        ));
    }

    @Test
    void usesTheCurrentAuthorizationCodeTtlForEachNewGrant() {
        facade = new AuthorizationFacade(
                clients,
                codes,
                memberships,
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> RAW_CODE,
                () -> Duration.ofSeconds(90)
        );

        AuthorizationFacade.AuthorizationResult result = facade.authorize(
                validRequest(),
                "alice-sub"
        );

        assertEquals(NOW.plusSeconds(90), result.expiresAt());
        assertEquals(Duration.ofSeconds(90), codes.singleTtl());
    }

    private AuthorizationRequest validRequest() {
        return new AuthorizationRequest(
                "code",
                CLIENT_ID,
                REDIRECT_URI,
                AUDIENCE,
                "tenant-a",
                "state-123",
                "nonce-123",
                s256(VERIFIER),
                "S256"
        );
    }

    private void assertOAuthError(
            String expectedError,
            AuthorizationRequest request
    ) {
        OAuthException exception = assertThrows(
                OAuthException.class,
                () -> facade.authorize(request, "alice-sub")
        );
        assertEquals(expectedError, exception.oauthError());
    }

    private void assertOAuthGrantFailure(Runnable action) {
        OAuthException exception = assertThrows(
                OAuthException.class,
                action::run
        );
        assertEquals("invalid_grant", exception.oauthError());
    }

    private static String s256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    digest
            );
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class FakeOAuthClientStore
            implements OAuthClientStore {
        private final Map<String, OAuthClient> values = new HashMap<>();

        @Override
        public Optional<OAuthClient> findById(String clientId) {
            return Optional.ofNullable(values.get(clientId));
        }

        void put(OAuthClient client) {
            values.put(client.clientId(), client);
        }

        OAuthClient get(String clientId) {
            return values.get(clientId);
        }
    }

    private static final class FakeAuthorizationCodeStore
            implements AuthorizationCodeStore {
        private final Map<String, AuthorizationCode> values = new HashMap<>();
        private final Map<String, Duration> timeouts = new HashMap<>();

        @Override
        public void put(
                String codeDigest,
                AuthorizationCode code,
                Duration ttl
        ) {
            values.put(codeDigest, code);
            timeouts.put(codeDigest, ttl);
        }

        @Override
        public AuthorizationCode consume(String codeDigest) {
            return values.remove(codeDigest);
        }

        String singleDigest() {
            return values.keySet().iterator().next();
        }

        Duration singleTtl() {
            return timeouts.values().iterator().next();
        }
    }

    private static final class FakeTenantMembershipPort
            implements TenantMembershipPort {
        private final Map<String, TenantMembership> values = new HashMap<>();

        @Override
        public TenantMembership resolve(
                String identitySub,
                String tenantId,
                String clientId
        ) {
            TenantMembership membership = values.get(key(
                    identitySub,
                    tenantId,
                    clientId
            ));
            if (membership == null) {
                throw new TenantMembershipException(
                        "active tenant membership was not found"
                );
            }
            return membership;
        }

        @Override
        public List<TenantMembership> list(
                String identitySub,
                String clientId
        ) {
            return values.values().stream()
                    .filter(value -> value.identitySub().equals(identitySub))
                    .toList();
        }

        void put(TenantMembership membership) {
            values.put(key(
                    membership.identitySub(),
                    membership.tenantId(),
                    CLIENT_ID
            ), membership);
        }

        void remove(String identitySub, String tenantId, String clientId) {
            values.remove(key(identitySub, tenantId, clientId));
        }

        private String key(
                String identitySub,
                String tenantId,
                String clientId
        ) {
            return identitySub + ':' + tenantId + ':' + clientId;
        }
    }
}
