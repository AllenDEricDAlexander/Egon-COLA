package top.egon.cola.platform.tianquan.jianshen.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.tianquan.jianshen.contract.activation.ReplaceActiveRolesRequest;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.ActiveRoleDescriptor;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.ApplicationAccessScope;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.BusinessAccessScope;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.GatewayBizAppScopeSnapshot;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.SystemAuthorizationSnapshot;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.UserAuthorizationSnapshot;
import top.egon.cola.platform.tianquan.jianshen.contract.error.Rbac3ErrorCode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void snapshotSerializesIdentityBindingAndNoSessionFields() throws Exception {
        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(fixtureSnapshot()));

        assertEquals("subject-a", json.path("identitySub").textValue());
        assertTrue(json.path("appContexts").isArray());
        assertFalse(json.has("sessionId"));
        assertFalse(json.has("sessionVersion"));
    }

    @Test
    void systemAuthorizationSnapshotSerializesResourceCodes() throws Exception {
        Instant generated = Instant.parse("2026-07-30T08:00:00Z");
        SystemAuthorizationSnapshot snapshot = new SystemAuthorizationSnapshot(
                "tenant-a", "subject-a", "101", "finance", 43L, 3L,
                List.of("role-1"), Set.of("finance:payment:read"), Map.of(), Map.of(),
                "sha256:snapshot", generated, generated.plusSeconds(3600));

        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(snapshot));

        assertTrue(json.has("resourceCodes"));
        assertTrue(json.path("resourceCodes").isArray());
    }

    @Test
    void activeRoleDescriptorCarriesOnlyStableRoleFacts() throws Exception {
        ActiveRoleDescriptor descriptor = new ActiveRoleDescriptor(
                "role-1", "finance-operator", "finance-web");

        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(descriptor));

        assertEquals("role-1", json.path("roleId").textValue());
        assertEquals("finance-operator", json.path("roleCode").textValue());
        assertEquals("finance-web", json.path("applicationCode").textValue());
        assertFalse(json.has("permissions"));
        assertFalse(json.has("tenantId"));
    }

    @Test
    void replaceActiveRolesUsesExpectedAuthVersion() {
        List<String> roleIds = new ArrayList<>(List.of("50001", "51001"));
        ReplaceActiveRolesRequest request = new ReplaceActiveRolesRequest(roleIds, 2L);
        roleIds.add("52001");

        assertEquals(List.of("50001", "51001"), request.roleIds());
        assertEquals(2L, request.expectedAuthVersion());
        assertThrows(UnsupportedOperationException.class,
                () -> request.roleIds().add("53001"));
        assertThrows(IllegalArgumentException.class,
                () -> new ReplaceActiveRolesRequest(List.of("50001"), -1L));
    }

    @Test
    void snapshotCollectionsAreDefensivelyImmutable() {
        List<AppAuthorizationContext> contexts = new ArrayList<>();
        contexts.add(fixtureContext());
        UserAuthorizationSnapshot snapshot = new UserAuthorizationSnapshot(
                "finance", "tenant-a", "subject-a", "101", 43L, 3L,
                contexts, "sha256:snapshot",
                Instant.parse("2026-07-30T08:00:00Z"),
                Instant.parse("2026-07-30T09:00:00Z"));
        contexts.clear();

        assertEquals(1, snapshot.appContexts().size());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.appContexts().clear());
    }

    @Test
    void nestedContextCollectionsAreDefensivelyImmutable() {
        List<String> roots = new ArrayList<>(List.of("50001"));
        Set<String> permissions = new java.util.LinkedHashSet<>(
                Set.of("finance:payment:approve"));
        AppAuthorizationContext context = new AppAuthorizationContext(
                "71001", "finance-web", roots, List.of("60001"),
                List.of("50001", "50010"), permissions, Map.of(), Map.of(),
                List.of(), "payment-approvals");
        roots.add("50002");
        permissions.add("finance:payment:read");

        assertEquals(List.of("50001"), context.activationRootRoleIds());
        assertEquals(Set.of("finance:payment:approve"), context.permissions());
        assertThrows(UnsupportedOperationException.class,
                () -> context.dataScopes().put("other", null));
    }

    @Test
    void blankSecurityIdentifiersAndNegativeVersionsAreRejected() {
        Instant generated = Instant.parse("2026-07-30T08:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> new UserAuthorizationSnapshot(
                "finance", "tenant-a", " ", "101", 1L, 1L, List.of(),
                "sha256:snapshot", generated, generated.plusSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new UserAuthorizationSnapshot(
                "finance", "tenant-a", "subject-a", "101", -1L, 1L,
                List.of(), "sha256:snapshot", generated, generated.plusSeconds(1)));
    }

    @Test
    void roundTripsGatewayBizAppScopeSnapshot() throws Exception {
        GatewayBizAppScopeSnapshot snapshot = fixtureGatewayScope();

        String json = objectMapper.writeValueAsString(snapshot);

        assertEquals(snapshot, objectMapper.readValue(
                json, GatewayBizAppScopeSnapshot.class));
    }

    @Test
    void gatewayScopeContainsNoPermissionPayload() throws Exception {
        String json = objectMapper.writeValueAsString(fixtureGatewayScope());

        assertFalse(json.contains("permissions"));
        assertFalse(json.contains("dataScopes"));
        assertFalse(json.contains("fieldPolicies"));
        assertFalse(json.contains("resources"));
    }

    private UserAuthorizationSnapshot fixtureSnapshot() {
        Instant generated = Instant.parse("2026-07-30T08:00:00Z");
        return new UserAuthorizationSnapshot(
                "finance", "tenant-a", "subject-a", "101", 43L, 3L,
                List.of(fixtureContext()), "sha256:snapshot", generated,
                generated.plusSeconds(3600));
    }

    private AppAuthorizationContext fixtureContext() {
        return new AppAuthorizationContext(
                "71001", "finance-web", List.of("50001"), List.of("60001"),
                List.of("50001", "50010"), Set.of("finance:payment:approve"),
                Map.of(), Map.of(), List.of(), "payment-approvals");
    }

    private GatewayBizAppScopeSnapshot fixtureGatewayScope() {
        Instant generated = Instant.parse("2026-07-30T08:00:00Z");
        return new GatewayBizAppScopeSnapshot(
                "tenant-a",
                "subject-a",
                "101",
                43L,
                3L,
                List.of(new BusinessAccessScope(
                        "business-1",
                        "finance",
                        List.of(new ApplicationAccessScope(
                                "application-1", "finance-web")))),
                "sha256:gateway-scope",
                generated,
                generated.plusSeconds(3600));
    }
}
