package top.egon.cola.platform.rbac3.admin.participation.application;

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

/**
 * 类型 `ParticipationFacade` 位于当前包内，是类型，用于承载 `Participation Facade` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ParticipationFacade` is a type in its package and carries the responsibility, state, or contract for `Participation Facade`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Enforces application binding and same-object duty separation before append.
 */
public final class ParticipationFacade {

    /**
     * 字段 `ruleSource` 表示 `ParticipationFacade` 中与 `rule Source` 相关的状态、依赖、配置或结果（声明类型 `OperationSodRuleSource`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `ruleSource` stores the `rule Source`-related state, dependency, configuration, or result of `ParticipationFacade` (declared type `OperationSodRuleSource`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `ruleSource` 时应保持 `ParticipationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `ruleSource`, preserve `ParticipationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final OperationSodRuleSource ruleSource;
    /**
     * 字段 `store` 表示 `ParticipationFacade` 中与 `store` 相关的状态、依赖、配置或结果（声明类型 `ParticipationStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `store` stores the `store`-related state, dependency, configuration, or result of `ParticipationFacade` (declared type `ParticipationStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `store` 时应保持 `ParticipationFacade` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `store`, preserve `ParticipationFacade`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ParticipationStore store;
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
            OperationSodRuleSource ruleSource,
            ParticipationStore store,
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
    public RecordResult record(
            ServiceIdentityPrincipal caller,
            String tenantId,
            BusinessParticipationCommand command) {
        requireBinding(caller, tenantId, command.applicationCode());
        List<PriorActionRule> rules = ruleSource.rules(
                tenantId, command.applicationCode(), command.businessResource(),
                command.actionCode(), clock.instant());
        ParticipationRecord record = new ParticipationRecord(
                tenantId, command.applicationCode(), command.businessResource(),
                command.businessId(), command.actorUserId(), command.actionCode(),
                command.businessEventId(), command.occurredAt(), command.traceId(),
                digest(tenantId, command));
        AppendResult result = store.appendAtomically(record, rules);
        if (!result.conflictingEvidenceIds().isEmpty()) {
            throw new Rbac3RuleViolation(
                    "OPERATION_SOD_VIOLATION", result.conflictingEvidenceIds());
        }
        return new RecordResult(
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
    public ConflictDecision conflicts(
            ServiceIdentityPrincipal caller,
            String tenantId,
            ConflictQuery query) {
        requireBinding(caller, tenantId, query.applicationCode());
        List<PriorActionRule> rules = ruleSource.rules(
                tenantId, query.applicationCode(), query.businessResource(),
                query.requestedAction(), clock.instant());
        Instant lookbackFrom = rules.stream()
                .map(PriorActionRule::lookbackFrom)
                .min(Instant::compareTo)
                .orElse(clock.instant());
        List<ParticipationFact> facts = store.find(query, tenantId, lookbackFrom).stream()
                .filter(fact -> rules.stream().anyMatch(rule ->
                        rule.actionCode().equals(fact.actionCode())
                                && !fact.occurredAt().isBefore(rule.lookbackFrom())))
                .toList();
        Set<String> forbidden = rules.stream()
                .map(PriorActionRule::actionCode)
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
        return new ConflictDecision(
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

    /**
     * 类型 `OperationSodRuleSource` 位于 `ParticipationFacade` 内，是接口，用于承载 `Operation Sod Rule Source` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `OperationSodRuleSource` is an interface inside `ParticipationFacade` and carries the responsibility, state, or contract for `Operation Sod Rule Source`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `OperationSodRuleSource` 作为 `ParticipationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `OperationSodRuleSource` as the responsibility boundary of `ParticipationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface OperationSodRuleSource {
        /**
         * 方法 `rules` 按照 `OperationSodRuleSource` 的职责处理输入，完成 `rules` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `rules` processes its inputs according to `OperationSodRuleSource`'s responsibility, performs the `rules` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `rules` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `rules`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applicationCode 输入参数 `applicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param businessResource 输入参数 `businessResource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param laterAction 输入参数 `laterAction`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param at 输入参数 `at`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<PriorActionRule> rules(
                String tenantId,
                String applicationCode,
                String businessResource,
                String laterAction,
                Instant at);
    }

    /**
     * 类型 `ParticipationStore` 位于 `ParticipationFacade` 内，是接口，用于承载 `Participation Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ParticipationStore` is an interface inside `ParticipationFacade` and carries the responsibility, state, or contract for `Participation Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ParticipationStore` 作为 `ParticipationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ParticipationStore` as the responsibility boundary of `ParticipationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface ParticipationStore {
        /**
         * 方法 `appendAtomically` 按照 `ParticipationStore` 的职责处理输入，完成 `append Atomically` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `appendAtomically` processes its inputs according to `ParticipationStore`'s responsibility, performs the `append Atomically` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `appendAtomically` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `appendAtomically`, then continue the business flow using its result, exception, or side effect.
         *
         * @param record 输入参数 `record`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param rules 输入参数 `rules`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        AppendResult appendAtomically(
                ParticipationRecord record,
                List<PriorActionRule> rules);

        /**
         * 方法 `find` 按照 `ParticipationStore` 的职责处理输入，完成 `find` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `find` processes its inputs according to `ParticipationStore`'s responsibility, performs the `find` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `find` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `find`, then continue the business flow using its result, exception, or side effect.
         *
         * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param lookbackFrom 输入参数 `lookbackFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        List<ParticipationFact> find(
                ConflictQuery query,
                String tenantId,
                Instant lookbackFrom);
    }

    /**
     * 类型 `ParticipationRecord` 位于 `ParticipationFacade` 内，是记录类型，用于承载 `Participation Record` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ParticipationRecord` is a record inside `ParticipationFacade` and carries the responsibility, state, or contract for `Participation Record`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ParticipationRecord` 作为 `ParticipationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ParticipationRecord` as the responsibility boundary of `ParticipationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationCode 记录组件 `applicationCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationCode` carries constructor data whose meaning is defined by the record contract.
     * @param businessResource 记录组件 `businessResource` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessResource` carries constructor data whose meaning is defined by the record contract.
     * @param businessId 记录组件 `businessId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessId` carries constructor data whose meaning is defined by the record contract.
     * @param actorUserId 记录组件 `actorUserId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorUserId` carries constructor data whose meaning is defined by the record contract.
     * @param actionCode 记录组件 `actionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actionCode` carries constructor data whose meaning is defined by the record contract.
     * @param businessEventId 记录组件 `businessEventId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessEventId` carries constructor data whose meaning is defined by the record contract.
     * @param occurredAt 记录组件 `occurredAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `occurredAt` carries constructor data whose meaning is defined by the record contract.
     * @param traceId 记录组件 `traceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `traceId` carries constructor data whose meaning is defined by the record contract.
     * @param payloadDigest 记录组件 `payloadDigest` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `payloadDigest` carries constructor data whose meaning is defined by the record contract.
     */
    public record ParticipationRecord(
            /**
             * 字段 `tenantId` 表示 `ParticipationRecord` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ParticipationRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ParticipationRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ParticipationRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `applicationCode` 表示 `ParticipationRecord` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `ParticipationRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `ParticipationRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `ParticipationRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationCode,
            /**
             * 字段 `businessResource` 表示 `ParticipationRecord` 中与 `business Resource` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessResource` stores the `business Resource`-related state, dependency, configuration, or result of `ParticipationRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessResource` 时应保持 `ParticipationRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessResource`, preserve `ParticipationRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessResource,
            /**
             * 字段 `businessId` 表示 `ParticipationRecord` 中与 `business Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessId` stores the `business Id`-related state, dependency, configuration, or result of `ParticipationRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessId` 时应保持 `ParticipationRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessId`, preserve `ParticipationRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessId,
            /**
             * 字段 `actorUserId` 表示 `ParticipationRecord` 中与 `actor User Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorUserId` stores the `actor User Id`-related state, dependency, configuration, or result of `ParticipationRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorUserId` 时应保持 `ParticipationRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorUserId`, preserve `ParticipationRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorUserId,
            /**
             * 字段 `actionCode` 表示 `ParticipationRecord` 中与 `action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actionCode` stores the `action Code`-related state, dependency, configuration, or result of `ParticipationRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actionCode` 时应保持 `ParticipationRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actionCode`, preserve `ParticipationRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actionCode,
            /**
             * 字段 `businessEventId` 表示 `ParticipationRecord` 中与 `business Event Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessEventId` stores the `business Event Id`-related state, dependency, configuration, or result of `ParticipationRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessEventId` 时应保持 `ParticipationRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessEventId`, preserve `ParticipationRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessEventId,
            /**
             * 字段 `occurredAt` 表示 `ParticipationRecord` 中与 `occurred At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `occurredAt` stores the `occurred At`-related state, dependency, configuration, or result of `ParticipationRecord` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `occurredAt` 时应保持 `ParticipationRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `occurredAt`, preserve `ParticipationRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant occurredAt,
            /**
             * 字段 `traceId` 表示 `ParticipationRecord` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `ParticipationRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `traceId` 时应保持 `ParticipationRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `traceId`, preserve `ParticipationRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String traceId,
            /**
             * 字段 `payloadDigest` 表示 `ParticipationRecord` 中与 `payload Digest` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `payloadDigest` stores the `payload Digest`-related state, dependency, configuration, or result of `ParticipationRecord` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `payloadDigest` 时应保持 `ParticipationRecord` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `payloadDigest`, preserve `ParticipationRecord`'s lifecycle, immutability, and thread-safety constraints.
             */
            String payloadDigest) {
    }

    /**
     * 类型 `ParticipationFact` 位于 `ParticipationFacade` 内，是记录类型，用于承载 `Participation Fact` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ParticipationFact` is a record inside `ParticipationFacade` and carries the responsibility, state, or contract for `Participation Fact`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ParticipationFact` 作为 `ParticipationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ParticipationFact` as the responsibility boundary of `ParticipationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param participationId 记录组件 `participationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `participationId` carries constructor data whose meaning is defined by the record contract.
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationCode 记录组件 `applicationCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationCode` carries constructor data whose meaning is defined by the record contract.
     * @param businessResource 记录组件 `businessResource` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessResource` carries constructor data whose meaning is defined by the record contract.
     * @param businessId 记录组件 `businessId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessId` carries constructor data whose meaning is defined by the record contract.
     * @param actorUserId 记录组件 `actorUserId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorUserId` carries constructor data whose meaning is defined by the record contract.
     * @param actionCode 记录组件 `actionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actionCode` carries constructor data whose meaning is defined by the record contract.
     * @param businessEventId 记录组件 `businessEventId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessEventId` carries constructor data whose meaning is defined by the record contract.
     * @param occurredAt 记录组件 `occurredAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `occurredAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record ParticipationFact(
            /**
             * 字段 `participationId` 表示 `ParticipationFact` 中与 `participation Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `participationId` stores the `participation Id`-related state, dependency, configuration, or result of `ParticipationFact` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `participationId` 时应保持 `ParticipationFact` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `participationId`, preserve `ParticipationFact`'s lifecycle, immutability, and thread-safety constraints.
             */
            String participationId,
            /**
             * 字段 `tenantId` 表示 `ParticipationFact` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ParticipationFact` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ParticipationFact` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ParticipationFact`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `applicationCode` 表示 `ParticipationFact` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `ParticipationFact` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `ParticipationFact` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `ParticipationFact`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationCode,
            /**
             * 字段 `businessResource` 表示 `ParticipationFact` 中与 `business Resource` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessResource` stores the `business Resource`-related state, dependency, configuration, or result of `ParticipationFact` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessResource` 时应保持 `ParticipationFact` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessResource`, preserve `ParticipationFact`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessResource,
            /**
             * 字段 `businessId` 表示 `ParticipationFact` 中与 `business Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessId` stores the `business Id`-related state, dependency, configuration, or result of `ParticipationFact` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessId` 时应保持 `ParticipationFact` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessId`, preserve `ParticipationFact`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessId,
            /**
             * 字段 `actorUserId` 表示 `ParticipationFact` 中与 `actor User Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorUserId` stores the `actor User Id`-related state, dependency, configuration, or result of `ParticipationFact` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorUserId` 时应保持 `ParticipationFact` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorUserId`, preserve `ParticipationFact`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorUserId,
            /**
             * 字段 `actionCode` 表示 `ParticipationFact` 中与 `action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actionCode` stores the `action Code`-related state, dependency, configuration, or result of `ParticipationFact` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actionCode` 时应保持 `ParticipationFact` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actionCode`, preserve `ParticipationFact`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actionCode,
            /**
             * 字段 `businessEventId` 表示 `ParticipationFact` 中与 `business Event Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessEventId` stores the `business Event Id`-related state, dependency, configuration, or result of `ParticipationFact` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessEventId` 时应保持 `ParticipationFact` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessEventId`, preserve `ParticipationFact`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessEventId,
            /**
             * 字段 `occurredAt` 表示 `ParticipationFact` 中与 `occurred At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `occurredAt` stores the `occurred At`-related state, dependency, configuration, or result of `ParticipationFact` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `occurredAt` 时应保持 `ParticipationFact` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `occurredAt`, preserve `ParticipationFact`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant occurredAt) {
    }

    /**
     * 类型 `PriorActionRule` 位于 `ParticipationFacade` 内，是记录类型，用于承载 `Prior Action Rule` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `PriorActionRule` is a record inside `ParticipationFacade` and carries the responsibility, state, or contract for `Prior Action Rule`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `PriorActionRule` 作为 `ParticipationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `PriorActionRule` as the responsibility boundary of `ParticipationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param ruleId 记录组件 `ruleId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `ruleId` carries constructor data whose meaning is defined by the record contract.
     * @param actionCode 记录组件 `actionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actionCode` carries constructor data whose meaning is defined by the record contract.
     * @param lookbackFrom 记录组件 `lookbackFrom` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `lookbackFrom` carries constructor data whose meaning is defined by the record contract.
     */
    public record PriorActionRule(
            /**
             * 字段 `ruleId` 表示 `PriorActionRule` 中与 `rule Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `ruleId` stores the `rule Id`-related state, dependency, configuration, or result of `PriorActionRule` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `ruleId` 时应保持 `PriorActionRule` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `ruleId`, preserve `PriorActionRule`'s lifecycle, immutability, and thread-safety constraints.
             */
            String ruleId,
            /**
             * 字段 `actionCode` 表示 `PriorActionRule` 中与 `action Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actionCode` stores the `action Code`-related state, dependency, configuration, or result of `PriorActionRule` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actionCode` 时应保持 `PriorActionRule` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actionCode`, preserve `PriorActionRule`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actionCode,
            /**
             * 字段 `lookbackFrom` 表示 `PriorActionRule` 中与 `lookback From` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `lookbackFrom` stores the `lookback From`-related state, dependency, configuration, or result of `PriorActionRule` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `lookbackFrom` 时应保持 `PriorActionRule` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `lookbackFrom`, preserve `PriorActionRule`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant lookbackFrom) {
        /**
         * 构造器 `PriorActionRule` 用于创建并初始化 `PriorActionRule` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `PriorActionRule` creates and initializes `PriorActionRule`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `PriorActionRule` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `PriorActionRule`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param ruleId 输入参数 `ruleId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actionCode 输入参数 `actionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param lookbackFrom 输入参数 `lookbackFrom`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public PriorActionRule {
            if (ruleId == null || ruleId.isBlank()) {
                throw new IllegalArgumentException("ruleId is required");
            }
            if (actionCode == null || actionCode.isBlank()) {
                throw new IllegalArgumentException("actionCode is required");
            }
            lookbackFrom = Objects.requireNonNull(lookbackFrom, "lookbackFrom");
        }
    }

    /**
     * 类型 `AppendResult` 位于 `ParticipationFacade` 内，是记录类型，用于承载 `Append Result` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AppendResult` is a record inside `ParticipationFacade` and carries the responsibility, state, or contract for `Append Result`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AppendResult` 作为 `ParticipationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AppendResult` as the responsibility boundary of `ParticipationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param created 记录组件 `created` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `created` carries constructor data whose meaning is defined by the record contract.
     * @param participationId 记录组件 `participationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `participationId` carries constructor data whose meaning is defined by the record contract.
     * @param conflictingEvidenceIds 记录组件 `conflictingEvidenceIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `conflictingEvidenceIds` carries constructor data whose meaning is defined by the record contract.
     */
    public record AppendResult(
            /**
             * 字段 `created` 表示 `AppendResult` 中与 `created` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `created` stores the `created`-related state, dependency, configuration, or result of `AppendResult` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `created` 时应保持 `AppendResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `created`, preserve `AppendResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean created,
            /**
             * 字段 `participationId` 表示 `AppendResult` 中与 `participation Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `participationId` stores the `participation Id`-related state, dependency, configuration, or result of `AppendResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `participationId` 时应保持 `AppendResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `participationId`, preserve `AppendResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String participationId,
            /**
             * 字段 `conflictingEvidenceIds` 表示 `AppendResult` 中与 `conflicting Evidence Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `conflictingEvidenceIds` stores the `conflicting Evidence Ids`-related state, dependency, configuration, or result of `AppendResult` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `conflictingEvidenceIds` 时应保持 `AppendResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `conflictingEvidenceIds`, preserve `AppendResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> conflictingEvidenceIds) {
        /**
         * 构造器 `AppendResult` 用于创建并初始化 `AppendResult` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `AppendResult` creates and initializes `AppendResult`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `AppendResult` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `AppendResult`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param created 输入参数 `created`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param participationId 输入参数 `participationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param conflictingEvidenceIds 输入参数 `conflictingEvidenceIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public AppendResult {
            conflictingEvidenceIds = List.copyOf(conflictingEvidenceIds);
        }
    }

    /**
     * 类型 `RecordResult` 位于 `ParticipationFacade` 内，是记录类型，用于承载 `Record Result` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RecordResult` is a record inside `ParticipationFacade` and carries the responsibility, state, or contract for `Record Result`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RecordResult` 作为 `ParticipationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RecordResult` as the responsibility boundary of `ParticipationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param created 记录组件 `created` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `created` carries constructor data whose meaning is defined by the record contract.
     * @param participationId 记录组件 `participationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `participationId` carries constructor data whose meaning is defined by the record contract.
     * @param reasonCode 记录组件 `reasonCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reasonCode` carries constructor data whose meaning is defined by the record contract.
     */
    public record RecordResult(
            /**
             * 字段 `created` 表示 `RecordResult` 中与 `created` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `created` stores the `created`-related state, dependency, configuration, or result of `RecordResult` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `created` 时应保持 `RecordResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `created`, preserve `RecordResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean created,
            /**
             * 字段 `participationId` 表示 `RecordResult` 中与 `participation Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `participationId` stores the `participation Id`-related state, dependency, configuration, or result of `RecordResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `participationId` 时应保持 `RecordResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `participationId`, preserve `RecordResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String participationId,
            /**
             * 字段 `reasonCode` 表示 `RecordResult` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `RecordResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `RecordResult` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `RecordResult`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode) {
    }

    /**
     * 类型 `ConflictQuery` 位于 `ParticipationFacade` 内，是记录类型，用于承载 `Conflict Query` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ConflictQuery` is a record inside `ParticipationFacade` and carries the responsibility, state, or contract for `Conflict Query`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ConflictQuery` 作为 `ParticipationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ConflictQuery` as the responsibility boundary of `ParticipationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param applicationCode 记录组件 `applicationCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationCode` carries constructor data whose meaning is defined by the record contract.
     * @param businessResource 记录组件 `businessResource` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessResource` carries constructor data whose meaning is defined by the record contract.
     * @param businessId 记录组件 `businessId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `businessId` carries constructor data whose meaning is defined by the record contract.
     * @param actorUserId 记录组件 `actorUserId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorUserId` carries constructor data whose meaning is defined by the record contract.
     * @param requestedAction 记录组件 `requestedAction` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requestedAction` carries constructor data whose meaning is defined by the record contract.
     */
    public record ConflictQuery(
            /**
             * 字段 `applicationCode` 表示 `ConflictQuery` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `ConflictQuery` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `ConflictQuery` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `ConflictQuery`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationCode,
            /**
             * 字段 `businessResource` 表示 `ConflictQuery` 中与 `business Resource` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessResource` stores the `business Resource`-related state, dependency, configuration, or result of `ConflictQuery` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessResource` 时应保持 `ConflictQuery` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessResource`, preserve `ConflictQuery`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessResource,
            /**
             * 字段 `businessId` 表示 `ConflictQuery` 中与 `business Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `businessId` stores the `business Id`-related state, dependency, configuration, or result of `ConflictQuery` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `businessId` 时应保持 `ConflictQuery` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `businessId`, preserve `ConflictQuery`'s lifecycle, immutability, and thread-safety constraints.
             */
            String businessId,
            /**
             * 字段 `actorUserId` 表示 `ConflictQuery` 中与 `actor User Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorUserId` stores the `actor User Id`-related state, dependency, configuration, or result of `ConflictQuery` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorUserId` 时应保持 `ConflictQuery` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorUserId`, preserve `ConflictQuery`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorUserId,
            /**
             * 字段 `requestedAction` 表示 `ConflictQuery` 中与 `requested Action` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requestedAction` stores the `requested Action`-related state, dependency, configuration, or result of `ConflictQuery` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requestedAction` 时应保持 `ConflictQuery` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requestedAction`, preserve `ConflictQuery`'s lifecycle, immutability, and thread-safety constraints.
             */
            String requestedAction) {
    }

    /**
     * 类型 `ConflictDecision` 位于 `ParticipationFacade` 内，是记录类型，用于承载 `Conflict Decision` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ConflictDecision` is a record inside `ParticipationFacade` and carries the responsibility, state, or contract for `Conflict Decision`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ConflictDecision` 作为 `ParticipationFacade` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ConflictDecision` as the responsibility boundary of `ParticipationFacade`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param allowed 记录组件 `allowed` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `allowed` carries constructor data whose meaning is defined by the record contract.
     * @param reasonCode 记录组件 `reasonCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reasonCode` carries constructor data whose meaning is defined by the record contract.
     * @param evidenceIds 记录组件 `evidenceIds` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `evidenceIds` carries constructor data whose meaning is defined by the record contract.
     * @param conflictingPriorActions 记录组件 `conflictingPriorActions` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `conflictingPriorActions` carries constructor data whose meaning is defined by the record contract.
     */
    public record ConflictDecision(
            /**
             * 字段 `allowed` 表示 `ConflictDecision` 中与 `allowed` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `allowed` stores the `allowed`-related state, dependency, configuration, or result of `ConflictDecision` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `allowed` 时应保持 `ConflictDecision` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `allowed`, preserve `ConflictDecision`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean allowed,
            /**
             * 字段 `reasonCode` 表示 `ConflictDecision` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `ConflictDecision` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `ConflictDecision` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `ConflictDecision`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode,
            /**
             * 字段 `evidenceIds` 表示 `ConflictDecision` 中与 `evidence Ids` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `evidenceIds` stores the `evidence Ids`-related state, dependency, configuration, or result of `ConflictDecision` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `evidenceIds` 时应保持 `ConflictDecision` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `evidenceIds`, preserve `ConflictDecision`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<String> evidenceIds,
            /**
             * 字段 `conflictingPriorActions` 表示 `ConflictDecision` 中与 `conflicting Prior Actions` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `conflictingPriorActions` stores the `conflicting Prior Actions`-related state, dependency, configuration, or result of `ConflictDecision` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `conflictingPriorActions` 时应保持 `ConflictDecision` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `conflictingPriorActions`, preserve `ConflictDecision`'s lifecycle, immutability, and thread-safety constraints.
             */
            Set<String> conflictingPriorActions) {
    }
}
