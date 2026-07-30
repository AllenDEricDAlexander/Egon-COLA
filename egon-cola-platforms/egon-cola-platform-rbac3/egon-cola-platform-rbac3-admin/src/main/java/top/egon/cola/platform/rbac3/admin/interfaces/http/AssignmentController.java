package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.assignment.application.AssignmentFacade;
import top.egon.cola.platform.rbac3.admin.runtime.application.IdempotencyService;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/rbac3/v1/users/{userId}/role-assignments")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "role-assignment",
        name = "角色任职接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class AssignmentController {

    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final AssignmentFacade facade;
    private final IdempotencyService idempotencyService;
    private final SessionStrengthPort sessionStrengthPort;
    private final DatabaseClock databaseClock;

    public AssignmentController(
            AssignmentFacade facade,
            IdempotencyService idempotencyService,
            SessionStrengthPort sessionStrengthPort,
            DatabaseClock databaseClock
    ) {
        this.facade = facade;
        this.idempotencyService = idempotencyService;
        this.sessionStrengthPort = sessionStrengthPort;
        this.databaseClock = databaseClock;
    }

    @GetMapping
    @GatewayOperation(
            name = "rbac3-assignment-list-v1",
            summary = "查询用户角色任职及历史状态",
            externalAccessible = true,
            tags = {"rbac3", "assignment"})
    public ApiEnvelope<List<AssignmentFacade.AssignmentView>> assignments(
            @PathVariable String userId,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        if (!principal.userId().equals(userId)
                && !principal.hasPermission("system:role-assignment:read")) {
            throw new Rbac3RuleViolation("PERMISSION_DENIED");
        }
        return ApiEnvelope.success(facade.assignments(
                tenantId(), userId, databaseClock.transactionNow()));
    }

    @PostMapping
    @RequiresRbac3Permission(permission = "system:role-assignment:manage")
    @GatewayOperation(
            name = "rbac3-assignment-create-v1",
            summary = "按完整委托策略创建角色任职",
            externalAccessible = true,
            tags = {"rbac3", "assignment"})
    public ApiEnvelope<AssignmentFacade.AssignmentResult> assign(
            @PathVariable String userId,
            @Valid @RequestBody AssignRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        Instant now = databaseClock.transactionNow();
        String operation = "POST:/users/{userId}/role-assignments";
        IdempotencyService.Claim claim = claim(
                principal, operation, idempotencyKey,
                userId + '|' + request, now);
        if (claim.outcome() == IdempotencyService.Outcome.REPLAY) {
            return ApiEnvelope.success(new AssignmentFacade.AssignmentResult(
                    claim.resourceId(), null, true, "IDEMPOTENT_REPLAY", null));
        }
        AssignmentFacade.AssignmentResult result = facade.assign(
                new AssignmentFacade.AssignRequest(
                        tenantId(), principal.userId(), userId, request.roleId(),
                        request.assignmentType(), request.validFrom(), request.validTo(),
                        request.reason(), request.ticketNo(),
                        sessionStrengthPort.authenticationStrength(
                                tenantId(), principal.sessionId()),
                        principal.platformAdministrator(),
                        request.expectedUserAuthVersion(), claim.recordId(), now));
        return complete(claim, result, now);
    }

    @PostMapping("/{assignmentId}/revoke")
    @RequiresRbac3Permission(permission = "system:role-assignment:manage")
    @GatewayOperation(
            name = "rbac3-assignment-revoke-v1",
            summary = "撤销角色任职并保留历史",
            externalAccessible = true,
            tags = {"rbac3", "assignment"})
    public ApiEnvelope<AssignmentFacade.AssignmentResult> revoke(
            @PathVariable String userId,
            @PathVariable String assignmentId,
            @Valid @RequestBody ChangeRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return change(userId, assignmentId, AssignmentFacade.ChangeOperation.REVOKE,
                request, idempotencyKey, principal);
    }

    @PostMapping("/{assignmentId}/suspend")
    @RequiresRbac3Permission(permission = "system:role-assignment:manage")
    @GatewayOperation(
            name = "rbac3-assignment-suspend-v1",
            summary = "暂停角色任职",
            externalAccessible = true,
            tags = {"rbac3", "assignment"})
    public ApiEnvelope<AssignmentFacade.AssignmentResult> suspend(
            @PathVariable String userId,
            @PathVariable String assignmentId,
            @Valid @RequestBody ChangeRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return change(userId, assignmentId, AssignmentFacade.ChangeOperation.SUSPEND,
                request, idempotencyKey, principal);
    }

    @PostMapping("/{assignmentId}/resume")
    @RequiresRbac3Permission(permission = "system:role-assignment:manage")
    @GatewayOperation(
            name = "rbac3-assignment-resume-v1",
            summary = "恢复角色任职",
            externalAccessible = true,
            tags = {"rbac3", "assignment"})
    public ApiEnvelope<AssignmentFacade.AssignmentResult> resume(
            @PathVariable String userId,
            @PathVariable String assignmentId,
            @Valid @RequestBody ChangeRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return change(userId, assignmentId, AssignmentFacade.ChangeOperation.RESUME,
                request, idempotencyKey, principal);
    }

    private ApiEnvelope<AssignmentFacade.AssignmentResult> change(
            String userId,
            String assignmentId,
            AssignmentFacade.ChangeOperation operation,
            ChangeRequest request,
            String idempotencyKey,
            CurrentRbac3Principal principal
    ) {
        Instant now = databaseClock.transactionNow();
        String operationCode = "POST:/users/{userId}/role-assignments/{assignmentId}/"
                + operation.name().toLowerCase(java.util.Locale.ROOT);
        IdempotencyService.Claim claim = claim(
                principal, operationCode, idempotencyKey,
                userId + '|' + assignmentId + '|' + operation + '|' + request, now);
        if (claim.outcome() == IdempotencyService.Outcome.REPLAY) {
            return ApiEnvelope.success(new AssignmentFacade.AssignmentResult(
                    claim.resourceId(), null, true, "IDEMPOTENT_REPLAY", null));
        }
        AssignmentFacade.AssignmentResult result = facade.change(
                new AssignmentFacade.ChangeRequest(
                        tenantId(), principal.userId(), userId, assignmentId, operation,
                        request.reason(), request.ticketNo(),
                        sessionStrengthPort.authenticationStrength(
                                tenantId(), principal.sessionId()),
                        principal.platformAdministrator(),
                        request.expectedAssignmentVersion(),
                        request.expectedUserAuthVersion(), claim.recordId(), now));
        return complete(claim, result, now);
    }

    private IdempotencyService.Claim claim(
            CurrentRbac3Principal principal,
            String operation,
            String idempotencyKey,
            String canonicalRequest,
            Instant now
    ) {
        requireIdempotencyKey(idempotencyKey);
        return idempotencyService.claim(new IdempotencyService.Command(
                tenantId(), "USER", principal.userId(), operation,
                idempotencyKey, canonicalRequest, now.plus(IDEMPOTENCY_TTL), now));
    }

    private ApiEnvelope<AssignmentFacade.AssignmentResult> complete(
            IdempotencyService.Claim claim,
            AssignmentFacade.AssignmentResult result,
            Instant now
    ) {
        int status = result.completed() ? 200 : 503;
        idempotencyService.complete(
                claim.recordId(), "ROLE_ASSIGNMENT", result.assignmentId(), status,
                result.assignmentId() + '|' + result.reasonCode(), now);
        if (!result.completed()) {
            throw new Rbac3RuleViolation(
                    "AUTH_PROPAGATION_PENDING", List.of(result.mutationId()));
        }
        return ApiEnvelope.success(result);
    }

    private static void requireIdempotencyKey(String value) {
        if (value == null || value.isBlank() || value.length() > 128
                || !value.chars().allMatch(character -> character >= 0x21
                && character <= 0x7e)) {
            throw new IllegalArgumentException("Idempotency-Key must be 1-128 ASCII characters");
        }
    }

    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }

    @FunctionalInterface
    public interface SessionStrengthPort {
        String authenticationStrength(String tenantId, String sessionId);
    }

    public record AssignRequest(
            @NotBlank String roleId,
            @NotNull Instant validFrom,
            Instant validTo,
            @NotBlank String assignmentType,
            String reason,
            String ticketNo,
            @PositiveOrZero long expectedUserAuthVersion
    ) {
    }

    public record ChangeRequest(
            String reason,
            String ticketNo,
            @PositiveOrZero long expectedAssignmentVersion,
            @PositiveOrZero long expectedUserAuthVersion
    ) {
    }
}
