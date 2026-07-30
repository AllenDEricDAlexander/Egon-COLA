package top.egon.cola.platform.rbac3.starter.authorization;

import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.AuthorizationDecision;
import top.egon.cola.platform.rbac3.contract.authorization.AuthorizationFenceDecision;
import top.egon.cola.platform.rbac3.contract.authorization.DataScopeDecision;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.FieldPolicyDecision;
import top.egon.cola.platform.rbac3.contract.authorization.OperationSodDecision;
import top.egon.cola.platform.rbac3.contract.authorization.PermissionRequest;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Snapshot-based authorization service. Every failure path remains closed.
 */
public final class DefaultAuthorizationService implements AuthorizationService {

    private final RuntimeContextSource contextSource;
    private final OperationSodEvaluator operationSodEvaluator;
    private final FenceVerifier fenceVerifier;
    private final Clock clock;

    public DefaultAuthorizationService(
            RuntimeContextSource contextSource,
            OperationSodEvaluator operationSodEvaluator,
            FenceVerifier fenceVerifier,
            Clock clock
    ) {
        this.contextSource = Objects.requireNonNull(contextSource, "contextSource");
        this.operationSodEvaluator = Objects.requireNonNull(
                operationSodEvaluator, "operationSodEvaluator");
        this.fenceVerifier = Objects.requireNonNull(fenceVerifier, "fenceVerifier");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public AuthorizationDecision requirePermission(PermissionRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            RuntimeAuthorizationContext context = contextSource.load();
            if (context.fenced()) {
                return permissionDecision(
                        context.claims(), request.permissionCode(), Decision.DENY,
                        "AUTHORIZATION_FENCED");
            }
            boolean allowed = context.snapshot().appContexts().stream()
                    .map(AppAuthorizationContext::permissions)
                    .anyMatch(permissions -> permissions.contains(request.permissionCode()));
            return permissionDecision(
                    context.claims(), request.permissionCode(),
                    allowed ? Decision.ALLOW : Decision.DENY,
                    allowed ? "ALLOW" : "PERMISSION_DENIED");
        } catch (RuntimeUnavailableException exception) {
            return permissionDecision(
                    exception.claims(), request.permissionCode(),
                    Decision.INDETERMINATE, exception.reasonCode());
        }
    }

    @Override
    public DataScopeDecision decideDataScope(DataScopeRequest request) {
        try {
            RuntimeAuthorizationContext context = contextSource.load();
            if (context.fenced()) {
                return dataScopeDecision(
                        context.claims(), request.permissionCode(), Decision.DENY,
                        "AUTHORIZATION_FENCED");
            }
            if (!hasPermission(context, request.permissionCode())) {
                return dataScopeDecision(
                        context.claims(), request.permissionCode(), Decision.DENY,
                        "PERMISSION_DENIED");
            }
            return context.snapshot().appContexts().stream()
                    .map(AppAuthorizationContext::dataScopes)
                    .map(scopes -> scopes.get(request.permissionCode()))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseGet(() -> dataScopeDecision(
                            context.claims(), request.permissionCode(), Decision.DENY,
                            "DATA_SCOPE_MISSING"));
        } catch (RuntimeUnavailableException exception) {
            return dataScopeDecision(
                    exception.claims(), request.permissionCode(), Decision.INDETERMINATE,
                    exception.reasonCode());
        }
    }

    @Override
    public FieldPolicyDecision decideFields(FieldPolicyRequest request) {
        try {
            RuntimeAuthorizationContext context = contextSource.load();
            String key = request.permissionCode() + ':'
                    + request.applicationCode() + ':' + request.resourceCode();
            if (!context.fenced() && hasPermission(context, request.permissionCode())) {
                FieldPolicyDecision decision = context.snapshot().appContexts().stream()
                        .map(AppAuthorizationContext::fieldPolicies)
                        .map(policies -> policies.get(key))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
                if (decision != null) {
                    return decision;
                }
            }
            return fieldDecision(
                    context.claims(), request, Decision.DENY,
                    context.fenced() ? "AUTHORIZATION_FENCED" : "FIELD_POLICY_MISSING");
        } catch (RuntimeUnavailableException exception) {
            return fieldDecision(
                    exception.claims(), request, Decision.INDETERMINATE,
                    exception.reasonCode());
        }
    }

    @Override
    public OperationSodDecision checkParticipation(OperationSodRequest request) {
        try {
            RuntimeAuthorizationContext context = contextSource.load();
            OperationSodResult result = !context.fenced()
                    && hasPermission(context, request.permissionCode())
                    ? operationSodEvaluator.evaluate(request)
                    : new OperationSodResult(false,
                    context.fenced() ? "AUTHORIZATION_FENCED" : "PERMISSION_DENIED",
                    List.of(), List.of());
            return operationDecision(
                    context.claims(), request,
                    result.permitted() ? Decision.ALLOW : Decision.DENY, result);
        } catch (RuntimeUnavailableException exception) {
            return operationDecision(
                    exception.claims(), request, Decision.INDETERMINATE,
                    new OperationSodResult(
                            false, exception.reasonCode(), List.of(), List.of()));
        }
    }

    @Override
    public AuthorizationFenceDecision verifyFence(AuthorizationFenceRequest request) {
        try {
            RuntimeAuthorizationContext context = contextSource.load();
            FenceResult result = !context.fenced()
                    && hasPermission(context, request.permissionCode())
                    ? fenceVerifier.verify(request)
                    : new FenceResult(false,
                    context.fenced() ? "AUTHORIZATION_FENCED" : "PERMISSION_DENIED",
                    clock.instant(), List.of());
            return fenceDecision(
                    context.claims(), request, context.snapshot().checksum(),
                    result.permitted() ? Decision.ALLOW : Decision.DENY, result);
        } catch (RuntimeUnavailableException exception) {
            return fenceDecision(
                    exception.claims(), request, "unavailable", Decision.INDETERMINATE,
                    new FenceResult(
                            false, exception.reasonCode(), clock.instant(), List.of()));
        }
    }

    private boolean hasPermission(
            RuntimeAuthorizationContext context,
            String permissionCode
    ) {
        return context.snapshot().appContexts().stream()
                .anyMatch(app -> app.permissions().contains(permissionCode));
    }

    private AuthorizationDecision permissionDecision(
            Rbac3TokenClaims claims,
            String permissionCode,
            Decision decision,
            String reasonCode
    ) {
        return new AuthorizationDecision(
                decision, reasonCode, claims.tid(), claims.sub(), permissionCode,
                claims.av(), claims.sv(), claims.pv(), List.of(), clock.instant());
    }

    private DataScopeDecision dataScopeDecision(
            Rbac3TokenClaims claims,
            String permissionCode,
            Decision decision,
            String reasonCode
    ) {
        return new DataScopeDecision(
                decision, reasonCode, claims.tid(), claims.sub(), permissionCode,
                "NONE", false, Set.of(), false, Set.of(), false, Set.of(),
                false, null, "unavailable", 0L, claims.av(), claims.sv(), claims.pv(),
                List.of(), clock.instant());
    }

    private FieldPolicyDecision fieldDecision(
            Rbac3TokenClaims claims,
            FieldPolicyRequest request,
            Decision decision,
            String reasonCode
    ) {
        return new FieldPolicyDecision(
                decision, reasonCode, claims.tid(), claims.sub(),
                request.permissionCode(), request.applicationCode(), request.resourceCode(),
                Map.of(), claims.av(), claims.sv(), claims.pv(), List.of(), clock.instant());
    }

    private OperationSodDecision operationDecision(
            Rbac3TokenClaims claims,
            OperationSodRequest request,
            Decision decision,
            OperationSodResult result
    ) {
        return new OperationSodDecision(
                decision, result.reasonCode(), claims.tid(), claims.sub(),
                request.permissionCode(), request.applicationCode(),
                request.businessResource(), request.businessId(), request.actionCode(),
                result.conflictingActionCodes(), claims.av(), claims.sv(), claims.pv(),
                result.evidenceIds(), clock.instant());
    }

    private AuthorizationFenceDecision fenceDecision(
            Rbac3TokenClaims claims,
            AuthorizationFenceRequest request,
            String checksum,
            Decision decision,
            FenceResult result
    ) {
        return new AuthorizationFenceDecision(
                decision, result.reasonCode(), claims.tid(), claims.sub(),
                request.permissionCode(), claims.sid(), checksum, request.businessResource(),
                request.businessId(), request.traceId(), claims.av(), claims.sv(), claims.pv(),
                result.evidenceIds(), clock.instant(), result.verifiedAt());
    }

    public static final class AuthorizationDeniedException extends RuntimeException {

        private final String reasonCode;

        public AuthorizationDeniedException(String reasonCode, String permissionCode) {
            super(reasonCode + ": " + permissionCode);
            this.reasonCode = reasonCode;
        }

        public String reasonCode() {
            return reasonCode;
        }
    }
}
