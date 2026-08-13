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
import top.egon.cola.platform.rbac3.admin.shared.repository.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.management.application.ManagementPolicyFacade;
import top.egon.cola.platform.rbac3.admin.runtime.application.IdempotencyService;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.TenantContext;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;

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
    public ApiEnvelopeVO<List<ManagementPolicyFacade.PolicyView>> policies() {
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
    public ApiEnvelopeVO<ManagementPolicyFacade.PolicyView> policy(
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
    public ApiEnvelopeVO<ManagementPolicyFacade.PolicyView> create(
            @Valid @RequestBody PolicyRequest request,
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
    public ApiEnvelopeVO<ManagementPolicyFacade.PolicyView> update(
            @PathVariable String policyId,
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody PolicyRequest request,
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
    public ApiEnvelopeVO<ManagementPolicyFacade.PolicyView> disable(
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
            return ApiEnvelopeVO.success(facade.policy(tenantId(), claim.resourceId()));
        }
        ManagementPolicyFacade.PolicyView view = facade.disable(
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
    public ApiEnvelopeVO<ManagementPolicyFacade.CapabilityView> capabilities(
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
    public ApiEnvelopeVO<List<ManagementPolicyFacade.ManagedUserView>> manageableUsers(
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
    public ApiEnvelopeVO<List<ManagementPolicyFacade.ManagedRoleView>> manageableRoles(
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
    private ApiEnvelopeVO<ManagementPolicyFacade.PolicyView> save(
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
            return ApiEnvelopeVO.success(facade.policy(tenantId(), claim.resourceId()));
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
            IdempotencyService.Claim claim,
            ManagementPolicyFacade.PolicyView view,
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

    /**
     * 类型 `PolicyRequest` 位于 `ManagementPolicyController` 内，是记录类型，用于承载 `Policy Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `PolicyRequest` is a record inside `ManagementPolicyController` and carries the responsibility, state, or contract for `Policy Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `PolicyRequest` 作为 `ManagementPolicyController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `PolicyRequest` as the responsibility boundary of `ManagementPolicyController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param policyCode 记录组件 `policyCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyCode` carries constructor data whose meaning is defined by the record contract.
     * @param name 记录组件 `name` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `name` carries constructor data whose meaning is defined by the record contract.
     * @param validFrom 记录组件 `validFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validFrom` carries constructor data whose meaning is defined by the record contract.
     * @param validTo 记录组件 `validTo` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `validTo` carries constructor data whose meaning is defined by the record contract.
     * @param subjects 记录组件 `subjects` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `subjects` carries constructor data whose meaning is defined by the record contract.
     * @param scopes 记录组件 `scopes` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `scopes` carries constructor data whose meaning is defined by the record contract.
     * @param activationRootRoleIds 记录组件 `activationRootRoleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activationRootRoleIds` carries constructor data whose meaning is defined by the record contract.
     * @param operations 记录组件 `operations` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `operations` carries constructor data whose meaning is defined by the record contract.
     * @param restrictions 记录组件 `restrictions` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `restrictions` carries constructor data whose meaning is defined by the record contract.
     */
    public record PolicyRequest(
            /**
             * 字段 `policyCode` 表示 `PolicyRequest` 中与 `policy Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyCode` stores the `policy Code`-related state, dependency, configuration, or result of `PolicyRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyCode` 时应保持 `PolicyRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyCode`, preserve `PolicyRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String policyCode,
            /**
             * 字段 `name` 表示 `PolicyRequest` 中与 `name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `name` stores the `name`-related state, dependency, configuration, or result of `PolicyRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `name` 时应保持 `PolicyRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `name`, preserve `PolicyRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String name,
            /**
             * 字段 `validFrom` 表示 `PolicyRequest` 中与 `valid From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validFrom` stores the `valid From`-related state, dependency, configuration, or result of `PolicyRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validFrom` 时应保持 `PolicyRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validFrom`, preserve `PolicyRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull Instant validFrom,
            /**
             * 字段 `validTo` 表示 `PolicyRequest` 中与 `valid To` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `validTo` stores the `valid To`-related state, dependency, configuration, or result of `PolicyRequest` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `validTo` 时应保持 `PolicyRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `validTo`, preserve `PolicyRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant validTo,
            /**
             * 字段 `subjects` 表示 `PolicyRequest` 中与 `subjects` 相关的状态、依赖、配置或结果（声明类型 `List&lt;@Valid Subject&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `subjects` stores the `subjects`-related state, dependency, configuration, or result of `PolicyRequest` (declared type `List&lt;@Valid Subject&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `subjects` 时应保持 `PolicyRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `subjects`, preserve `PolicyRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotEmpty List<@Valid Subject> subjects,
            /**
             * 字段 `scopes` 表示 `PolicyRequest` 中与 `scopes` 相关的状态、依赖、配置或结果（声明类型 `List&lt;@Valid Scope&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `scopes` stores the `scopes`-related state, dependency, configuration, or result of `PolicyRequest` (declared type `List&lt;@Valid Scope&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `scopes` 时应保持 `PolicyRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `scopes`, preserve `PolicyRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotEmpty List<@Valid Scope> scopes,
            /**
             * 字段 `activationRootRoleIds` 表示 `PolicyRequest` 中与 `activation Root Role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;@NotBlank String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activationRootRoleIds` stores the `activation Root Role Ids`-related state, dependency, configuration, or result of `PolicyRequest` (declared type `List&lt;@NotBlank String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activationRootRoleIds` 时应保持 `PolicyRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activationRootRoleIds`, preserve `PolicyRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotEmpty List<@NotBlank String> activationRootRoleIds,
            /**
             * 字段 `operations` 表示 `PolicyRequest` 中与 `operations` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;@NotBlank String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `operations` stores the `operations`-related state, dependency, configuration, or result of `PolicyRequest` (declared type `Set&lt;@NotBlank String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `operations` 时应保持 `PolicyRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `operations`, preserve `PolicyRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotEmpty Set<@NotBlank String> operations,
            /**
             * 字段 `restrictions` 表示 `PolicyRequest` 中与 `restrictions` 相关的状态、依赖、配置或结果（声明类型 `Restrictions`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `restrictions` stores the `restrictions`-related state, dependency, configuration, or result of `PolicyRequest` (declared type `Restrictions`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `restrictions` 时应保持 `PolicyRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `restrictions`, preserve `PolicyRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotNull @Valid Restrictions restrictions
    ) {
        /**
         * 构造器 `PolicyRequest` 用于创建并初始化 `PolicyRequest` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `PolicyRequest` creates and initializes `PolicyRequest`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `PolicyRequest` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `PolicyRequest`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param policyCode 输入参数 `policyCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validFrom 输入参数 `validFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param validTo 输入参数 `validTo`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param subjects 输入参数 `subjects`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param scopes 输入参数 `scopes`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param activationRootRoleIds 输入参数 `activationRootRoleIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param operations 输入参数 `operations`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param restrictions 输入参数 `restrictions`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public PolicyRequest {
            subjects = List.copyOf(subjects);
            scopes = List.copyOf(scopes);
            activationRootRoleIds = activationRootRoleIds.stream()
                    .sorted(Comparator.naturalOrder()).toList();
            operations = Set.copyOf(operations);
        }
    }

    /**
     * 类型 `Subject` 位于 `ManagementPolicyController` 内，是记录类型，用于承载 `Subject` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Subject` is a record inside `ManagementPolicyController` and carries the responsibility, state, or contract for `Subject`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Subject` 作为 `ManagementPolicyController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Subject` as the responsibility boundary of `ManagementPolicyController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param type 记录组件 `type` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `type` carries constructor data whose meaning is defined by the record contract.
     * @param id 记录组件 `id` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `id` carries constructor data whose meaning is defined by the record contract.
     */
    public record Subject(/**
 * 字段 `type` 表示 `Subject` 中与 `type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `type` stores the `type`-related state, dependency, configuration, or result of `Subject` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `type` 时应保持 `Subject` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `type`, preserve `Subject`'s lifecycle, immutability, and thread-safety constraints.
 */ @NotBlank String type, /**
 * 字段 `id` 表示 `Subject` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `id` stores the `id`-related state, dependency, configuration, or result of `Subject` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `id` 时应保持 `Subject` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `id`, preserve `Subject`'s lifecycle, immutability, and thread-safety constraints.
 */ @NotBlank String id) {
    }

    /**
     * 类型 `Scope` 位于 `ManagementPolicyController` 内，是记录类型，用于承载 `Scope` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Scope` is a record inside `ManagementPolicyController` and carries the responsibility, state, or contract for `Scope`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Scope` 作为 `ManagementPolicyController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Scope` as the responsibility boundary of `ManagementPolicyController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param type 记录组件 `type` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `type` carries constructor data whose meaning is defined by the record contract.
     * @param referenceId 记录组件 `referenceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `referenceId` carries constructor data whose meaning is defined by the record contract.
     */
    public record Scope(/**
 * 字段 `type` 表示 `Scope` 中与 `type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `type` stores the `type`-related state, dependency, configuration, or result of `Scope` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `type` 时应保持 `Scope` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `type`, preserve `Scope`'s lifecycle, immutability, and thread-safety constraints.
 */ @NotBlank String type, /**
 * 字段 `referenceId` 表示 `Scope` 中与 `reference Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `referenceId` stores the `reference Id`-related state, dependency, configuration, or result of `Scope` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `referenceId` 时应保持 `Scope` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `referenceId`, preserve `Scope`'s lifecycle, immutability, and thread-safety constraints.
 */ String referenceId) {
    }

    /**
     * 类型 `Restrictions` 位于 `ManagementPolicyController` 内，是记录类型，用于承载 `Restrictions` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Restrictions` is a record inside `ManagementPolicyController` and carries the responsibility, state, or contract for `Restrictions`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Restrictions` 作为 `ManagementPolicyController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Restrictions` as the responsibility boundary of `ManagementPolicyController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param maximumAssignmentDays 记录组件 `maximumAssignmentDays` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumAssignmentDays` carries constructor data whose meaning is defined by the record contract.
     * @param maximumRiskLevel 记录组件 `maximumRiskLevel` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `maximumRiskLevel` carries constructor data whose meaning is defined by the record contract.
     * @param requiredAuthenticationStrength 记录组件 `requiredAuthenticationStrength` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requiredAuthenticationStrength` carries constructor data whose meaning is defined by the record contract.
     * @param requireReason 记录组件 `requireReason` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requireReason` carries constructor data whose meaning is defined by the record contract.
     * @param requireTicket 记录组件 `requireTicket` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requireTicket` carries constructor data whose meaning is defined by the record contract.
     * @param includeInheritedSubjectRoles 记录组件 `includeInheritedSubjectRoles` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `includeInheritedSubjectRoles` carries constructor data whose meaning is defined by the record contract.
     * @param requireAllAffiliationsInScope 记录组件 `requireAllAffiliationsInScope` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requireAllAffiliationsInScope` carries constructor data whose meaning is defined by the record contract.
     */
    public record Restrictions(
            /**
             * 字段 `maximumAssignmentDays` 表示 `Restrictions` 中与 `maximum Assignment Days` 相关的状态、依赖、配置或结果（声明类型 `Integer`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumAssignmentDays` stores the `maximum Assignment Days`-related state, dependency, configuration, or result of `Restrictions` (declared type `Integer`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumAssignmentDays` 时应保持 `Restrictions` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumAssignmentDays`, preserve `Restrictions`'s lifecycle, immutability, and thread-safety constraints.
             */
            Integer maximumAssignmentDays,
            /**
             * 字段 `maximumRiskLevel` 表示 `Restrictions` 中与 `maximum Risk Level` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `maximumRiskLevel` stores the `maximum Risk Level`-related state, dependency, configuration, or result of `Restrictions` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `maximumRiskLevel` 时应保持 `Restrictions` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `maximumRiskLevel`, preserve `Restrictions`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String maximumRiskLevel,
            /**
             * 字段 `requiredAuthenticationStrength` 表示 `Restrictions` 中与 `required Authentication Strength` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requiredAuthenticationStrength` stores the `required Authentication Strength`-related state, dependency, configuration, or result of `Restrictions` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requiredAuthenticationStrength` 时应保持 `Restrictions` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requiredAuthenticationStrength`, preserve `Restrictions`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String requiredAuthenticationStrength,
            /**
             * 字段 `requireReason` 表示 `Restrictions` 中与 `require Reason` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requireReason` stores the `require Reason`-related state, dependency, configuration, or result of `Restrictions` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requireReason` 时应保持 `Restrictions` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requireReason`, preserve `Restrictions`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean requireReason,
            /**
             * 字段 `requireTicket` 表示 `Restrictions` 中与 `require Ticket` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requireTicket` stores the `require Ticket`-related state, dependency, configuration, or result of `Restrictions` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requireTicket` 时应保持 `Restrictions` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requireTicket`, preserve `Restrictions`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean requireTicket,
            /**
             * 字段 `includeInheritedSubjectRoles` 表示 `Restrictions` 中与 `include Inherited Subject Roles` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `includeInheritedSubjectRoles` stores the `include Inherited Subject Roles`-related state, dependency, configuration, or result of `Restrictions` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `includeInheritedSubjectRoles` 时应保持 `Restrictions` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `includeInheritedSubjectRoles`, preserve `Restrictions`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean includeInheritedSubjectRoles,
            /**
             * 字段 `requireAllAffiliationsInScope` 表示 `Restrictions` 中与 `require All Affiliations In Scope` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requireAllAffiliationsInScope` stores the `require All Affiliations In Scope`-related state, dependency, configuration, or result of `Restrictions` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requireAllAffiliationsInScope` 时应保持 `Restrictions` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requireAllAffiliationsInScope`, preserve `Restrictions`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean requireAllAffiliationsInScope
    ) {
    }
}
