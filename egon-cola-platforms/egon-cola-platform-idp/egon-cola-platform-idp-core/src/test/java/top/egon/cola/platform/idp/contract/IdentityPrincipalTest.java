package top.egon.cola.platform.idp.contract;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentityPrincipalTest {

    @Test
    void rejectsBlankStableIdentityClaims() {
        assertThrows(IllegalArgumentException.class, () ->
                new IdentityPrincipal(
                        " ",
                        "tenant-a",
                        "jti-a",
                        Set.of("mock-api"),
                        Instant.EPOCH,
                        Instant.EPOCH.plusSeconds(300),
                        AuthenticationContext.password()
                ));
    }

    @Test
    void accessIdentityContainsNoAuthorizationFacts() {
        Set<String> components = Arrays.stream(
                        IdentityPrincipal.class.getRecordComponents()
                )
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        assertFalse(components.contains("roles"));
        assertFalse(components.contains("permissions"));
        assertFalse(components.contains("authVersion"));
        assertFalse(components.contains("contextVersion"));
        assertFalse(components.contains("policyVersion"));
    }

    @Test
    void distinguishesUserAndServicePrincipalContracts() {
        IdentityPrincipal user = new IdentityPrincipal(
                "alice-sub",
                "tenant-a",
                "jti-a",
                Set.of("https://api.egon.internal/prod/permission/idp"),
                Instant.EPOCH,
                Instant.EPOCH.plusSeconds(300),
                AuthenticationContext.password()
        );
        ServiceIdentityPrincipal service = new ServiceIdentityPrincipal(
                "idp-service",
                "tenant-a",
                "idp-service",
                "jti-service",
                URI.create(
                        "https://api.egon.internal/prod/permission/rbac3"
                ),
                7L,
                Set.of("rbac3:policy:read"),
                "permission",
                "idp",
                "prod",
                "key-2026-01",
                Instant.EPOCH,
                Instant.EPOCH.plusSeconds(300)
        );

        assertEquals(PrincipalType.USER, user.principalType());
        assertEquals(PrincipalType.SERVICE, service.principalType());
        assertEquals(Set.of("rbac3:policy:read"), service.scopes());
    }
}
