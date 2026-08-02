package top.egon.cola.platform.rbac3.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.contract.activation.ReplaceActiveRolesRequest;
import top.egon.cola.platform.rbac3.contract.auth.LoginResult;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;
import top.egon.cola.platform.rbac3.contract.auth.RefreshResult;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.contract.error.Rbac3ErrorCode;
import top.egon.cola.platform.rbac3.contract.error.Rbac3ErrorResponse;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void tokenClaimsContainOnlyIdentityAndVersionClaims() {
        List<String> names = Arrays.stream(
                        Rbac3TokenClaims.class.getRecordComponents()
                )
                .map(RecordComponent::getName)
                .toList();

        assertEquals(List.of(
                "iss",
                "aud",
                "sub",
                "tid",
                "sid",
                "av",
                "sv",
                "pv",
                "jti",
                "iat",
                "nbf",
                "exp",
                "kid"
        ), names);
        assertFalse(names.contains("roles"));
        assertFalse(names.contains("permissions"));
        assertFalse(names.contains("dataScopes"));
        assertFalse(names.contains("fieldPolicies"));
    }

    @Test
    void bigintIdsSerializeAsDecimalStrings() throws Exception {
        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(fixtureSnapshot())
        );

        assertTrue(json.path("sessionId").isTextual());
        assertEquals("40001", json.path("sessionId").textValue());
        assertTrue(json.path("appContexts")
                .get(0)
                .path("applicationId")
                .isTextual());
        assertEquals(
                "71001",
                json.path("appContexts")
                        .get(0)
                        .path("applicationId")
                        .textValue()
        );
    }

    @Test
    void replaceActiveRolesRequiresTheWholeSetAndExpectedVersion() {
        List<String> roleIds = new ArrayList<>(
                List.of("50001", "51001")
        );
        ReplaceActiveRolesRequest request = new ReplaceActiveRolesRequest(
                roleIds,
                2L
        );
        roleIds.add("52001");

        assertEquals(List.of("50001", "51001"), request.roleIds());
        assertEquals(2L, request.expectedContextVersion());
        assertThrows(
                UnsupportedOperationException.class,
                () -> request.roleIds().add("53001")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new ReplaceActiveRolesRequest(List.of("50001"), -1L)
        );
        assertEquals(
                List.of("roleIds", "expectedContextVersion"),
                Arrays.stream(
                                ReplaceActiveRolesRequest.class
                                        .getRecordComponents()
                        )
                        .map(RecordComponent::getName)
                        .toList()
        );
    }

    @Test
    void snapshotCollectionsAreDefensivelyImmutable() {
        List<AppAuthorizationContext> contexts = new ArrayList<>();
        contexts.add(fixtureContext());
        SessionAuthorizationSnapshot snapshot = new SessionAuthorizationSnapshot(
                "40001",
                43L,
                3L,
                18L,
                contexts,
                "sha256:snapshot",
                Instant.parse("2026-07-30T08:00:00Z")
        );
        contexts.clear();

        assertEquals(1, snapshot.appContexts().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.appContexts().clear()
        );
    }

    @Test
    void nestedContextCollectionsAreDefensivelyImmutable() {
        List<String> roots = new ArrayList<>(List.of("50001"));
        Set<String> permissions = new java.util.LinkedHashSet<>(
                Set.of("finance:payment:approve")
        );
        AppAuthorizationContext context = new AppAuthorizationContext(
                "71001",
                "finance-web",
                roots,
                List.of("60001"),
                List.of("50001", "50010"),
                permissions,
                Map.of(),
                Map.of(),
                List.of(),
                "payment-approvals"
        );
        roots.add("50002");
        permissions.add("finance:payment:read");

        assertEquals(List.of("50001"), context.activationRootRoleIds());
        assertEquals(
                Set.of("finance:payment:approve"),
                context.permissions()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> context.dataScopes().put("other", null)
        );
    }

    @Test
    void errorJsonUsesOnlyTypedSafeEvidenceAndFixedMetadata() throws Exception {
        Rbac3ErrorResponse response = new Rbac3ErrorResponse(
                new Rbac3ErrorResponse.Error(
                        Rbac3ErrorCode.SSD_CONSTRAINT_VIOLATION,
                        "Target assignment violates separation of duties",
                        false,
                        List.of(new Rbac3ErrorResponse.Detail(
                                "roleId",
                                "SSD_SET_LIMIT_EXCEEDED",
                                "sod-set-9001"
                        ))
                ),
                new Rbac3ErrorResponse.Meta(
                        "req-01",
                        "trace-01",
                        Instant.parse("2026-07-30T08:00:00Z")
                )
        );

        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(response)
        );
        assertEquals(
                "SSD_CONSTRAINT_VIOLATION",
                json.path("error").path("code").textValue()
        );
        assertEquals(
                "sod-set-9001",
                json.path("error")
                        .path("details")
                        .get(0)
                        .path("evidenceId")
                        .textValue()
        );
        assertEquals(
                "trace-01",
                json.path("meta").path("traceId").textValue()
        );
        assertFalse(json.toString().contains("stackTrace"));
        assertFalse(json.toString().contains("redisKey"));
    }

    @Test
    void blankSecurityIdentifiersAndNegativeVersionsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SessionAuthorizationSnapshot(
                        " ",
                        1L,
                        1L,
                        1L,
                        List.of(),
                        "sha256:snapshot",
                        Instant.parse("2026-07-30T08:00:00Z")
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new SessionAuthorizationSnapshot(
                        "40001",
                        -1L,
                        1L,
                        1L,
                        List.of(),
                        "sha256:snapshot",
                        Instant.parse("2026-07-30T08:00:00Z")
                )
        );
    }

    @Test
    void browserTokenResultsPermitRefreshCredentialOnlyInHttpOnlyCookie() {
        LoginResult login = new LoginResult(
                "Bearer",
                "access-token",
                900L,
                null,
                604800L,
                "40001",
                true,
                2,
                "/api/rbac3/v1/auth/role-activation-candidates",
                false
        );
        RefreshResult refresh = new RefreshResult(
                "Bearer",
                "refreshed-access-token",
                900L,
                null,
                604800L,
                "40001",
                43L,
                3L,
                18L,
                false,
                null,
                true
        );

        assertNull(login.refreshToken());
        assertNull(refresh.refreshToken());
    }

    private SessionAuthorizationSnapshot fixtureSnapshot() {
        return new SessionAuthorizationSnapshot(
                "40001",
                43L,
                3L,
                18L,
                List.of(fixtureContext()),
                "sha256:snapshot",
                Instant.parse("2026-07-30T08:00:00Z")
        );
    }

    private AppAuthorizationContext fixtureContext() {
        return new AppAuthorizationContext(
                "71001",
                "finance-web",
                List.of("50001"),
                List.of("60001"),
                List.of("50001", "50010"),
                Set.of("finance:payment:approve"),
                Map.of(),
                Map.of(),
                List.of(),
                "payment-approvals"
        );
    }
}
