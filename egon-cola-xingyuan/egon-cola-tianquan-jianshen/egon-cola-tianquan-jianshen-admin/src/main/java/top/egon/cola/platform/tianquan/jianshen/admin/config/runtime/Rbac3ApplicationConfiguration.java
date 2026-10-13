package top.egon.cola.platform.tianquan.jianshen.admin.config.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.Validator;
import org.redisson.api.RedissonClient;
import org.redisson.spring.starter.RedissonAutoConfigurationV2;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizationAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxAutoConfiguration;
import top.egon.cola.component.outbox.autoconfigure.OutboxMybatisPlusAutoConfiguration;
import top.egon.cola.component.outbox.autoconfigure.OutboxStateMachineAutoConfiguration;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.service.RoleResourceGrantService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.repository.jpa.JpaRoleResourceGrantRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.convertor.RoleFieldRuleReplacementConverter;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.convertor.RoleFieldRuleReplacementConverterImpl;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.apibinding.repository.jpa.JpaResourceApiBindingRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.state.repository.jpa.JpaTenantAuthorizationStateRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.AuthorizationEventPublisher;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.outbox.TransactionalOutboxAuthorizationEventPublisher;
import top.egon.cola.component.common.id.autoconfigure.IdGeneratorAutoConfiguration;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaShardingAutoConfiguration;
import top.egon.cola.component.rpc.config.EgonRpcAutoConfig;
import top.egon.cola.component.rpc.provider.registration.RpcLeaseOperationResult;
import top.egon.cola.component.rpc.provider.registration.RpcProviderLease;
import top.egon.cola.component.rpc.provider.registration.RpcProviderLeaseIdentity;
import top.egon.cola.component.rpc.provider.registration.RpcProviderRegistration;
import top.egon.cola.component.rpc.provider.registration.RpcProviderRegistry;
import top.egon.cola.component.rpc.tianshu.autoconfigure.DdcRpcAutoConfiguration;
import top.egon.cola.component.rpc.tianshu.client.registry.RpcDdcServiceRegistryClient;
import top.egon.cola.component.rpc.tianshu.registry.RpcDdcRegistrySnapshotLoader;
import top.egon.cola.component.tianshu.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.tianshu.listener.registry.DdcRegistrySubscriptionCoordinator;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.service.PermissionCatalogService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.controller.cli.Rbac3ApplicationCatalogRegistrationCli;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.service.DeploymentPermissionRegistrationConverterImpl;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.service.GlobalResourceCatalogService;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.repository.IdentityProfileDirectory;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.repository.IdentityTenantMembershipDirectory;
import top.egon.cola.platform.tianquan.jianshen.admin.shared.repository.jpa.JpaDatabaseClock;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterAutoConfiguration;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpClientCredentialsRequestEntityConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityMetricsComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry;
import top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcClientCredentialInterceptorFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.tianshu.api.client.DdcManagementClient;
import top.egon.cola.component.rpc.tianshu.client.DdcRpcClientFactory;
import top.egon.cola.component.rpc.tianshu.client.DdcRpcClientHandle;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.repository.jpa.JpaRoleActivationFactRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.repository.jpa.JpaUserActiveRoleRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.service.RoleActivationCandidateService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.activation.service.RoleActivationFacade;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.userrole.repository.jdbc.PostgresqlAssignmentLockRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.userrole.repository.jpa.JpaAssignmentRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.userrole.service.AssignmentFacade;
import top.egon.cola.platform.tianquan.jianshen.admin.audit.repository.AuditPort;
import top.egon.cola.platform.tianquan.jianshen.admin.audit.repository.internal.AuditCursorCodec;
import top.egon.cola.platform.tianquan.jianshen.admin.audit.repository.jdbc.PostgresqlAuditRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.audit.service.AuditQueryService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.decision.repository.jpa.JpaAuthorizationRuleRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.decision.service.AuthorizationDecisionService;
import top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.controller.cli.Rbac3PlatformAdminBootstrapCli;
import top.egon.cola.platform.tianquan.jianshen.admin.bootstrap.service.PlatformAdminBootstrapService;
import top.egon.cola.platform.tianquan.jianshen.admin.config.properties.Rbac3AdminProperties;
import top.egon.cola.platform.tianquan.jianshen.admin.config.properties.Rbac3SecurityProperties;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.repository.jpa.JpaConstraintRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.service.ConstraintFacade;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.management.repository.jpa.JpaManagementPolicyRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.management.service.ManagementPolicyFacade;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.participation.repository.jdbc.PostgresqlParticipationRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.participation.service.ParticipationFacade;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.BusinessCatalogService;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.DdcCatalogGateway;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.RpcDdcCatalogGateway;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.business.service.UserBusinessAccessFacade;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.business.repository.UserBusinessAccessRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.application.service.TenantApplicationFacade;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.application.repository.jpa.JpaTenantApplicationRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.service.CiResourceRegistrationService;
import top.egon.cola.platform.tianquan.jianshen.admin.registration.ci.service.JpaCiResourceRegistrationStore;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.role.repository.jpa.JpaRoleRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.role.service.RoleFacade;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.role.service.RoleEligibilityService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.dto.MutationWorkDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.InitialAuthorizationContextRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.Rbac3RuntimePolicy;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.tianshu.AtomicRbac3RuntimePolicy;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.jpa.JpaAuthorizationMutationRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.jpa.JpaAuthorizationPublicationGuardRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.jpa.JpaIdempotencyRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.redis.RedisAuthorizationRuntimeRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service.AuthorizationMutationCoordinator;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service.AuthorizationMutationRecoveryService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service.AuthorizationPublicationGuardService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service.IdempotencyService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service.Rbac3RuntimeProjectionRecovery;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service.RuntimeQueryService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service.SystemAuthorizationSnapshotService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service.UserAuthorizationSnapshotProjector;
import top.egon.cola.platform.tianquan.jianshen.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.simulation.repository.jdbc.PostgresqlRoleImpactRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.simulation.service.AuthorizationSimulationService;
import top.egon.cola.platform.tianquan.jianshen.core.delegation.ManagementPolicyDecisionService;
import top.egon.cola.platform.tianquan.jianshen.core.runtime.Rbac3RuntimeKeyFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

/**
 * 类型 `Rbac3ApplicationConfiguration` 位于当前包内，是类型，用于承载 `Rbac3 Application Configuration` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3ApplicationConfiguration` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Application Configuration`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Explicit production assembly for the Tianquan-Jianshen application and persistence ports.
 */
@lombok.extern.slf4j.Slf4j
@Configuration(value = "rbac3ApplicationConfiguration", proxyBeanMethods = false)
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
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    AuthorizationMutationCoordinator authorizationMutationCoordinator(
            JpaAuthorizationMutationRepository repository,
            AuthorizationPublicationGuardService fenceService,
            Rbac3RuntimeProjectionRecovery projectionRecovery,
            TransactionTemplate transactionTemplate,
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
                () -> Long.toString(SnowflakeIdGenerator.nextLongId()),
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
            prefix = "egon.cola.component.tianshu",
            name = "enabled",
            havingValue = "true")
    DdcRpcClientHandle<DdcManagementClient> rbac3DdcManagementClientHandle(
            DdcRpcClientFactory factory) {
        return factory.managementClient();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.tianshu",
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
            public java.util.Optional<top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.BusinessCatalogEntry>
                    findBusiness(String ddcBusinessId) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.List<top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.BusinessCatalogEntry>
                    listBusinesses(String keyword) {
                return java.util.List.of();
            }

            @Override
            public java.util.Optional<top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.ApplicationCatalogEntry>
                    findApplication(String ddcApplicationId) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.List<top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.ApplicationCatalogEntry>
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
    CiResourceRegistrationService ciResourceRegistrationService(
            DdcCatalogGateway catalog,
            JpaCiResourceRegistrationStore store) {
        return new CiResourceRegistrationService(catalog, store);
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
            InitialAuthorizationContextRepository initialContexts,
            Clock clock,
            @Value("${egon.tianquan-jianshen.initial-context.tianshu-enabled:false}")
            boolean ddcInitialContextEnabled) {
        return new SystemAuthorizationSnapshotService(
                snapshots, initialContexts, clock, ddcInitialContextEnabled);
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
            top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.service.ControlPlaneRuntimeStatusPort status,
            JpaAuthorizationMutationRepository mutations,
            AuthorizationMutationRecoveryService recovery) {
        return new RuntimeQueryService(status, mutations, recovery);
    }

    @Bean("ddcCatalogGateway")
    @Primary
    @ConditionalOnMissingBean(name = "ddcCatalogGateway")
    DdcCatalogGateway namedCatalogGateway(ConfigurableListableBeanFactory beans) {
        var candidates = java.util.Arrays.stream(beans.getBeanNamesForType(DdcCatalogGateway.class, false, false))
                .filter(name -> !name.equals("ddcCatalogGateway")).toList();
        if (candidates.size() != 1) { throw new IllegalStateException("Exactly one existing DDC catalog gateway is required"); }
        return beans.getBean(candidates.getFirst(), DdcCatalogGateway.class);
    }

    @Bean("rbac3ApplicationCatalogRegistrationCli")
    public Rbac3ApplicationCatalogRegistrationCli catalogRegistrationCli(
            @Qualifier("globalResourceCatalogService") GlobalResourceCatalogService catalog,
            @Qualifier("identityTenantMembershipDirectory") IdentityTenantMembershipDirectory memberships,
            @Qualifier("identityProfileDirectory") IdentityProfileDirectory profiles,
            @Qualifier("jacksonObjectMapper") ObjectMapper mapper,
            @Qualifier("defaultValidator") Validator validator,
            @Qualifier("roleResourceGrantService") RoleResourceGrantService roleGrants,
            @Qualifier("ddcCatalogGateway") DdcCatalogGateway deploymentCatalog) {
        return new Rbac3ApplicationCatalogRegistrationCli(catalog, memberships, profiles, mapper, validator, roleGrants, deploymentCatalog);
    }

    /** Exact operator context: no main application scan, HTTP/RPC provider, scheduler or development bootstrap imports. */
    @lombok.extern.slf4j.Slf4j
    @Configuration(value = "rbac3CatalogCommandConfiguration", proxyBeanMethods = false)
    @Profile("rbac3-catalog-command")
    @EntityScan(basePackages = "top.egon.cola.platform.tianquan.jianshen.admin")
    @ImportAutoConfiguration({
            ValidationAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            TransactionManagerCustomizationAutoConfiguration.class,
            IdGeneratorAutoConfiguration.class,
            EgonColaMybatisPlusAutoConfiguration.class,
            EgonColaShardingAutoConfiguration.class,
            EgonRpcAutoConfig.class,
            DdcRpcAutoConfiguration.class})
    @Import({
            GlobalResourceCatalogService.class,
            PermissionCatalogService.class,
            CatalogClientConfiguration.class,
            FieldRuleCommandConfiguration.class})
    public static class CatalogCommandConfiguration {
        @Bean("deploymentPermissionRegistrationConverterImpl")
        GlobalResourceCatalogService.PermissionRegistrationConverter permissionConverter() {
            return new DeploymentPermissionRegistrationConverterImpl();
        }

        @Bean("jpaDatabaseClock")
        DatabaseClock databaseClock(@Qualifier("jpaSharedEM_entityManagerFactory") EntityManager manager) {
            return new JpaDatabaseClock(manager);
        }

        @Bean("identityProfileDirectory")
        IdentityProfileDirectory identityProfiles() {
            return new IdentityProfileDirectory();
        }

        @Bean("identityTenantMembershipDirectory")
        IdentityTenantMembershipDirectory identityMemberships() {
            return new IdentityTenantMembershipDirectory();
        }

        @Bean(name = "rbac3CatalogDdcClientHandle", destroyMethod = "close")
        DdcRpcClientHandle<DdcManagementClient> managementHandle(
                @Qualifier("ddcRpcClientFactory") DdcRpcClientFactory factory) {
            return factory.managementClient();
        }

        @Bean("ddcCatalogGateway")
        DdcCatalogGateway catalogGateway(@Qualifier("rbac3CatalogDdcClientHandle") DdcRpcClientHandle<DdcManagementClient> handle) {
            return new RpcDdcCatalogGateway(handle.client());
        }

        @Bean("jpaSharedEM_entityManagerFactory")
        EntityManager sharedEntityManager(@Qualifier("entityManagerFactory") EntityManagerFactory factory) {
            return SharedEntityManagerCreator.createSharedEntityManager(factory);
        }

        @Bean("rbac3ApplicationCatalogRegistrationCli")
        Rbac3ApplicationCatalogRegistrationCli registrationCli(
                @Qualifier("globalResourceCatalogService") GlobalResourceCatalogService catalog,
                @Qualifier("identityTenantMembershipDirectory") IdentityTenantMembershipDirectory memberships,
                @Qualifier("identityProfileDirectory") IdentityProfileDirectory profiles,
                @Qualifier("jacksonObjectMapper") ObjectMapper mapper,
                @Qualifier("defaultValidator") Validator validator,
                @Qualifier("roleResourceGrantService") ObjectProvider<RoleResourceGrantService> roleGrants,
                @Qualifier("ddcCatalogGateway") DdcCatalogGateway deploymentCatalog) {
            return new Rbac3ApplicationCatalogRegistrationCli(catalog, memberships, profiles, mapper, validator, roleGrants.getIfAvailable(), deploymentCatalog);
        }
    }

    /** Imports the existing transactional writer only for an explicit field-rule command. */
    @lombok.extern.slf4j.Slf4j
    @Configuration(value = "rbac3FieldRuleCommandConfiguration", proxyBeanMethods = false)
    @Profile("rbac3-catalog-command")
    @ConditionalOnProperty(prefix = "rbac3.catalog", name = "field-rules-enabled", havingValue = "true")
    @Import({RoleResourceGrantService.class, JpaRoleResourceGrantRepository.class})
    @ImportAutoConfiguration({TransactionalOutboxAutoConfiguration.class, OutboxMybatisPlusAutoConfiguration.class,
            OutboxStateMachineAutoConfiguration.class})
    public static class FieldRuleCommandConfiguration {
        @Bean("jpaResourceApiBindingRepository")
        JpaResourceApiBindingRepository resourceBindings(@Qualifier("jpaSharedEM_entityManagerFactory") EntityManager manager) {
            return new JpaResourceApiBindingRepository(manager);
        }

        @Bean("jpaTenantAuthorizationStateRepository")
        JpaTenantAuthorizationStateRepository tenantState(@Qualifier("jpaSharedEM_entityManagerFactory") EntityManager manager,
                @Qualifier("jpaDatabaseClock") DatabaseClock clock) {
            return new JpaTenantAuthorizationStateRepository(manager, clock);
        }

        @Bean("roleFieldRuleReplacementConverterImpl")
        RoleFieldRuleReplacementConverter fieldConverter() { return new RoleFieldRuleReplacementConverterImpl(); }

        @Bean("authorizationEventPort")
        AuthorizationEventPublisher fieldEvents(@Qualifier("transactionalOutbox") TransactionalOutbox outbox) {
            return new TransactionalOutboxAuthorizationEventPublisher(outbox, Clock.systemUTC());
        }

        @Bean("catalogProjectionEventProducerAdapter")
        DeliveryHandler producerDestinationValidator() { return new CatalogProjectionEventProducerAdapter(); }

        @Bean("catalogOutboxProducerOnlyBeanFactoryPostProcessor")
        static org.springframework.beans.factory.config.BeanFactoryPostProcessor producerOnlyOutbox() {
            return factory -> {
                var definitions = (org.springframework.beans.factory.support.BeanDefinitionRegistry) factory;
                // Keep the Starter's durable writer, MP store and lifecycle engine; this process never consumes events.
                for (String name : List.of("outboxDeliveryExecutor", "outboxTaskScheduler", "outboxDispatcher", "outboxCleanupJob",
                        "outboxCommittedEventListener", "outboxPoller")) {
                    if (definitions.containsBeanDefinition(name)) { definitions.removeBeanDefinition(name); }
                }
            };
        }
    }

    /** Producer-side adapter validates the same field-event destination without running projection workers in the CLI. */
    @lombok.extern.slf4j.Slf4j
    @lombok.RequiredArgsConstructor
    private static class CatalogProjectionEventProducerAdapter implements DeliveryHandler {
        @Override public String channel() { return TransactionalOutboxAuthorizationEventPublisher.CHANNEL; }
        @Override public void validateDestination(String destination) {
            if (!"tianquan-jianshen.role.policy-changed.v1".equals(destination)) { throw new IllegalArgumentException("Unsupported field-rule event destination"); }
        }
        @Override public DeliveryResult deliver(DeliveryContext context) {
            throw new IllegalStateException("The deployment CLI cannot deliver projection events");
        }
    }

    /** Client-only dependencies: use existing OAuth and read-only registry adapters, never register this process as a provider. */
    @lombok.extern.slf4j.Slf4j
    @Configuration(value = "rbac3CatalogClientConfiguration", proxyBeanMethods = false)
    @Profile("rbac3-catalog-command")
    @ConditionalOnProperty(prefix = "egon.cola.component.rpc.consumer", name = "enabled", havingValue = "true")
    @ImportAutoConfiguration({
            OAuth2ClientAutoConfiguration.class,
            RedissonAutoConfigurationV2.class,
            MetricsAutoConfiguration.class,
            SimpleMetricsExportAutoConfiguration.class})
    public static class CatalogClientConfiguration {
        @Bean("catalogForbiddenProviderRegistry")
        RpcProviderRegistry forbiddenProviderRegistry() {
            return new CommandProviderRegistrationDeniedStrategy();
        }

        @Bean("idpStarterProperties")
        @Primary
        @ConfigurationProperties("egon.cola.platform.tianquan.shoubing")
        IdpStarterProperties properties() {
            return new IdpStarterProperties();
        }

        @Bean("idpClock")
        Clock clientClock() { return Clock.systemUTC(); }

        @Bean("trustedIssuerRegistry")
        TrustedIssuerRegistry trustedIssuers(
                @Qualifier("idpStarterProperties") IdpStarterProperties properties) {
            return new TrustedIssuerRegistry(properties);
        }

        @Bean("idpClientCredentialsRequestEntityConverter")
        IdpClientCredentialsRequestEntityConverter credentialConverter() {
            return new IdpStarterAutoConfiguration().idpClientCredentialsRequestEntityConverter();
        }

        @Bean("idpOAuth2AuthorizedClientService")
        OAuth2AuthorizedClientService authorizedClients(
                @Qualifier("clientRegistrationRepository") ClientRegistrationRepository registrations) {
            return new IdpStarterAutoConfiguration().idpOAuth2AuthorizedClientService(registrations);
        }

        @Bean("idpOAuth2AuthorizedClientManager")
        OAuth2AuthorizedClientManager clientManager(
                @Qualifier("clientRegistrationRepository") ClientRegistrationRepository registrations,
                @Qualifier("idpOAuth2AuthorizedClientService") OAuth2AuthorizedClientService clients,
                @Qualifier("idpClientCredentialsRequestEntityConverter") IdpClientCredentialsRequestEntityConverter converter,
                @Qualifier("idpStarterProperties") IdpStarterProperties properties) {
            return new IdpStarterAutoConfiguration().idpOAuth2AuthorizedClientManager(registrations, clients, converter, properties);
        }

        @Bean("oauthUsageObservationProperties")
        OAuthUsageObservationProperties clientObservationProperties() {
            return new OAuthUsageObservationProperties();
        }

        @Bean("idpSecurityMetricsRecorder")
        IdpSecurityMetricsRecorder clientMetrics(
                ObjectProvider<MeterRegistry> registries,
                @Qualifier("defaultValidator") Validator validator,
                @Qualifier("oauthUsageObservationProperties") OAuthUsageObservationProperties usage) {
            new ValidationUtils(validator).validate(usage);
            registries.getObject();
            return new IdpSecurityMetricsComponent(registries::getObject, validator, usage);
        }

        @Bean("idpServiceOAuth2Client")
        IdpServiceOAuth2Client serviceTokens(
                @Qualifier("idpOAuth2AuthorizedClientManager") OAuth2AuthorizedClientManager manager,
                @Qualifier("idpClientCredentialsRequestEntityConverter") IdpClientCredentialsRequestEntityConverter converter,
                @Qualifier("clientRegistrationRepository") ClientRegistrationRepository registrations,
                @Qualifier("idpOAuth2AuthorizedClientService") OAuth2AuthorizedClientService clients,
                @Qualifier("trustedIssuerRegistry") TrustedIssuerRegistry issuers,
                @Qualifier("idpStarterProperties") IdpStarterProperties properties,
                @Qualifier("idpClock") Clock clock,
                @Qualifier("idpSecurityMetricsRecorder") IdpSecurityMetricsRecorder metrics) {
            return IdpServiceOAuth2Client.createConfigured(manager, converter, clock,
                    properties.getServiceClient().getRenewalSkew(), registrations, clients, issuers, properties,
                    metrics);
        }

        @Bean("idpRpcClientCredentialInterceptorFactory")
        IdpRpcClientCredentialInterceptorFactory credentials(
                @Qualifier("idpServiceOAuth2Client") IdpServiceOAuth2Client client,
                @Qualifier("idpStarterProperties") IdpStarterProperties properties,
                @Qualifier("defaultValidator") Validator validator) {
            return IdpRpcClientCredentialInterceptorFactory.createConfigured(client, properties, validator);
        }

        @Bean(name = "rbac3CatalogRegistryClientHandle", destroyMethod = "close")
        DdcRpcClientHandle<DdcServiceRegistryClient> registryHandle(
                @Qualifier("ddcRpcClientFactory") DdcRpcClientFactory factory) {
            return factory.registryClient();
        }

        @Bean(name = "ddcRedissonClient", destroyMethod = "")
        RedissonClient sharedRedis(@Qualifier("redisson") RedissonClient primary) { return primary; }

        @Bean("rpcDdcRegistrySnapshotLoader")
        RpcDdcRegistrySnapshotLoader snapshots(
                @Qualifier("rbac3CatalogRegistryClientHandle") DdcRpcClientHandle<DdcServiceRegistryClient> handle) {
            return new RpcDdcRegistrySnapshotLoader(
                    (RpcDdcServiceRegistryClient) handle.client());
        }

        // The existing DdcRpcAutoConfiguration owns the read-only subscription coordinator and its close callback.
        @Bean("ddcRpcServiceRegistryClient")
        DdcServiceRegistryClient registry(
                @Qualifier("rbac3CatalogRegistryClientHandle") DdcRpcClientHandle<DdcServiceRegistryClient> handle,
                @Qualifier("ddcRpcRegistrySubscriptions") DdcRegistrySubscriptionCoordinator subscriptions) {
            return new DdcRpcAutoConfiguration().ddcRpcServiceRegistryClient(handle, subscriptions);
        }
    }

    @lombok.extern.slf4j.Slf4j
    private static class CommandProviderRegistrationDeniedStrategy implements RpcProviderRegistry {
        @Override
        public RpcProviderLease register(RpcProviderRegistration registration) {
            throw new IllegalStateException("Catalog command cannot publish a RPC Provider");
        }
        @Override
        public RpcLeaseOperationResult heartbeat(RpcProviderLeaseIdentity lease) {
            throw new IllegalStateException("Catalog command cannot renew a RPC Provider");
        }
        @Override
        public RpcLeaseOperationResult deregister(RpcProviderLeaseIdentity lease) {
            throw new IllegalStateException("Catalog command cannot deregister a RPC Provider");
        }
    }

}
