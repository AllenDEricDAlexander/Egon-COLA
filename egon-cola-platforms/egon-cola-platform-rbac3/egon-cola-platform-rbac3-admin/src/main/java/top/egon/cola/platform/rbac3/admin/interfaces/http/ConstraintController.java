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

/**
 * 类型 `ConstraintController` 位于当前包内，是类型，用于承载 `Constraint Controller` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ConstraintController` is a type in its package and carries the responsibility, state, or contract for `Constraint Controller`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ConstraintController` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ConstraintController` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
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

    /**
     * 字段 `facade` 表示 `ConstraintController` 中与 `facade` 相关的状态、依赖、配置或结果（声明类型 `ConstraintFacade`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `facade` stores the `facade`-related state, dependency, configuration, or result of `ConstraintController` (declared type `ConstraintFacade`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `facade` 时应保持 `ConstraintController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `facade`, preserve `ConstraintController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ConstraintFacade facade;

    /**
     * 构造器 `ConstraintController` 用于创建并初始化 `ConstraintController` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ConstraintController` creates and initializes `ConstraintController`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ConstraintController` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ConstraintController`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param facade 输入参数 `facade`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ConstraintController(ConstraintFacade facade) {
        this.facade = facade;
    }

    /**
     * 方法 `sodSets` 按照 `ConstraintController` 的职责处理输入，完成 `sod Sets` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `sodSets` processes its inputs according to `ConstraintController`'s responsibility, performs the `sod Sets` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `sodSets` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `sodSets`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/sod-sets")
    @RequiresRbac3Permission(permission = "system:authorization-constraint:read")
    @GatewayOperation(name = "rbac3-sod-set-list-v1", summary = "查询SSD和DSD集合",
            externalAccessible = true, tags = {"rbac3", "constraint"})
    public ApiEnvelope<List<ConstraintFacade.SodView>> sodSets() {
        return ApiEnvelope.success(facade.sodSets(tenantId()));
    }

    /**
     * 方法 `createSodSet` 按照 `ConstraintController` 的职责处理输入，完成 `create Sod Set` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `createSodSet` processes its inputs according to `ConstraintController`'s responsibility, performs the `create Sod Set` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `createSodSet` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `createSodSet`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/sod-sets")
    @RequiresRbac3Permission(permission = "system:authorization-constraint:manage")
    @GatewayOperation(name = "rbac3-sod-set-create-v1", summary = "创建SSD或DSD集合",
            externalAccessible = true, tags = {"rbac3", "constraint"})
    public ApiEnvelope<ConstraintFacade.MutationResult> createSodSet(
            @Valid @RequestBody SodSetRequest request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelope.success(facade.saveSod(sodCommand(null, request, principal)));
    }

    /**
     * 方法 `updateSodSet` 按照 `ConstraintController` 的职责处理输入，完成 `update Sod Set` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `updateSodSet` processes its inputs according to `ConstraintController`'s responsibility, performs the `update Sod Set` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `updateSodSet` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `updateSodSet`, then continue the business flow using its result, exception, or side effect.
     *
     * @param setId 输入参数 `setId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `prerequisites` 按照 `ConstraintController` 的职责处理输入，完成 `prerequisites` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `prerequisites` processes its inputs according to `ConstraintController`'s responsibility, performs the `prerequisites` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `prerequisites` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `prerequisites`, then continue the business flow using its result, exception, or side effect.
     *
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `cardinality` 按照 `ConstraintController` 的职责处理输入，完成 `cardinality` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `cardinality` processes its inputs according to `ConstraintController`'s responsibility, performs the `cardinality` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `cardinality` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `cardinality`, then continue the business flow using its result, exception, or side effect.
     *
     * @param roleId 输入参数 `roleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `dataRules` 按照 `ConstraintController` 的职责处理输入，完成 `data Rules` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `dataRules` processes its inputs according to `ConstraintController`'s responsibility, performs the `data Rules` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `dataRules` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `dataRules`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/data-rules")
    @RequiresRbac3Permission(permission = "system:data-rule:read")
    @GatewayOperation(name = "rbac3-data-rule-list-v1", summary = "查询数据规则",
            externalAccessible = true, tags = {"rbac3", "data-rule"})
    public ApiEnvelope<List<ConstraintFacade.DataRuleView>> dataRules() {
        return ApiEnvelope.success(facade.dataRules(tenantId()));
    }

    /**
     * 方法 `createDataRule` 按照 `ConstraintController` 的职责处理输入，完成 `create Data Rule` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `createDataRule` processes its inputs according to `ConstraintController`'s responsibility, performs the `create Data Rule` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `createDataRule` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `createDataRule`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `updateDataRule` 按照 `ConstraintController` 的职责处理输入，完成 `update Data Rule` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `updateDataRule` processes its inputs according to `ConstraintController`'s responsibility, performs the `update Data Rule` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `updateDataRule` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `updateDataRule`, then continue the business flow using its result, exception, or side effect.
     *
     * @param ruleId 输入参数 `ruleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `fieldRules` 按照 `ConstraintController` 的职责处理输入，完成 `field Rules` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `fieldRules` processes its inputs according to `ConstraintController`'s responsibility, performs the `field Rules` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `fieldRules` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `fieldRules`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/field-rules")
    @RequiresRbac3Permission(permission = "system:field-rule:read")
    @GatewayOperation(name = "rbac3-field-rule-list-v1", summary = "查询字段规则",
            externalAccessible = true, tags = {"rbac3", "field-rule"})
    public ApiEnvelope<List<ConstraintFacade.FieldRuleView>> fieldRules() {
        return ApiEnvelope.success(facade.fieldRules(tenantId()));
    }

    /**
     * 方法 `createFieldRule` 按照 `ConstraintController` 的职责处理输入，完成 `create Field Rule` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `createFieldRule` processes its inputs according to `ConstraintController`'s responsibility, performs the `create Field Rule` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `createFieldRule` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `createFieldRule`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `updateFieldRule` 按照 `ConstraintController` 的职责处理输入，完成 `update Field Rule` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `updateFieldRule` processes its inputs according to `ConstraintController`'s responsibility, performs the `update Field Rule` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `updateFieldRule` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `updateFieldRule`, then continue the business flow using its result, exception, or side effect.
     *
     * @param ruleId 输入参数 `ruleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `operationSodRules` 按照 `ConstraintController` 的职责处理输入，完成 `operation Sod Rules` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `operationSodRules` processes its inputs according to `ConstraintController`'s responsibility, performs the `operation Sod Rules` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `operationSodRules` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `operationSodRules`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/operation-sod-rules")
    @RequiresRbac3Permission(permission = "system:operation-sod:read")
    @GatewayOperation(name = "rbac3-operation-sod-list-v1", summary = "查询同对象职责分离规则",
            externalAccessible = true, tags = {"rbac3", "operation-sod"})
    public ApiEnvelope<List<ConstraintFacade.OperationSodRuleView>> operationSodRules() {
        return ApiEnvelope.success(facade.operationSodRules(tenantId()));
    }

    /**
     * 方法 `createOperationSodRule` 按照 `ConstraintController` 的职责处理输入，完成 `create Operation Sod Rule` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `createOperationSodRule` processes its inputs according to `ConstraintController`'s responsibility, performs the `create Operation Sod Rule` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `createOperationSodRule` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `createOperationSodRule`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `updateOperationSodRule` 按照 `ConstraintController` 的职责处理输入，完成 `update Operation Sod Rule` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `updateOperationSodRule` processes its inputs according to `ConstraintController`'s responsibility, performs the `update Operation Sod Rule` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `updateOperationSodRule` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `updateOperationSodRule`, then continue the business flow using its result, exception, or side effect.
     *
     * @param ruleId 输入参数 `ruleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `sodCommand` 按照 `ConstraintController` 的职责处理输入，完成 `sod Command` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `sodCommand` processes its inputs according to `ConstraintController`'s responsibility, performs the `sod Command` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `sodCommand` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `sodCommand`, then continue the business flow using its result, exception, or side effect.
     *
     * @param setId 输入参数 `setId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `dataRuleCommand` 按照 `ConstraintController` 的职责处理输入，完成 `data Rule Command` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `dataRuleCommand` processes its inputs according to `ConstraintController`'s responsibility, performs the `data Rule Command` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `dataRuleCommand` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `dataRuleCommand`, then continue the business flow using its result, exception, or side effect.
     *
     * @param ruleId 输入参数 `ruleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `fieldRuleCommand` 按照 `ConstraintController` 的职责处理输入，完成 `field Rule Command` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `fieldRuleCommand` processes its inputs according to `ConstraintController`'s responsibility, performs the `field Rule Command` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `fieldRuleCommand` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `fieldRuleCommand`, then continue the business flow using its result, exception, or side effect.
     *
     * @param ruleId 输入参数 `ruleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static ConstraintFacade.FieldRuleCommand fieldRuleCommand(
            String ruleId,
            FieldRuleRequest request,
            CurrentRbac3Principal principal) {
        return new ConstraintFacade.FieldRuleCommand(
                tenantId(), ruleId, request.applicationId(), request.roleId(),
                request.permissionId(), request.fieldDefinitionId(), request.accessLevel(),
                request.validFrom(), request.validTo(), request.expectedVersion(), principal.userId());
    }

    /**
     * 方法 `operationSodCommand` 按照 `ConstraintController` 的职责处理输入，完成 `operation Sod Command` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `operationSodCommand` processes its inputs according to `ConstraintController`'s responsibility, performs the `operation Sod Command` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `operationSodCommand` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `operationSodCommand`, then continue the business flow using its result, exception, or side effect.
     *
     * @param ruleId 输入参数 `ruleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static ConstraintFacade.OperationSodRuleCommand operationSodCommand(
            String ruleId,
            OperationSodRuleRequest request,
            CurrentRbac3Principal principal) {
        return new ConstraintFacade.OperationSodRuleCommand(
                tenantId(), ruleId, request.applicationCode(), request.businessResource(),
                request.priorActionCode(), request.forbiddenLaterActionCode(), request.lookbackFrom(),
                request.validFrom(), request.validTo(), request.expectedVersion(), principal.userId());
    }

    /**
     * 方法 `tenantId` 按照 `ConstraintController` 的职责处理输入，完成 `tenant Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantId` processes its inputs according to `ConstraintController`'s responsibility, performs the `tenant Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenantId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenantId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }

    /**
     * 类型 `SodSetRequest` 位于 `ConstraintController` 内，是记录类型，用于承载 `Sod Set Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SodSetRequest` is a record inside `ConstraintController` and carries the responsibility, state, or contract for `Sod Set Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SodSetRequest` 作为 `ConstraintController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SodSetRequest` as the responsibility boundary of `ConstraintController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param setCode 记录组件 `setCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `setCode` carries constructor data whose meaning is defined by the record contract.
     * @param constraintType 记录组件 `constraintType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `constraintType` carries constructor data whose meaning is defined by the record contract.
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param maximumActiveRoles 记录组件 `maximumActiveRoles` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumActiveRoles` carries constructor data whose meaning is defined by the record contract.
     * @param memberRoleIds 记录组件 `memberRoleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `memberRoleIds` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param expectedVersion 记录组件 `expectedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record SodSetRequest(
            /**
             * 字段 `setCode` 表示 `SodSetRequest` 中与 `set Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `setCode` stores the `set Code`-related state, dependency, configuration, or result of `SodSetRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `setCode` 时应保持 `SodSetRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `setCode`, preserve `SodSetRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String setCode,
            /**
             * 字段 `constraintType` 表示 `SodSetRequest` 中与 `constraint Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `constraintType` stores the `constraint Type`-related state, dependency, configuration, or result of `SodSetRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `constraintType` 时应保持 `SodSetRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `constraintType`, preserve `SodSetRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String constraintType,
            /**
             * 字段 `applicationId` 表示 `SodSetRequest` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `SodSetRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `SodSetRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `SodSetRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationId,
            /**
             * 字段 `maximumActiveRoles` 表示 `SodSetRequest` 中与 `maximum Active Roles` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumActiveRoles` stores the `maximum Active Roles`-related state, dependency, configuration, or result of `SodSetRequest` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumActiveRoles` 时应保持 `SodSetRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumActiveRoles`, preserve `SodSetRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @Positive int maximumActiveRoles,
            /**
             * 字段 `memberRoleIds` 表示 `SodSetRequest` 中与 `member Role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;@NotBlank String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `memberRoleIds` stores the `member Role Ids`-related state, dependency, configuration, or result of `SodSetRequest` (declared type `List&lt;@NotBlank String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `memberRoleIds` 时应保持 `SodSetRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `memberRoleIds`, preserve `SodSetRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotEmpty List<@NotBlank String> memberRoleIds,
            /**
             * 字段 `validFrom` 表示 `SodSetRequest` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `SodSetRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `SodSetRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `SodSetRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull Instant validFrom,
            /**
             * 字段 `validTo` 表示 `SodSetRequest` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `SodSetRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `SodSetRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `SodSetRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `expectedVersion` 表示 `SodSetRequest` 中与 `expected Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedVersion` stores the `expected Version`-related state, dependency, configuration, or result of `SodSetRequest` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedVersion` 时应保持 `SodSetRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedVersion`, preserve `SodSetRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedVersion) {
    }

    /**
     * 类型 `PrerequisiteGroupRequest` 位于 `ConstraintController` 内，是记录类型，用于承载 `Prerequisite Group Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `PrerequisiteGroupRequest` is a record inside `ConstraintController` and carries the responsibility, state, or contract for `Prerequisite Group Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `PrerequisiteGroupRequest` 作为 `ConstraintController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `PrerequisiteGroupRequest` as the responsibility boundary of `ConstraintController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param groupCode 记录组件 `groupCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `groupCode` carries constructor data whose meaning is defined by the record contract.
     * @param matchMode 记录组件 `matchMode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `matchMode` carries constructor data whose meaning is defined by the record contract.
     * @param prerequisiteRoleIds 记录组件 `prerequisiteRoleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `prerequisiteRoleIds` carries constructor data whose meaning is defined by the record contract.
     * @param expectedRoleVersion 记录组件 `expectedRoleVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedRoleVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record PrerequisiteGroupRequest(
            /**
             * 字段 `groupCode` 表示 `PrerequisiteGroupRequest` 中与 `group Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `groupCode` stores the `group Code`-related state, dependency, configuration, or result of `PrerequisiteGroupRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `groupCode` 时应保持 `PrerequisiteGroupRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `groupCode`, preserve `PrerequisiteGroupRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String groupCode,
            /**
             * 字段 `matchMode` 表示 `PrerequisiteGroupRequest` 中与 `match Mode` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `matchMode` stores the `match Mode`-related state, dependency, configuration, or result of `PrerequisiteGroupRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `matchMode` 时应保持 `PrerequisiteGroupRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `matchMode`, preserve `PrerequisiteGroupRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String matchMode,
            /**
             * 字段 `prerequisiteRoleIds` 表示 `PrerequisiteGroupRequest` 中与 `prerequisite Role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;@NotBlank String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `prerequisiteRoleIds` stores the `prerequisite Role Ids`-related state, dependency, configuration, or result of `PrerequisiteGroupRequest` (declared type `List&lt;@NotBlank String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `prerequisiteRoleIds` 时应保持 `PrerequisiteGroupRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `prerequisiteRoleIds`, preserve `PrerequisiteGroupRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotEmpty List<@NotBlank String> prerequisiteRoleIds,
            /**
             * 字段 `expectedRoleVersion` 表示 `PrerequisiteGroupRequest` 中与 `expected Role Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedRoleVersion` stores the `expected Role Version`-related state, dependency, configuration, or result of `PrerequisiteGroupRequest` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedRoleVersion` 时应保持 `PrerequisiteGroupRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedRoleVersion`, preserve `PrerequisiteGroupRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedRoleVersion) {
    }

    /**
     * 类型 `CardinalityRequest` 位于 `ConstraintController` 内，是记录类型，用于承载 `Cardinality Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CardinalityRequest` is a record inside `ConstraintController` and carries the responsibility, state, or contract for `Cardinality Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CardinalityRequest` 作为 `ConstraintController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CardinalityRequest` as the responsibility boundary of `ConstraintController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param scopeType 记录组件 `scopeType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeType` carries constructor data whose meaning is defined by the record contract.
     * @param maximumActive 记录组件 `maximumActive` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumActive` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param expectedVersion 记录组件 `expectedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record CardinalityRequest(
            /**
             * 字段 `scopeType` 表示 `CardinalityRequest` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `CardinalityRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `CardinalityRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `CardinalityRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String scopeType,
            /**
             * 字段 `maximumActive` 表示 `CardinalityRequest` 中与 `maximum Active` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumActive` stores the `maximum Active`-related state, dependency, configuration, or result of `CardinalityRequest` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumActive` 时应保持 `CardinalityRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumActive`, preserve `CardinalityRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @Positive int maximumActive,
            /**
             * 字段 `validFrom` 表示 `CardinalityRequest` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `CardinalityRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `CardinalityRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `CardinalityRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull Instant validFrom,
            /**
             * 字段 `validTo` 表示 `CardinalityRequest` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `CardinalityRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `CardinalityRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `CardinalityRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `expectedVersion` 表示 `CardinalityRequest` 中与 `expected Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedVersion` stores the `expected Version`-related state, dependency, configuration, or result of `CardinalityRequest` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedVersion` 时应保持 `CardinalityRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedVersion`, preserve `CardinalityRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedVersion) {
    }

    /**
     * 类型 `DataRuleRequest` 位于 `ConstraintController` 内，是记录类型，用于承载 `Data Rule Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DataRuleRequest` is a record inside `ConstraintController` and carries the responsibility, state, or contract for `Data Rule Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DataRuleRequest` 作为 `ConstraintController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DataRuleRequest` as the responsibility boundary of `ConstraintController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param permissionId 记录组件 `permissionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionId` carries constructor data whose meaning is defined by the record contract.
     * @param scopeType 记录组件 `scopeType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopeType` carries constructor data whose meaning is defined by the record contract.
     * @param directorySnapshotVersion 记录组件 `directorySnapshotVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `directorySnapshotVersion` carries constructor data whose meaning is defined by the record contract.
     * @param references 记录组件 `references` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `references` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param expectedVersion 记录组件 `expectedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record DataRuleRequest(
            /**
             * 字段 `applicationId` 表示 `DataRuleRequest` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `DataRuleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `DataRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `DataRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String applicationId,
            /**
             * 字段 `roleId` 表示 `DataRuleRequest` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `DataRuleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `DataRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `DataRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String roleId,
            /**
             * 字段 `permissionId` 表示 `DataRuleRequest` 中与 `permission Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionId` stores the `permission Id`-related state, dependency, configuration, or result of `DataRuleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionId` 时应保持 `DataRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionId`, preserve `DataRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String permissionId,
            /**
             * 字段 `scopeType` 表示 `DataRuleRequest` 中与 `scope Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopeType` stores the `scope Type`-related state, dependency, configuration, or result of `DataRuleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopeType` 时应保持 `DataRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopeType`, preserve `DataRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String scopeType,
            /**
             * 字段 `directorySnapshotVersion` 表示 `DataRuleRequest` 中与 `directory Snapshot Version` 相关的状态、依赖、配置或结果（声明类型 `Long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `directorySnapshotVersion` stores the `directory Snapshot Version`-related state, dependency, configuration, or result of `DataRuleRequest` (declared type `Long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `directorySnapshotVersion` 时应保持 `DataRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `directorySnapshotVersion`, preserve `DataRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Long directorySnapshotVersion,
            /**
             * 字段 `references` 表示 `DataRuleRequest` 中与 `references` 相关的状态、依赖、配置或结果（声明类型 `List&lt;ConstraintFacade.RuleReference&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `references` stores the `references`-related state, dependency, configuration, or result of `DataRuleRequest` (declared type `List&lt;ConstraintFacade.RuleReference&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `references` 时应保持 `DataRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `references`, preserve `DataRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull List<ConstraintFacade.RuleReference> references,
            /**
             * 字段 `validFrom` 表示 `DataRuleRequest` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `DataRuleRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `DataRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `DataRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull Instant validFrom,
            /**
             * 字段 `validTo` 表示 `DataRuleRequest` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `DataRuleRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `DataRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `DataRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `expectedVersion` 表示 `DataRuleRequest` 中与 `expected Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedVersion` stores the `expected Version`-related state, dependency, configuration, or result of `DataRuleRequest` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedVersion` 时应保持 `DataRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedVersion`, preserve `DataRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedVersion) {
    }

    /**
     * 类型 `FieldRuleRequest` 位于 `ConstraintController` 内，是记录类型，用于承载 `Field Rule Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `FieldRuleRequest` is a record inside `ConstraintController` and carries the responsibility, state, or contract for `Field Rule Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `FieldRuleRequest` 作为 `ConstraintController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `FieldRuleRequest` as the responsibility boundary of `ConstraintController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param applicationId 记录组件 `applicationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationId` carries constructor data whose meaning is defined by the record contract.
     * @param roleId 记录组件 `roleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleId` carries constructor data whose meaning is defined by the record contract.
     * @param permissionId 记录组件 `permissionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionId` carries constructor data whose meaning is defined by the record contract.
     * @param fieldDefinitionId 记录组件 `fieldDefinitionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `fieldDefinitionId` carries constructor data whose meaning is defined by the record contract.
     * @param accessLevel 记录组件 `accessLevel` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `accessLevel` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param expectedVersion 记录组件 `expectedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record FieldRuleRequest(
            /**
             * 字段 `applicationId` 表示 `FieldRuleRequest` 中与 `application Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationId` stores the `application Id`-related state, dependency, configuration, or result of `FieldRuleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationId` 时应保持 `FieldRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationId`, preserve `FieldRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String applicationId,
            /**
             * 字段 `roleId` 表示 `FieldRuleRequest` 中与 `role Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleId` stores the `role Id`-related state, dependency, configuration, or result of `FieldRuleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleId` 时应保持 `FieldRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleId`, preserve `FieldRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String roleId,
            /**
             * 字段 `permissionId` 表示 `FieldRuleRequest` 中与 `permission Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionId` stores the `permission Id`-related state, dependency, configuration, or result of `FieldRuleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionId` 时应保持 `FieldRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionId`, preserve `FieldRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String permissionId,
            /**
             * 字段 `fieldDefinitionId` 表示 `FieldRuleRequest` 中与 `field Definition Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `fieldDefinitionId` stores the `field Definition Id`-related state, dependency, configuration, or result of `FieldRuleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `fieldDefinitionId` 时应保持 `FieldRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `fieldDefinitionId`, preserve `FieldRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String fieldDefinitionId,
            /**
             * 字段 `accessLevel` 表示 `FieldRuleRequest` 中与 `access Level` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `accessLevel` stores the `access Level`-related state, dependency, configuration, or result of `FieldRuleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `accessLevel` 时应保持 `FieldRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `accessLevel`, preserve `FieldRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String accessLevel,
            /**
             * 字段 `validFrom` 表示 `FieldRuleRequest` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `FieldRuleRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `FieldRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `FieldRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull Instant validFrom,
            /**
             * 字段 `validTo` 表示 `FieldRuleRequest` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `FieldRuleRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `FieldRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `FieldRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `expectedVersion` 表示 `FieldRuleRequest` 中与 `expected Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedVersion` stores the `expected Version`-related state, dependency, configuration, or result of `FieldRuleRequest` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedVersion` 时应保持 `FieldRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedVersion`, preserve `FieldRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedVersion) {
    }

    /**
     * 类型 `OperationSodRuleRequest` 位于 `ConstraintController` 内，是记录类型，用于承载 `Operation Sod Rule Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `OperationSodRuleRequest` is a record inside `ConstraintController` and carries the responsibility, state, or contract for `Operation Sod Rule Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `OperationSodRuleRequest` 作为 `ConstraintController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `OperationSodRuleRequest` as the responsibility boundary of `ConstraintController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param applicationCode 记录组件 `applicationCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationCode` carries constructor data whose meaning is defined by the record contract.
     * @param businessResource 记录组件 `businessResource` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessResource` carries constructor data whose meaning is defined by the record contract.
     * @param priorActionCode 记录组件 `priorActionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `priorActionCode` carries constructor data whose meaning is defined by the record contract.
     * @param forbiddenLaterActionCode 记录组件 `forbiddenLaterActionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `forbiddenLaterActionCode` carries constructor data whose meaning is defined by the record contract.
     * @param lookbackFrom 记录组件 `lookbackFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `lookbackFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param expectedVersion 记录组件 `expectedVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expectedVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record OperationSodRuleRequest(
            /**
             * 字段 `applicationCode` 表示 `OperationSodRuleRequest` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `OperationSodRuleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `OperationSodRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `OperationSodRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String applicationCode,
            /**
             * 字段 `businessResource` 表示 `OperationSodRuleRequest` 中与 `business Resource` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessResource` stores the `business Resource`-related state, dependency, configuration, or result of `OperationSodRuleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessResource` 时应保持 `OperationSodRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessResource`, preserve `OperationSodRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String businessResource,
            /**
             * 字段 `priorActionCode` 表示 `OperationSodRuleRequest` 中与 `prior Action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `priorActionCode` stores the `prior Action Code`-related state, dependency, configuration, or result of `OperationSodRuleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `priorActionCode` 时应保持 `OperationSodRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `priorActionCode`, preserve `OperationSodRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String priorActionCode,
            /**
             * 字段 `forbiddenLaterActionCode` 表示 `OperationSodRuleRequest` 中与 `forbidden Later Action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `forbiddenLaterActionCode` stores the `forbidden Later Action Code`-related state, dependency, configuration, or result of `OperationSodRuleRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `forbiddenLaterActionCode` 时应保持 `OperationSodRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `forbiddenLaterActionCode`, preserve `OperationSodRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String forbiddenLaterActionCode,
            /**
             * 字段 `lookbackFrom` 表示 `OperationSodRuleRequest` 中与 `lookback From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `lookbackFrom` stores the `lookback From`-related state, dependency, configuration, or result of `OperationSodRuleRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `lookbackFrom` 时应保持 `OperationSodRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `lookbackFrom`, preserve `OperationSodRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant lookbackFrom,
            /**
             * 字段 `validFrom` 表示 `OperationSodRuleRequest` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `OperationSodRuleRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `OperationSodRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `OperationSodRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull Instant validFrom,
            /**
             * 字段 `validTo` 表示 `OperationSodRuleRequest` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `OperationSodRuleRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `OperationSodRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `OperationSodRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `expectedVersion` 表示 `OperationSodRuleRequest` 中与 `expected Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expectedVersion` stores the `expected Version`-related state, dependency, configuration, or result of `OperationSodRuleRequest` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expectedVersion` 时应保持 `OperationSodRuleRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expectedVersion`, preserve `OperationSodRuleRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @PositiveOrZero long expectedVersion) {
    }
}
