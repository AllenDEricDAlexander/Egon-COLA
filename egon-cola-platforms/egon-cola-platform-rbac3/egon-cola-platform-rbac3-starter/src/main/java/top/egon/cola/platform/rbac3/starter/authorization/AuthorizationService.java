package top.egon.cola.platform.rbac3.starter.authorization;

import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.authorization.AuthorizationDecision;
import top.egon.cola.platform.rbac3.contract.authorization.AuthorizationFenceDecision;
import top.egon.cola.platform.rbac3.contract.authorization.DataScopeDecision;
import top.egon.cola.platform.rbac3.contract.authorization.FieldPolicyDecision;
import top.egon.cola.platform.rbac3.contract.authorization.OperationSodDecision;
import top.egon.cola.platform.rbac3.contract.authorization.PermissionRequest;
import top.egon.cola.platform.rbac3.contract.authorization.SystemAuthorizationSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Final policy-enforcement API exposed to business applications.
 */
public interface AuthorizationService {

    AuthorizationDecision requirePermission(PermissionRequest request);

    DataScopeDecision decideDataScope(DataScopeRequest request);

    FieldPolicyDecision decideFields(FieldPolicyRequest request);

    OperationSodDecision checkParticipation(OperationSodRequest request);

    AuthorizationFenceDecision verifyFence(AuthorizationFenceRequest request);

    record DataScopeRequest(String permissionCode) {
        public DataScopeRequest {
            permissionCode = required(permissionCode, "permissionCode");
        }
    }

    record FieldPolicyRequest(
            String permissionCode,
            String applicationCode,
            String resourceCode
    ) {
        public FieldPolicyRequest {
            permissionCode = required(permissionCode, "permissionCode");
            applicationCode = required(applicationCode, "applicationCode");
            resourceCode = required(resourceCode, "resourceCode");
        }
    }

    record OperationSodRequest(
            String permissionCode,
            String applicationCode,
            String businessResource,
            String businessId,
            String actionCode
    ) {
        public OperationSodRequest {
            permissionCode = required(permissionCode, "permissionCode");
            applicationCode = required(applicationCode, "applicationCode");
            businessResource = required(businessResource, "businessResource");
            businessId = required(businessId, "businessId");
            actionCode = required(actionCode, "actionCode");
        }
    }

    record AuthorizationFenceRequest(
            String permissionCode,
            String businessResource,
            String businessId,
            String traceId
    ) {
        public AuthorizationFenceRequest {
            permissionCode = required(permissionCode, "permissionCode");
            businessResource = required(businessResource, "businessResource");
            businessId = required(businessId, "businessId");
            traceId = required(traceId, "traceId");
        }
    }

    record RuntimeAuthorizationContext(
            IdentityPrincipal identity,
            SystemAuthorizationSnapshot snapshot,
            boolean fenced
    ) {
        public RuntimeAuthorizationContext {
            identity = Objects.requireNonNull(identity, "identity");
            snapshot = Objects.requireNonNull(snapshot, "snapshot");
            if (!identity.subject().equals(snapshot.identitySub())
                    || !identity.tenantId().equals(snapshot.tenantId())
                    || !identity.sessionId().equals(snapshot.sessionId())) {
                throw new RuntimeUnavailableException(
                        "AUTHORIZATION_IDENTITY_MISMATCH", identity);
            }
        }
    }

    @FunctionalInterface
    interface RuntimeContextSource {
        RuntimeAuthorizationContext load();
    }

    @FunctionalInterface
    interface OperationSodEvaluator {
        OperationSodResult evaluate(OperationSodRequest request);
    }

    @FunctionalInterface
    interface FenceVerifier {
        FenceResult verify(AuthorizationFenceRequest request);
    }

    record OperationSodResult(
            boolean permitted,
            String reasonCode,
            List<String> conflictingActionCodes,
            List<String> evidenceIds
    ) {
        public OperationSodResult {
            reasonCode = required(reasonCode, "reasonCode");
            conflictingActionCodes = List.copyOf(conflictingActionCodes);
            evidenceIds = List.copyOf(evidenceIds);
        }

        public static OperationSodResult allowed() {
            return new OperationSodResult(true, "ALLOW", List.of(), List.of());
        }
    }

    record FenceResult(
            boolean permitted,
            String reasonCode,
            Instant verifiedAt,
            List<String> evidenceIds
    ) {
        public FenceResult {
            reasonCode = required(reasonCode, "reasonCode");
            verifiedAt = Objects.requireNonNull(verifiedAt, "verifiedAt");
            evidenceIds = List.copyOf(evidenceIds);
        }

        public static FenceResult allowed(Instant verifiedAt) {
            return new FenceResult(true, "ALLOW", verifiedAt, List.of());
        }
    }

    final class RuntimeUnavailableException extends RuntimeException {

        private final String reasonCode;
        private final IdentityPrincipal identity;

        public RuntimeUnavailableException(
                String reasonCode,
                IdentityPrincipal identity
        ) {
            super(required(reasonCode, "reasonCode"));
            this.reasonCode = reasonCode;
            this.identity = Objects.requireNonNull(identity, "identity");
        }

        public String reasonCode() {
            return reasonCode;
        }

        public IdentityPrincipal identity() {
            return identity;
        }
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
