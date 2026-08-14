package top.egon.cola.platform.rbac3.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.contract.authorization.AuthorizationFenceDecision;
import top.egon.cola.platform.rbac3.contract.authorization.DataScopeDecision;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.FieldAccessLevel;
import top.egon.cola.platform.rbac3.contract.authorization.FieldPolicyDecision;
import top.egon.cola.platform.rbac3.contract.authorization.OperationSodDecision;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void allowDecisionRejectsNoneScope() {
        assertThrows(
                IllegalArgumentException.class,
                () -> scope("NONE", false, Set.of(), false, null)
        );
    }

    @Test
    void allowDecisionRejectsEmptyScope() {
        assertThrows(
                IllegalArgumentException.class,
                () -> scope(
                        "CUSTOM_USER",
                        false,
                        Set.of(),
                        false,
                        null
                )
        );
    }

    @Test
    void allowDecisionAcceptsConcreteSelfScope() {
        DataScopeDecision scope = scope(
                "SELF",
                false,
                Set.of(),
                true,
                "10001"
        );

        assertEquals(Decision.ALLOW, scope.decision());
        assertEquals("10001", scope.selfUserId());
    }

    @Test
    void dataScopeKeepsExactJsonFieldsAndRoundTrips() throws Exception {
        DataScopeDecision scope = scope(
                "CUSTOM_USER",
                false,
                Set.of("10002"),
                false,
                null
        );

        assertJsonFields(scope, List.of(
                "decision",
                "reasonCode",
                "tenantId",
                "subjectId",
                "permissionCode",
                "scopeType",
                "allInTenant",
                "allowedOrgIds",
                "includeOrgDescendants",
                "allowedDeptIds",
                "includeDeptDescendants",
                "allowedUserIds",
                "includeSelf",
                "selfUserId",
                "directorySnapshotVersion",
                "decisionVersion",
                "authVersion",
                "policyVersion",
                "evidenceIds",
                "decidedAt"
        ));
        assertEquals(
                scope,
                objectMapper.readValue(
                        objectMapper.writeValueAsBytes(scope),
                        DataScopeDecision.class
                )
        );
    }

    @Test
    void fieldPolicyKeepsExactJsonFieldsAndRoundTrips() throws Exception {
        FieldPolicyDecision decision = new FieldPolicyDecision(
                Decision.ALLOW,
                "FIELD_POLICY_RESOLVED",
                "20001",
                "10001",
                "finance:customer:read",
                "finance-web",
                "finance:customer",
                Map.of("mobile", new FieldPolicyDecision.FieldAccess(
                        FieldAccessLevel.MASKED_READ,
                        "MOBILE"
                )),
                43L,
                3L,
                List.of("field-policy-01"),
                Instant.parse("2026-07-30T08:00:00Z")
        );

        assertJsonFields(decision, List.of(
                "decision",
                "reasonCode",
                "tenantId",
                "subjectId",
                "permissionCode",
                "applicationCode",
                "resourceCode",
                "fields",
                "authVersion",
                "policyVersion",
                "evidenceIds",
                "decidedAt"
        ));
        assertEquals(decision, roundTrip(decision, FieldPolicyDecision.class));
    }

    @Test
    void operationSodKeepsExactJsonFieldsAndRoundTrips() throws Exception {
        OperationSodDecision decision = new OperationSodDecision(
                Decision.DENY,
                "OPERATION_SOD_VIOLATION",
                "20001",
                "10001",
                "finance:payment:approve",
                "finance-web",
                "PAYMENT",
                "payment-9001",
                "APPROVE",
                List.of("CREATE"),
                43L,
                3L,
                List.of("operation-sod-01"),
                Instant.parse("2026-07-30T08:00:00Z")
        );

        assertJsonFields(decision, List.of(
                "decision",
                "reasonCode",
                "tenantId",
                "subjectId",
                "permissionCode",
                "applicationCode",
                "businessResource",
                "businessId",
                "actionCode",
                "conflictingActionCodes",
                "authVersion",
                "policyVersion",
                "evidenceIds",
                "decidedAt"
        ));
        assertEquals(decision, roundTrip(decision, OperationSodDecision.class));
    }

    @Test
    void authorizationFenceKeepsExactJsonFieldsAndRoundTrips()
            throws Exception {
        AuthorizationFenceDecision decision = new AuthorizationFenceDecision(
                Decision.ALLOW,
                "AUTHORIZATION_FENCE_VERIFIED",
                "20001",
                "10001",
                "finance:payment:approve",
                "sha256:snapshot",
                "PAYMENT",
                "payment-9001",
                "trace-01",
                43L,
                3L,
                List.of("fence-01"),
                Instant.parse("2026-07-30T08:00:00Z"),
                Instant.parse("2026-07-30T08:00:00.010Z")
        );

        assertJsonFields(decision, List.of(
                "decision",
                "reasonCode",
                "tenantId",
                "subjectId",
                "permissionCode",
                "snapshotChecksum",
                "businessResource",
                "businessId",
                "traceId",
                "authVersion",
                "policyVersion",
                "evidenceIds",
                "decidedAt",
                "verifiedAt"
        ));
        assertEquals(
                decision,
                roundTrip(decision, AuthorizationFenceDecision.class)
        );
    }

    private DataScopeDecision scope(
            String scopeType,
            boolean allInTenant,
            Set<String> allowedUserIds,
            boolean includeSelf,
            String selfUserId) {
        return new DataScopeDecision(
                Decision.ALLOW,
                "DATA_SCOPE_RESOLVED",
                "20001",
                "10001",
                "finance:payment:read",
                scopeType,
                allInTenant,
                Set.of(),
                false,
                Set.of(),
                false,
                allowedUserIds,
                includeSelf,
                selfUserId,
                "hr-20260730-42",
                18L,
                43L,
                3L,
                List.of("71001"),
                Instant.parse("2026-07-30T08:00:00Z")
        );
    }

    private void assertJsonFields(Object value, List<String> fields)
            throws Exception {
        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsBytes(value)
        );
        List<String> actual = new ArrayList<>();
        json.fieldNames().forEachRemaining(actual::add);
        assertEquals(fields, actual);
    }

    private <T> T roundTrip(Object value, Class<T> type) throws Exception {
        return objectMapper.readValue(
                objectMapper.writeValueAsBytes(value),
                type
        );
    }
}
