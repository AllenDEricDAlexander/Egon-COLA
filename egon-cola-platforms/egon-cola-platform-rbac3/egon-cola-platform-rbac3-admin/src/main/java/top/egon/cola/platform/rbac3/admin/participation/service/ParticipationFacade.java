package top.egon.cola.platform.rbac3.admin.participation.service;

import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.participation.BusinessParticipationCommand;
import top.egon.cola.platform.rbac3.core.participation.OperationSodSpecification;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import top.egon.cola.platform.rbac3.admin.participation.repository.OperationSodRuleRepository;
import top.egon.cola.platform.rbac3.admin.participation.repository.ParticipationRepository;
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.ParticipationRecordVO;
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.ParticipationFactVO;
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.PriorActionRuleVO;
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.AppendResultVO;
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.RecordResultVO;
import top.egon.cola.platform.rbac3.admin.participation.domain.dto.ConflictQueryDTO;
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.ConflictDecisionVO;

/**
 * 类型 `ParticipationFacade` 位于当前包内，是类型，用于承载 `Participation Facade` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ParticipationFacade` is a type in its package and carries the responsibility, state, or contract for `Participation Facade`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Enforces application binding and same-object duty separation before append.
 */
public final class ParticipationFacade {

    /**
     * 字段 `ruleSource` 表示 `ParticipationFacade` 中与 `rule Source` 相关的状态、依赖、配置或结果（声明类型 `OperationSodRuleRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `ruleSource` stores the `rule Source`-related state, dependency, configuration, or result of `ParticipationFacade` (declared type `OperationSodRuleRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `ruleSource` 时应保持 `ParticipationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `ruleSource`, preserve `ParticipationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final OperationSodRuleRepository ruleSource;
    /**
     * 字段 `store` 表示 `ParticipationFacade` 中与 `store` 相关的状态、依赖、配置或结果（声明类型 `ParticipationRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `store` stores the `store`-related state, dependency, configuration, or result of `ParticipationFacade` (declared type `ParticipationRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `store` 时应保持 `ParticipationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `store`, preserve `ParticipationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ParticipationRepository store;
    /**
     * 字段 `clock` 表示 `ParticipationFacade` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `ParticipationFacade` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `ParticipationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `ParticipationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;
    /**
     * 字段 `specification` 表示 `ParticipationFacade` 中与 `specification` 相关的状态、依赖、配置或结果（声明类型 `OperationSodSpecification`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `specification` stores the `specification`-related state, dependency, configuration, or result of `ParticipationFacade` (declared type `OperationSodSpecification`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `specification` 时应保持 `ParticipationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `specification`, preserve `ParticipationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final OperationSodSpecification specification = new OperationSodSpecification();

    /**
     * 构造器 `ParticipationFacade` 用于创建并初始化 `ParticipationFacade` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ParticipationFacade` creates and initializes `ParticipationFacade`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ParticipationFacade` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ParticipationFacade`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param ruleSource 输入参数 `ruleSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param store 输入参数 `store`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ParticipationFacade(
            OperationSodRuleRepository ruleSource,
            ParticipationRepository store,
            Clock clock) {
        this.ruleSource = Objects.requireNonNull(ruleSource, "ruleSource");
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 方法 `record` 按照 `ParticipationFacade` 的职责处理输入，完成 `record` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `record` processes its inputs according to `ParticipationFacade`'s responsibility, performs the `record` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `record` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `record`, then continue the business flow using its result, exception, or side effect.
     *
     * @param caller 输入参数 `caller`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RecordResultVO record(
            ServiceIdentityPrincipal caller,
            String tenantId,
            BusinessParticipationCommand command) {
        requireBinding(caller, tenantId, command.applicationCode());
        List<PriorActionRuleVO> rules = ruleSource.rules(
                tenantId, command.applicationCode(), command.businessResource(),
                command.actionCode(), clock.instant());
        ParticipationRecordVO record = new ParticipationRecordVO(
                tenantId, command.applicationCode(), command.businessResource(),
                command.businessId(), command.actorUserId(), command.actionCode(),
                command.businessEventId(), command.occurredAt(), command.traceId(),
                digest(tenantId, command));
        AppendResultVO result = store.appendAtomically(record, rules);
        if (!result.conflictingEvidenceIds().isEmpty()) {
            throw new Rbac3RuleViolation(
                    "OPERATION_SOD_VIOLATION", result.conflictingEvidenceIds());
        }
        return new RecordResultVO(
                result.created(), result.participationId(),
                result.created() ? "CREATED" : "IDEMPOTENT_REPLAY");
    }

    /**
     * 方法 `conflicts` 按照 `ParticipationFacade` 的职责处理输入，完成 `conflicts` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `conflicts` processes its inputs according to `ParticipationFacade`'s responsibility, performs the `conflicts` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `conflicts` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `conflicts`, then continue the business flow using its result, exception, or side effect.
     *
     * @param caller 输入参数 `caller`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public ConflictDecisionVO conflicts(
            ServiceIdentityPrincipal caller,
            String tenantId,
            ConflictQueryDTO query) {
        requireBinding(caller, tenantId, query.applicationCode());
        List<PriorActionRuleVO> rules = ruleSource.rules(
                tenantId, query.applicationCode(), query.businessResource(),
                query.requestedAction(), clock.instant());
        Instant lookbackFrom = rules.stream()
                .map(PriorActionRuleVO::lookbackFrom)
                .min(Instant::compareTo)
                .orElse(clock.instant());
        List<ParticipationFactVO> facts = store.find(query, tenantId, lookbackFrom).stream()
                .filter(fact -> rules.stream().anyMatch(rule ->
                        rule.actionCode().equals(fact.actionCode())
                                && !fact.occurredAt().isBefore(rule.lookbackFrom())))
                .toList();
        Set<String> forbidden = rules.stream()
                .map(PriorActionRuleVO::actionCode)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        var coreFacts = facts.stream()
                .map(fact -> new OperationSodSpecification.ParticipationFact(
                        fact.participationId(), fact.businessResource(), fact.businessId(),
                        fact.actorUserId(), fact.actionCode()))
                .toList();
        var result = specification.evaluate(
                query.businessResource(), query.businessId(), query.actorUserId(),
                query.requestedAction(), coreFacts,
                Map.of(query.requestedAction(), forbidden));
        return new ConflictDecisionVO(
                result.allowed(), result.reasonCode(), result.evidenceIds(), forbidden);
    }

    /**
     * 方法 `requireBinding` 按照 `ParticipationFacade` 的职责处理输入，完成 `require Binding` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireBinding` processes its inputs according to `ParticipationFacade`'s responsibility, performs the `require Binding` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireBinding` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireBinding`, then continue the business flow using its result, exception, or side effect.
     *
     * @param caller 输入参数 `caller`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param applicationCode 输入参数 `applicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void requireBinding(
            ServiceIdentityPrincipal caller,
            String tenantId,
            String applicationCode) {
        Objects.requireNonNull(caller, "caller");
        if (!caller.tenantId().equals(tenantId)) {
            throw new Rbac3RuleViolation("SERVICE_IDENTITY_DENIED");
        }
        if (!caller.sourceAppCode().equals(applicationCode)) {
            throw new Rbac3RuleViolation("APPLICATION_BINDING_DENIED");
        }
    }

    /**
     * 方法 `digest` 按照 `ParticipationFacade` 的职责处理输入，完成 `digest` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `digest` processes its inputs according to `ParticipationFacade`'s responsibility, performs the `digest` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `digest` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `digest`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String digest(String tenantId, BusinessParticipationCommand command) {
        String canonical = String.join("\u001f",
                tenantId, command.applicationCode(), command.businessResource(),
                command.businessId(), command.actorUserId(), command.actionCode(),
                command.businessEventId(), command.occurredAt().toString(), command.traceId());
        try {
            return "sha256:" + HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }









    }
