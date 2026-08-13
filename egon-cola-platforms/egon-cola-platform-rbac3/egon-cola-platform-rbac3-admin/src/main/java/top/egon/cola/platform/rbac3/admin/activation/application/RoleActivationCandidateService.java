package top.egon.cola.platform.rbac3.admin.activation.application;

import top.egon.cola.platform.rbac3.contract.activation.RoleActivationCandidate;
import top.egon.cola.platform.rbac3.contract.activation.RoleActivationCandidateView;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.rbac3.core.activation.DsdSetFact;
import top.egon.cola.platform.rbac3.core.activation.EligibleAssignmentFact;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationCandidateResolver;
import top.egon.cola.platform.rbac3.core.activation.UniqueActivationRootSpecification;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 类型 `RoleActivationCandidateService` 位于当前包内，是类型，用于承载 `Role Activation Candidate Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RoleActivationCandidateService` is a type in its package and carries the responsibility, state, or contract for `Role Activation Candidate Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Converts assignment and hierarchy facts into deterministic activation candidates.
 */
public final class RoleActivationCandidateService {

    /**
     * 字段 `factSource` 表示 `RoleActivationCandidateService` 中与 `fact Source` 相关的状态、依赖、配置或结果（声明类型 `ActivationFactSource`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `factSource` stores the `fact Source`-related state, dependency, configuration, or result of `RoleActivationCandidateService` (declared type `ActivationFactSource`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `factSource` 时应保持 `RoleActivationCandidateService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `factSource`, preserve `RoleActivationCandidateService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ActivationFactSource factSource;
    /**
     * 字段 `candidateResolver` 表示 `RoleActivationCandidateService` 中与 `candidate Resolver` 相关的状态、依赖、配置或结果（声明类型 `RoleActivationCandidateResolver`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `candidateResolver` stores the `candidate Resolver`-related state, dependency, configuration, or result of `RoleActivationCandidateService` (declared type `RoleActivationCandidateResolver`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `candidateResolver` 时应保持 `RoleActivationCandidateService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `candidateResolver`, preserve `RoleActivationCandidateService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleActivationCandidateResolver candidateResolver;

    /**
     * 构造器 `RoleActivationCandidateService` 用于创建并初始化 `RoleActivationCandidateService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RoleActivationCandidateService` creates and initializes `RoleActivationCandidateService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RoleActivationCandidateService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RoleActivationCandidateService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param factSource 输入参数 `factSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RoleActivationCandidateService(ActivationFactSource factSource) {
        this(factSource, new RoleActivationCandidateResolver());
    }

    /**
     * 构造器 `RoleActivationCandidateService` 用于创建并初始化 `RoleActivationCandidateService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RoleActivationCandidateService` creates and initializes `RoleActivationCandidateService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RoleActivationCandidateService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RoleActivationCandidateService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param factSource 输入参数 `factSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param candidateResolver 输入参数 `candidateResolver`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    RoleActivationCandidateService(
            ActivationFactSource factSource,
            RoleActivationCandidateResolver candidateResolver
    ) {
        this.factSource = Objects.requireNonNull(factSource, "factSource");
        this.candidateResolver = Objects.requireNonNull(
                candidateResolver, "candidateResolver");
    }

    /**
     * 方法 `candidates` 按照 `RoleActivationCandidateService` 的职责处理输入，完成 `candidates` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `candidates` processes its inputs according to `RoleActivationCandidateService`'s responsibility, performs the `candidates` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `candidates` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `candidates`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseNow 输入参数 `databaseNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public RoleActivationCandidateView candidates(
            String tenantId,
            String userId,
            Instant databaseNow
    ) {
        ActivationFacts facts = factSource.load(tenantId, userId, databaseNow);
        Map<String, Set<String>> assignmentIdsByRoot = candidateResolver.resolve(
                facts.assignments(), facts.hierarchy(), databaseNow);
        var rootsByApplication = new TreeMap<String, List<RoleActivationCandidate>>();
        for (Map.Entry<String, Set<String>> entry : assignmentIdsByRoot.entrySet()) {
            String rootId = entry.getKey();
            RoleNode root = facts.hierarchy().requireNode(rootId);
            rootsByApplication.computeIfAbsent(
                    root.applicationId(), ignored -> new ArrayList<>()).add(
                    candidate(rootId, entry.getValue(), facts, databaseNow));
        }
        var applications = new ArrayList<RoleActivationCandidateView.ApplicationCandidates>();
        rootsByApplication.forEach((applicationId, candidates) -> {
            ApplicationFact application = facts.applications().get(applicationId);
            if (application == null) {
                throw new IllegalArgumentException(
                        "missing application fact: " + applicationId);
            }
            candidates.sort(Comparator.comparing(RoleActivationCandidate::rootRoleCode));
            applications.add(new RoleActivationCandidateView.ApplicationCandidates(
                    application.id(), application.code(), candidates));
        });
        applications.sort(Comparator.comparing(
                RoleActivationCandidateView.ApplicationCandidates::applicationCode));
        return new RoleActivationCandidateView(
                applications,
                facts.authVersion(),
                facts.policyVersion(),
                facts.directorySnapshotVersion(),
                List.of(),
                databaseNow);
    }

    /**
     * 方法 `candidate` 按照 `RoleActivationCandidateService` 的职责处理输入，完成 `candidate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `candidate` processes its inputs according to `RoleActivationCandidateService`'s responsibility, performs the `candidate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `candidate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `candidate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param rootId 输入参数 `rootId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param eligibleAssignmentIds 输入参数 `eligibleAssignmentIds`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param facts 输入参数 `facts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseNow 输入参数 `databaseNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private RoleActivationCandidate candidate(
            String rootId,
            Set<String> eligibleAssignmentIds,
            ActivationFacts facts,
            Instant databaseNow
    ) {
        Set<String> sourceRoleIds = new TreeSet<>();
        var rootSpecification = new UniqueActivationRootSpecification();
        for (EligibleAssignmentFact assignment : facts.assignments()) {
            if (assignment.eligibleAt(databaseNow)
                    && rootId.equals(rootSpecification.requireUniqueRoot(
                    assignment.roleId(), facts.hierarchy()))) {
                sourceRoleIds.add(assignment.roleId());
            }
        }
        Set<String> family = facts.hierarchy().descendantsIncludingSelf(rootId);
        RoleNode.RiskLevel risk = family.stream()
                .map(id -> facts.hierarchy().requireNode(id).riskLevel())
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(RoleNode.RiskLevel.LOW);
        List<String> mutexSetIds = facts.dsdSets().stream()
                .filter(set -> set.rootRoleIds().contains(rootId))
                .map(DsdSetFact::id)
                .sorted()
                .toList();
        RoleNode root = facts.hierarchy().requireNode(rootId);
        return new RoleActivationCandidate(
                rootId,
                root.code(),
                facts.roleDisplayNames().getOrDefault(rootId, root.code()),
                new ArrayList<>(sourceRoleIds),
                new ArrayList<>(new TreeSet<>(eligibleAssignmentIds)),
                mutexSetIds,
                risk.name(),
                requiredStrength(risk),
                root.landingRouteCode());
    }

    /**
     * 方法 `requiredStrength` 按照 `RoleActivationCandidateService` 的职责处理输入，完成 `required Strength` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requiredStrength` processes its inputs according to `RoleActivationCandidateService`'s responsibility, performs the `required Strength` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requiredStrength` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requiredStrength`, then continue the business flow using its result, exception, or side effect.
     *
     * @param risk 输入参数 `risk`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String requiredStrength(RoleNode.RiskLevel risk) {
        return switch (risk) {
            case LOW, MEDIUM -> "PASSWORD";
            case HIGH -> "MFA";
            case CRITICAL -> "STRONG";
        };
    }

    /**
     * 类型 `ActivationFactSource` 位于 `RoleActivationCandidateService` 内，是接口，用于承载 `Activation Fact Source` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ActivationFactSource` is an interface inside `RoleActivationCandidateService` and carries the responsibility, state, or contract for `Activation Fact Source`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ActivationFactSource` 作为 `RoleActivationCandidateService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ActivationFactSource` as the responsibility boundary of `RoleActivationCandidateService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface ActivationFactSource {

        /**
         * 方法 `load` 按照 `ActivationFactSource` 的职责处理输入，完成 `load` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `load` processes its inputs according to `ActivationFactSource`'s responsibility, performs the `load` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param databaseNow 输入参数 `databaseNow`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        ActivationFacts load(String tenantId, String userId, Instant databaseNow);
    }

    /**
     * 类型 `ActivationFacts` 位于 `RoleActivationCandidateService` 内，是记录类型，用于承载 `Activation Facts` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ActivationFacts` is a record inside `RoleActivationCandidateService` and carries the responsibility, state, or contract for `Activation Facts`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ActivationFacts` 作为 `RoleActivationCandidateService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ActivationFacts` as the responsibility boundary of `RoleActivationCandidateService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param hierarchy 记录组件 `hierarchy` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `hierarchy` carries constructor data whose meaning is defined by the record contract.
     * @param assignments 记录组件 `assignments` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `assignments` carries constructor data whose meaning is defined by the record contract.
     * @param dsdSets 记录组件 `dsdSets` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `dsdSets` carries constructor data whose meaning is defined by the record contract.
     * @param authorizationFacts 记录组件 `authorizationFacts` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authorizationFacts` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param directorySnapshotVersion 记录组件 `directorySnapshotVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `directorySnapshotVersion` carries constructor data whose meaning is defined by the record contract.
     * @param applications 记录组件 `applications` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applications` carries constructor data whose meaning is defined by the record contract.
     * @param roleDisplayNames 记录组件 `roleDisplayNames` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `roleDisplayNames` carries constructor data whose meaning is defined by the record contract.
     */
    public record ActivationFacts(
            /**
             * 字段 `tenantId` 表示 `ActivationFacts` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ActivationFacts` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ActivationFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ActivationFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `userId` 表示 `ActivationFacts` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `ActivationFacts` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `ActivationFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `ActivationFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `hierarchy` 表示 `ActivationFacts` 中与 `hierarchy` 相关的状态、依赖、配置或结果（声明类型 `RoleHierarchy`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `hierarchy` stores the `hierarchy`-related state, dependency, configuration, or result of `ActivationFacts` (declared type `RoleHierarchy`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `hierarchy` 时应保持 `ActivationFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `hierarchy`, preserve `ActivationFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            RoleHierarchy hierarchy,
            /**
             * 字段 `assignments` 表示 `ActivationFacts` 中与 `assignments` 相关的状态、依赖、配置或结果（声明类型 `List&lt;EligibleAssignmentFact&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `assignments` stores the `assignments`-related state, dependency, configuration, or result of `ActivationFacts` (declared type `List&lt;EligibleAssignmentFact&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `assignments` 时应保持 `ActivationFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `assignments`, preserve `ActivationFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<EligibleAssignmentFact> assignments,
            /**
             * 字段 `dsdSets` 表示 `ActivationFacts` 中与 `dsd Sets` 相关的状态、依赖、配置或结果（声明类型 `List&lt;DsdSetFact&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `dsdSets` stores the `dsd Sets`-related state, dependency, configuration, or result of `ActivationFacts` (declared type `List&lt;DsdSetFact&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `dsdSets` 时应保持 `ActivationFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `dsdSets`, preserve `ActivationFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            List<DsdSetFact> dsdSets,
            /**
             * 字段 `authorizationFacts` 表示 `ActivationFacts` 中与 `authorization Facts` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationRuleFacts`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authorizationFacts` stores the `authorization Facts`-related state, dependency, configuration, or result of `ActivationFacts` (declared type `AuthorizationRuleFacts`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authorizationFacts` 时应保持 `ActivationFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authorizationFacts`, preserve `ActivationFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            AuthorizationRuleFacts authorizationFacts,
            /**
             * 字段 `authVersion` 表示 `ActivationFacts` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `ActivationFacts` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `ActivationFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `ActivationFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `policyVersion` 表示 `ActivationFacts` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `ActivationFacts` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `ActivationFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `ActivationFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `directorySnapshotVersion` 表示 `ActivationFacts` 中与 `directory Snapshot Version` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `directorySnapshotVersion` stores the `directory Snapshot Version`-related state, dependency, configuration, or result of `ActivationFacts` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `directorySnapshotVersion` 时应保持 `ActivationFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `directorySnapshotVersion`, preserve `ActivationFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            String directorySnapshotVersion,
            /**
             * 字段 `applications` 表示 `ActivationFacts` 中与 `applications` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, ApplicationFact&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applications` stores the `applications`-related state, dependency, configuration, or result of `ActivationFacts` (declared type `Map&lt;String, ApplicationFact&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applications` 时应保持 `ActivationFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applications`, preserve `ActivationFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, ApplicationFact> applications,
            /**
             * 字段 `roleDisplayNames` 表示 `ActivationFacts` 中与 `role Display Names` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `roleDisplayNames` stores the `role Display Names`-related state, dependency, configuration, or result of `ActivationFacts` (declared type `Map&lt;String, String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `roleDisplayNames` 时应保持 `ActivationFacts` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `roleDisplayNames`, preserve `ActivationFacts`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, String> roleDisplayNames
    ) {

        /**
         * 构造器 `ActivationFacts` 用于创建并初始化 `ActivationFacts` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `ActivationFacts` creates and initializes `ActivationFacts`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `ActivationFacts` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `ActivationFacts`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param hierarchy 输入参数 `hierarchy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param assignments 输入参数 `assignments`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param dsdSets 输入参数 `dsdSets`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param authorizationFacts 输入参数 `authorizationFacts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param authVersion 输入参数 `authVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param directorySnapshotVersion 输入参数 `directorySnapshotVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param applications 输入参数 `applications`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param roleDisplayNames 输入参数 `roleDisplayNames`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public ActivationFacts {
            Objects.requireNonNull(tenantId, "tenantId");
            Objects.requireNonNull(userId, "userId");
            Objects.requireNonNull(hierarchy, "hierarchy");
            assignments = List.copyOf(assignments);
            dsdSets = List.copyOf(dsdSets);
            Objects.requireNonNull(authorizationFacts, "authorizationFacts");
            Objects.requireNonNull(directorySnapshotVersion, "directorySnapshotVersion");
            applications = Map.copyOf(applications);
            roleDisplayNames = Map.copyOf(roleDisplayNames);
            if (authVersion < 0 || policyVersion < 0) {
                throw new IllegalArgumentException("versions must not be negative");
            }
        }
    }

    /**
     * 类型 `ApplicationFact` 位于 `RoleActivationCandidateService` 内，是记录类型，用于承载 `Application Fact` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ApplicationFact` is a record inside `RoleActivationCandidateService` and carries the responsibility, state, or contract for `Application Fact`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ApplicationFact` 作为 `RoleActivationCandidateService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ApplicationFact` as the responsibility boundary of `RoleActivationCandidateService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param id 记录组件 `id` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `id` carries constructor data whose meaning is defined by the record contract.
     * @param code 记录组件 `code` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `code` carries constructor data whose meaning is defined by the record contract.
     * @param displayName 记录组件 `displayName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `displayName` carries constructor data whose meaning is defined by the record contract.
     */
    public record ApplicationFact(/**
 * 字段 `id` 表示 `ApplicationFact` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `id` stores the `id`-related state, dependency, configuration, or result of `ApplicationFact` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `id` 时应保持 `ApplicationFact` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `id`, preserve `ApplicationFact`'s lifecycle, immutability, and thread-safety constraints.
 */ String id, /**
 * 字段 `code` 表示 `ApplicationFact` 中与 `code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `code` stores the `code`-related state, dependency, configuration, or result of `ApplicationFact` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `code` 时应保持 `ApplicationFact` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `code`, preserve `ApplicationFact`'s lifecycle, immutability, and thread-safety constraints.
 */ String code, /**
 * 字段 `displayName` 表示 `ApplicationFact` 中与 `display Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `displayName` stores the `display Name`-related state, dependency, configuration, or result of `ApplicationFact` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `displayName` 时应保持 `ApplicationFact` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `displayName`, preserve `ApplicationFact`'s lifecycle, immutability, and thread-safety constraints.
 */ String displayName) {

        /**
         * 构造器 `ApplicationFact` 用于创建并初始化 `ApplicationFact` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `ApplicationFact` creates and initializes `ApplicationFact`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `ApplicationFact` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `ApplicationFact`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param id 输入参数 `id`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param code 输入参数 `code`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param displayName 输入参数 `displayName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public ApplicationFact {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(displayName, "displayName");
        }
    }
}
