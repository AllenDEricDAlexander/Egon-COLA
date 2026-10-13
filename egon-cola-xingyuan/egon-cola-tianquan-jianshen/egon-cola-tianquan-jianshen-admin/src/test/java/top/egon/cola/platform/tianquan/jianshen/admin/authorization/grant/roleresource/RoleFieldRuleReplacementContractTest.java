package top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.io.TempDir;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.util.Map;
import java.util.HexFormat;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.convertor.RoleFieldRuleReplacementConverter;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.domain.po.PermissionPO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.domain.enums.PermissionRiskLevelEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.state.domain.po.TenantAuthorizationStatePO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.state.repository.jpa.JpaTenantAuthorizationStateRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.role.domain.po.RolePO;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.role.domain.enums.RoleTypeEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.role.domain.enums.RoleRiskLevelEnum;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.outbox.TransactionalOutboxAuthorizationEventPublisher;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.controller.cli.Rbac3ApplicationCatalogRegistrationCli;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.service.GlobalResourceCatalogService;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.DdcCatalogGateway;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.BusinessCatalogEntry;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.ApplicationCatalogEntry;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.repository.IdentityProfileDirectory;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.user.repository.IdentityTenantMembershipDirectory;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.FieldAccessLevel;
import top.egon.cola.platform.tianquan.jianshen.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.tianquan.jianshen.core.decision.FieldPolicyMerger;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.dto.ReplaceRoleFieldRulesCommandDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.dto.ReplaceRoleFieldRulesCommandDTO.RoleFieldRuleDTO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.vo.RoleFieldRuleMutationVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.repository.jpa.JpaRoleResourceGrantRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.service.RoleResourceGrantService;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.domain.enums.FieldRuleStatusEnum;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.domain.po.FieldRulePO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.apibinding.repository.ResourceApiBindingRepository;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.repository.AuthorizationEventPublisher;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.state.repository.TenantAuthorizationStateRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;

class RoleFieldRuleReplacementContractTest {
    private static final Instant NOW = Instant.parse("2026-10-13T00:00:00Z");
    private static final LocalValidatorFactoryBean VALIDATOR = new LocalValidatorFactoryBean();

    @BeforeAll
    static void initializeExistingIdEngine() {
        top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator.initialize(0L, java.time.Duration.ofMillis(5));
        VALIDATOR.afterPropertiesSet();
    }

    @AfterAll static void closeOwnedValidator() { VALIDATOR.close(); }

    @Test
    void definitionCapDoesNotGrantWriteWithoutARoleRule() {
        var actual = new FieldPolicyMerger().merge(List.of(), definitions(FieldAccessLevel.WRITE), Set.of("501"));
        assertThat(actual.getOrDefault("sample-light.api.user.create#name", FieldAccessLevel.NONE)).isEqualTo(FieldAccessLevel.NONE);
        var otherRole = new FieldPolicyMerger().merge(List.of(new AuthorizationRuleFacts.FieldRuleFact("999", "sample-light.api.user.create", "name", FieldAccessLevel.WRITE)),
                definitions(FieldAccessLevel.WRITE), Set.of("501"));
        assertThat(otherRole).isEmpty();
    }

    @Test
    void realCoreCapsWriteAndReadWithoutPromotingReadOrMissingRules() {
        var requested = List.of(new AuthorizationRuleFacts.FieldRuleFact("501", "sample-light.api.user.create", "name", FieldAccessLevel.WRITE));
        assertThat(new FieldPolicyMerger().merge(requested, definitions(FieldAccessLevel.READ), Set.of("501")).get("sample-light.api.user.create#name"))
                .isEqualTo(FieldAccessLevel.READ);
        assertThat(new FieldPolicyMerger().merge(requested, List.of(), Set.of("501")).get("sample-light.api.user.create#name"))
                .isEqualTo(FieldAccessLevel.NONE);
    }

    @Test
    void mutationGroupRejectsWindowDuplicatesAndForeignActorSyntax() {
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet(); var command = command(FieldAccessLevel.WRITE);
            assertThat(validator.validate(command, ReplaceRoleFieldRulesCommandDTO.Mutation.class)).isEmpty();
            command.getRules().getFirst().setValidTo(NOW);
            assertThat(validator.validate(command, ReplaceRoleFieldRulesCommandDTO.Mutation.class)).isNotEmpty();
            command = command(FieldAccessLevel.WRITE); command.setRules(List.of(command.getRules().getFirst(), command.getRules().getFirst()));
            assertThat(validator.validate(command, ReplaceRoleFieldRulesCommandDTO.Mutation.class)).isNotEmpty();
            command = command(FieldAccessLevel.WRITE); command.setActorId("actor with spaces");
            assertThat(validator.validate(command, ReplaceRoleFieldRulesCommandDTO.Mutation.class)).isNotEmpty();
            command.setActorId("deployment-actor").setRules(List.of());
            assertThat(validator.validate(command, ReplaceRoleFieldRulesCommandDTO.Mutation.class)).isEmpty();
        }
    }

    @Test
    void explicitWriterPersistsRulesThatTheRealCoreCanUseForWrite() {
        var fixture = new Fixture(); var failed = new AtomicReference<Throwable>();
        var receipt = attempt(() -> fixture.service.replaceFieldRules(command(FieldAccessLevel.WRITE)), failed);
        assertThat(receipt).as("Explicit writer must persist a rule; current failure=%s", failed.get() == null ? "none" : failed.get().getClass().getSimpleName()).isNotNull();
        assertThat(receipt.status()).isEqualTo("CONFIRMED");
        assertThat(receipt.roleVersion()).isEqualTo(1L);
        assertThat(receipt.policyVersion()).isEqualTo(1L);
        assertThat(fixture.messages).hasSize(1);
        assertThat(fixture.messages.getFirst().destination()).isEqualTo("tianquan-jianshen.role.policy-changed.v1");
        assertThat(fixture.persisted).isNotEmpty();
        var facts = fixture.persisted.stream().map(row -> new AuthorizationRuleFacts.FieldRuleFact(row.getRoleId().toString(),
                "sample-light.api.user.create", "name", FieldAccessLevel.valueOf(row.getAccessLevel().name()))).toList();
        assertThat(new FieldPolicyMerger().merge(facts, definitions(FieldAccessLevel.WRITE), Set.of("501"))
                .get("sample-light.api.user.create#name")).isEqualTo(FieldAccessLevel.WRITE);
    }

    @Test
    void explicitEmptyReplacementIsAConfirmedRevokeInsteadOfAnImplicitDefaultGrant() {
        var fixture = new Fixture(); var command = command(FieldAccessLevel.WRITE).setRules(List.of());
        var receipt = attempt(() -> fixture.service.replaceFieldRules(command), new AtomicReference<>());
        assertThat(receipt).as("Explicit empty replacement must be processed through the existing owner").isNotNull();
        assertThat(receipt.ruleCount()).isZero(); assertThat(fixture.persisted).isEmpty();
        assertThat(receipt.roleVersion()).isZero(); assertThat(receipt.policyVersion()).isZero();
        assertThat(fixture.messages).isEmpty();
    }

    @Test
    void identicalContentIsANoopAndExplicitRevokeDisablesWithoutDeletingHistory() {
        var fixture = new Fixture();
        fixture.service.replaceFieldRules(command(FieldAccessLevel.WRITE));
        var noop = fixture.service.replaceFieldRules(command(FieldAccessLevel.WRITE).setExpectedRoleVersion(1L));
        assertThat(noop.addedCount()).isZero(); assertThat(noop.changedCount()).isZero();
        assertThat(noop.roleVersion()).isEqualTo(1L); assertThat(noop.policyVersion()).isEqualTo(1L);
        assertThat(fixture.messages).hasSize(1);
        var revoke = fixture.service.replaceFieldRules(command(FieldAccessLevel.WRITE).setExpectedRoleVersion(1L).setRules(List.of()));
        assertThat(revoke.disabledCount()).isEqualTo(1); assertThat(revoke.roleVersion()).isEqualTo(2L);
        assertThat(revoke.policyVersion()).isEqualTo(2L); assertThat(fixture.persisted).hasSize(1);
        assertThat(fixture.persisted.getFirst().getStatus()).isEqualTo(FieldRuleStatusEnum.DISABLED);
        assertThat(new FieldPolicyMerger().merge(fixture.activeFacts(), definitions(FieldAccessLevel.WRITE), Set.of("501"))).isEmpty();
        assertThat(fixture.messages).hasSize(2);
    }

    @Test
    void sameKeyContentChangePreservesIdentityAndBoundsReadWithoutPromotingIt() {
        var fixture = new Fixture(); fixture.service.replaceFieldRules(command(FieldAccessLevel.WRITE));
        Long identity = fixture.persisted.getFirst().getId();
        var read = command(FieldAccessLevel.READ).setExpectedRoleVersion(1L);
        read.getRules().getFirst().setValidTo(NOW.plusSeconds(60));
        var result = fixture.service.replaceFieldRules(read);
        assertThat(result.changedCount()).isEqualTo(1); assertThat(result.addedCount()).isZero();
        assertThat(fixture.persisted.getFirst().getId()).isEqualTo(identity);
        assertThat(fixture.persisted.getFirst().getValidTo()).isEqualTo(NOW.plusSeconds(60));
        assertThat(new FieldPolicyMerger().merge(fixture.activeFacts(), definitions(FieldAccessLevel.WRITE), Set.of("501"))
                .get("sample-light.api.user.create#name")).isEqualTo(FieldAccessLevel.READ);
    }

    @Test
    void noneFutureAndExpiredRulesDoNotProduceEffectiveWrite() {
        var none = new Fixture(); none.service.replaceFieldRules(command(FieldAccessLevel.NONE));
        assertThat(new FieldPolicyMerger().merge(none.activeFacts(), definitions(FieldAccessLevel.WRITE), Set.of("501"))
                .get("sample-light.api.user.create#name")).isEqualTo(FieldAccessLevel.NONE);
        var future = new Fixture(); var futureCommand = command(FieldAccessLevel.WRITE);
        futureCommand.getRules().getFirst().setValidFrom(NOW.plusSeconds(120));
        future.service.replaceFieldRules(futureCommand);
        assertThat(future.activeFacts()).isEmpty();
        var expired = new Fixture(); var expiredCommand = command(FieldAccessLevel.WRITE);
        expiredCommand.getRules().getFirst().setStatus(FieldRuleStatusEnum.EXPIRED);
        expired.service.replaceFieldRules(expiredCommand);
        assertThat(expired.persisted.getFirst().getStatus()).isEqualTo(FieldRuleStatusEnum.EXPIRED);
        assertThat(expired.activeFacts()).isEmpty();
    }

    @Test
    void realCoreMergesRulesFromEffectiveRolesUnderTheDefinitionCap() {
        var rules = List.of(new AuthorizationRuleFacts.FieldRuleFact("501", "sample-light.api.user.create", "name", FieldAccessLevel.READ),
                new AuthorizationRuleFacts.FieldRuleFact("502", "sample-light.api.user.create", "name", FieldAccessLevel.WRITE));
        var merger = new FieldPolicyMerger();
        assertThat(merger.merge(rules, definitions(FieldAccessLevel.WRITE), Set.of("501"))
                .get("sample-light.api.user.create#name")).isEqualTo(FieldAccessLevel.READ);
        assertThat(merger.merge(rules, definitions(FieldAccessLevel.WRITE), Set.of("501", "502"))
                .get("sample-light.api.user.create#name")).isEqualTo(FieldAccessLevel.WRITE);
        assertThat(merger.merge(rules, definitions(FieldAccessLevel.READ), Set.of("501", "502"))
                .get("sample-light.api.user.create#name")).isEqualTo(FieldAccessLevel.READ);
    }

    @Test
    void staleVersionForeignTenantAndMissingEntitlementFailBeforeMutation() {
        var fixture = new Fixture(); fixture.service.replaceFieldRules(command(FieldAccessLevel.WRITE));
        assertThatThrownBy(() -> fixture.service.replaceFieldRules(command(FieldAccessLevel.READ))).hasMessageContaining("RESOURCE_VERSION_CONFLICT");
        assertThat(fixture.persisted).hasSize(1); assertThat(fixture.messages).hasSize(1);
        var foreign = new Fixture();
        assertThatThrownBy(() -> foreign.service.replaceFieldRules(command(FieldAccessLevel.WRITE).setTenantId(1002L))).hasMessageContaining("RESOURCE_NOT_FOUND");
        foreign.entitled = false;
        assertThatThrownBy(() -> foreign.service.replaceFieldRules(command(FieldAccessLevel.WRITE))).hasMessageContaining("ROLE_APPLICATION_MISMATCH");
        assertThat(foreign.persisted).isEmpty(); assertThat(foreign.messages).isEmpty();
    }

    @Test
    void readonlyCapMissingDefinitionAndMissingResourceGrantRejectWrites() {
        var fixture = new Fixture(); fixture.writable = false;
        assertThatThrownBy(() -> fixture.service.replaceFieldRules(command(FieldAccessLevel.WRITE))).hasMessageContaining("FIELD_ACCESS_DENIED");
        fixture.definitionPresent = false;
        assertThatThrownBy(() -> fixture.service.replaceFieldRules(command(FieldAccessLevel.READ))).hasMessageContaining("RESOURCE_NOT_FOUND");
        fixture.definitionPresent = true; fixture.granted = false;
        assertThatThrownBy(() -> fixture.service.replaceFieldRules(command(FieldAccessLevel.READ))).hasMessageContaining("FIELD_ACCESS_DENIED");
        assertThat(fixture.persisted).isEmpty(); assertThat(fixture.messages).isEmpty();
        assertThat(fixture.state.getPolicyVersion()).isZero();
    }

    @Test
    void generatedMappingPreservesExplicitStatusServerIdentityAndInstantPrecision() {
        var converter = Mappers.getMapper(RoleFieldRuleReplacementConverter.class);
        var rule = command(FieldAccessLevel.MASKED_READ).getRules().getFirst().setStatus(FieldRuleStatusEnum.EXPIRED)
                .setValidFrom(NOW.plusNanos(123456000)).setValidTo(NOW.plusSeconds(1));
        var row = converter.toTarget(rule, new RoleFieldRuleReplacementConverter.RulePersistenceContextVO(99L, 1001L, 71L, 501L, "actor", NOW));
        assertThat(row.getId()).isEqualTo(99L); assertThat(row.getTenantId()).isEqualTo(1001L);
        assertThat(row.getRoleId()).isEqualTo(501L); assertThat(row.getStatus()).isEqualTo(FieldRuleStatusEnum.EXPIRED);
        assertThat(row.getAccessLevel().name()).isEqualTo("MASKED_READ"); assertThat(row.getValidFrom()).isEqualTo(rule.getValidFrom());
        assertThatThrownBy(() -> converter.toTarget(rule)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void outboxFailurePropagatesAndWriterUsesTheExistingWritableTransaction() throws Exception {
        var fixture = new Fixture();
        doThrow(new IllegalStateException("outbox unavailable")).when(fixture.outbox).enqueue(any());
        assertThatThrownBy(() -> fixture.service.replaceFieldRules(command(FieldAccessLevel.WRITE))).hasMessage("outbox unavailable");
        var method = RoleResourceGrantService.class.getMethod("replaceFieldRules", ReplaceRoleFieldRulesCommandDTO.class);
        var attribute = new AnnotationTransactionAttributeSource().getTransactionAttribute(method, RoleResourceGrantService.class);
        assertThat(attribute).isNotNull(); assertThat(attribute.isReadOnly()).isFalse();
        assertThat(attribute.rollbackOn(new IllegalStateException())).isTrue();
        // Physical JPA/MP rollback and fresh-connection state checks are retained in the Step 18 PostgreSQL gate.
    }

    @Test
    void protectedFieldCliResolvesGlobalApplicationAndRejectsUnboundTargetsBeforeWriting(@TempDir Path temporary) throws Exception {
        Path root = temporary.toRealPath(); Files.setPosixFilePermissions(root, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        ObjectMapper mapper = org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json().build();
        var artifact = root.resolve("application.jar"); Files.writeString(artifact, "controlled artifact bytes");
        var rules = root.resolve("rules.json"); Files.writeString(rules, mapper.writeValueAsString(Map.of("rules", command(FieldAccessLevel.WRITE).getRules())));
        var entry = Map.of("businessCode", "sample", "applicationCode", "sample-light", "buildId", "build-1", "artifactFile", artifact.toString(),
                "artifactSha256", sha256(artifact), "catalogSha256", "0".repeat(64), "fieldRuleTargets",
                List.of(Map.of("roleId", 501L, "expectedRoleVersion", 0L, "rulesFile", rules.toString(), "rulesSha256", sha256(rules))));
        var manifest = root.resolve("manifest.json"); Files.writeString(manifest, mapper.writeValueAsString(Map.of("schemaVersion", 1, "runId", java.util.UUID.randomUUID().toString(),
                "rootPath", root.toString(), "identitySub", "deployment-actor", "tenantId", "1001", "applications", List.of(entry))));
        for (Path path : List.of(artifact, rules, manifest)) { Files.setPosixFilePermissions(path, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)); }
        var fixture = new Fixture();
        var rpc = mock(top.egon.cola.platform.tianquan.shoubing.rpc.contract.IdentityDirectoryRpc.class);
        when(rpc.batchGetIdentityProfiles(any())).thenReturn(top.egon.cola.platform.tianquan.shoubing.rpc.contract.proto.v1.BatchGetIdentityProfilesResponse.newBuilder()
                .addProfiles(top.egon.cola.platform.tianquan.shoubing.rpc.contract.proto.v1.IdentityProfile.newBuilder().setSubject("deployment-actor").setStatus("ACTIVE").setVersion(1)).build());
        when(rpc.getTenantMembership(any())).thenReturn(top.egon.cola.platform.tianquan.shoubing.rpc.contract.proto.v1.GetTenantMembershipResponse.newBuilder()
                .setProfile(top.egon.cola.platform.tianquan.shoubing.rpc.contract.proto.v1.TenantMembershipProfile.newBuilder().setTenantId("1001").setIdentitySub("deployment-actor")
                        .setTenantStatus(top.egon.cola.platform.tianquan.shoubing.rpc.contract.proto.v1.TenantStatus.TENANT_STATUS_ACTIVE)
                        .setIdentityStatus(top.egon.cola.platform.tianquan.shoubing.rpc.contract.proto.v1.IdentityStatus.IDENTITY_STATUS_ACTIVE)
                        .setMembershipStatus(top.egon.cola.platform.tianquan.shoubing.rpc.contract.proto.v1.MembershipStatus.MEMBERSHIP_STATUS_ACTIVE).setMembershipVersion(1)).build());
        var profiles = new IdentityProfileDirectory(); var memberships = new IdentityTenantMembershipDirectory();
        ReflectionTestUtils.setField(profiles, "rpc", rpc); ReflectionTestUtils.setField(memberships, "rpc", rpc);
        var directory = mock(DdcCatalogGateway.class);
        when(directory.listBusinesses("sample")).thenReturn(List.of(new BusinessCatalogEntry("9000", "sample", "Sample", true)));
        when(directory.listApplications("9000", "sample-light")).thenReturn(List.of(new ApplicationCatalogEntry("9001", "9000", "sample", "sample-light", "Sample", true, true)));
        var cli = new Rbac3ApplicationCatalogRegistrationCli(new GlobalResourceCatalogService(fixture.entityManager, () -> NOW), memberships, profiles,
                mapper, VALIDATOR, fixture.service, directory);
        String[] args = {Rbac3ApplicationCatalogRegistrationCli.FIELD_COMMAND, "--manifest", manifest.toString(), "--rules-file", rules.toString(),
                "--tenant-id", "1001", "--application-code", "sample-light", "--role-id", "501", "--expected-role-version", "0", "--actor-id", "deployment-actor"};
        for (int index : List.of(6, 10, 14)) {
            var invalid = args.clone(); invalid[index] = "999";
            assertThat(cli.run(invalid)).isEqualTo(2);
        }
        assertThat(fixture.persisted).isEmpty(); org.mockito.Mockito.verifyNoInteractions(rpc, directory);
        var output = new java.io.ByteArrayOutputStream(); var previous = System.out;
        try (var stdout = new java.io.PrintStream(output, true, java.nio.charset.StandardCharsets.UTF_8)) {
            System.setOut(stdout); assertThat(cli.run(args)).isZero();
        } finally { System.setOut(previous); }
        assertThat(mapper.readTree(output.toByteArray()).get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(fixture.persisted.getFirst().getApplicationId()).isEqualTo(71L);
        assertThat(fixture.messages).hasSize(1); assertThat(cli.run(args)).isEqualTo(3);
        Files.writeString(rules, "{\"rules\":[]}"); assertThat(cli.run(args)).isEqualTo(2);
        assertThat(fixture.persisted).hasSize(1); assertThat(fixture.messages).hasSize(1);
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    @Test
    void fieldCommandWiresTheRealStarterWriterAndJpaTransactionWithoutConsumersOrSockets() throws Exception {
        var emf = mock(jakarta.persistence.EntityManagerFactory.class, org.mockito.Mockito.withSettings().extraInterfaces(org.springframework.orm.jpa.EntityManagerFactoryInfo.class));
        var datasource = mock(javax.sql.DataSource.class);
        when(((org.springframework.orm.jpa.EntityManagerFactoryInfo) emf).getDataSource()).thenReturn(datasource);
        when(((org.springframework.orm.jpa.EntityManagerFactoryInfo) emf).getJpaDialect()).thenReturn(new org.springframework.orm.jpa.vendor.HibernateJpaDialect());
        var factory = mock(top.egon.cola.component.rpc.tianshu.client.DdcRpcClientFactory.class);
        var handle = mock(top.egon.cola.component.rpc.tianshu.client.DdcRpcClientHandle.class);
        when(handle.client()).thenReturn(mock(top.egon.cola.component.tianshu.api.client.DdcManagementClient.class)); when(factory.managementClient()).thenReturn(handle);
        var storage = mock(top.egon.cola.component.outbox.store.OutboxStore.class);
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("rbac3-catalog-command"))
                .withUserConfiguration(top.egon.cola.platform.tianquan.jianshen.admin.config.runtime.Rbac3ApplicationConfiguration.CatalogCommandConfiguration.class)
                .withBean("entityManagerFactory", jakarta.persistence.EntityManagerFactory.class, () -> emf)
                .withBean("dataSource", javax.sql.DataSource.class, () -> datasource)
                .withBean("outboxStore", top.egon.cola.component.outbox.store.OutboxStore.class, () -> storage)
                .withBean("egonColaMybatisPlusClock", Clock.class, () -> Clock.fixed(NOW, ZoneOffset.UTC))
                .withBean("ddcRpcClientFactory", top.egon.cola.component.rpc.tianshu.client.DdcRpcClientFactory.class, () -> factory)
                .withPropertyValues("spring.autoconfigure.exclude=top.egon.cola.component.common.mybatis.autoconfigure.EgonColaShardingAutoConfiguration,top.egon.cola.component.outbox.autoconfigure.OutboxMybatisPlusAutoConfiguration",
                        "rbac3.catalog.field-rules-enabled=true", "egon.cola.component.transactional-outbox.enabled=true",
                        "egon.cola.component.transactional-outbox.storage.validate-schema=false", "egon.cola.component.id.machine-id=0", "egon.cola.component.id.max-clock-backward=5ms",
                        "egon.cola.component.mybatis-plus.enabled=false", "egon.cola.component.rpc.enabled=false", "egon.cola.component.tianshu.enabled=false",
                        "spring.jpa.database=POSTGRESQL", "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect", "spring.jpa.hibernate.ddl-auto=none")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean("transactionalOutbox")).isInstanceOf(top.egon.cola.component.outbox.transaction.DefaultTransactionalOutbox.class);
                    var infrastructure = context.getBean(top.egon.cola.component.outbox.autoconfigure.OutboxInfrastructure.class);
                    assertThat(infrastructure.dataSource()).isSameAs(datasource);
                    assertThat(infrastructure.transactionManager()).isInstanceOf(org.springframework.orm.jpa.JpaTransactionManager.class);
                    assertThat(org.springframework.aop.support.AopUtils.isAopProxy(context.getBean("roleResourceGrantService"))).isTrue();
                    assertThat(context.getBean("jpaSharedEM_entityManagerFactory")).isInstanceOf(org.springframework.orm.jpa.EntityManagerProxy.class);
                    assertThat(context.getBeansOfType(org.springframework.scheduling.TaskScheduler.class)).isEmpty();
                    assertThat(context.getBeansOfType(top.egon.cola.component.outbox.dispatch.OutboxPoller.class)).isEmpty();
                    assertThat(context.getBeansOfType(org.springframework.boot.web.servlet.server.ServletWebServerFactory.class)).isEmpty();
                    assertThat(context.getBeanDefinitionNames()).noneMatch(name -> name.toLowerCase(java.util.Locale.ROOT).contains("development")
                            || name.toLowerCase(java.util.Locale.ROOT).contains("providerexport"));
                    var adapter = context.getBean("catalogProjectionEventProducerAdapter", top.egon.cola.component.outbox.delivery.DeliveryHandler.class);
                    assertThatThrownBy(() -> adapter.validateDestination("foreign.destination")).isInstanceOf(IllegalArgumentException.class);
                    assertThatThrownBy(() -> adapter.deliver(null)).isInstanceOf(IllegalStateException.class);
                });
        org.mockito.Mockito.verify(datasource, org.mockito.Mockito.never()).getConnection();
        org.mockito.Mockito.verifyNoInteractions(storage);
    }

    private static ReplaceRoleFieldRulesCommandDTO command(FieldAccessLevel access) {
        return ReplaceRoleFieldRulesCommandDTO.builder().tenantId(1001L).applicationId(71L).roleId(501L).expectedRoleVersion(0L).actorId("deployment-actor")
                .rules(List.of(RoleFieldRuleDTO.builder().permissionId(701L).fieldDefinitionId(801L).accessLevel(access).validFrom(NOW).status(FieldRuleStatusEnum.ACTIVE).build())).build();
    }

    private static List<AuthorizationRuleFacts.FieldDefinitionFact> definitions(FieldAccessLevel cap) {
        return List.of(new AuthorizationRuleFacts.FieldDefinitionFact("sample-light.api.user.create", "name", cap));
    }

    private static RoleFieldRuleMutationVO attempt(java.util.concurrent.Callable<RoleFieldRuleMutationVO> operation, AtomicReference<Throwable> failure) {
        try { return operation.call(); }
        catch (Exception unavailable) { failure.set(unavailable); return null; }
    }

    /** Only persistence/Directory/event ports are replaced; writer and Core decisions remain real. */
    private static final class Fixture {
        private final EntityManager entityManager = mock(EntityManager.class);
        private final List<FieldRulePO> persisted = new ArrayList<>();
        private final List<OutboxMessage> messages = new ArrayList<>();
        private final TransactionalOutbox outbox = mock(TransactionalOutbox.class);
        private final TenantAuthorizationStatePO state = new TenantAuthorizationStatePO(1001L, "actor", NOW);
        private final RolePO role = new RolePO(501L, 1001L, 71L, "WRITER", "Writer", RoleTypeEnum.POSITION,
                RoleRiskLevelEnum.LOW, false, null, 0, null, "actor", NOW.minusSeconds(1));
        private boolean entitled = true, writable = true, definitionPresent = true, granted = true;
        private Instant transactionTime = NOW;
        private Instant flushedRoleTime = role.getUpdatedAt();
        private final RoleResourceGrantService service;
        private Fixture() {
            when(entityManager.find(RolePO.class, 501L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(role);
            when(entityManager.find(PermissionPO.class, 701L, LockModeType.PESSIMISTIC_READ)).thenReturn(new PermissionPO(
                    701L, 1001L, 71L, "sample-light.api.user.create", "Create", PermissionRiskLevelEnum.LOW, null, "actor", NOW));
            when(entityManager.find(TenantAuthorizationStatePO.class, 1001L)).thenReturn(state);
            when(entityManager.find(TenantAuthorizationStatePO.class, 1001L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(state);
            when(entityManager.createNativeQuery(anyString())).thenAnswer(call -> {
                String sql = call.getArgument(0); var query = mock(Query.class, RETURNS_SELF);
                if (sql.contains("rbac3_field_definition")) {
                    when(query.getResultList()).thenAnswer(ignored -> definitionPresent ? List.<Object[]>of(new Object[]{901L, writable, "READ"}) : List.of());
                } else if (sql.contains("order by display_priority")) {
                    when(query.getResultList()).thenReturn(List.<Object[]>of(new Object[]{71L, "sample-light", "Sample", "ACTIVE", 0L}));
                } else {
                    when(query.getSingleResult()).thenAnswer(ignored -> (sql.contains("rbac3_tenant_application") ? entitled : granted) ? 1L : 0L);
                }
                return query;
            });
            var query = mock(TypedQuery.class, RETURNS_SELF);
            when(query.getResultList()).thenAnswer(ignored -> List.copyOf(persisted));
            when(entityManager.createQuery(anyString(), org.mockito.ArgumentMatchers.eq(FieldRulePO.class))).thenReturn(query);
            doAnswer(call -> { persisted.add(call.getArgument(0)); return null; }).when(entityManager).persist(any(FieldRulePO.class));
            doAnswer(call -> {
                if (!role.getUpdatedAt().equals(flushedRoleTime)) {
                    ReflectionTestUtils.setField(role, "version", role.getVersion() + 1);
                    flushedRoleTime = role.getUpdatedAt();
                }
                return null;
            }).when(entityManager).flush();
            when(outbox.enqueue(any())).thenAnswer(call -> {
                OutboxMessage message = call.getArgument(0); messages.add(message);
                return new OutboxReceipt(message.messageId(), message.idempotencyKey(), true);
            });
            var validator = VALIDATOR;
            var converter = Mappers.getMapper(RoleFieldRuleReplacementConverter.class);
            var repository = new JpaRoleResourceGrantRepository(entityManager, converter, validator);
            service = new RoleResourceGrantService(repository, mock(ResourceApiBindingRepository.class),
                    new JpaTenantAuthorizationStateRepository(entityManager, () -> transactionTime),
                    new TransactionalOutboxAuthorizationEventPublisher(outbox, Clock.fixed(NOW, ZoneOffset.UTC)), converter, validator,
                    () -> { transactionTime = transactionTime.plusSeconds(1); return transactionTime; });
        }

        private List<AuthorizationRuleFacts.FieldRuleFact> activeFacts() {
            return persisted.stream().filter(row -> row.getStatus() == FieldRuleStatusEnum.ACTIVE && !row.getValidFrom().isAfter(NOW)
                            && (row.getValidTo() == null || row.getValidTo().isAfter(NOW)))
                    .map(row -> new AuthorizationRuleFacts.FieldRuleFact(row.getRoleId().toString(), "sample-light.api.user.create", "name",
                            FieldAccessLevel.valueOf(row.getAccessLevel().name()))).toList();
        }
    }
}
