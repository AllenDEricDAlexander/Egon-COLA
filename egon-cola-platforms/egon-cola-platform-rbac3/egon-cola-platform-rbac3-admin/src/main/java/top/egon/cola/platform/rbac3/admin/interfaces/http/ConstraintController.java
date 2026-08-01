package top.egon.cola.platform.rbac3.admin.interfaces.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.constraint.application.ConstraintFacade;
import top.egon.cola.platform.rbac3.admin.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/rbac3/v1")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "authorization-constraint",
        name = "授权约束接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class ConstraintController {

    private final ConstraintFacade facade;

    public ConstraintController(ConstraintFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/sod-sets")
    @RequiresRbac3Permission(permission = "system:authorization-constraint:read")
    @GatewayOperation(name = "rbac3-sod-set-list-v1", summary = "查询SSD和DSD集合",
            externalAccessible = true, tags = {"rbac3", "constraint"})
    public ApiEnvelope<List<ConstraintFacade.SodView>> sodSets() {
        return ApiEnvelope.success(facade.sodSets(tenantId()));
    }

    @PostMapping("/sod-sets")
    @RequiresRbac3Permission(permission = "system:authorization-constraint:manage")
    @GatewayOperation(name = "rbac3-sod-set-create-v1", summary = "创建SSD或DSD集合",
            externalAccessible = true, tags = {"rbac3", "constraint"})
    public ApiEnvelope<ConstraintFacade.MutationResult> createSodSet(
            @Valid @RequestBody SodSetRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(facade.saveSod(sodCommand(null, request, principal)));
    }

    @PutMapping("/sod-sets/{setId}")
    @RequiresRbac3Permission(permission = "system:authorization-constraint:manage")
    @GatewayOperation(name = "rbac3-sod-set-update-v1", summary = "更新SSD或DSD集合",
            externalAccessible = true, tags = {"rbac3", "constraint"})
    public ApiEnvelope<ConstraintFacade.MutationResult> updateSodSet(
            @PathVariable String setId,
            @Valid @RequestBody SodSetRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(facade.saveSod(sodCommand(setId, request, principal)));
    }

    @PostMapping("/roles/{roleId}/prerequisite-groups")
    @RequiresRbac3Permission(permission = "system:authorization-constraint:manage")
    @GatewayOperation(name = "rbac3-role-prerequisite-save-v1",
            summary = "替换角色前置条件组", externalAccessible = true,
            tags = {"rbac3", "constraint"})
    public ApiEnvelope<ConstraintFacade.MutationResult> prerequisites(
            @PathVariable String roleId,
            @Valid @RequestBody PrerequisiteGroupRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(facade.savePrerequisites(
                new ConstraintFacade.PrerequisiteGroupCommand(
                        tenantId(),
                        roleId,
                        request.groupCode(),
                        request.matchMode(),
                        request.prerequisiteRoleIds(),
                        request.expectedRoleVersion(),
                        principal.userId())));
    }

    @PutMapping("/roles/{roleId}/cardinality")
    @RequiresRbac3Permission(permission = "system:authorization-constraint:manage")
    @GatewayOperation(name = "rbac3-role-cardinality-save-v1",
            summary = "配置角色容量", externalAccessible = true,
            tags = {"rbac3", "constraint"})
    public ApiEnvelope<ConstraintFacade.MutationResult> cardinality(
            @PathVariable String roleId,
            @Valid @RequestBody CardinalityRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(facade.saveCardinality(
                new ConstraintFacade.CardinalityCommand(
                        tenantId(),
                        roleId,
                        request.scopeType(),
                        request.maximumActive(),
                        request.validFrom(),
                        request.validTo(),
                        request.expectedVersion(),
                        principal.userId())));
    }

    @GetMapping("/data-rules")
    @RequiresRbac3Permission(permission = "system:data-rule:read")
    @GatewayOperation(name = "rbac3-data-rule-list-v1", summary = "查询数据规则",
            externalAccessible = true, tags = {"rbac3", "data-rule"})
    public ApiEnvelope<List<ConstraintFacade.DataRuleView>> dataRules() {
        return ApiEnvelope.success(facade.dataRules(tenantId()));
    }

    @PostMapping("/data-rules")
    @RequiresRbac3Permission(permission = "system:data-rule:manage")
    @GatewayOperation(name = "rbac3-data-rule-create-v1", summary = "创建类型化数据规则",
            externalAccessible = true, tags = {"rbac3", "data-rule"})
    public ApiEnvelope<ConstraintFacade.MutationResult> createDataRule(
            @Valid @RequestBody DataRuleRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(facade.saveDataRule(
                dataRuleCommand(null, request, principal)));
    }

    @PutMapping("/data-rules/{ruleId}")
    @RequiresRbac3Permission(permission = "system:data-rule:manage")
    @GatewayOperation(name = "rbac3-data-rule-update-v1", summary = "更新类型化数据规则",
            externalAccessible = true, tags = {"rbac3", "data-rule"})
    public ApiEnvelope<ConstraintFacade.MutationResult> updateDataRule(
            @PathVariable String ruleId,
            @Valid @RequestBody DataRuleRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(facade.saveDataRule(
                dataRuleCommand(ruleId, request, principal)));
    }

    @GetMapping("/field-rules")
    @RequiresRbac3Permission(permission = "system:field-rule:read")
    @GatewayOperation(name = "rbac3-field-rule-list-v1", summary = "查询字段规则",
            externalAccessible = true, tags = {"rbac3", "field-rule"})
    public ApiEnvelope<List<ConstraintFacade.FieldRuleView>> fieldRules() {
        return ApiEnvelope.success(facade.fieldRules(tenantId()));
    }

    @PostMapping("/field-rules")
    @RequiresRbac3Permission(permission = "system:field-rule:manage")
    @GatewayOperation(name = "rbac3-field-rule-create-v1", summary = "创建字段访问规则",
            externalAccessible = true, tags = {"rbac3", "field-rule"})
    public ApiEnvelope<ConstraintFacade.MutationResult> createFieldRule(
            @Valid @RequestBody FieldRuleRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(facade.saveFieldRule(
                fieldRuleCommand(null, request, principal)));
    }

    @PutMapping("/field-rules/{ruleId}")
    @RequiresRbac3Permission(permission = "system:field-rule:manage")
    @GatewayOperation(name = "rbac3-field-rule-update-v1", summary = "更新字段访问规则",
            externalAccessible = true, tags = {"rbac3", "field-rule"})
    public ApiEnvelope<ConstraintFacade.MutationResult> updateFieldRule(
            @PathVariable String ruleId,
            @Valid @RequestBody FieldRuleRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(facade.saveFieldRule(
                fieldRuleCommand(ruleId, request, principal)));
    }

    @GetMapping("/operation-sod-rules")
    @RequiresRbac3Permission(permission = "system:operation-sod:read")
    @GatewayOperation(name = "rbac3-operation-sod-list-v1", summary = "查询同对象职责分离规则",
            externalAccessible = true, tags = {"rbac3", "operation-sod"})
    public ApiEnvelope<List<ConstraintFacade.OperationSodRuleView>> operationSodRules() {
        return ApiEnvelope.success(facade.operationSodRules(tenantId()));
    }

    @PostMapping("/operation-sod-rules")
    @RequiresRbac3Permission(permission = "system:operation-sod:manage")
    @GatewayOperation(name = "rbac3-operation-sod-create-v1",
            summary = "创建同对象职责分离规则", externalAccessible = true,
            tags = {"rbac3", "operation-sod"})
    public ApiEnvelope<ConstraintFacade.MutationResult> createOperationSodRule(
            @Valid @RequestBody OperationSodRuleRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(facade.saveOperationSodRule(
                operationSodCommand(null, request, principal)));
    }

    @PutMapping("/operation-sod-rules/{ruleId}")
    @RequiresRbac3Permission(permission = "system:operation-sod:manage")
    @GatewayOperation(name = "rbac3-operation-sod-update-v1",
            summary = "更新同对象职责分离规则", externalAccessible = true,
            tags = {"rbac3", "operation-sod"})
    public ApiEnvelope<ConstraintFacade.MutationResult> updateOperationSodRule(
            @PathVariable String ruleId,
            @Valid @RequestBody OperationSodRuleRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(facade.saveOperationSodRule(
                operationSodCommand(ruleId, request, principal)));
    }

    private static ConstraintFacade.SaveSodCommand sodCommand(
            String setId,
            SodSetRequest request,
            CurrentRbac3Principal principal) {
        return new ConstraintFacade.SaveSodCommand(
                tenantId(), setId, request.setCode(),
                ConstraintFacade.ConstraintType.valueOf(request.constraintType()),
                request.applicationId(), request.maximumActiveRoles(), request.memberRoleIds(),
                request.validFrom(), request.validTo(), request.expectedVersion(), principal.userId());
    }

    private static ConstraintFacade.DataRuleCommand dataRuleCommand(
            String ruleId,
            DataRuleRequest request,
            CurrentRbac3Principal principal) {
        return new ConstraintFacade.DataRuleCommand(
                tenantId(), ruleId, request.applicationId(), request.roleId(),
                request.permissionId(), request.scopeType(), request.directorySnapshotVersion(),
                request.references(), request.validFrom(), request.validTo(),
                request.expectedVersion(), principal.userId());
    }

    private static ConstraintFacade.FieldRuleCommand fieldRuleCommand(
            String ruleId,
            FieldRuleRequest request,
            CurrentRbac3Principal principal) {
        return new ConstraintFacade.FieldRuleCommand(
                tenantId(), ruleId, request.applicationId(), request.roleId(),
                request.permissionId(), request.fieldDefinitionId(), request.accessLevel(),
                request.validFrom(), request.validTo(), request.expectedVersion(), principal.userId());
    }

    private static ConstraintFacade.OperationSodRuleCommand operationSodCommand(
            String ruleId,
            OperationSodRuleRequest request,
            CurrentRbac3Principal principal) {
        return new ConstraintFacade.OperationSodRuleCommand(
                tenantId(), ruleId, request.applicationCode(), request.businessResource(),
                request.priorActionCode(), request.forbiddenLaterActionCode(), request.lookbackFrom(),
                request.validFrom(), request.validTo(), request.expectedVersion(), principal.userId());
    }

    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }

    public record SodSetRequest(
            @NotBlank String setCode,
            @NotBlank String constraintType,
            String applicationId,
            @Positive int maximumActiveRoles,
            @NotEmpty List<@NotBlank String> memberRoleIds,
            @NotNull Instant validFrom,
            Instant validTo,
            @PositiveOrZero long expectedVersion) {
    }

    public record PrerequisiteGroupRequest(
            @NotBlank String groupCode,
            @NotBlank String matchMode,
            @NotEmpty List<@NotBlank String> prerequisiteRoleIds,
            @PositiveOrZero long expectedRoleVersion) {
    }

    public record CardinalityRequest(
            @NotBlank String scopeType,
            @Positive int maximumActive,
            @NotNull Instant validFrom,
            Instant validTo,
            @PositiveOrZero long expectedVersion) {
    }

    public record DataRuleRequest(
            @NotBlank String applicationId,
            @NotBlank String roleId,
            @NotBlank String permissionId,
            @NotBlank String scopeType,
            Long directorySnapshotVersion,
            @NotNull List<ConstraintFacade.RuleReference> references,
            @NotNull Instant validFrom,
            Instant validTo,
            @PositiveOrZero long expectedVersion) {
    }

    public record FieldRuleRequest(
            @NotBlank String applicationId,
            @NotBlank String roleId,
            @NotBlank String permissionId,
            @NotBlank String fieldDefinitionId,
            @NotBlank String accessLevel,
            @NotNull Instant validFrom,
            Instant validTo,
            @PositiveOrZero long expectedVersion) {
    }

    public record OperationSodRuleRequest(
            @NotBlank String applicationCode,
            @NotBlank String businessResource,
            @NotBlank String priorActionCode,
            @NotBlank String forbiddenLaterActionCode,
            Instant lookbackFrom,
            @NotNull Instant validFrom,
            Instant validTo,
            @PositiveOrZero long expectedVersion) {
    }
}
