package top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.permission.service.PermissionCatalogService;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.DdcCatalogGateway;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.BusinessCatalogEntry;
import top.egon.cola.platform.tianquan.jianshen.admin.iam.business.service.ApplicationCatalogEntry;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.service.DeploymentPermissionRegistrationConverterImpl;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.command.DeploymentCatalogRegistrationCommand;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.command.DeploymentCatalogRegistrationCommand.FieldDefinitionBO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.command.DeploymentCatalogRegistrationCommand.PermissionDefinitionBO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.command.DeploymentCatalogRegistrationCommand.ResourceDefinitionBO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.domain.vo.DeploymentCatalogRegistrationResultVO;
import top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.service.GlobalResourceCatalogService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeploymentCatalogRegistrationContractTest {
    private static final Instant NOW = Instant.parse("2026-10-13T00:00:00Z");
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path temporary;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final LocalValidatorFactoryBean boundaryValidator = new LocalValidatorFactoryBean();
    { boundaryValidator.afterPropertiesSet(); }
    @BeforeAll static void initializeExistingIdEngine() { top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator.initialize(0L, java.time.Duration.ofMillis(5)); }
    @AfterEach void closeValidator() { boundaryValidator.close(); }

    private GlobalResourceCatalogService owner(CatalogSqlPortFixture database) {
        var catalog = mock(DdcCatalogGateway.class);
        when(catalog.findBusiness("business-1")).thenReturn(java.util.Optional.of(new BusinessCatalogEntry("business-1", "student-management", "Student", true)));
        when(catalog.findApplication("application-1")).thenReturn(java.util.Optional.of(new ApplicationCatalogEntry("application-1", "business-1", "student-management", "sample-light", "Sample", true, true)));
        return new GlobalResourceCatalogService(database.entityManager, () -> NOW, new PermissionCatalogService(database.entityManager, () -> NOW),
                catalog, mapper, boundaryValidator, new DeploymentPermissionRegistrationConverterImpl());
    }

    @Test
    void registrationGroupRejectsDuplicateUnknownAndUnsafeDefinitions() throws Exception {
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet(); var command = command();
            assertThat(validator.validate(command, DeploymentCatalogRegistrationCommand.Registration.class)).isEmpty();
            command.setExpectedApplicationVersion(-1L);
            assertThat(validator.validate(command, DeploymentCatalogRegistrationCommand.Registration.class)).isNotEmpty();
            command.setExpectedApplicationVersion(0L).setPermissions(List.of(command.getPermissions().getFirst(), command.getPermissions().getFirst()));
            assertThat(validator.validate(command, DeploymentCatalogRegistrationCommand.Registration.class)).isNotEmpty();
            command = command(); command.getResources().getFirst().setPath("https://foreign.example/operation");
            assertThat(validator.validate(command, DeploymentCatalogRegistrationCommand.Registration.class)).isNotEmpty();
            command = command(); command.getFields().getFirst().setResourceCode("other.api.create");
            assertThat(validator.validate(command, DeploymentCatalogRegistrationCommand.Registration.class)).isNotEmpty();
            command = command(); command.getFields().getFirst().setDataType("BINARY");
            assertThat(validator.validate(command, DeploymentCatalogRegistrationCommand.Registration.class)).isNotEmpty();
        }
    }

    @Test
    void strictCatalogJsonCarriesNoSecurityOrGrantFields() throws Exception {
        var reader = mapper.readerFor(DeploymentCatalogRegistrationCommand.class)
                .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .with(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        String json = mapper.writeValueAsString(command());
        assertThat(json).doesNotContain("definitionSetValid", "transportValid", "tenantId", "actorId", "writable", "defaultAccess", "roleId");
        assertThat(reader.<DeploymentCatalogRegistrationCommand>readValue(json).getPermissions()).hasSize(1);
        assertThatThrownBy(() -> reader.readValue(json.replaceFirst("\\{", "{\"grants\":[],"))).isInstanceOf(com.fasterxml.jackson.databind.JsonMappingException.class);
        assertThatThrownBy(() -> reader.readValue(json.replaceFirst("\\{", "{\"businessCode\":\"other\","))).isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
    }

    @Test
    void pendingReceiptHasExactlyNineFieldsAndCannotRepresentAnAuthorization() throws Exception {
        var value = new DeploymentCatalogRegistrationResultVO("71", "sample-light", "build-1", "0".repeat(64), 1, 1, 1, 1, "REGISTERED_PENDING_REVIEW");
        assertThat(mapper.valueToTree(value).size()).isEqualTo(9);
        assertThatThrownBy(() -> new DeploymentCatalogRegistrationResultVO("71", "sample-light", "build-1", "0".repeat(64), 1, 1, 1, 1, "ACTIVE"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DeploymentCatalogRegistrationResultVO("9223372036854775808", "sample-light", "build-1", "0".repeat(64), 1, 1, 1, 1, "REGISTERED_PENDING_REVIEW"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validDirectoryIsPendingWithoutGrants() throws Exception {
        var database = new CatalogSqlPortFixture(); var owner = owner(database);
        var failure = new AtomicReference<Throwable>(); var result = attempt(() -> owner.registerDeploymentCatalog(command(), "deployment-actor"), failure);
        assertThat(result).as("The actual owner must commit a pending mechanical directory; failure=%s", failure.get() == null ? "none" : failure.get().getClass().getSimpleName()).isNotNull();
        assertThat(result.status()).isEqualTo("REGISTERED_PENDING_REVIEW"); assertThat(result.permissionCount()).isEqualTo(1);
        assertThat(result.resourceCount()).isEqualTo(1); assertThat(result.fieldCount()).isEqualTo(1);
        assertThat(database.writes).noneMatch(sql -> sql.contains("role_resource_grant") || sql.contains("field_rule") || sql.contains("insert into rbac3_field_definition"));
        assertThat(database.writes).anyMatch(sql -> sql.contains("insert into rbac3_permission") && sql.contains("PENDING_VALIDATION"));
    }

    @Test
    void checksumReplayIsIdempotent() throws Exception {
        var database = new CatalogSqlPortFixture(); var owner = owner(database);
        var command = command(); var failure = new AtomicReference<Throwable>(); var first = attempt(() -> owner.registerDeploymentCatalog(command, "deployment-actor"), failure);
        assertThat(first).as("A first real registration must exist before replay").isNotNull(); int writes = database.writes.size();
        var replay = owner.registerDeploymentCatalog(command, "deployment-actor");
        assertThat(replay).isEqualTo(first); assertThat(database.writes).hasSize(writes);
    }

    @Test
    void foreignNamespaceAndStaleVersionWriteNothing() throws Exception {
        var database = new CatalogSqlPortFixture(); database.version = 4;
        var owner = owner(database);
        assertThatThrownBy(() -> owner.registerDeploymentCatalog(command(), "deployment-actor"))
                .isInstanceOf(top.egon.cola.platform.tianquan.jianshen.core.rule.Rbac3RuleViolation.class);
        assertThat(database.writes).isEmpty();
        var foreign = command(); foreign.getPermissions().getFirst().setPermissionCode("other:user:create");
        assertThatThrownBy(() -> owner.registerDeploymentCatalog(foreign, "deployment-actor")).isInstanceOf(IllegalArgumentException.class);
        assertThat(database.writes).isEmpty();
    }

    @Test
    void cliUsesActualDirectoryAdaptersAndOnlyManifestBoundActor() throws Exception {
        var root = temporary.toRealPath(); java.nio.file.Files.setPosixFilePermissions(root, Set.of(java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                java.nio.file.attribute.PosixFilePermission.OWNER_WRITE, java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE));
        var command = command(); var artifact = root.resolve("application.jar"); java.nio.file.Files.writeString(artifact, "controlled artifact bytes");
        var catalogFile = root.resolve("catalog.json"); java.nio.file.Files.writeString(catalogFile, mapper.writeValueAsString(command));
        var manifestFile = root.resolve("manifest.json");
        var entry = Map.of("businessCode", command.getBusinessCode(), "applicationCode", command.getApplicationCode(), "buildId", command.getBuildId(),
                "artifactFile", artifact.toString(), "artifactSha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(java.nio.file.Files.readAllBytes(artifact))),
                "catalogSha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(java.nio.file.Files.readAllBytes(catalogFile))));
        java.nio.file.Files.writeString(manifestFile, mapper.writeValueAsString(Map.of("schemaVersion", 1, "runId", java.util.UUID.randomUUID().toString(),
                "rootPath", root.toString(), "identitySub", "deployment-actor", "tenantId", "1001", "applications", List.of(entry))));
        for (var file : List.of(artifact, catalogFile, manifestFile)) { java.nio.file.Files.setPosixFilePermissions(file, Set.of(java.nio.file.attribute.PosixFilePermission.OWNER_READ, java.nio.file.attribute.PosixFilePermission.OWNER_WRITE)); }
        var rpc = mock(top.egon.cola.platform.tianquan.shoubing.rpc.contract.IdentityDirectoryRpc.class);
        when(rpc.batchGetIdentityProfiles(org.mockito.ArgumentMatchers.any())).thenReturn(top.egon.cola.platform.tianquan.shoubing.rpc.contract.proto.v1.BatchGetIdentityProfilesResponse.newBuilder()
                .addProfiles(top.egon.cola.platform.tianquan.shoubing.rpc.contract.proto.v1.IdentityProfile.newBuilder().setSubject("deployment-actor").setStatus("ACTIVE").setVersion(1)).build());
        when(rpc.getTenantMembership(org.mockito.ArgumentMatchers.any())).thenReturn(top.egon.cola.platform.tianquan.shoubing.rpc.contract.proto.v1.GetTenantMembershipResponse.newBuilder()
                .setProfile(top.egon.cola.platform.tianquan.shoubing.rpc.contract.proto.v1.TenantMembershipProfile.newBuilder().setTenantId("1001").setIdentitySub("deployment-actor")
                        .setTenantStatus(top.egon.cola.platform.tianquan.shoubing.rpc.contract.proto.v1.TenantStatus.TENANT_STATUS_ACTIVE)
                        .setIdentityStatus(top.egon.cola.platform.tianquan.shoubing.rpc.contract.proto.v1.IdentityStatus.IDENTITY_STATUS_ACTIVE)
                        .setMembershipStatus(top.egon.cola.platform.tianquan.shoubing.rpc.contract.proto.v1.MembershipStatus.MEMBERSHIP_STATUS_ACTIVE).setMembershipVersion(1)).build());
        var profiles = new top.egon.cola.platform.tianquan.jianshen.admin.iam.user.repository.IdentityProfileDirectory();
        var memberships = new top.egon.cola.platform.tianquan.jianshen.admin.iam.user.repository.IdentityTenantMembershipDirectory();
        org.springframework.test.util.ReflectionTestUtils.setField(profiles, "rpc", rpc); org.springframework.test.util.ReflectionTestUtils.setField(memberships, "rpc", rpc);
        var database = new CatalogSqlPortFixture();
        var cli = new top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.controller.cli.Rbac3ApplicationCatalogRegistrationCli(owner(database), memberships, profiles, mapper, boundaryValidator);
        String[] args = {"register-application-catalog", "--manifest", manifestFile.toString(), "--catalog-file", catalogFile.toString(), "--business-code", command.getBusinessCode(),
                "--application-code", command.getApplicationCode(), "--expected-version", "0", "--actor-id", "deployment-actor"};
        var badActor = args.clone(); badActor[badActor.length - 1] = "untrusted-actor";
        assertThat(cli.run(badActor)).isEqualTo(2); assertThat(database.writes).isEmpty(); org.mockito.Mockito.verifyNoInteractions(rpc);
        assertThat(cli.run(args)).isZero(); assertThat(database.version).isEqualTo(1);
        org.mockito.Mockito.verify(rpc).batchGetIdentityProfiles(org.mockito.ArgumentMatchers.any()); org.mockito.Mockito.verify(rpc).getTenantMembership(org.mockito.ArgumentMatchers.any());
        var injectedOption = java.util.Arrays.copyOf(args, args.length + 2); injectedOption[args.length] = "--spring.main.web-application-type"; injectedOption[args.length + 1] = "servlet";
        int writes = database.writes.size(); assertThat(cli.run(injectedOption)).isEqualTo(2); assertThat(database.writes).hasSize(writes);
        org.mockito.Mockito.clearInvocations(rpc);
        var link = root.resolve("catalog-link.json"); java.nio.file.Files.createSymbolicLink(link, catalogFile);
        var linkedArgs = args.clone(); linkedArgs[4] = link.toString();
        assertThat(cli.run(linkedArgs)).isEqualTo(2); org.mockito.Mockito.verifyNoInteractions(rpc); assertThat(database.writes).hasSize(writes);
        java.nio.file.Files.writeString(artifact, "changed artifact bytes");
        assertThat(cli.run(args)).isEqualTo(2); org.mockito.Mockito.verifyNoInteractions(rpc); assertThat(database.writes).hasSize(writes);
        java.nio.file.Files.writeString(artifact, "controlled artifact bytes");
        when(rpc.batchGetIdentityProfiles(org.mockito.ArgumentMatchers.any())).thenReturn(null);
        assertThat(cli.run(args)).isEqualTo(4); assertThat(database.writes).hasSize(writes);
    }

    @Test
    void localInputContextImportsNoDatasourceRpcSchedulerOrApplication() {
        try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("rbac3-catalog-input");
            context.register(top.egon.cola.platform.tianquan.jianshen.admin.Rbac3AdminApplication.CatalogInputConfiguration.class); context.refresh();
            assertThat(context.getBean("jacksonObjectMapper")).isInstanceOf(ObjectMapper.class);
            assertThat(context.getBean("defaultValidator")).isInstanceOf(jakarta.validation.Validator.class);
            assertThat(context.getBeansOfType(javax.sql.DataSource.class)).isEmpty();
            assertThat(context.getBeansOfType(EntityManager.class)).isEmpty();
            assertThat(context.getBeansOfType(org.springframework.scheduling.TaskScheduler.class)).isEmpty();
            assertThat(context.getBeansOfType(top.egon.cola.platform.tianquan.shoubing.rpc.contract.IdentityDirectoryRpc.class)).isEmpty();
        }
        var runtime = top.egon.cola.platform.tianquan.jianshen.admin.Rbac3AdminApplication.catalogCommandRuntimeProperties();
        assertThat(runtime.get("egon.cola.component.rpc.provider.enabled")).isEqualTo(false);
        assertThat(runtime.get("egon.cola.component.tianshu.registry.enabled")).isEqualTo(false);
        assertThat(runtime.get("egon.cola.component.transactional-outbox.polling.enabled")).isEqualTo(false);
    }

    @Test
    void actualCommandConfigurationWiresQualifiedBeansWithoutProvidersOrSchedulers() throws Exception {
        var emf = mock(jakarta.persistence.EntityManagerFactory.class, org.mockito.Mockito.withSettings().extraInterfaces(org.springframework.orm.jpa.EntityManagerFactoryInfo.class));
        var datasource = mock(javax.sql.DataSource.class);
        when(((org.springframework.orm.jpa.EntityManagerFactoryInfo) emf).getDataSource()).thenReturn(datasource);
        when(((org.springframework.orm.jpa.EntityManagerFactoryInfo) emf).getJpaDialect()).thenReturn(new org.springframework.orm.jpa.vendor.HibernateJpaDialect());
        var factory = mock(top.egon.cola.component.rpc.tianshu.client.DdcRpcClientFactory.class);
        var handle = mock(top.egon.cola.component.rpc.tianshu.client.DdcRpcClientHandle.class);
        when(handle.client()).thenReturn(mock(top.egon.cola.component.tianshu.api.client.DdcManagementClient.class)); when(factory.managementClient()).thenReturn(handle);
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("rbac3-catalog-command"))
                .withUserConfiguration(top.egon.cola.platform.tianquan.jianshen.admin.config.runtime.Rbac3ApplicationConfiguration.CatalogCommandConfiguration.class)
                .withBean("entityManagerFactory", jakarta.persistence.EntityManagerFactory.class, () -> emf)
                .withBean("dataSource", javax.sql.DataSource.class, () -> datasource)
                .withBean("ddcRpcClientFactory", top.egon.cola.component.rpc.tianshu.client.DdcRpcClientFactory.class, () -> factory)
                .withPropertyValues("spring.autoconfigure.exclude=top.egon.cola.component.common.mybatis.autoconfigure.EgonColaShardingAutoConfiguration",
                        "egon.cola.component.id.machine-id=0", "egon.cola.component.id.max-clock-backward=5ms",
                        "egon.cola.component.mybatis-plus.enabled=false", "egon.cola.component.rpc.enabled=false",
                        "egon.cola.component.tianshu.enabled=false", "egon.cola.component.tianshu.registry.enabled=false",
                        "spring.jpa.database=POSTGRESQL", "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect", "spring.jpa.hibernate.ddl-auto=none")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean("rbac3ApplicationCatalogRegistrationCli")).isInstanceOf(top.egon.cola.platform.tianquan.jianshen.admin.authorization.resource.controller.cli.Rbac3ApplicationCatalogRegistrationCli.class);
                    assertThat(context.getBean("jpaSharedEM_entityManagerFactory")).isInstanceOf(org.springframework.orm.jpa.EntityManagerProxy.class);
                    assertThat(context.getBeansOfType(org.springframework.scheduling.TaskScheduler.class)).isEmpty();
                    assertThat(context.getBeansOfType(org.springframework.boot.web.servlet.server.ServletWebServerFactory.class)).isEmpty();
                    assertThat(context.getBeanDefinitionNames()).noneMatch(name -> name.toLowerCase(java.util.Locale.ROOT).contains("development")
                            || name.toLowerCase(java.util.Locale.ROOT).contains("serverbootstrap") || name.toLowerCase(java.util.Locale.ROOT).contains("providerexport"));
                });
        org.mockito.Mockito.verify(datasource, org.mockito.Mockito.never()).getConnection();
    }

    @Test
    void enabledConsumerClientConfigurationUsesExistingOauthCacheAndSharedRedisWithoutProvider() {
        var registration = org.springframework.security.oauth2.client.registration.ClientRegistration.withRegistrationId("operator")
                .clientId("operator-client").clientSecret("controlled-secret")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .tokenUri("https://idp.example/tenants/1001/oauth2/token").issuerUri("https://idp.example/tenants/1001").scope("identity:read").build();
        var redis = mock(org.redisson.api.RedissonClient.class);
        var factory = mock(top.egon.cola.component.rpc.tianshu.client.DdcRpcClientFactory.class);
        var handle = mock(top.egon.cola.component.rpc.tianshu.client.DdcRpcClientHandle.class);
        var registry = mock(top.egon.cola.component.rpc.tianshu.client.registry.RpcDdcServiceRegistryClient.class);
        when(handle.client()).thenReturn(registry); when(factory.registryClient()).thenReturn(handle);
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(top.egon.cola.component.rpc.tianshu.autoconfigure.DdcRpcAutoConfiguration.class))
                .withInitializer(context -> context.getEnvironment().setActiveProfiles("rbac3-catalog-command", "rbac3-catalog-input"))
                .withUserConfiguration(top.egon.cola.platform.tianquan.jianshen.admin.config.runtime.Rbac3ApplicationConfiguration.CatalogClientConfiguration.class,
                        top.egon.cola.platform.tianquan.jianshen.admin.Rbac3AdminApplication.CatalogInputConfiguration.class)
                .withBean("redisson", org.redisson.api.RedissonClient.class, () -> redis, bean -> bean.setDestroyMethodName(""))
                .withBean("ddcRpcClientFactory", top.egon.cola.component.rpc.tianshu.client.DdcRpcClientFactory.class, () -> factory)
                .withBean("clientRegistrationRepository", org.springframework.security.oauth2.client.registration.ClientRegistrationRepository.class,
                        () -> new org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository(registration))
                .withPropertyValues("egon.cola.component.rpc.consumer.enabled=true",
                        "egon.cola.component.tianshu.enabled=false", "egon.cola.component.tianshu.registry.enabled=false", "egon.cola.component.tianshu.tenant-id=1001",
                        "egon.cola.component.tianshu.biz-code=permission", "egon.cola.component.tianshu.app-code=tianquan-jianshen",
                        "egon.cola.platform.tianquan.shoubing.resource-server-id=operator-resource",
                        "egon.cola.platform.tianquan.shoubing.resource-uri=https://api.example/operator",
                        "egon.cola.platform.tianquan.shoubing.revocation.primary-endpoint=rediss://redis.example:6379",
                        "egon.cola.platform.tianquan.shoubing.revocation.epoch=controlled-epoch",
                        "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2",
                        "egon.cola.platform.tianquan.shoubing.usage-observation.enabled=true",
                        "egon.cola.platform.tianquan.shoubing.usage-observation.receiver=JIANSHEN",
                        "egon.cola.platform.tianquan.shoubing.usage-observation.fallback-issuer=https://idp.example/tenants/1001",
                        "egon.cola.platform.tianquan.shoubing.service-client.app-id=operator",
                        "egon.cola.platform.tianquan.shoubing.service-client.registration-id=operator",
                        "egon.cola.platform.tianquan.shoubing.service-client.credential-versions.operator=1",
                        "egon.cola.platform.tianquan.shoubing.trusted-issuers.test.issuer=https://idp.example/tenants/1001",
                        "egon.cola.platform.tianquan.shoubing.trusted-issuers.test.jwks-uri=https://idp.example/tenants/1001/.well-known/jwks.json",
                        "egon.cola.platform.tianquan.shoubing.trusted-issuers.test.realm-key=tenant:1001",
                        "egon.cola.platform.tianquan.shoubing.trusted-issuers.test.realm-type=TENANT",
                        "egon.cola.platform.tianquan.shoubing.trusted-issuers.test.tenant-id=1001",
                        "egon.cola.platform.tianquan.shoubing.trusted-issuers.test.status=ACTIVE",
                        "egon.cola.platform.tianquan.shoubing.trusted-issuers.test.trust-version=1")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean("idpServiceOAuth2Client")).isInstanceOf(top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client.class);
                    assertThat(context.getBean("idpOAuth2AuthorizedClientService")).isInstanceOf(org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService.class);
                    assertThat(context.getBean("idpRpcClientCredentialInterceptorFactory")).isInstanceOf(top.egon.cola.platform.tianquan.shoubing.starter.security.rpc.IdpRpcClientCredentialInterceptorFactory.class);
                    assertThat(context.getBean("idpSecurityMetricsRecorder")).isInstanceOf(top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityMetricsComponent.class);
                    assertThat(context.getBean(io.micrometer.core.instrument.MeterRegistry.class)).isNotNull();
                    assertThat(context.getBean("ddcRedissonClient")).isSameAs(redis);
                    assertThat(context.getBean("ddcRpcServiceRegistryClient")).isSameAs(registry);
                    assertThat(context.getBean("ddcRpcRegistrySubscriptions")).isInstanceOf(top.egon.cola.component.tianshu.listener.registry.DdcRegistrySubscriptionCoordinator.class);
                    assertThat(context).doesNotHaveBean("ddcRpcProviderRegistry");
                    var denied = context.getBean("catalogForbiddenProviderRegistry", top.egon.cola.component.rpc.provider.registration.RpcProviderRegistry.class);
                    assertThatThrownBy(() -> denied.register(null)).isInstanceOf(IllegalStateException.class);
                    assertThatThrownBy(() -> denied.heartbeat(null)).isInstanceOf(IllegalStateException.class);
                    assertThatThrownBy(() -> denied.deregister(null)).isInstanceOf(IllegalStateException.class);
                    assertThat(context.getBeanDefinitionNames()).noneMatch(name -> name.contains("ProviderLifecycle") || name.contains("ServerBootstrap"));
                });
        org.mockito.Mockito.verify(redis, org.mockito.Mockito.never()).shutdown();
        org.mockito.Mockito.verify(registry, org.mockito.Mockito.never()).getInstances(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void laterMechanicalReportPreservesArchivedStatusAndExistingMetadata() throws Exception {
        var database = new CatalogSqlPortFixture(); var owner = owner(database);
        owner.registerDeploymentCatalog(command(), "deployment-actor");
        var row = database.resources.values().iterator().next(); row.put("status", "ARCHIVED"); row.put("requiredPermissionId", 777L);
        var facts = mapper.readTree((String) row.get("facts")); ((com.fasterxml.jackson.databind.node.ObjectNode) facts).put("existingMetadata", "retained");
        row.put("facts", mapper.writeValueAsString(facts));
        var next = command("build-2").setExpectedApplicationVersion(1L);
        owner.registerDeploymentCatalog(next, "deployment-actor");
        assertThat(row.get("status")).isEqualTo("ARCHIVED"); assertThat(row.get("requiredPermissionId")).isEqualTo(777L);
        assertThat(mapper.readTree((String) row.get("facts")).path("existingMetadata").asText()).isEqualTo("retained");
        assertThat(database.writes).filteredOn(sql -> sql.contains("update rbac3_resource")).allSatisfy(sql ->
                assertThat(sql).doesNotContain("required_permission_id", "status =", "display_metadata ="));
    }

    @Test
    void changedOperationMeaningIsRejectedBeforeAnyNewWrite() throws Exception {
        var database = new CatalogSqlPortFixture(); var owner = owner(database); owner.registerDeploymentCatalog(command(), "deployment-actor");
        var row = database.resources.values().iterator().next(); var facts = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree((String) row.get("facts"));
        facts.put("method", "DELETE"); row.put("facts", mapper.writeValueAsString(facts));
        int writes = database.writes.size(); var next = command("build-2").setExpectedApplicationVersion(1L);
        assertThatThrownBy(() -> owner.registerDeploymentCatalog(next, "deployment-actor")).isInstanceOf(top.egon.cola.platform.tianquan.jianshen.core.rule.Rbac3RuleViolation.class);
        assertThat(database.writes).hasSize(writes); assertThat(database.version).isEqualTo(1);
    }

    @Test
    void namedDdcAliasPreservesCustomAdapterAndRejectsAmbiguousCandidates() {
        var factory = new org.springframework.beans.factory.support.DefaultListableBeanFactory(); var custom = mock(DdcCatalogGateway.class);
        factory.registerSingleton("customCatalogGateway", custom);
        var configuration = new top.egon.cola.platform.tianquan.jianshen.admin.config.runtime.Rbac3ApplicationConfiguration();
        Object alias = org.springframework.test.util.ReflectionTestUtils.invokeMethod(configuration, "namedCatalogGateway", factory);
        assertThat(alias).isSameAs(custom);
        factory.registerSingleton("secondCatalogGateway", mock(DdcCatalogGateway.class));
        assertThatThrownBy(() -> org.springframework.test.util.ReflectionTestUtils.invokeMethod(configuration, "namedCatalogGateway", factory))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void wrongChecksumAndForeignPermissionOwnerWriteNothing() throws Exception {
        var database = new CatalogSqlPortFixture(); var owner = owner(database); var bad = command().setChecksum("f".repeat(64));
        assertThatThrownBy(() -> owner.registerDeploymentCatalog(bad, "deployment-actor")).isInstanceOf(IllegalArgumentException.class);
        assertThat(database.writes).isEmpty();
        var foreign = new java.util.LinkedHashMap<String, Object>(); foreign.put("applicationId", 99L); foreign.put("name", "Create user");
        foreign.put("risk", "HIGH"); foreign.put("description", "Explicit deployment intent");
        database.permissions.put("sample-light:user:create", foreign);
        assertThatThrownBy(() -> owner.registerDeploymentCatalog(command(), "deployment-actor"))
                .isInstanceOf(top.egon.cola.platform.tianquan.jianshen.core.rule.Rbac3RuleViolation.class);
        assertThat(database.writes).isEmpty(); assertThat(database.version).isZero();
    }

    private DeploymentCatalogRegistrationCommand command() throws Exception { return command("build-1"); }

    private DeploymentCatalogRegistrationCommand command(String buildId) throws Exception {
        var result = DeploymentCatalogRegistrationCommand.builder().businessCode("student-management").applicationCode("sample-light")
                .buildId(buildId).checksum("0".repeat(64)).expectedApplicationVersion(0L)
                .permissions(List.of(PermissionDefinitionBO.builder().permissionCode("sample-light:user:create").permissionName("Create user").riskLevel("HIGH").description("Explicit deployment intent").build()))
                .resources(List.of(ResourceDefinitionBO.builder().resourceCode("sample-light.api.user.create").name("Create user").transport("REST")
                        .method("POST").path("/api/users").operationId("nativeLightUserCreateV1").suggestedPermissionCode("sample-light:user:create").build()))
                .fields(List.of(FieldDefinitionBO.builder().resourceCode("sample-light.api.user.create").fieldCode("name").jsonPath("$.name").dataType("STRING").build())).build();
        // Independent, explicit canonical payload oracle: actor, expected version and checksum are not hashed.
        var permission = new TreeMap<String, Object>(); permission.put("permissionCode", "sample-light:user:create"); permission.put("permissionName", "Create user");
        permission.put("riskLevel", "HIGH"); permission.put("description", "Explicit deployment intent");
        var resource = new TreeMap<String, Object>(); resource.put("resourceCode", "sample-light.api.user.create"); resource.put("name", "Create user"); resource.put("transport", "REST");
        resource.put("method", "POST"); resource.put("path", "/api/users"); resource.put("operationId", "nativeLightUserCreateV1"); resource.put("fullMethodName", null); resource.put("suggestedPermissionCode", "sample-light:user:create");
        var field = new TreeMap<String, Object>(); field.put("resourceCode", "sample-light.api.user.create"); field.put("fieldCode", "name"); field.put("jsonPath", "$.name"); field.put("dataType", "STRING");
        var payload = new TreeMap<String, Object>(); payload.put("businessCode", result.getBusinessCode()); payload.put("applicationCode", result.getApplicationCode()); payload.put("buildId", result.getBuildId());
        payload.put("permissions", List.of(permission)); payload.put("resources", List.of(resource)); payload.put("fields", List.of(field));
        result.setChecksum(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mapper.writeValueAsBytes(payload)))); return result;
    }

    private static DeploymentCatalogRegistrationResultVO attempt(java.util.concurrent.Callable<DeploymentCatalogRegistrationResultVO> operation, AtomicReference<Throwable> failure) {
        try { return operation.call(); }
        catch (Exception unavailable) { failure.set(unavailable); return null; }
    }

    /** Only SQL storage is substituted; owner validation, checksums, generated projection and permission service execute. */
    private static final class CatalogSqlPortFixture {
        private final EntityManager entityManager = mock(EntityManager.class);
        private final List<String> writes = new ArrayList<>();
        private final Map<String, Map<String, Object>> permissions = new java.util.LinkedHashMap<>();
        private final Map<String, Map<String, Object>> resources = new java.util.LinkedHashMap<>();
        private long version;
        private String buildId;
        private String checksum;
        private CatalogSqlPortFixture() {
            when(entityManager.createNativeQuery(anyString())).thenAnswer(call -> {
                String sql = call.getArgument(0); var query = mock(Query.class, RETURNS_SELF); Map<String, Object> parameters = new java.util.LinkedHashMap<>();
                org.mockito.Mockito.doAnswer(value -> { parameters.put(value.getArgument(0), value.getArgument(1)); return query; })
                        .when(query).setParameter(anyString(), org.mockito.ArgumentMatchers.any());
                when(query.getResultList()).thenAnswer(ignored -> read(sql, parameters));
                when(query.getSingleResult()).thenAnswer(ignored -> {
                    assertThat(sql).contains("count(*) from rbac3_application"); return 1L;
                });
                when(query.executeUpdate()).thenAnswer(ignored -> write(sql, parameters)); return query;
            });
        }

        private List<?> read(String sql, Map<String, Object> parameters) {
            if (sql.contains("from rbac3_application")) {
                assertThat(sql).contains("for update");
                return java.util.Collections.singletonList(new Object[]{71L, "sample-light", "ACTIVE", version, "business-1", "application-1", buildId, checksum});
            }
            if (sql.contains("from rbac3_permission")) {
                if (sql.contains("where p.id")) {
                    return permissions.values().stream().filter(row -> row.get("id").equals(parameters.get("id")))
                            .map(row -> new Object[]{row.get("id"), 71L, row.get("code"), row.get("name"), row.get("risk"), "PENDING_VALIDATION", "MANUAL", null, null, 0L}).toList();
                }
                var row = permissions.get(parameters.get("code"));
                return row == null ? List.of() : java.util.Collections.singletonList(new Object[]{row.getOrDefault("applicationId", 71L), row.get("name"), row.get("risk"), row.get("description"), "PENDING_VALIDATION"});
            }
            if (sql.contains("from rbac3_resource")) {
                var row = resources.get(parameters.get("code"));
                return row == null ? List.of() : java.util.Collections.singletonList(new Object[]{row.get("id"), row.get("name"), row.getOrDefault("status", "ACTIVE"), row.getOrDefault("version", 0L), row.get("facts")});
            }
            throw new AssertionError("Unexpected SQL read in catalog fixture");
        }

        private int write(String sql, Map<String, Object> parameters) {
            writes.add(sql);
            if (sql.contains("insert into rbac3_permission")) { permissions.put((String) parameters.get("code"), new java.util.LinkedHashMap<>(parameters)); return 1; }
            if (sql.contains("insert into rbac3_resource")) {
                assertThat(sql).doesNotContain("required_permission_id"); resources.put((String) parameters.get("code"), new java.util.LinkedHashMap<>(parameters)); return 1;
            }
            if (sql.contains("update rbac3_resource")) {
                var row = resources.values().stream().filter(value -> value.get("id").equals(parameters.get("id"))).findFirst().orElseThrow();
                long current = ((Number) row.getOrDefault("version", 0L)).longValue();
                if (((Number) parameters.get("version")).longValue() != current) { return 0; }
                row.put("facts", parameters.get("facts")); row.put("version", current + 1); return 1;
            }
            if (sql.contains("update rbac3_application")) {
                if (((Number) parameters.get("version")).longValue() != version) { return 0; }
                buildId = (String) parameters.get("buildId"); checksum = (String) parameters.get("checksum"); version++; return 1;
            }
            throw new AssertionError("Unexpected SQL mutation in catalog fixture");
        }
    }
}
