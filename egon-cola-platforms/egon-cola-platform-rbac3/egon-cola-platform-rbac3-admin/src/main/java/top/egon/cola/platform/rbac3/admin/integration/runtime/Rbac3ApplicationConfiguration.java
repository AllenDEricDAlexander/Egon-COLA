package top.egon.cola.platform.rbac3.admin.integration.runtime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationCandidateService;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.activation.infrastructure.RoleActivationFactStore;
import top.egon.cola.platform.rbac3.admin.activation.infrastructure.SessionActiveRoleRepository;
import top.egon.cola.platform.rbac3.admin.application.port.AuditPort;
import top.egon.cola.platform.rbac3.admin.application.port.AuthorizationEventPort;
import top.egon.cola.platform.rbac3.admin.application.port.Rbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.integration.ddc.AtomicRbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.assignment.application.AssignmentFacade;
import top.egon.cola.platform.rbac3.admin.assignment.infrastructure.AssignmentRepository;
import top.egon.cola.platform.rbac3.admin.assignment.infrastructure.PostgresqlAssignmentLockStore;
import top.egon.cola.platform.rbac3.admin.audit.application.AuditQueryService;
import top.egon.cola.platform.rbac3.admin.audit.infrastructure.AuditCursorCodec;
import top.egon.cola.platform.rbac3.admin.audit.infrastructure.PostgresqlAuditStore;
import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.authorization.infrastructure.AuthorizationRuleRepository;
import top.egon.cola.platform.rbac3.admin.bootstrap.application.BootstrapQueryService;
import top.egon.cola.platform.rbac3.admin.bootstrap.cli.Rbac3PlatformAdminBootstrapCli;
import top.egon.cola.platform.rbac3.admin.bootstrap.infrastructure.PostgresqlPlatformAdminBootstrapStore;
import top.egon.cola.platform.rbac3.admin.config.Rbac3AdminProperties;
import top.egon.cola.platform.rbac3.admin.config.Rbac3SecurityProperties;
import top.egon.cola.platform.rbac3.admin.constraint.application.ConstraintFacade;
import top.egon.cola.platform.rbac3.admin.constraint.infrastructure.ConstraintRepository;
import top.egon.cola.platform.rbac3.admin.identity.infrastructure.IdentityRepositories;
import top.egon.cola.platform.rbac3.admin.identity.application.IdentityMappingFacade;
import top.egon.cola.platform.rbac3.admin.management.application.ManagementPolicyFacade;
import top.egon.cola.platform.rbac3.admin.management.infrastructure.ManagementPolicyRepository;
import top.egon.cola.platform.rbac3.admin.participation.application.ParticipationFacade;
import top.egon.cola.platform.rbac3.admin.participation.infrastructure.PostgresqlParticipationStore;
import top.egon.cola.platform.rbac3.admin.resource.application.ApplicationResourceFacade;
import top.egon.cola.platform.rbac3.admin.resource.application.ManifestFacade;
import top.egon.cola.platform.rbac3.admin.resource.infrastructure.ResourceManifestRepository;
import top.egon.cola.platform.rbac3.admin.role.application.RoleFacade;
import top.egon.cola.platform.rbac3.admin.role.infrastructure.RoleRepository;
import top.egon.cola.platform.rbac3.admin.runtime.application.AuthorizationFenceService;
import top.egon.cola.platform.rbac3.admin.runtime.application.AuthorizationMutationCoordinator;
import top.egon.cola.platform.rbac3.admin.runtime.application.IdempotencyService;
import top.egon.cola.platform.rbac3.admin.runtime.application.RuntimeQueryService;
import top.egon.cola.platform.rbac3.admin.runtime.infrastructure.AuthorizationMutationRepository;
import top.egon.cola.platform.rbac3.admin.runtime.infrastructure.IdempotencyRepository;
import top.egon.cola.platform.rbac3.admin.session.application.SessionFacade;
import top.egon.cola.platform.rbac3.admin.session.application.AuthorizationContextFacade;
import top.egon.cola.platform.rbac3.admin.session.application.SessionSecurityEventRecorder;
import top.egon.cola.platform.rbac3.admin.session.infrastructure.JpaSessionStore;
import top.egon.cola.platform.rbac3.admin.session.infrastructure.AuthorizationContextRepository;
import top.egon.cola.platform.rbac3.admin.simulation.application.AuthorizationSimulationService;
import top.egon.cola.platform.rbac3.admin.simulation.infrastructure.PostgresqlRoleImpactSource;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SessionSnapshotProjector;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SystemAuthorizationSnapshotService;
import top.egon.cola.platform.rbac3.admin.snapshot.infrastructure.RedisAuthorizationRuntimeStore;
import top.egon.cola.platform.rbac3.admin.worker.AuthorizationMutationRecoveryWorker;
import top.egon.cola.platform.rbac3.admin.worker.Rbac3RuntimeProjectionRecovery;
import top.egon.cola.platform.rbac3.core.delegation.ManagementPolicyDecisionService;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.Set;

/**
 * Explicit production assembly for the RBAC3 application and persistence ports.
 */
@Configuration(proxyBeanMethods = false)
public class Rbac3ApplicationConfiguration {

    @Bean
    AtomicRbac3RuntimePolicy rbac3RuntimePolicy(Rbac3AdminProperties properties) {
        return new AtomicRbac3RuntimePolicy(properties);
    }

    @Bean
    Rbac3RuntimeKeyFactory rbac3RuntimeKeyFactory() {
        return new Rbac3RuntimeKeyFactory();
    }

    @Bean
    SessionSnapshotProjector sessionSnapshotProjector() {
        return new SessionSnapshotProjector();
    }

    @Bean
    RoleActivationCandidateService roleActivationCandidateService(
            RoleActivationFactStore factStore) {
        return new RoleActivationCandidateService(factStore);
    }

    @Bean
    RoleActivationFacade roleActivationFacade(
            RoleActivationFactStore factStore,
            SessionActiveRoleRepository transaction,
            SessionSnapshotProjector projector,
            RedisAuthorizationRuntimeStore runtimeStore,
            Rbac3RuntimePolicy runtimePolicy,
            Clock clock) {
        return new RoleActivationFacade(
                factStore, transaction, projector, runtimeStore,
                runtimePolicy, clock);
    }

    @Bean
    ManagementPolicyDecisionService managementPolicyDecisionService() {
        return new ManagementPolicyDecisionService();
    }

    @Bean
    ManagementPolicyFacade managementPolicyFacade(
            ManagementPolicyDecisionService decisionService,
            ManagementPolicyRepository repository) {
        return new ManagementPolicyFacade(decisionService, repository);
    }

    @Bean
    AuthorizationFenceService authorizationFenceService(
            Rbac3AuthorizationFenceStore store,
            Clock clock) {
        return new AuthorizationFenceService(store, clock);
    }

    @Bean
    AuthorizationMutationCoordinator authorizationMutationCoordinator(
            AuthorizationMutationRepository repository,
            AuthorizationFenceService fenceService,
            Rbac3RuntimeProjectionRecovery projectionRecovery,
            TransactionTemplate transactionTemplate,
            LongIdGenerator idGenerator,
            Clock clock) {
        return new AuthorizationMutationCoordinator(
                repository,
                fenceService,
                mutation -> projectionRecovery.project(
                        new AuthorizationMutationRecoveryWorker.MutationWork(
                                mutation.mutationId(), mutation.scope().tenantId(),
                                mutation.scope().scopeType(), mutation.scope().scopeId(),
                                "COMMITTED")),
                work -> transactionTemplate.execute(status -> work.get()),
                () -> Long.toString(idGenerator.nextLongId()),
                clock);
    }

    @Bean
    AssignmentFacade assignmentFacade(
            ManagementPolicyFacade policyFacade,
            AssignmentRepository repository,
            PostgresqlAssignmentLockStore lockStore,
            AuthorizationMutationCoordinator mutationCoordinator) {
        return new AssignmentFacade(
                policyFacade, repository, lockStore, repository, mutationCoordinator);
    }

    @Bean
    ConstraintFacade constraintFacade(ConstraintRepository repository) {
        return new ConstraintFacade(repository, repository);
    }

    @Bean
    RoleFacade roleFacade(RoleRepository repository) {
        return new RoleFacade(repository, repository);
    }

    @Bean
    ManifestFacade.ComponentKeyRegistry componentKeyRegistry(
            Rbac3AdminProperties properties) {
        Set<String> known = properties.getComponentKeys();
        return known::contains;
    }

    @Bean
    ManifestFacade manifestFacade(
            ResourceManifestRepository repository,
            ManifestFacade.ComponentKeyRegistry registry) {
        return new ManifestFacade(repository, registry);
    }

    @Bean
    ApplicationResourceFacade applicationResourceFacade(
            ResourceManifestRepository repository) {
        return new ApplicationResourceFacade(repository);
    }

    @Bean
    IdentityMappingFacade identityMappingFacade(
            IdentityRepositories identities,
            LongIdGenerator idGenerator) {
        return new IdentityMappingFacade(identities, idGenerator::nextLongId);
    }

    @Bean
    AuthorizationContextFacade authorizationContextFacade(
            IdentityMappingFacade identities,
            AuthorizationContextRepository contexts,
            LongIdGenerator idGenerator) {
        return new AuthorizationContextFacade(
                (tenantId, identitySub) -> identities.resolve(
                                identitySub, tenantId, "rbac3-authorization")
                        .map(membership -> new AuthorizationContextFacade.ActiveMembership(
                                membership.tenantId(), membership.identitySub(),
                                membership.rbac3UserId(), membership.authVersion(),
                                membership.policyVersion())),
                contexts, idGenerator::nextLongId);
    }

    @Bean
    SystemAuthorizationSnapshotService systemAuthorizationSnapshotService(
            AuthorizationContextFacade contexts,
            RedisAuthorizationRuntimeStore snapshots,
            Clock clock) {
        return new SystemAuthorizationSnapshotService(contexts::open, snapshots, clock);
    }

    @Bean
    SessionFacade sessionFacade(
            LongIdGenerator idGenerator,
            JpaSessionStore store,
            Rbac3RuntimePolicy runtimePolicy) {
        return new SessionFacade(idGenerator, store, runtimePolicy);
    }

    @Bean
    SessionSecurityEventRecorder sessionSecurityEventRecorder(
            AuditPort auditPort,
            AuthorizationEventPort eventPort) {
        return new SessionSecurityEventRecorder(auditPort, eventPort);
    }

    @Bean
    BootstrapQueryService bootstrapQueryService(
            Rbac3IdentitySessionQueryStore source) {
        return new BootstrapQueryService(source);
    }

    @Bean
    Rbac3PlatformAdminBootstrapCli rbac3PlatformAdminBootstrapCli(
            PostgresqlPlatformAdminBootstrapStore store) {
        return new Rbac3PlatformAdminBootstrapCli(store);
    }

    @Bean
    AuthorizationDecisionService authorizationDecisionService(
            RedisAuthorizationRuntimeStore runtimeStore,
            Clock clock) {
        return new AuthorizationDecisionService(runtimeStore, runtimeStore, clock);
    }

    @Bean
    IdempotencyService idempotencyService(IdempotencyRepository repository) {
        return new IdempotencyService(repository);
    }

    @Bean
    ParticipationFacade participationFacade(
            AuthorizationRuleRepository rules,
            PostgresqlParticipationStore store,
            Clock clock) {
        return new ParticipationFacade(rules, store, clock);
    }

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

    @Bean
    AuditQueryService auditQueryService(PostgresqlAuditStore store, Clock clock) {
        return new AuditQueryService(store, clock);
    }

    @Bean
    AuthorizationSimulationService authorizationSimulationService(
            AuthorizationDecisionService decisionService,
            PostgresqlRoleImpactSource impactSource,
            PostgresqlAuditStore auditStore,
            Clock clock) {
        return new AuthorizationSimulationService(
                decisionService, impactSource, auditStore, clock);
    }

    @Bean
    RuntimeQueryService runtimeQueryService(
            top.egon.cola.platform.rbac3.admin.runtime.application.ControlPlaneRuntimeStatusPort status,
            AuthorizationMutationRepository mutations,
            AuthorizationMutationRecoveryWorker recovery) {
        return new RuntimeQueryService(status, mutations, recovery);
    }

}
