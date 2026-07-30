package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.management.application.ManagementPolicyFacade;
import top.egon.cola.platform.rbac3.admin.runtime.application.IdempotencyService;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rbac3/v1")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "management-policy",
        name = "委托管理策略接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class ManagementPolicyController {

    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final ManagementPolicyFacade facade;
    private final IdempotencyService idempotencyService;
    private final DatabaseClock databaseClock;

    public ManagementPolicyController(
            ManagementPolicyFacade facade,
            IdempotencyService idempotencyService,
            DatabaseClock databaseClock
    ) {
        this.facade = facade;
        this.idempotencyService = idempotencyService;
        this.databaseClock = databaseClock;
    }

    @GetMapping("/management-policies")
    @RequiresRbac3Permission(permission = "system:management-policy:read")
    @GatewayOperation(
            name = "rbac3-management-policy-list-v1",
            summary = "查询完整委托管理策略",
            externalAccessible = true,
            tags = {"rbac3", "management-policy"})
    public ApiEnvelope<List<ManagementPolicyFacade.PolicyView>> policies() {
        return ApiEnvelope.success(facade.policies(tenantId()));
    }

    @GetMapping("/management-policies/{policyId}")
    @RequiresRbac3Permission(permission = "system:management-policy:read")
    @GatewayOperation(
            name = "rbac3-management-policy-get-v1",
            summary = "读取委托管理策略完整聚合",
            externalAccessible = true,
            tags = {"rbac3", "management-policy"})
    public ApiEnvelope<ManagementPolicyFacade.PolicyView> policy(
            @PathVariable String policyId
    ) {
        return ApiEnvelope.success(facade.policy(tenantId(), policyId));
    }

    @PostMapping("/management-policies")
    @RequiresRbac3Permission(permission = "system:management-policy:manage")
    @GatewayOperation(
            name = "rbac3-management-policy-create-v1",
            summary = "创建完整委托管理策略",
            externalAccessible = true,
            tags = {"rbac3", "management-policy"})
    public ApiEnvelope<ManagementPolicyFacade.PolicyView> create(
            @Valid @RequestBody PolicyRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return save(null, 0L, request, idempotencyKey, principal,
                "POST:/management-policies");
    }

    @PutMapping("/management-policies/{policyId}")
    @RequiresRbac3Permission(permission = "system:management-policy:manage")
    @GatewayOperation(
            name = "rbac3-management-policy-update-v1",
            summary = "按版本完整替换委托管理策略",
            externalAccessible = true,
            tags = {"rbac3", "management-policy"})
    public ApiEnvelope<ManagementPolicyFacade.PolicyView> update(
            @PathVariable String policyId,
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody PolicyRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return save(policyId, expectedVersion(ifMatch), request, idempotencyKey,
                principal, "PUT:/management-policies/{policyId}");
    }

    @PostMapping("/management-policies/{policyId}/disable")
    @RequiresRbac3Permission(permission = "system:management-policy:manage")
    @GatewayOperation(
            name = "rbac3-management-policy-disable-v1",
            summary = "禁用委托管理策略并保留历史明细",
            externalAccessible = true,
            tags = {"rbac3", "management-policy"})
    public ApiEnvelope<ManagementPolicyFacade.PolicyView> disable(
            @PathVariable String policyId,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        Instant now = databaseClock.transactionNow();
        IdempotencyService.Claim claim = claim(
                principal, "POST:/management-policies/{policyId}/disable",
                idempotencyKey, policyId + '|' + expectedVersion(ifMatch), now);
        if (claim.outcome() == IdempotencyService.Outcome.REPLAY) {
            return ApiEnvelope.success(facade.policy(tenantId(), claim.resourceId()));
        }
        ManagementPolicyFacade.PolicyView view = facade.disable(
                tenantId(), policyId, expectedVersion(ifMatch), principal.userId());
        complete(claim, view, now);
        return ApiEnvelope.success(view);
    }

    @GetMapping("/management-capabilities/me")
    @GatewayOperation(
            name = "rbac3-management-capabilities-mine-v1",
            summary = "查询当前操作者委托管理能力",
            externalAccessible = true,
            tags = {"rbac3", "management-policy", "capability"})
    public ApiEnvelope<ManagementPolicyFacade.CapabilityView> capabilities(
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return ApiEnvelope.success(facade.capabilities(
                tenantId(), principal.userId(), databaseClock.transactionNow()));
    }

    @GetMapping("/manageable-users")
    @GatewayOperation(
            name = "rbac3-manageable-user-search-v1",
            summary = "按委托范围搜索可管理用户",
            externalAccessible = true,
            tags = {"rbac3", "management-policy", "user"})
    public ApiEnvelope<List<ManagementPolicyFacade.ManagedUserView>> manageableUsers(
            @RequestParam(required = false) String query,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return ApiEnvelope.success(facade.manageableUsers(
                tenantId(), principal.userId(), query,
                databaseClock.transactionNow()));
    }

    @GetMapping("/manageable-roles")
    @GatewayOperation(
            name = "rbac3-manageable-role-search-v1",
            summary = "按委托白名单搜索可管理角色根",
            externalAccessible = true,
            tags = {"rbac3", "management-policy", "role"})
    public ApiEnvelope<List<ManagementPolicyFacade.ManagedRoleView>> manageableRoles(
            @RequestParam(required = false) String query,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return ApiEnvelope.success(facade.manageableRoles(
                tenantId(), principal.userId(), query,
                databaseClock.transactionNow()));
    }

    private ApiEnvelope<ManagementPolicyFacade.PolicyView> save(
            String policyId,
            long expectedVersion,
            PolicyRequest request,
            String idempotencyKey,
            CurrentRbac3Principal principal,
            String operation
    ) {
        Instant now = databaseClock.transactionNow();
        IdempotencyService.Claim claim = claim(
                principal, operation, idempotencyKey,
                canonical(policyId, expectedVersion, request), now);
        if (claim.outcome() == IdempotencyService.Outcome.REPLAY) {
            return ApiEnvelope.success(facade.policy(tenantId(), claim.resourceId()));
        }
        Restrictions requestRestrictions = request.restrictions();
        ManagementPolicyFacade.PolicyView view = facade.save(
                new ManagementPolicyFacade.SaveCommand(
                        tenantId(), policyId, request.policyCode(), request.name(),
                        request.validFrom(), request.validTo(),
                        new ManagementPolicyFacade.Restrictions(
                                requestRestrictions.maximumAssignmentDays(),
                                requestRestrictions.maximumRiskLevel(),
                                requestRestrictions.requiredAuthenticationStrength(),
                                requestRestrictions.requireReason(),
                                requestRestrictions.requireTicket(),
                                requestRestrictions.includeInheritedSubjectRoles(),
                                requestRestrictions.requireAllAffiliationsInScope()),
                        request.subjects().stream()
                                .map(subject -> new ManagementPolicyFacade.Subject(
                                        subject.type(), subject.id()))
                                .toList(),
                        request.scopes().stream()
                                .map(scope -> new ManagementPolicyFacade.Scope(
                                        scope.type(), scope.referenceId()))
                                .toList(),
                        request.activationRootRoleIds(), request.operations(),
                        expectedVersion, principal.userId()));
        complete(claim, view, now);
        return ApiEnvelope.success(view);
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

    private void complete(
            IdempotencyService.Claim claim,
            ManagementPolicyFacade.PolicyView view,
            Instant now
    ) {
        idempotencyService.complete(
                claim.recordId(), "MANAGEMENT_POLICY", view.policyId(), 200,
                view.policyId() + '|' + view.version() + '|' + view.status(), now);
    }

    private static String canonical(
            String policyId,
            long expectedVersion,
            PolicyRequest request
    ) {
        String subjects = request.subjects().stream()
                .map(subject -> subject.type() + ':' + subject.id())
                .sorted().collect(Collectors.joining(","));
        String scopes = request.scopes().stream()
                .map(scope -> scope.type() + ':' + scope.referenceId())
                .sorted().collect(Collectors.joining(","));
        String roles = request.activationRootRoleIds().stream()
                .sorted().collect(Collectors.joining(","));
        String operations = request.operations().stream()
                .sorted().collect(Collectors.joining(","));
        return List.of(
                String.valueOf(policyId), Long.toString(expectedVersion),
                request.policyCode(), request.name(), request.validFrom().toString(),
                String.valueOf(request.validTo()), request.restrictions().toString(),
                subjects, scopes, roles, operations).toString();
    }

    private static long expectedVersion(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new IllegalArgumentException("If-Match is required");
        }
        String value = ifMatch.trim();
        if (value.startsWith("W/")) {
            value = value.substring(2);
        }
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        try {
            long version = Long.parseLong(value);
            if (version < 0L) {
                throw new NumberFormatException("negative version");
            }
            return version;
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("If-Match must contain a non-negative version");
        }
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

    public record PolicyRequest(
            @NotBlank String policyCode,
            @NotBlank String name,
            @NotNull Instant validFrom,
            Instant validTo,
            @NotEmpty List<@Valid Subject> subjects,
            @NotEmpty List<@Valid Scope> scopes,
            @NotEmpty List<@NotBlank String> activationRootRoleIds,
            @NotEmpty Set<@NotBlank String> operations,
            @NotNull @Valid Restrictions restrictions
    ) {
        public PolicyRequest {
            subjects = List.copyOf(subjects);
            scopes = List.copyOf(scopes);
            activationRootRoleIds = activationRootRoleIds.stream()
                    .sorted(Comparator.naturalOrder()).toList();
            operations = Set.copyOf(operations);
        }
    }

    public record Subject(@NotBlank String type, @NotBlank String id) {
    }

    public record Scope(@NotBlank String type, String referenceId) {
    }

    public record Restrictions(
            Integer maximumAssignmentDays,
            @NotBlank String maximumRiskLevel,
            @NotBlank String requiredAuthenticationStrength,
            boolean requireReason,
            boolean requireTicket,
            boolean includeInheritedSubjectRoles,
            boolean requireAllAffiliationsInScope
    ) {
    }
}
