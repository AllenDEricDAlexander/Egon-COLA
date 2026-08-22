package top.egon.cola.platform.rbac3.admin.config.runtime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientFactory;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientHandle;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.repository.jpa.JpaRoleActivationFactRepository;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.repository.jpa.JpaUserActiveRoleRepository;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.service.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.admin.iam.role.activation.service.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.iam.role.assignment.repository.jdbc.PostgresqlAssignmentLockRepository;
import top.egon.cola.platform.rbac3.admin.iam.role.assignment.repository.jpa.JpaAssignmentRepository;
import top.egon.cola.platform.rbac3.admin.iam.role.assignment.service.AssignmentFacade;
import top.egon.cola.platform.rbac3.admin.audit.repository.AuditPort;
import top.egon.cola.platform.rbac3.admin.audit.repository.internal.AuditCursorCodec;
import top.egon.cola.platform.rbac3.admin.audit.repository.jdbc.PostgresqlAuditRepository;
import top.egon.cola.platform.rbac3.admin.audit.service.AuditQueryService;
import top.egon.cola.platform.rbac3.admin.authorization.repository.jpa.JpaAuthorizationRuleRepository;
import top.egon.cola.platform.rbac3.admin.authorization.service.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.bootstrap.controller.cli.Rbac3PlatformAdminBootstrapCli;
import top.egon.cola.platform.rbac3.admin.bootstrap.service.PlatformAdminBootstrapService;
import top.egon.cola.platform.rbac3.admin.config.properties.Rbac3AdminProperties;
import top.egon.cola.platform.rbac3.admin.config.properties.Rbac3SecurityProperties;
import top.egon.cola.platform.rbac3.admin.iam.policy.repository.jpa.JpaConstraintRepository;
import top.egon.cola.platform.rbac3.admin.iam.policy.service.ConstraintFacade;
import top.egon.cola.platform.rbac3.admin.management.repository.jpa.JpaManagementPolicyRepository;
import top.egon.cola.platform.rbac3.admin.management.service.ManagementPolicyFacade;
import top.egon.cola.platform.rbac3.admin.participation.repository.jdbc.PostgresqlParticipationRepository;
import top.egon.cola.platform.rbac3.admin.participation.service.ParticipationFacade;
import top.egon.cola.platform.rbac3.admin.iam.business.service.BusinessCatalogService;
import top.egon.cola.platform.rbac3.admin.iam.business.service.DdcCatalogGateway;
import top.egon.cola.platform.rbac3.admin.iam.business.service.RpcDdcCatalogGateway;
import top.egon.cola.platform.rbac3.admin.iam.business.service.UserBusinessAccessFacade;
import top.egon.cola.platform.rbac3.admin.iam.business.repository.UserBusinessAccessRepository;
import top.egon.cola.platform.rbac3.admin.iam.application.service.TenantApplicationFacade;
import top.egon.cola.platform.rbac3.admin.iam.application.repository.jpa.JpaTenantApplicationRepository;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.service.CiResourceReportService;
import top.egon.cola.platform.rbac3.admin.iam.resource.report.service.JpaCiResourceReportStore;
import top.egon.cola.platform.rbac3.admin.iam.role.repository.jpa.JpaRoleRepository;
import top.egon.cola.platform.rbac3.admin.iam.role.service.RoleFacade;
import top.egon.cola.platform.rbac3.admin.iam.role.service.RoleEligibilityService;
import top.egon.cola.platform.rbac3.admin.runtime.domain.dto.MutationWorkDTO;
import top.egon.cola.platform.rbac3.admin.runtime.repository.Rbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.runtime.repository.ddc.AtomicRbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.runtime.repository.jpa.JpaAuthorizationMutationRepository;
import top.egon.cola.platform.rbac3.admin.runtime.repository.jpa.JpaAuthorizationPublicationGuardRepository;
import top.egon.cola.platform.rbac3.admin.runtime.repository.jpa.JpaIdempotencyRepository;
import top.egon.cola.platform.rbac3.admin.runtime.repository.redis.RedisAuthorizationRuntimeRepository;
import top.egon.cola.platform.rbac3.admin.runtime.service.AuthorizationMutationCoordinator;
import top.egon.cola.platform.rbac3.admin.runtime.service.AuthorizationMutationRecoveryService;
import top.egon.cola.platform.rbac3.admin.runtime.service.AuthorizationPublicationGuardService;
import top.egon.cola.platform.rbac3.admin.runtime.service.IdempotencyService;
import top.egon.cola.platform.rbac3.admin.runtime.service.Rbac3RuntimeProjectionRecovery;
import top.egon.cola.platform.rbac3.admin.runtime.service.RuntimeQueryService;
import top.egon.cola.platform.rbac3.admin.runtime.service.SystemAuthorizationSnapshotService;
import top.egon.cola.platform.rbac3.admin.runtime.service.UserAuthorizationSnapshotProjector;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.simulation.repository.jdbc.PostgresqlRoleImpactRepository;
import top.egon.cola.platform.rbac3.admin.simulation.service.AuthorizationSimulationService;
import top.egon.cola.platform.rbac3.core.delegation.ManagementPolicyDecisionService;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

/**
 * 类型 `Rbac3ApplicationConfiguration` 位于当前包内，是类型，用于承载 `Rbac3 Application Configuration` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3ApplicationConfiguration` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Application Configuration`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Explicit production assembly for the RBAC3 application and persistence ports.
 */
@Configuration(proxyBeanMethods = false)
public class Rbac3ApplicationConfiguration {

    /**
     * 方法 `rbac3RuntimePolicy` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `rbac3 Runtime Policy` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3RuntimePolicy` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `rbac3 Runtime Policy` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3RuntimePolicy` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3RuntimePolicy`, then continue the business flow using its result, exception, or side effect.
     *
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    AtomicRbac3RuntimePolicy rbac3RuntimePolicy(Rbac3AdminProperties properties) {
        return new AtomicRbac3RuntimePolicy(properties);
    }

    /**
     * 方法 `rbac3RuntimeKeyFactory` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `rbac3 Runtime Key Factory` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3RuntimeKeyFactory` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `rbac3 Runtime Key Factory` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3RuntimeKeyFactory` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3RuntimeKeyFactory`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    Rbac3RuntimeKeyFactory rbac3RuntimeKeyFactory() {
        return new Rbac3RuntimeKeyFactory();
    }

    @Bean
    UserAuthorizationSnapshotProjector userAuthorizationSnapshotProjector(
            RoleEligibilityService roleEligibility) {
        return new UserAuthorizationSnapshotProjector(roleEligibility);
    }

    /**
     * 方法 `roleActivationCandidateService` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `role Activation Candidate Service` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `roleActivationCandidateService` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `role Activation Candidate Service` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `roleActivationCandidateService` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `roleActivationCandidateService`, then continue the business flow using its result, exception, or side effect.
     *
     * @param factStore 输入参数 `factStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    RoleActivationCandidateService roleActivationCandidateService(
            JpaRoleActivationFactRepository factStore,
            RoleEligibilityService roleEligibility) {
        return new RoleActivationCandidateService(factStore, roleEligibility);
    }

    /**
     * 组装登录状态与角色激活候选的聚合边界。
     * Assembles the boundary that combines login state and activation candidates.
     *
     * @param stateData 登录状态基础数据仓储；base login-state data repository
     * @param candidates 角色激活候选服务；role-activation candidate service
     * @return 登录状态边界；login-state boundary
     */
    /**
     * 方法 `roleActivationFacade` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `role Activation Facade` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `roleActivationFacade` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `role Activation Facade` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `roleActivationFacade` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `roleActivationFacade`, then continue the business flow using its result, exception, or side effect.
     *
     * @param factStore 输入参数 `factStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param transaction 输入参数 `transaction`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param projector 输入参数 `projector`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimeStore 输入参数 `runtimeStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimePolicy 输入参数 `runtimePolicy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    RoleActivationFacade roleActivationFacade(
            JpaRoleActivationFactRepository factStore,
            JpaUserActiveRoleRepository transaction,
            UserAuthorizationSnapshotProjector projector,
            RedisAuthorizationRuntimeRepository runtimeStore,
            Rbac3RuntimePolicy runtimePolicy,
            Clock clock) {
        return new RoleActivationFacade(
                factStore, transaction, projector, runtimeStore,
                runtimePolicy, clock);
    }

    /**
     * 方法 `managementPolicyDecisionService` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `management Policy Decision Service` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `managementPolicyDecisionService` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `management Policy Decision Service` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `managementPolicyDecisionService` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `managementPolicyDecisionService`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    ManagementPolicyDecisionService managementPolicyDecisionService() {
        return new ManagementPolicyDecisionService();
    }

    /**
     * 方法 `managementPolicyFacade` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `management Policy Facade` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `managementPolicyFacade` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `management Policy Facade` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `managementPolicyFacade` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `managementPolicyFacade`, then continue the business flow using its result, exception, or side effect.
     *
     * @param decisionService 输入参数 `decisionService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param repository 输入参数 `repository`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    ManagementPolicyFacade managementPolicyFacade(
            ManagementPolicyDecisionService decisionService,
            JpaManagementPolicyRepository repository) {
        return new ManagementPolicyFacade(decisionService, repository);
    }

    /**
     * 方法 `authorizationFenceService` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `authorization Fence Service` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authorizationFenceService` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `authorization Fence Service` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authorizationFenceService` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authorizationFenceService`, then continue the business flow using its result, exception, or side effect.
     *
     * @param store 输入参数 `store`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    AuthorizationPublicationGuardService authorizationFenceService(
            JpaAuthorizationPublicationGuardRepository store,
            Clock clock) {
        return new AuthorizationPublicationGuardService(store, clock);
    }

    /**
     * 方法 `authorizationMutationCoordinator` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `authorization Mutation Coordinator` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authorizationMutationCoordinator` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `authorization Mutation Coordinator` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authorizationMutationCoordinator` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authorizationMutationCoordinator`, then continue the business flow using its result, exception, or side effect.
     *
     * @param repository 输入参数 `repository`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param fenceService 输入参数 `fenceService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param projectionRecovery 输入参数 `projectionRecovery`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param transactionTemplate 输入参数 `transactionTemplate`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    AuthorizationMutationCoordinator authorizationMutationCoordinator(
            JpaAuthorizationMutationRepository repository,
            AuthorizationPublicationGuardService fenceService,
            Rbac3RuntimeProjectionRecovery projectionRecovery,
            TransactionTemplate transactionTemplate,
            LongIdGenerator idGenerator,
            Clock clock) {
        return new AuthorizationMutationCoordinator(
                repository,
                fenceService,
                mutation -> projectionRecovery.project(
                        new MutationWorkDTO(
                                mutation.mutationId(), mutation.scope().tenantId(),
                                mutation.scope().scopeType(), mutation.scope().scopeId(),
                                "COMMITTED")),
                work -> transactionTemplate.execute(status -> work.get()),
                () -> Long.toString(idGenerator.nextLongId()),
                clock);
    }

    /**
     * 方法 `assignmentFacade` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `assignment Facade` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `assignmentFacade` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `assignment Facade` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `assignmentFacade` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `assignmentFacade`, then continue the business flow using its result, exception, or side effect.
     *
     * @param policyFacade 输入参数 `policyFacade`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param repository 输入参数 `repository`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param lockStore 输入参数 `lockStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param mutationCoordinator 输入参数 `mutationCoordinator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    AssignmentFacade assignmentFacade(
            ManagementPolicyFacade policyFacade,
            JpaAssignmentRepository repository,
            PostgresqlAssignmentLockRepository lockStore,
            AuthorizationMutationCoordinator mutationCoordinator,
            RoleEligibilityService roleEligibility) {
        return new AssignmentFacade(
                policyFacade, repository, lockStore, repository,
                mutationCoordinator, roleEligibility);
    }

    /**
     * 方法 `constraintFacade` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `constraint Facade` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `constraintFacade` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `constraint Facade` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `constraintFacade` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `constraintFacade`, then continue the business flow using its result, exception, or side effect.
     *
     * @param repository 输入参数 `repository`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    ConstraintFacade constraintFacade(JpaConstraintRepository repository) {
        return new ConstraintFacade(repository, repository);
    }

    /**
     * 方法 `roleFacade` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `role Facade` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `roleFacade` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `role Facade` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `roleFacade` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `roleFacade`, then continue the business flow using its result, exception, or side effect.
     *
     * @param repository 输入参数 `repository`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    RoleFacade roleFacade(JpaRoleRepository repository) {
        return new RoleFacade(repository, repository);
    }

    /**
     * 方法 `componentKeyRegistry` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `component Key Registry` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `componentKeyRegistry` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `component Key Registry` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `componentKeyRegistry` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `componentKeyRegistry`, then continue the business flow using its result, exception, or side effect.
     *
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */


    /**
     * 方法 `manifestFacade` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `manifest Facade` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `manifestFacade` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `manifest Facade` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `manifestFacade` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `manifestFacade`, then continue the business flow using its result, exception, or side effect.
     *
     * @param repository 输入参数 `repository`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param registry 输入参数 `registry`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */


    /**
     * 方法 `applicationResourceFacade` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `application Resource Facade` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `applicationResourceFacade` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `application Resource Facade` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `applicationResourceFacade` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `applicationResourceFacade`, then continue the business flow using its result, exception, or side effect.
     *
     * @param repository 输入参数 `repository`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */


    @Bean(name = "rbac3DdcManagementClientHandle", destroyMethod = "close")
    @ConditionalOnProperty(
            prefix = "egon.cola.component.ddc",
            name = "enabled",
            havingValue = "true")
    DdcRpcClientHandle<DdcManagementClient> rbac3DdcManagementClientHandle(
            DdcRpcClientFactory factory) {
        return factory.managementClient();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.ddc",
            name = "enabled",
            havingValue = "true")
    DdcCatalogGateway rbac3DdcCatalogGateway(
            DdcRpcClientHandle<DdcManagementClient> handle) {
        return new RpcDdcCatalogGateway(handle.client());
    }

    @Bean
    @ConditionalOnMissingBean(DdcCatalogGateway.class)
    DdcCatalogGateway unavailableDdcCatalogGateway() {
        return new DdcCatalogGateway() {
            @Override
            public java.util.Optional<top.egon.cola.platform.rbac3.admin.iam.business.service.BusinessCatalogEntry>
                    findBusiness(String ddcBusinessId) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.List<top.egon.cola.platform.rbac3.admin.iam.business.service.BusinessCatalogEntry>
                    listBusinesses(String keyword) {
                return java.util.List.of();
            }

            @Override
            public java.util.Optional<top.egon.cola.platform.rbac3.admin.iam.business.service.ApplicationCatalogEntry>
                    findApplication(String ddcApplicationId) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.List<top.egon.cola.platform.rbac3.admin.iam.business.service.ApplicationCatalogEntry>
                    listApplications(String ddcBusinessId, String keyword) {
                return java.util.List.of();
            }
        };
    }

    @Bean
    BusinessCatalogService businessCatalogService(DdcCatalogGateway catalog) {
        return new BusinessCatalogService(catalog);
    }

    @Bean
    CiResourceReportService ciResourceReportService(
            DdcCatalogGateway catalog,
            JpaCiResourceReportStore store) {
        return new CiResourceReportService(catalog, store);
    }

    @Bean
    TenantApplicationFacade tenantApplicationFacade(
            DdcCatalogGateway catalog,
            JpaTenantApplicationRepository repository) {
        return new TenantApplicationFacade(catalog, repository);
    }

    @Bean
    UserBusinessAccessFacade userBusinessAccessFacade(
            UserBusinessAccessRepository repository,
            DdcCatalogGateway catalog,
            AuthorizationMutationCoordinator mutationCoordinator,
            DatabaseClock databaseClock) {
        return new UserBusinessAccessFacade(
                repository, catalog, mutationCoordinator, databaseClock);
    }

    /**
     * 方法 `identityMappingFacade` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `identity Mapping Facade` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `identityMappingFacade` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `identity Mapping Facade` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `identityMappingFacade` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `identityMappingFacade`, then continue the business flow using its result, exception, or side effect.
     *
     * @param identities 输入参数 `identities`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    /**
     * 方法 `authorizationContextFacade` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `authorization Context Facade` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authorizationContextFacade` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `authorization Context Facade` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authorizationContextFacade` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authorizationContextFacade`, then continue the business flow using its result, exception, or side effect.
     *
     * @param identities 输入参数 `identities`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param contexts 输入参数 `contexts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    /**
     * 方法 `systemAuthorizationSnapshotService` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `system Authorization Snapshot Service` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `systemAuthorizationSnapshotService` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `system Authorization Snapshot Service` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `systemAuthorizationSnapshotService` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `systemAuthorizationSnapshotService`, then continue the business flow using its result, exception, or side effect.
     *
     * @param contexts 输入参数 `contexts`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param snapshots 输入参数 `snapshots`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param candidates 输入参数 `candidates`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param activator 输入参数 `activator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param environment 输入参数 `environment`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    SystemAuthorizationSnapshotService systemAuthorizationSnapshotService(
            RedisAuthorizationRuntimeRepository snapshots,
            Clock clock) {
        return new SystemAuthorizationSnapshotService(snapshots, clock);
    }

    /**
     * 方法 `bootstrapQueryService` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `bootstrap Query Service` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `bootstrapQueryService` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `bootstrap Query Service` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `bootstrapQueryService` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `bootstrapQueryService`, then continue the business flow using its result, exception, or side effect.
     *
     * @param source 输入参数 `source`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    /**
     * 方法 `rbac3PlatformAdminBootstrapCli` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `rbac3 Platform Admin Bootstrap Cli` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3PlatformAdminBootstrapCli` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `rbac3 Platform Admin Bootstrap Cli` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3PlatformAdminBootstrapCli` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3PlatformAdminBootstrapCli`, then continue the business flow using its result, exception, or side effect.
     *
     * @param store 输入参数 `store`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    Rbac3PlatformAdminBootstrapCli rbac3PlatformAdminBootstrapCli(
            PlatformAdminBootstrapService service) {
        return new Rbac3PlatformAdminBootstrapCli(service);
    }

    /**
     * 方法 `authorizationDecisionService` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `authorization Decision Service` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authorizationDecisionService` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `authorization Decision Service` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authorizationDecisionService` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authorizationDecisionService`, then continue the business flow using its result, exception, or side effect.
     *
     * @param runtimeStore 输入参数 `runtimeStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    AuthorizationDecisionService authorizationDecisionService(
            RedisAuthorizationRuntimeRepository runtimeStore,
            Clock clock,
            RoleEligibilityService roleEligibility) {
        return new AuthorizationDecisionService(
                runtimeStore, runtimeStore, clock, roleEligibility);
    }

    /**
     * 方法 `idempotencyService` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `idempotency Service` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `idempotencyService` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `idempotency Service` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `idempotencyService` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `idempotencyService`, then continue the business flow using its result, exception, or side effect.
     *
     * @param repository 输入参数 `repository`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    IdempotencyService idempotencyService(JpaIdempotencyRepository repository) {
        return new IdempotencyService(repository);
    }

    /**
     * 方法 `participationFacade` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `participation Facade` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `participationFacade` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `participation Facade` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `participationFacade` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `participationFacade`, then continue the business flow using its result, exception, or side effect.
     *
     * @param rules 输入参数 `rules`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param store 输入参数 `store`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    ParticipationFacade participationFacade(
            JpaAuthorizationRuleRepository rules,
            PostgresqlParticipationRepository store,
            Clock clock) {
        return new ParticipationFacade(rules, store, clock);
    }

    /**
     * 方法 `auditCursorCodec` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `audit Cursor Codec` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `auditCursorCodec` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `audit Cursor Codec` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `auditCursorCodec` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `auditCursorCodec`, then continue the business flow using its result, exception, or side effect.
     *
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    AuditCursorCodec auditCursorCodec(Rbac3SecurityProperties properties) {
        try {
            byte[] secret = Files.readAllBytes(Path.of(
                    properties.requireAuditCursorSecretFile()));
            return new AuditCursorCodec(secret);
        } catch (IOException error) {
            throw new IllegalStateException("cannot read audit cursor signing key", error);
        }
    }

    /**
     * 方法 `auditQueryService` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `audit Query Service` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `auditQueryService` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `audit Query Service` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `auditQueryService` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `auditQueryService`, then continue the business flow using its result, exception, or side effect.
     *
     * @param store 输入参数 `store`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    AuditQueryService auditQueryService(PostgresqlAuditRepository store, Clock clock) {
        return new AuditQueryService(store, clock);
    }

    /**
     * 方法 `authorizationSimulationService` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `authorization Simulation Service` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authorizationSimulationService` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `authorization Simulation Service` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authorizationSimulationService` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authorizationSimulationService`, then continue the business flow using its result, exception, or side effect.
     *
     * @param decisionService 输入参数 `decisionService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param impactSource 输入参数 `impactSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param auditStore 输入参数 `auditStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    AuthorizationSimulationService authorizationSimulationService(
            AuthorizationDecisionService decisionService,
            PostgresqlRoleImpactRepository impactSource,
            AuditPort auditStore,
            Clock clock) {
        return new AuthorizationSimulationService(
                decisionService, impactSource, auditStore, clock);
    }

    /**
     * 方法 `runtimeQueryService` 按照 `Rbac3ApplicationConfiguration` 的职责处理输入，完成 `runtime Query Service` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `runtimeQueryService` processes its inputs according to `Rbac3ApplicationConfiguration`'s responsibility, performs the `runtime Query Service` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `runtimeQueryService` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `runtimeQueryService`, then continue the business flow using its result, exception, or side effect.
     *
     * @param status 输入参数 `status`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param mutations 输入参数 `mutations`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param recovery 输入参数 `recovery`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    RuntimeQueryService runtimeQueryService(
            top.egon.cola.platform.rbac3.admin.runtime.service.ControlPlaneRuntimeStatusPort status,
            JpaAuthorizationMutationRepository mutations,
            AuthorizationMutationRecoveryService recovery) {
        return new RuntimeQueryService(status, mutations, recovery);
    }

}
