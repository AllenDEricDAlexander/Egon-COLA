package top.egon.cola.platform.rbac3.admin.management.controller;

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
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.management.service.ManagementPolicyFacade;
import top.egon.cola.platform.rbac3.admin.runtime.service.IdempotencyService;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.TenantContext;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;
import top.egon.cola.platform.rbac3.admin.management.domain.dto.PolicyRequestDTO;
import top.egon.cola.platform.rbac3.admin.management.domain.dto.ManagementPolicyRestrictionsDTO;
import top.egon.cola.platform.rbac3.admin.management.domain.dto.SaveCommandDTO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.PolicyVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.CapabilityVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagedUserVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagedRoleVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagementPolicyRestrictionsVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagementPolicyScopeVO;
import top.egon.cola.platform.rbac3.admin.management.domain.vo.ManagementPolicySubjectVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.dto.IdempotencyCommandDTO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.IdempotencyClaimVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.enums.IdempotencyOutcomeEnum;

/**
 * 类型 `ManagementPolicyController` 位于当前包内，是类型，用于承载 `Management Policy Controller` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ManagementPolicyController` is a type in its package and carries the responsibility, state, or contract for `Management Policy Controller`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ManagementPolicyController` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ManagementPolicyController` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
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

    /**
     * 字段 `IDEMPOTENCY_TTL` 表示 `ManagementPolicyController` 中与 `IDEMPOTENCY TTL` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `IDEMPOTENCY_TTL` stores the `IDEMPOTENCY TTL`-related state, dependency, configuration, or result of `ManagementPolicyController` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `IDEMPOTENCY_TTL` 时应保持 `ManagementPolicyController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `IDEMPOTENCY_TTL`, preserve `ManagementPolicyController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    /**
     * 字段 `facade` 表示 `ManagementPolicyController` 中与 `facade` 相关的状态、依赖、配置或结果（声明类型 `ManagementPolicyFacade`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `facade` stores the `facade`-related state, dependency, configuration, or result of `ManagementPolicyController` (declared type `ManagementPolicyFacade`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `facade` 时应保持 `ManagementPolicyController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `facade`, preserve `ManagementPolicyController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ManagementPolicyFacade facade;
    /**
     * 字段 `idempotencyService` 表示 `ManagementPolicyController` 中与 `idempotency Service` 相关的状态、依赖、配置或结果（声明类型 `IdempotencyService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idempotencyService` stores the `idempotency Service`-related state, dependency, configuration, or result of `ManagementPolicyController` (declared type `IdempotencyService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idempotencyService` 时应保持 `ManagementPolicyController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idempotencyService`, preserve `ManagementPolicyController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final IdempotencyService idempotencyService;
    /**
     * 字段 `databaseClock` 表示 `ManagementPolicyController` 中与 `database Clock` 相关的状态、依赖、配置或结果（声明类型 `DatabaseClock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `databaseClock` stores the `database Clock`-related state, dependency, configuration, or result of `ManagementPolicyController` (declared type `DatabaseClock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `databaseClock` 时应保持 `ManagementPolicyController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `databaseClock`, preserve `ManagementPolicyController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DatabaseClock databaseClock;

    /**
     * 构造器 `ManagementPolicyController` 用于创建并初始化 `ManagementPolicyController` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ManagementPolicyController` creates and initializes `ManagementPolicyController`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ManagementPolicyController` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ManagementPolicyController`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param facade 输入参数 `facade`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idempotencyService 输入参数 `idempotencyService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseClock 输入参数 `databaseClock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ManagementPolicyController(
            ManagementPolicyFacade facade,
            IdempotencyService idempotencyService,
            DatabaseClock databaseClock
    ) {
        this.facade = facade;
        this.idempotencyService = idempotencyService;
        this.databaseClock = databaseClock;
    }

    /**
     * 方法 `policies` 按照 `ManagementPolicyController` 的职责处理输入，完成 `policies` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `policies` processes its inputs according to `ManagementPolicyController`'s responsibility, performs the `policies` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `policies` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `policies`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/management-policies")
    @RequiresRbac3Permission(permission = "system:management-policy:read")
    @GatewayOperation(
            name = "rbac3-management-policy-list-v1",
            summary = "查询完整委托管理策略",
            externalAccessible = true,
            tags = {"rbac3", "management-policy"})
    public ApiEnvelopeVO<List<PolicyVO>> policies() {
        return ApiEnvelopeVO.success(facade.policies(tenantId()));
    }

    /**
     * 方法 `policy` 按照 `ManagementPolicyController` 的职责处理输入，完成 `policy` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `policy` processes its inputs according to `ManagementPolicyController`'s responsibility, performs the `policy` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `policy` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `policy`, then continue the business flow using its result, exception, or side effect.
     *
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/management-policies/{policyId}")
    @RequiresRbac3Permission(permission = "system:management-policy:read")
    @GatewayOperation(
            name = "rbac3-management-policy-get-v1",
            summary = "读取委托管理策略完整聚合",
            externalAccessible = true,
            tags = {"rbac3", "management-policy"})
    public ApiEnvelopeVO<PolicyVO> policy(
            @PathVariable String policyId
    ) {
        return ApiEnvelopeVO.success(facade.policy(tenantId(), policyId));
    }

    /**
     * 方法 `create` 按照 `ManagementPolicyController` 的职责处理输入，完成 `create` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `create` processes its inputs according to `ManagementPolicyController`'s responsibility, performs the `create` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `create` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `create`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idempotencyKey 输入参数 `idempotencyKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/management-policies")
    @RequiresRbac3Permission(permission = "system:management-policy:manage")
    @GatewayOperation(
            name = "rbac3-management-policy-create-v1",
            summary = "创建完整委托管理策略",
            externalAccessible = true,
            tags = {"rbac3", "management-policy"})
    public ApiEnvelopeVO<PolicyVO> create(
            @Valid @RequestBody PolicyRequestDTO request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return save(null, 0L, request, idempotencyKey, principal,
                "POST:/management-policies");
    }

    /**
     * 方法 `update` 按照 `ManagementPolicyController` 的职责处理输入，完成 `update` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `update` processes its inputs according to `ManagementPolicyController`'s responsibility, performs the `update` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `update` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `update`, then continue the business flow using its result, exception, or side effect.
     *
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param ifMatch 输入参数 `ifMatch`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idempotencyKey 输入参数 `idempotencyKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PutMapping("/management-policies/{policyId}")
    @RequiresRbac3Permission(permission = "system:management-policy:manage")
    @GatewayOperation(
            name = "rbac3-management-policy-update-v1",
            summary = "按版本完整替换委托管理策略",
            externalAccessible = true,
            tags = {"rbac3", "management-policy"})
    public ApiEnvelopeVO<PolicyVO> update(
            @PathVariable String policyId,
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody PolicyRequestDTO request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return save(policyId, expectedVersion(ifMatch), request, idempotencyKey,
                principal, "PUT:/management-policies/{policyId}");
    }

    /**
     * 方法 `disable` 按照 `ManagementPolicyController` 的职责处理输入，完成 `disable` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `disable` processes its inputs according to `ManagementPolicyController`'s responsibility, performs the `disable` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `disable` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `disable`, then continue the business flow using its result, exception, or side effect.
     *
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param ifMatch 输入参数 `ifMatch`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idempotencyKey 输入参数 `idempotencyKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/management-policies/{policyId}/disable")
    @RequiresRbac3Permission(permission = "system:management-policy:manage")
    @GatewayOperation(
            name = "rbac3-management-policy-disable-v1",
            summary = "禁用委托管理策略并保留历史明细",
            externalAccessible = true,
            tags = {"rbac3", "management-policy"})
    public ApiEnvelopeVO<PolicyVO> disable(
            @PathVariable String policyId,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        Instant now = databaseClock.transactionNow();
        IdempotencyClaimVO claim = claim(
                principal, "POST:/management-policies/{policyId}/disable",
                idempotencyKey, policyId + '|' + expectedVersion(ifMatch), now);
        if (claim.outcome() == IdempotencyOutcomeEnum.REPLAY) {
            return ApiEnvelopeVO.success(facade.policy(tenantId(), claim.resourceId()));
        }
        PolicyVO view = facade.disable(
                tenantId(), policyId, expectedVersion(ifMatch), principal.userId());
        complete(claim, view, now);
        return ApiEnvelopeVO.success(view);
    }

    /**
     * 方法 `capabilities` 按照 `ManagementPolicyController` 的职责处理输入，完成 `capabilities` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `capabilities` processes its inputs according to `ManagementPolicyController`'s responsibility, performs the `capabilities` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `capabilities` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `capabilities`, then continue the business flow using its result, exception, or side effect.
     *
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/management-capabilities/me")
    @GatewayOperation(
            name = "rbac3-management-capabilities-mine-v1",
            summary = "查询当前操作者委托管理能力",
            externalAccessible = true,
            tags = {"rbac3", "management-policy", "capability"})
    public ApiEnvelopeVO<CapabilityVO> capabilities(
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return ApiEnvelopeVO.success(facade.capabilities(
                tenantId(), principal.userId(), databaseClock.transactionNow()));
    }

    /**
     * 方法 `manageableUsers` 按照 `ManagementPolicyController` 的职责处理输入，完成 `manageable Users` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `manageableUsers` processes its inputs according to `ManagementPolicyController`'s responsibility, performs the `manageable Users` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `manageableUsers` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `manageableUsers`, then continue the business flow using its result, exception, or side effect.
     *
     * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/manageable-users")
    @GatewayOperation(
            name = "rbac3-manageable-user-search-v1",
            summary = "按委托范围搜索可管理用户",
            externalAccessible = true,
            tags = {"rbac3", "management-policy", "user"})
    public ApiEnvelopeVO<List<ManagedUserVO>> manageableUsers(
            @RequestParam(required = false) String query,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return ApiEnvelopeVO.success(facade.manageableUsers(
                tenantId(), principal.userId(), query,
                databaseClock.transactionNow()));
    }

    /**
     * 方法 `manageableRoles` 按照 `ManagementPolicyController` 的职责处理输入，完成 `manageable Roles` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `manageableRoles` processes its inputs according to `ManagementPolicyController`'s responsibility, performs the `manageable Roles` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `manageableRoles` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `manageableRoles`, then continue the business flow using its result, exception, or side effect.
     *
     * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/manageable-roles")
    @GatewayOperation(
            name = "rbac3-manageable-role-search-v1",
            summary = "按委托白名单搜索可管理角色根",
            externalAccessible = true,
            tags = {"rbac3", "management-policy", "role"})
    public ApiEnvelopeVO<List<ManagedRoleVO>> manageableRoles(
            @RequestParam(required = false) String query,
            @AuthenticationPrincipal CurrentRbac3Principal principal
    ) {
        return ApiEnvelopeVO.success(facade.manageableRoles(
                tenantId(), principal.userId(), query,
                databaseClock.transactionNow()));
    }

    /**
     * 方法 `save` 按照 `ManagementPolicyController` 的职责处理输入，完成 `save` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `save` processes its inputs according to `ManagementPolicyController`'s responsibility, performs the `save` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `save` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `save`, then continue the business flow using its result, exception, or side effect.
     *
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expectedVersion 输入参数 `expectedVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idempotencyKey 输入参数 `idempotencyKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param operation 输入参数 `operation`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private ApiEnvelopeVO<PolicyVO> save(
            String policyId,
            long expectedVersion,
            PolicyRequestDTO request,
            String idempotencyKey,
            CurrentRbac3Principal principal,
            String operation
    ) {
        Instant now = databaseClock.transactionNow();
        IdempotencyClaimVO claim = claim(
                principal, operation, idempotencyKey,
                canonical(policyId, expectedVersion, request), now);
        if (claim.outcome() == IdempotencyOutcomeEnum.REPLAY) {
            return ApiEnvelopeVO.success(facade.policy(tenantId(), claim.resourceId()));
        }
        ManagementPolicyRestrictionsDTO requestRestrictions = request.restrictions();
        PolicyVO view = facade.save(
                new SaveCommandDTO(
                        tenantId(), policyId, request.policyCode(), request.name(),
                        request.validFrom(), request.validTo(),
                        new ManagementPolicyRestrictionsVO(
                                requestRestrictions.maximumAssignmentDays(),
                                requestRestrictions.maximumRiskLevel(),
                                requestRestrictions.requiredAuthenticationStrength(),
                                requestRestrictions.requireReason(),
                                requestRestrictions.requireTicket(),
                                requestRestrictions.includeInheritedSubjectRoles(),
                                requestRestrictions.requireAllAffiliationsInScope()),
                        request.subjects().stream()
                                .map(subject -> new ManagementPolicySubjectVO(
                                        subject.type(), subject.id()))
                                .toList(),
                        request.scopes().stream()
                                .map(scope -> new ManagementPolicyScopeVO(
                                        scope.type(), scope.referenceId()))
                                .toList(),
                        request.activationRootRoleIds(), request.operations(),
                        expectedVersion, principal.userId()));
        complete(claim, view, now);
        return ApiEnvelopeVO.success(view);
    }

    /**
     * 方法 `claim` 按照 `ManagementPolicyController` 的职责处理输入，完成 `claim` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `claim` processes its inputs according to `ManagementPolicyController`'s responsibility, performs the `claim` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `claim` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `claim`, then continue the business flow using its result, exception, or side effect.
     *
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param operation 输入参数 `operation`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idempotencyKey 输入参数 `idempotencyKey`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param canonicalRequest 输入参数 `canonicalRequest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private IdempotencyClaimVO claim(
            CurrentRbac3Principal principal,
            String operation,
            String idempotencyKey,
            String canonicalRequest,
            Instant now
    ) {
        requireIdempotencyKey(idempotencyKey);
        return idempotencyService.claim(new IdempotencyCommandDTO(
                tenantId(), "USER", principal.userId(), operation,
                idempotencyKey, canonicalRequest, now.plus(IDEMPOTENCY_TTL), now));
    }

    /**
     * 方法 `complete` 按照 `ManagementPolicyController` 的职责处理输入，完成 `complete` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `complete` processes its inputs according to `ManagementPolicyController`'s responsibility, performs the `complete` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `complete` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `complete`, then continue the business flow using its result, exception, or side effect.
     *
     * @param claim 输入参数 `claim`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param view 输入参数 `view`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void complete(
            IdempotencyClaimVO claim,
            PolicyVO view,
            Instant now
    ) {
        idempotencyService.complete(
                claim.recordId(), "MANAGEMENT_POLICY", view.policyId(), 200,
                view.policyId() + '|' + view.version() + '|' + view.status(), now);
    }

    /**
     * 方法 `canonical` 按照 `ManagementPolicyController` 的职责处理输入，完成 `canonical` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `canonical` processes its inputs according to `ManagementPolicyController`'s responsibility, performs the `canonical` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `canonical` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `canonical`, then continue the business flow using its result, exception, or side effect.
     *
     * @param policyId 输入参数 `policyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param expectedVersion 输入参数 `expectedVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String canonical(
            String policyId,
            long expectedVersion,
            PolicyRequestDTO request
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

    /**
     * 方法 `expectedVersion` 按照 `ManagementPolicyController` 的职责处理输入，完成 `expected Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `expectedVersion` processes its inputs according to `ManagementPolicyController`'s responsibility, performs the `expected Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `expectedVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `expectedVersion`, then continue the business flow using its result, exception, or side effect.
     *
     * @param ifMatch 输入参数 `ifMatch`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `requireIdempotencyKey` 按照 `ManagementPolicyController` 的职责处理输入，完成 `require Idempotency Key` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireIdempotencyKey` processes its inputs according to `ManagementPolicyController`'s responsibility, performs the `require Idempotency Key` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireIdempotencyKey` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireIdempotencyKey`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private static void requireIdempotencyKey(String value) {
        if (value == null || value.isBlank() || value.length() > 128
                || !value.chars().allMatch(character -> character >= 0x21
                && character <= 0x7e)) {
            throw new IllegalArgumentException("Idempotency-Key must be 1-128 ASCII characters");
        }
    }

    /**
     * 方法 `tenantId` 按照 `ManagementPolicyController` 的职责处理输入，完成 `tenant Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantId` processes its inputs according to `ManagementPolicyController`'s responsibility, performs the `tenant Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
