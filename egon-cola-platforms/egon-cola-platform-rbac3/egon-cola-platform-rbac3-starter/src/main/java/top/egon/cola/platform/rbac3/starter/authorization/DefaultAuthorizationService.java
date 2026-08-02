package top.egon.cola.platform.rbac3.starter.authorization;

import top.egon.cola.platform.idp.contract.IdentityPrincipal;
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
                        context, request.permissionCode(), Decision.DENY,
                        "AUTHORIZATION_FENCED");
            }
            boolean allowed = context.snapshot().permissions()
                    .contains(request.permissionCode());
            return permissionDecision(
                    context, request.permissionCode(),
                    allowed ? Decision.ALLOW : Decision.DENY,
                    allowed ? "ALLOW" : "PERMISSION_DENIED");
        } catch (RuntimeUnavailableException exception) {
            return permissionDecision(
                    unavailable(exception), request.permissionCode(),
                    Decision.INDETERMINATE, exception.reasonCode());
        }
    }

    @Override
    public DataScopeDecision decideDataScope(DataScopeRequest request) {
        try {
            RuntimeAuthorizationContext context = contextSource.load();
            if (context.fenced()) {
                return dataScopeDecision(
                        context, request.permissionCode(), Decision.DENY,
                        "AUTHORIZATION_FENCED");
            }
            if (!hasPermission(context, request.permissionCode())) {
                return dataScopeDecision(
                        context, request.permissionCode(), Decision.DENY,
                        "PERMISSION_DENIED");
            }
            DataScopeDecision decision = context.snapshot().dataScopes()
                    .get(request.permissionCode());
            return decision == null
                    ? dataScopeDecision(context, request.permissionCode(),
                    Decision.DENY, "DATA_SCOPE_MISSING")
                    : decision;
        } catch (RuntimeUnavailableException exception) {
            return dataScopeDecision(
                    unavailable(exception), request.permissionCode(), Decision.INDETERMINATE,
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
                FieldPolicyDecision decision = context.snapshot().fieldPolicies().get(key);
                if (decision != null) {
                    return decision;
                }
            }
            return fieldDecision(
                    context, request, Decision.DENY,
                    context.fenced() ? "AUTHORIZATION_FENCED" : "FIELD_POLICY_MISSING");
        } catch (RuntimeUnavailableException exception) {
            return fieldDecision(
                    unavailable(exception), request, Decision.INDETERMINATE,
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
                    context, request,
                    result.permitted() ? Decision.ALLOW : Decision.DENY, result);
        } catch (RuntimeUnavailableException exception) {
            return operationDecision(
                    unavailable(exception), request, Decision.INDETERMINATE,
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
                    context, request, context.snapshot().checksum(),
                    result.permitted() ? Decision.ALLOW : Decision.DENY, result);
        } catch (RuntimeUnavailableException exception) {
            return fenceDecision(
                    unavailable(exception), request, "unavailable", Decision.INDETERMINATE,
                    new FenceResult(
                            false, exception.reasonCode(), clock.instant(), List.of()));
        }
    }

    private boolean hasPermission(
            RuntimeAuthorizationContext context,
            String permissionCode
    ) {
        return context.snapshot().permissions().contains(permissionCode);
    }

    private AuthorizationDecision permissionDecision(
            RuntimeFacts facts,
            String permissionCode,
            Decision decision,
            String reasonCode
    ) {
        return new AuthorizationDecision(
                decision, reasonCode, facts.tenantId(), facts.rbac3UserId(), permissionCode,
                facts.authVersion(), facts.contextVersion(), facts.policyVersion(),
                facts.activeRoleIds(), clock.instant());
    }

    private DataScopeDecision dataScopeDecision(
            RuntimeFacts facts,
            String permissionCode,
            Decision decision,
            String reasonCode
    ) {
        return new DataScopeDecision(
                decision, reasonCode, facts.tenantId(), facts.rbac3UserId(), permissionCode,
                "NONE", false, Set.of(), false, Set.of(), false, Set.of(),
                false, null, "unavailable", 0L, facts.authVersion(),
                facts.contextVersion(), facts.policyVersion(),
                List.of(), clock.instant());
    }

    private FieldPolicyDecision fieldDecision(
            RuntimeFacts facts,
            FieldPolicyRequest request,
            Decision decision,
            String reasonCode
    ) {
        return new FieldPolicyDecision(
                decision, reasonCode, facts.tenantId(), facts.rbac3UserId(),
                request.permissionCode(), request.applicationCode(), request.resourceCode(),
                Map.of(), facts.authVersion(), facts.contextVersion(),
                facts.policyVersion(), List.of(), clock.instant());
    }

    private OperationSodDecision operationDecision(
            RuntimeFacts facts,
            OperationSodRequest request,
            Decision decision,
            OperationSodResult result
    ) {
        return new OperationSodDecision(
                decision, result.reasonCode(), facts.tenantId(), facts.rbac3UserId(),
                request.permissionCode(), request.applicationCode(),
                request.businessResource(), request.businessId(), request.actionCode(),
                result.conflictingActionCodes(), facts.authVersion(),
                facts.contextVersion(), facts.policyVersion(),
                result.evidenceIds(), clock.instant());
    }

    private AuthorizationFenceDecision fenceDecision(
            RuntimeFacts facts,
            AuthorizationFenceRequest request,
            String checksum,
            Decision decision,
            FenceResult result
    ) {
        return new AuthorizationFenceDecision(
                decision, result.reasonCode(), facts.tenantId(), facts.rbac3UserId(),
                request.permissionCode(), facts.sessionId(), checksum,
                request.businessResource(), request.businessId(), request.traceId(),
                facts.authVersion(), facts.contextVersion(), facts.policyVersion(),
                result.evidenceIds(), clock.instant(), result.verifiedAt());
    }

    private RuntimeFacts unavailable(RuntimeUnavailableException exception) {
        IdentityPrincipal identity = exception.identity();
        return new RuntimeFacts(identity.tenantId(), identity.subject(),
                identity.sessionId(), 0, 0, 0, List.of());
    }

    private RuntimeFacts facts(RuntimeAuthorizationContext context) {
        return new RuntimeFacts(
                context.snapshot().tenantId(), context.snapshot().rbac3UserId(),
                context.snapshot().sessionId(), context.snapshot().authVersion(),
                context.snapshot().contextVersion(), context.snapshot().policyVersion(),
                context.snapshot().activeRoleIds());
    }

    private AuthorizationDecision permissionDecision(
            RuntimeAuthorizationContext context,
            String permissionCode,
            Decision decision,
            String reasonCode) {
        return permissionDecision(facts(context), permissionCode, decision, reasonCode);
    }

    private DataScopeDecision dataScopeDecision(
            RuntimeAuthorizationContext context,
            String permissionCode,
            Decision decision,
            String reasonCode) {
        return dataScopeDecision(facts(context), permissionCode, decision, reasonCode);
    }

    private FieldPolicyDecision fieldDecision(
            RuntimeAuthorizationContext context,
            FieldPolicyRequest request,
            Decision decision,
            String reasonCode) {
        return fieldDecision(facts(context), request, decision, reasonCode);
    }

    private OperationSodDecision operationDecision(
            RuntimeAuthorizationContext context,
            OperationSodRequest request,
            Decision decision,
            OperationSodResult result) {
        return operationDecision(facts(context), request, decision, result);
    }

    private AuthorizationFenceDecision fenceDecision(
            RuntimeAuthorizationContext context,
            AuthorizationFenceRequest request,
            String checksum,
            Decision decision,
            FenceResult result) {
        return fenceDecision(facts(context), request, checksum, decision, result);
    }

    private record RuntimeFacts(
            String tenantId,
            String rbac3UserId,
            String sessionId,
            long authVersion,
            long contextVersion,
            long policyVersion,
            List<String> activeRoleIds) {
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
