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
 * 类型 `DefaultAuthorizationService` 位于当前包内，是类型，用于承载 `Default Authorization Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `DefaultAuthorizationService` is a type in its package and carries the responsibility, state, or contract for `Default Authorization Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Snapshot-based authorization service. Every failure path remains closed.
 */
public final class DefaultAuthorizationService implements AuthorizationService {

    /**
     * 字段 `contextSource` 表示 `DefaultAuthorizationService` 中与 `context Source` 相关的状态、依赖、配置或结果（声明类型 `RuntimeContextSource`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `contextSource` stores the `context Source`-related state, dependency, configuration, or result of `DefaultAuthorizationService` (declared type `RuntimeContextSource`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `contextSource` 时应保持 `DefaultAuthorizationService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `contextSource`, preserve `DefaultAuthorizationService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RuntimeContextSource contextSource;
    /**
     * 字段 `operationSodEvaluator` 表示 `DefaultAuthorizationService` 中与 `operation Sod Evaluator` 相关的状态、依赖、配置或结果（声明类型 `OperationSodEvaluator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `operationSodEvaluator` stores the `operation Sod Evaluator`-related state, dependency, configuration, or result of `DefaultAuthorizationService` (declared type `OperationSodEvaluator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `operationSodEvaluator` 时应保持 `DefaultAuthorizationService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `operationSodEvaluator`, preserve `DefaultAuthorizationService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final OperationSodEvaluator operationSodEvaluator;
    /**
     * 字段 `fenceVerifier` 表示 `DefaultAuthorizationService` 中与 `fence Verifier` 相关的状态、依赖、配置或结果（声明类型 `FenceVerifier`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `fenceVerifier` stores the `fence Verifier`-related state, dependency, configuration, or result of `DefaultAuthorizationService` (declared type `FenceVerifier`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `fenceVerifier` 时应保持 `DefaultAuthorizationService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `fenceVerifier`, preserve `DefaultAuthorizationService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final FenceVerifier fenceVerifier;
    /**
     * 字段 `clock` 表示 `DefaultAuthorizationService` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `DefaultAuthorizationService` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `DefaultAuthorizationService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `DefaultAuthorizationService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;

    /**
     * 构造器 `DefaultAuthorizationService` 用于创建并初始化 `DefaultAuthorizationService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `DefaultAuthorizationService` creates and initializes `DefaultAuthorizationService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `DefaultAuthorizationService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `DefaultAuthorizationService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param contextSource 输入参数 `contextSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param operationSodEvaluator 输入参数 `operationSodEvaluator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param fenceVerifier 输入参数 `fenceVerifier`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
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

    /**
     * 方法 `requirePermission` 按照 `DefaultAuthorizationService` 的职责处理输入，完成 `require Permission` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requirePermission` processes its inputs according to `DefaultAuthorizationService`'s responsibility, performs the `require Permission` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requirePermission` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requirePermission`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `decideDataScope` 按照 `DefaultAuthorizationService` 的职责处理输入，完成 `decide Data Scope` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `decideDataScope` processes its inputs according to `DefaultAuthorizationService`'s responsibility, performs the `decide Data Scope` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `decideDataScope` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `decideDataScope`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `decideFields` 按照 `DefaultAuthorizationService` 的职责处理输入，完成 `decide Fields` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `decideFields` processes its inputs according to `DefaultAuthorizationService`'s responsibility, performs the `decide Fields` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `decideFields` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `decideFields`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `checkParticipation` 按照 `DefaultAuthorizationService` 的职责处理输入，完成 `check Participation` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `checkParticipation` processes its inputs according to `DefaultAuthorizationService`'s responsibility, performs the `check Participation` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `checkParticipation` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `checkParticipation`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `verifyFence` 按照 `DefaultAuthorizationService` 的职责处理输入，完成 `verify Fence` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `verifyFence` processes its inputs according to `DefaultAuthorizationService`'s responsibility, performs the `verify Fence` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `verifyFence` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `verifyFence`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `hasPermission` 按照 `DefaultAuthorizationService` 的职责处理输入，完成 `has Permission` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `hasPermission` processes its inputs according to `DefaultAuthorizationService`'s responsibility, performs the `has Permission` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `hasPermission` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `hasPermission`, then continue the business flow using its result, exception, or side effect.
     *
     * @param context 输入参数 `context`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private boolean hasPermission(
            RuntimeAuthorizationContext context,
            String permissionCode
    ) {
        return context.snapshot().permissions().contains(permissionCode);
    }

    /**
     * 方法 `permissionDecision` 按照 `DefaultAuthorizationService` 的职责处理输入，完成 `permission Decision` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `permissionDecision` processes its inputs according to `DefaultAuthorizationService`'s responsibility, performs the `permission Decision` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `permissionDecision` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `permissionDecision`, then continue the business flow using its result, exception, or side effect.
     *
     * @param facts 输入参数 `facts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param decision 输入参数 `decision`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `dataScopeDecision` 按照 `DefaultAuthorizationService` 的职责处理输入，完成 `data Scope Decision` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `dataScopeDecision` processes its inputs according to `DefaultAuthorizationService`'s responsibility, performs the `data Scope Decision` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `dataScopeDecision` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `dataScopeDecision`, then continue the business flow using its result, exception, or side effect.
     *
     * @param facts 输入参数 `facts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param decision 输入参数 `decision`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `fieldDecision` 按照 `DefaultAuthorizationService` 的职责处理输入，完成 `field Decision` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `fieldDecision` processes its inputs according to `DefaultAuthorizationService`'s responsibility, performs the `field Decision` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `fieldDecision` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `fieldDecision`, then continue the business flow using its result, exception, or side effect.
     *
     * @param facts 输入参数 `facts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param decision 输入参数 `decision`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `operationDecision` 按照 `DefaultAuthorizationService` 的职责处理输入，完成 `operation Decision` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `operationDecision` processes its inputs according to `DefaultAuthorizationService`'s responsibility, performs the `operation Decision` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `operationDecision` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `operationDecision`, then continue the business flow using its result, exception, or side effect.
     *
     * @param facts 输入参数 `facts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param decision 输入参数 `decision`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param result 输入参数 `result`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `fenceDecision` 按照 `DefaultAuthorizationService` 的职责处理输入，完成 `fence Decision` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `fenceDecision` processes its inputs according to `DefaultAuthorizationService`'s responsibility, performs the `fence Decision` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `fenceDecision` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `fenceDecision`, then continue the business flow using its result, exception, or side effect.
     *
     * @param facts 输入参数 `facts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param checksum 输入参数 `checksum`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param decision 输入参数 `decision`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param result 输入参数 `result`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
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

    /**
     * 方法 `unavailable` 按照 `DefaultAuthorizationService` 的职责处理输入，完成 `unavailable` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `unavailable` processes its inputs according to `DefaultAuthorizationService`'s responsibility, performs the `unavailable` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `unavailable` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `unavailable`, then continue the business flow using its result, exception, or side effect.
     *
     * @param exception 输入参数 `exception`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private RuntimeFacts unavailable(RuntimeUnavailableException exception) {
        IdentityPrincipal identity = exception.identity();
        return new RuntimeFacts(identity.tenantId(), identity.subject(),
                identity.sessionId(), 0, 0, 0, List.of());
    }

    /**
     * 方法 `facts` 按照 `DefaultAuthorizationService` 的职责处理输入，完成 `facts` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `facts` processes its inputs according to `DefaultAuthorizationService`'s responsibility, performs the `facts` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `facts` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `facts`, then continue the business flow using its result, exception, or side effect.
     *
     * @param context 输入参数 `context`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private RuntimeFacts facts(RuntimeAuthorizationContext context) {
        return new RuntimeFacts(
                context.snapshot().tenantId(), context.snapshot().rbac3UserId(),
                context.snapshot().sessionId(), context.snapshot().authVersion(),
                context.snapshot().contextVersion(), context.snapshot().policyVersion(),
                context.snapshot().activeRoleIds());
    }

    /**
     * 方法 `permissionDecision` 按照 `DefaultAuthorizationService` 的职责处理输入，完成 `permission Decision` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `permissionDecision` processes its inputs according to `DefaultAuthorizationService`'s responsibility, performs the `permission Decision` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `permissionDecision` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `permissionDecision`, then continue the business flow using its result, exception, or side effect.
     *
     * @param context 输入参数 `context`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param decision 输入参数 `decision`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private AuthorizationDecision permissionDecision(
            RuntimeAuthorizationContext context,
            String permissionCode,
            Decision decision,
            String reasonCode) {
        return permissionDecision(facts(context), permissionCode, decision, reasonCode);
    }

    /**
     * 方法 `dataScopeDecision` 按照 `DefaultAuthorizationService` 的职责处理输入，完成 `data Scope Decision` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `dataScopeDecision` processes its inputs according to `DefaultAuthorizationService`'s responsibility, performs the `data Scope Decision` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `dataScopeDecision` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `dataScopeDecision`, then continue the business flow using its result, exception, or side effect.
     *
     * @param context 输入参数 `context`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param decision 输入参数 `decision`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private DataScopeDecision dataScopeDecision(
            RuntimeAuthorizationContext context,
            String permissionCode,
            Decision decision,
            String reasonCode) {
        return dataScopeDecision(facts(context), permissionCode, decision, reasonCode);
    }

    /**
     * 方法 `fieldDecision` 按照 `DefaultAuthorizationService` 的职责处理输入，完成 `field Decision` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `fieldDecision` processes its inputs according to `DefaultAuthorizationService`'s responsibility, performs the `field Decision` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `fieldDecision` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `fieldDecision`, then continue the business flow using its result, exception, or side effect.
     *
     * @param context 输入参数 `context`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param decision 输入参数 `decision`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private FieldPolicyDecision fieldDecision(
            RuntimeAuthorizationContext context,
            FieldPolicyRequest request,
            Decision decision,
            String reasonCode) {
        return fieldDecision(facts(context), request, decision, reasonCode);
    }

    /**
     * 方法 `operationDecision` 按照 `DefaultAuthorizationService` 的职责处理输入，完成 `operation Decision` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `operationDecision` processes its inputs according to `DefaultAuthorizationService`'s responsibility, performs the `operation Decision` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `operationDecision` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `operationDecision`, then continue the business flow using its result, exception, or side effect.
     *
     * @param context 输入参数 `context`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param decision 输入参数 `decision`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param result 输入参数 `result`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private OperationSodDecision operationDecision(
            RuntimeAuthorizationContext context,
            OperationSodRequest request,
            Decision decision,
            OperationSodResult result) {
        return operationDecision(facts(context), request, decision, result);
    }

    /**
     * 方法 `fenceDecision` 按照 `DefaultAuthorizationService` 的职责处理输入，完成 `fence Decision` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `fenceDecision` processes its inputs according to `DefaultAuthorizationService`'s responsibility, performs the `fence Decision` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `fenceDecision` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `fenceDecision`, then continue the business flow using its result, exception, or side effect.
     *
     * @param context 输入参数 `context`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param checksum 输入参数 `checksum`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param decision 输入参数 `decision`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param result 输入参数 `result`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private AuthorizationFenceDecision fenceDecision(
            RuntimeAuthorizationContext context,
            AuthorizationFenceRequest request,
            String checksum,
            Decision decision,
            FenceResult result) {
        return fenceDecision(facts(context), request, checksum, decision, result);
    }

    /**
     * 类型 `RuntimeFacts` 位于 `DefaultAuthorizationService` 内，是记录类型，用于承载 `Runtime Facts` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RuntimeFacts` is a record inside `DefaultAuthorizationService` and carries the responsibility, state, or contract for `Runtime Facts`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RuntimeFacts` 作为 `DefaultAuthorizationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RuntimeFacts` as the responsibility boundary of `DefaultAuthorizationService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param rbac3UserId 记录组件 `rbac3UserId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `rbac3UserId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param contextVersion 记录组件 `contextVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `contextVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param activeRoleIds 记录组件 `activeRoleIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `activeRoleIds` carries constructor data whose meaning is defined by the record contract.
     */
    private record RuntimeFacts(
            /**
             * 字段 `tenantId` 表示 `RuntimeFacts` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `RuntimeFacts` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `RuntimeFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `RuntimeFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `rbac3UserId` 表示 `RuntimeFacts` 中与 `rbac3 User Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `rbac3UserId` stores the `rbac3 User Id`-related state, dependency, configuration, or result of `RuntimeFacts` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `rbac3UserId` 时应保持 `RuntimeFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `rbac3UserId`, preserve `RuntimeFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            String rbac3UserId,
            /**
             * 字段 `sessionId` 表示 `RuntimeFacts` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `RuntimeFacts` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `RuntimeFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `RuntimeFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `authVersion` 表示 `RuntimeFacts` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `RuntimeFacts` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `RuntimeFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `RuntimeFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `contextVersion` 表示 `RuntimeFacts` 中与 `context Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `contextVersion` stores the `context Version`-related state, dependency, configuration, or result of `RuntimeFacts` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `contextVersion` 时应保持 `RuntimeFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `contextVersion`, preserve `RuntimeFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            long contextVersion,
            /**
             * 字段 `policyVersion` 表示 `RuntimeFacts` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `RuntimeFacts` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `RuntimeFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `RuntimeFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `activeRoleIds` 表示 `RuntimeFacts` 中与 `active Role Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `activeRoleIds` stores the `active Role Ids`-related state, dependency, configuration, or result of `RuntimeFacts` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `activeRoleIds` 时应保持 `RuntimeFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `activeRoleIds`, preserve `RuntimeFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> activeRoleIds) {
    }

    /**
     * 类型 `AuthorizationDeniedException` 位于 `DefaultAuthorizationService` 内，是类型，用于承载 `Authorization Denied Exception` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuthorizationDeniedException` is a type inside `DefaultAuthorizationService` and carries the responsibility, state, or contract for `Authorization Denied Exception`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuthorizationDeniedException` 作为 `DefaultAuthorizationService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuthorizationDeniedException` as the responsibility boundary of `DefaultAuthorizationService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public static final class AuthorizationDeniedException extends RuntimeException {

        /**
         * 字段 `reasonCode` 表示 `AuthorizationDeniedException` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `AuthorizationDeniedException` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `AuthorizationDeniedException` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `AuthorizationDeniedException`'s lifecycle, immutability, and thread-safety constraints.
         */
        private final String reasonCode;

        /**
         * 构造器 `AuthorizationDeniedException` 用于创建并初始化 `AuthorizationDeniedException` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `AuthorizationDeniedException` creates and initializes `AuthorizationDeniedException`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `AuthorizationDeniedException` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `AuthorizationDeniedException`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param permissionCode 输入参数 `permissionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public AuthorizationDeniedException(String reasonCode, String permissionCode) {
            super(reasonCode + ": " + permissionCode);
            this.reasonCode = reasonCode;
        }

        /**
         * 方法 `reasonCode` 按照 `AuthorizationDeniedException` 的职责处理输入，完成 `reason Code` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `reasonCode` processes its inputs according to `AuthorizationDeniedException`'s responsibility, performs the `reason Code` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `reasonCode` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `reasonCode`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        public String reasonCode() {
            return reasonCode;
        }
    }
}
