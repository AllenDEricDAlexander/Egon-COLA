package top.egon.cola.platform.rbac3.admin.iam.role.activation.service;

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
import top.egon.cola.platform.rbac3.admin.iam.role.activation.repository.RoleActivationFactRepository;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo.ActivationFactsVO;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo.ApplicationFactVO;

/**
 * 类型 `RoleActivationCandidateService` 位于当前包内，是类型，用于承载 `Role Activation Candidate Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RoleActivationCandidateService` is a type in its package and carries the responsibility, state, or contract for `Role Activation Candidate Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Converts assignment and hierarchy facts into deterministic activation candidates.
 */
public final class RoleActivationCandidateService {

    /**
     * 字段 `factSource` 表示 `RoleActivationCandidateService` 中与 `fact Source` 相关的状态、依赖、配置或结果（声明类型 `RoleActivationFactRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `factSource` stores the `fact Source`-related state, dependency, configuration, or result of `RoleActivationCandidateService` (declared type `RoleActivationFactRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `factSource` 时应保持 `RoleActivationCandidateService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `factSource`, preserve `RoleActivationCandidateService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RoleActivationFactRepository factSource;
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
    public RoleActivationCandidateService(RoleActivationFactRepository factSource) {
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
            RoleActivationFactRepository factSource,
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
        ActivationFactsVO facts = factSource.load(tenantId, userId, databaseNow);
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
            ApplicationFactVO application = facts.applications().get(applicationId);
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
            ActivationFactsVO facts,
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



    }
