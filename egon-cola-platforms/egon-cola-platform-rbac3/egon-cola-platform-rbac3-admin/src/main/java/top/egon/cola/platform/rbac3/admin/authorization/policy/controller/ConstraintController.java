package top.egon.cola.platform.rbac3.admin.authorization.policy.controller;

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
import top.egon.cola.component.common.core.pojo.ResultRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.rbac3.admin.authorization.policy.domain.dto.CardinalityCommandDTO;
import top.egon.cola.platform.rbac3.admin.authorization.policy.domain.dto.CardinalityRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.policy.domain.dto.DataRuleCommandDTO;
import top.egon.cola.platform.rbac3.admin.authorization.policy.domain.dto.DataRuleRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.policy.domain.dto.FieldRuleCommandDTO;
import top.egon.cola.platform.rbac3.admin.authorization.policy.domain.dto.FieldRuleRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.policy.domain.dto.OperationSodRuleCommandDTO;
import top.egon.cola.platform.rbac3.admin.authorization.policy.domain.dto.OperationSodRuleRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.policy.domain.dto.PrerequisiteGroupCommandDTO;
import top.egon.cola.platform.rbac3.admin.authorization.policy.domain.dto.PrerequisiteGroupRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.policy.domain.dto.SaveSodCommandDTO;
import top.egon.cola.platform.rbac3.admin.authorization.policy.domain.dto.SodSetRequestDTO;
import top.egon.cola.platform.rbac3.admin.authorization.policy.domain.enums.ConstraintTypeEnum;
import top.egon.cola.platform.rbac3.admin.authorization.policy.domain.vo.DataRuleVO;
import top.egon.cola.platform.rbac3.admin.authorization.policy.domain.vo.FieldRuleVO;
import top.egon.cola.platform.rbac3.admin.authorization.policy.domain.vo.MutationResultVO;
import top.egon.cola.platform.rbac3.admin.authorization.policy.domain.vo.OperationSodRuleVO;
import top.egon.cola.platform.rbac3.admin.authorization.policy.domain.vo.SodVO;
import top.egon.cola.platform.rbac3.admin.authorization.policy.service.ConstraintFacade;
import top.egon.cola.platform.rbac3.admin.shared.tenant.domain.TenantContext;
import top.egon.cola.platform.rbac3.starter.security.CurrentRbac3User;
import top.egon.cola.platform.rbac3.starter.security.Rbac3UserDetails;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;

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
@RequestMapping("/api/rbac3/v1/iam/policies")
@Tag(name = "authorization-constraint", description = "授权约束接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        interfaceGroupCode = "iam"
)
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
    @RequiresPermission(value = "system:authorization-constraint:read")
    @Operation(
            operationId = "rbac3-sod-set-list-v1",
            summary = "查询SSD和DSD集合",
            tags = {"rbac3", "constraint"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<List<SodVO>> sodSets() {
        return ResultRecord.success(facade.sodSets(tenantId()));
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
    @RequiresPermission(value = "system:authorization-constraint:manage")
    @Operation(
            operationId = "rbac3-sod-set-create-v1",
            summary = "创建SSD或DSD集合",
            tags = {"rbac3", "constraint"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<MutationResultVO> createSodSet(
            @Valid @RequestBody SodSetRequestDTO request
) {
        return ResultRecord.success(facade.saveSod(sodCommand(null, request , new CurrentRbac3User().require())));
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
    @RequiresPermission(value = "system:authorization-constraint:manage")
    @Operation(
            operationId = "rbac3-sod-set-update-v1",
            summary = "更新SSD或DSD集合",
            tags = {"rbac3", "constraint"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<MutationResultVO> updateSodSet(
            @PathVariable String setId,
            @Valid @RequestBody SodSetRequestDTO request
) {
        return ResultRecord.success(facade.saveSod(sodCommand(setId, request , new CurrentRbac3User().require())));
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
    @RequiresPermission(value = "system:authorization-constraint:manage")
    @Operation(
            operationId = "rbac3-role-prerequisite-save-v1",
            summary = "替换角色前置条件组",
            tags = {"rbac3", "constraint"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<MutationResultVO> prerequisites(
            @PathVariable String roleId,
            @Valid @RequestBody PrerequisiteGroupRequestDTO request
) {
        return ResultRecord.success(facade.savePrerequisites(
                new PrerequisiteGroupCommandDTO(
                        tenantId(),
                        roleId,
                        request.groupCode(),
                        request.matchMode(),
                        request.prerequisiteRoleIds(),
                        request.expectedRoleVersion(),
                        new CurrentRbac3User().require().rbac3UserId())));
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
    @RequiresPermission(value = "system:authorization-constraint:manage")
    @Operation(
            operationId = "rbac3-role-cardinality-save-v1",
            summary = "配置角色容量",
            tags = {"rbac3", "constraint"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<MutationResultVO> cardinality(
            @PathVariable String roleId,
            @Valid @RequestBody CardinalityRequestDTO request
) {
        return ResultRecord.success(facade.saveCardinality(
                new CardinalityCommandDTO(
                        tenantId(),
                        roleId,
                        request.scopeType(),
                        request.maximumActive(),
                        request.validFrom(),
                        request.validTo(),
                        request.expectedVersion(),
                        new CurrentRbac3User().require().rbac3UserId())));
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
    @RequiresPermission(value = "system:data-rule:read")
    @Operation(
            operationId = "rbac3-data-rule-list-v1",
            summary = "查询数据规则",
            tags = {"rbac3", "data-rule"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<List<DataRuleVO>> dataRules() {
        return ResultRecord.success(facade.dataRules(tenantId()));
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
    @RequiresPermission(value = "system:data-rule:manage")
    @Operation(
            operationId = "rbac3-data-rule-create-v1",
            summary = "创建类型化数据规则",
            tags = {"rbac3", "data-rule"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<MutationResultVO> createDataRule(
            @Valid @RequestBody DataRuleRequestDTO request
) {
        return ResultRecord.success(facade.saveDataRule(
                dataRuleCommand(null, request , new CurrentRbac3User().require())));
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
    @RequiresPermission(value = "system:data-rule:manage")
    @Operation(
            operationId = "rbac3-data-rule-update-v1",
            summary = "更新类型化数据规则",
            tags = {"rbac3", "data-rule"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<MutationResultVO> updateDataRule(
            @PathVariable String ruleId,
            @Valid @RequestBody DataRuleRequestDTO request
) {
        return ResultRecord.success(facade.saveDataRule(
                dataRuleCommand(ruleId, request , new CurrentRbac3User().require())));
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
    @RequiresPermission(value = "system:field-rule:read")
    @Operation(
            operationId = "rbac3-field-rule-list-v1",
            summary = "查询字段规则",
            tags = {"rbac3", "field-rule"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<List<FieldRuleVO>> fieldRules() {
        return ResultRecord.success(facade.fieldRules(tenantId()));
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
    @RequiresPermission(value = "system:field-rule:manage")
    @Operation(
            operationId = "rbac3-field-rule-create-v1",
            summary = "创建字段访问规则",
            tags = {"rbac3", "field-rule"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<MutationResultVO> createFieldRule(
            @Valid @RequestBody FieldRuleRequestDTO request
) {
        return ResultRecord.success(facade.saveFieldRule(
                fieldRuleCommand(null, request , new CurrentRbac3User().require())));
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
    @RequiresPermission(value = "system:field-rule:manage")
    @Operation(
            operationId = "rbac3-field-rule-update-v1",
            summary = "更新字段访问规则",
            tags = {"rbac3", "field-rule"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<MutationResultVO> updateFieldRule(
            @PathVariable String ruleId,
            @Valid @RequestBody FieldRuleRequestDTO request
) {
        return ResultRecord.success(facade.saveFieldRule(
                fieldRuleCommand(ruleId, request , new CurrentRbac3User().require())));
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
    @RequiresPermission(value = "system:operation-sod:read")
    @Operation(
            operationId = "rbac3-operation-sod-list-v1",
            summary = "查询同对象职责分离规则",
            tags = {"rbac3", "operation-sod"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<List<OperationSodRuleVO>> operationSodRules() {
        return ResultRecord.success(facade.operationSodRules(tenantId()));
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
    @RequiresPermission(value = "system:operation-sod:manage")
    @Operation(
            operationId = "rbac3-operation-sod-create-v1",
            summary = "创建同对象职责分离规则",
            tags = {"rbac3", "operation-sod"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<MutationResultVO> createOperationSodRule(
            @Valid @RequestBody OperationSodRuleRequestDTO request
) {
        return ResultRecord.success(facade.saveOperationSodRule(
                operationSodCommand(null, request , new CurrentRbac3User().require())));
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
    @RequiresPermission(value = "system:operation-sod:manage")
    @Operation(
            operationId = "rbac3-operation-sod-update-v1",
            summary = "更新同对象职责分离规则",
            tags = {"rbac3", "operation-sod"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<MutationResultVO> updateOperationSodRule(
            @PathVariable String ruleId,
            @Valid @RequestBody OperationSodRuleRequestDTO request
) {
        return ResultRecord.success(facade.saveOperationSodRule(
                operationSodCommand(ruleId, request , new CurrentRbac3User().require())));
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
    private static SaveSodCommandDTO sodCommand(
            String setId,
            SodSetRequestDTO request,
            Rbac3UserDetails principal) {
        return new SaveSodCommandDTO(
                tenantId(), setId, request.setCode(),
                ConstraintTypeEnum.valueOf(request.constraintType()),
                request.applicationId(), request.maximumActiveRoles(), request.memberRoleIds(),
                request.validFrom(), request.validTo(), request.expectedVersion(), new CurrentRbac3User().require().rbac3UserId());
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
    private static DataRuleCommandDTO dataRuleCommand(
            String ruleId,
            DataRuleRequestDTO request,
            Rbac3UserDetails principal) {
        return new DataRuleCommandDTO(
                tenantId(), ruleId, request.applicationId(), request.roleId(),
                request.permissionId(), request.scopeType(), request.directorySnapshotVersion(),
                request.references(), request.validFrom(), request.validTo(),
                request.expectedVersion(), new CurrentRbac3User().require().rbac3UserId());
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
    private static FieldRuleCommandDTO fieldRuleCommand(
            String ruleId,
            FieldRuleRequestDTO request,
            Rbac3UserDetails principal) {
        return new FieldRuleCommandDTO(
                tenantId(), ruleId, request.applicationId(), request.roleId(),
                request.permissionId(), request.fieldDefinitionId(), request.accessLevel(),
                request.validFrom(), request.validTo(), request.expectedVersion(), new CurrentRbac3User().require().rbac3UserId());
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
    private static OperationSodRuleCommandDTO operationSodCommand(
            String ruleId,
            OperationSodRuleRequestDTO request,
            Rbac3UserDetails principal) {
        return new OperationSodRuleCommandDTO(
                tenantId(), ruleId, request.applicationCode(), request.businessResource(),
                request.priorActionCode(), request.forbiddenLaterActionCode(), request.lookbackFrom(),
                request.validFrom(), request.validTo(), request.expectedVersion(), new CurrentRbac3User().require().rbac3UserId());
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






    }
