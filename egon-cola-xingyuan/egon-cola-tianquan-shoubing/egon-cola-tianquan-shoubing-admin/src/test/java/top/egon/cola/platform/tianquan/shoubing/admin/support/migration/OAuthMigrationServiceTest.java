package top.egon.cola.platform.tianquan.shoubing.admin.support.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.po.IdentityUserPO;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Method;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityDirectoryContext;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.IdentityMigrationDAO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.command.OAuthMigrationCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.AuditRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.ClientRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.CredentialRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.GrantRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.MembershipRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.RedirectUriRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.ResourceRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.TenantRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.UserRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.CreatedOAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.result.OAuthMigrationReportResult;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthMigrationServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthGrantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthResourceRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerService;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ResourceServerVO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox.TransactionalOutboxResourceServerEventAdapter;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthSecurityProjectionComponent;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.any;

class OAuthMigrationServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2024-02-03T04:05:06Z");

    @TempDir
    Path temporaryDirectory;

    @Test
    void exposesOnlyTheExplicitRealmImportOperationAndRedactedReport() throws Exception {
        Class<?> service = load(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthMigrationService"
        );
        Class<?> command = load(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.command.OAuthMigrationCommand"
        );
        Class<?> report = load(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.result.OAuthMigrationReportResult"
        );

        Method importRealm = service.getMethod("importRealm", command);
        assertThat(importRealm.getReturnType()).isEqualTo(report);
        assertThat(report.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("passwordHash", "secretHash", "clientSecret", "accessToken", "refreshToken");
    }

    @Test
    void migrationCommandCarriesFreezeApprovalAndDurableCheckpointBoundaries() throws Exception {
        Class<?> command = load(
                "top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.command.OAuthMigrationCommand"
        );

        assertThat(command.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .contains("realmKey", "sourceSnapshotId", "dryRun", "freezeMarkerFile", "ownershipMapFile",
                        "checkpointFile")
                .contains("approvedReportSha256");
    }

    @Test
    void dryRunRejectsClientScopeExpansionAndUnapprovedEmptyUserGrantScopes() throws Exception {
        IdentityMigrationDAO dao = mock(IdentityMigrationDAO.class);
        OAuthClientService clientService = mock(OAuthClientService.class);
        ResourceRowDTO resource = ResourceRowDTO.builder().id("source-resource-1")
                .resourceServerId("resource-1").resourceUri("https://api.example/resource-1")
                .bizCode("identity").appCode("portal").environment("prod").displayName("Portal API")
                .managementClientId("client-service").rbacApplicationCode("portal")
                .entryPermissionCode("portal:read").status("ACTIVE").version(0)
                .createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        ClientRowDTO serviceClient = ClientRowDTO.builder().clientId("client-service").appId("platform-service")
                .clientName("Platform service").clientType("CONFIDENTIAL").status("ACTIVE")
                .pkceRequired(false).accessTokenTtlSeconds(300).refreshTokenTtlSeconds(0).version(2)
                .createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        ClientRowDTO userClient = ClientRowDTO.builder().clientId("client-user").clientName("Portal")
                .clientType("PUBLIC").status("ACTIVE").pkceRequired(true).accessTokenTtlSeconds(300)
                .refreshTokenTtlSeconds(0).version(1).createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        ClientRowDTO userClientWithoutPkce = ClientRowDTO.builder().clientId("client-user-no-pkce")
                .clientName("Portal without PKCE").clientType("PUBLIC").status("ACTIVE").pkceRequired(false)
                .accessTokenTtlSeconds(300).refreshTokenTtlSeconds(0).version(1)
                .createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        ClientRowDTO serviceClientWithRefresh = ClientRowDTO.builder().clientId("client-service-refresh")
                .appId("platform-service-refresh").clientName("Platform service with refresh")
                .clientType("CONFIDENTIAL").status("ACTIVE").pkceRequired(false).accessTokenTtlSeconds(300)
                .refreshTokenTtlSeconds(3600).version(1).createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        GrantRowDTO serviceGrant = GrantRowDTO.builder().id("grant-service").clientId("client-service")
                .resourceServerId("resource-1").grantType("CLIENT_CREDENTIALS").grantContext("PLATFORM")
                .allowedScopes("[\"service.read\"]").status("ACTIVE").version(1)
                .createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        GrantRowDTO userGrant = GrantRowDTO.builder().id("grant-user").clientId("client-user")
                .resourceServerId("resource-1").grantType("USER_DELEGATION").allowedScopes("[]")
                .status("ACTIVE").version(1).createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        RedirectUriRowDTO redirect = RedirectUriRowDTO.builder().id("redirect-1").clientId("client-user")
                .redirectUri("https://portal.example/oauth/callback").createdAt(CREATED_AT).build();

        when(dao.selectUsersAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.selectCredentialsAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.selectTenantsAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.selectMembershipsAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.selectAuditAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        AtomicInteger clientPage = new AtomicInteger();
        when(dao.selectClientsAfter(nullable(String.class), eq(500))).thenAnswer(call ->
                clientPage.getAndIncrement() == 0
                        ? List.of(serviceClient, userClient, userClientWithoutPkce, serviceClientWithRefresh)
                        : List.of());
        when(dao.selectResourcesAfter(nullable(String.class), eq(500))).thenAnswer(call ->
                call.getArgument(0) == null ? List.of(resource) : List.of());
        when(dao.selectResourceById("resource-1")).thenReturn(resource);
        when(dao.selectClientById("client-service")).thenReturn(serviceClient);
        when(dao.selectGrantsForClient("client-service", 65)).thenReturn(List.of(serviceGrant));
        when(dao.selectGrantsForClient("client-user", 65)).thenReturn(List.of(userGrant));
        when(dao.selectRedirectUrisForClient("client-service", 17)).thenReturn(List.of());
        when(dao.selectRedirectUrisForClient("client-user", 17)).thenReturn(List.of(redirect));
        when(dao.selectGrantsForClient("client-user-no-pkce", 65)).thenReturn(List.of());
        when(dao.selectRedirectUrisForClient("client-user-no-pkce", 17)).thenReturn(List.of());
        when(dao.selectOutboxEventsAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.countLegacyClientSecrets()).thenReturn(1L);
        when(dao.countLegacySigningKeys()).thenReturn(1L);
        when(dao.sourceTenantExists("1001")).thenReturn(1);
        when(dao.selectSourceTenantStatus("1001")).thenReturn("ACTIVE");

        Path freezeMarker = temporaryDirectory.resolve("source.freeze");
        Path ownershipMap = temporaryDirectory.resolve("owner-map.json");
        Path secretDirectory = temporaryDirectory.resolve("secrets");
        Files.createDirectory(secretDirectory);
        Files.setPosixFilePermissions(secretDirectory, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        writeOwnerOnly(freezeMarker, "realm=tenant:1001\nsnapshot=snapshot-1\n");
        writeOwnerOnly(ownershipMap, """
                {"schemaVersion":1,"sourceSnapshotId":"snapshot-1","assignments":[
                  {"clientId":"client-service","realmKey":"platform","purpose":"SERVICE",
                   "approvedGrantScopesByResource":{"resource-1":["service.admin"]}},
                  {"clientId":"client-user","realmKey":"tenant:1001","purpose":"USER",
                   "approvedGrantScopesByResource":{"resource-1":["user.read"]}},
                  {"clientId":"client-user-no-pkce","realmKey":"tenant:1001","purpose":"USER",
                   "approvedGrantScopesByResource":{"resource-1":["user.read"]}},
                  {"clientId":"client-service-refresh","realmKey":"platform","purpose":"SERVICE",
                   "approvedGrantScopesByResource":{"resource-1":["service.read"]}}
                ]}
                """);

        OAuthMigrationServiceImpl service = migrationService(dao, clientService);
        OAuthMigrationReportResult report = service.importRealm(OAuthMigrationCommand.builder()
                .realmKey("tenant:1001").sourceSnapshotId("snapshot-1").dryRun(true)
                .freezeMarkerFile(freezeMarker).ownershipMapFile(ownershipMap)
                .checkpointFile(temporaryDirectory.resolve("progress.json"))
                .secretDistributionDirectory(secretDirectory).build());

        assertThat(report.getConflictCount()).isEqualTo(4L);
        assertThat(report.getConflictCodes()).contains(
                "CLIENT:client-service:SERVICE_GRANT_PURPOSE_OR_SCOPE",
                "CLIENT:client-user:USER_GRANT_SCOPE_APPROVAL_REQUIRED",
                "CLIENT:client-user-no-pkce:USER_CLIENT_PKCE_REQUIRED",
                "CLIENT:client-service-refresh:SERVICE_CLIENT_REFRESH_TTL");
        assertThat(report.getConfidentialClientSecretFilesPending()).isZero();
        verify(dao, never()).insertUserMigration(org.mockito.ArgumentMatchers.any());
        verify(dao, never()).insertCredentialMigration(org.mockito.ArgumentMatchers.any());
        verify(dao, never()).insertTenantMigration(org.mockito.ArgumentMatchers.any());
        verify(dao, never()).insertMembershipMigration(org.mockito.ArgumentMatchers.any());
        verify(dao, never()).insertAuditMigration(org.mockito.ArgumentMatchers.any());
        verify(clientService, never()).create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void approvedDryRunDigestBindsPasswordHashesWithoutExposingThem() throws Exception {
        IdentityMigrationDAO dao = mock(IdentityMigrationDAO.class);
        OAuthClientService clientService = mock(OAuthClientService.class);
        AtomicReference<String> passwordHash = new AtomicReference<>("argon2id$legacy-hash-one");
        AtomicReference<TenantRowDTO> tenant = new AtomicReference<>(TenantRowDTO.builder().id("1001")
                .tenantCode("tenant-one").tenantName("Tenant One").status("ACTIVE")
                .settings("{\"theme\":\"one\"}").version(2).createdAt(CREATED_AT).updatedAt(CREATED_AT)
                .createdBy("operator").updatedBy("operator").build());
        AtomicReference<AuditRowDTO> audit = new AtomicReference<>(AuditRowDTO.builder().id("audit-1")
                .eventType("IDENTITY_LOGIN").actorSub("legacy-user").targetSub("legacy-user")
                .sourceIp("192.0.2.1").userAgent("legacy-agent-one").result("SUCCESS").reason("AUTHENTICATED")
                .traceId("trace-1").occurredAt(CREATED_AT).payload("{\"action\":\"login-one\"}").build());
        AtomicReference<ResourceRowDTO> resource = new AtomicReference<>(ResourceRowDTO.builder().id("resource-row-1")
                .resourceServerId("resource-1").resourceUri("https://api.example/resource-1")
                .bizCode("identity").appCode("portal").environment("prod").displayName("Portal API one")
                .managementClientId("manager-client").rbacApplicationCode("portal")
                .entryPermissionCode("portal:read").status("ACTIVE").version(0)
                .createdAt(CREATED_AT).updatedAt(CREATED_AT).build());
        ClientRowDTO managerClient = ClientRowDTO.builder().clientId("manager-client").appId("platform-manager")
                .clientName("Platform manager").clientType("CONFIDENTIAL").status("ACTIVE")
                .pkceRequired(false).accessTokenTtlSeconds(300).refreshTokenTtlSeconds(0).version(0)
                .createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        when(dao.selectUsersAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.selectCredentialsAfter(nullable(String.class), eq(500))).thenAnswer(call -> {
            if (call.getArgument(0) != null) {
                return List.of();
            }
            return List.of(CredentialRowDTO.builder().id("credential-1").identitySub("legacy-user")
                    .credentialType("PASSWORD").passwordHash(passwordHash.get()).passwordChangedAt(CREATED_AT)
                    .mustChangePassword(false).status("ACTIVE").version(4).createdAt(CREATED_AT)
                    .updatedAt(CREATED_AT).build());
        });
        when(dao.sourceUserExists("legacy-user")).thenReturn(1);
        when(dao.selectTenantsAfter(nullable(String.class), eq(500))).thenAnswer(call ->
                call.getArgument(0) == null ? List.of(tenant.get()) : List.of());
        when(dao.selectMembershipsAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.selectAuditAfter(nullable(String.class), eq(500))).thenAnswer(call ->
                call.getArgument(0) == null ? List.of(audit.get()) : List.of());
        when(dao.selectClientsAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.selectResourcesAfter(nullable(String.class), eq(500))).thenAnswer(call ->
                call.getArgument(0) == null ? List.of(resource.get()) : List.of());
        when(dao.selectClientById("manager-client")).thenReturn(managerClient);
        when(dao.selectOutboxEventsAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.countLegacyClientSecrets()).thenReturn(0L);
        when(dao.countLegacySigningKeys()).thenReturn(0L);

        Path freezeMarker = temporaryDirectory.resolve("credential-source.freeze");
        Path ownershipMap = temporaryDirectory.resolve("credential-owner-map.json");
        Path secretDirectory = temporaryDirectory.resolve("credential-secrets");
        Files.createDirectory(secretDirectory);
        Files.setPosixFilePermissions(secretDirectory, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        writeOwnerOnly(freezeMarker, "realm=platform\nsnapshot=snapshot-credentials\n");
        writeOwnerOnly(ownershipMap,
                "{\"schemaVersion\":1,\"sourceSnapshotId\":\"snapshot-credentials\",\"assignments\":[]}");
        OAuthMigrationServiceImpl service = migrationService(dao, clientService);
        OAuthMigrationCommand command = OAuthMigrationCommand.builder().realmKey("platform")
                .sourceSnapshotId("snapshot-credentials").dryRun(true).freezeMarkerFile(freezeMarker)
                .ownershipMapFile(ownershipMap).checkpointFile(temporaryDirectory.resolve("credential-checkpoint.json"))
                .secretDistributionDirectory(secretDirectory).build();

        OAuthMigrationReportResult first = service.importRealm(command);
        passwordHash.set("argon2id$legacy-hash-two");
        OAuthMigrationReportResult passwordChanged = service.importRealm(command);
        tenant.set(TenantRowDTO.builder().id("1001").tenantCode("tenant-one").tenantName("Tenant One")
                .status("ACTIVE").settings("{\"theme\":\"two\"}").version(2).createdAt(CREATED_AT)
                .updatedAt(CREATED_AT).createdBy("operator").updatedBy("operator").build());
        OAuthMigrationReportResult settingsChanged = service.importRealm(command);
        audit.set(AuditRowDTO.builder().id("audit-1").eventType("IDENTITY_LOGIN").actorSub("legacy-user")
                .targetSub("legacy-user").sourceIp("192.0.2.2").userAgent("legacy-agent-two")
                .result("SUCCESS").reason("AUTHENTICATED").traceId("trace-1").occurredAt(CREATED_AT)
                .payload("{\"action\":\"login-two\"}").build());
        OAuthMigrationReportResult auditChanged = service.importRealm(command);
        resource.set(ResourceRowDTO.builder().id("resource-row-1").resourceServerId("resource-1")
                .resourceUri("https://api.example/resource-1").bizCode("identity").appCode("portal")
                .environment("prod").displayName("Portal API two").managementClientId("manager-client")
                .rbacApplicationCode("portal").entryPermissionCode("portal:read").status("ACTIVE").version(0)
                .createdAt(CREATED_AT).updatedAt(CREATED_AT).build());
        OAuthMigrationReportResult resourceChanged = service.importRealm(command);

        assertThat(List.of(first.getReportSha256(), passwordChanged.getReportSha256(), settingsChanged.getReportSha256(),
                auditChanged.getReportSha256(), resourceChanged.getReportSha256())).doesNotHaveDuplicates();
        assertThat(first.toString()).doesNotContain("argon2id$legacy-hash-one", "argon2id$legacy-hash-two",
                "legacy-agent-one", "legacy-agent-two", "login-one", "login-two", "192.0.2.1", "192.0.2.2");
        verify(dao, never()).insertCredentialMigration(any());
    }

    @Test
    void importsPlatformServiceGrantsWithoutPuttingResourceUrisOnTheServiceClient() throws Exception {
        IdentityMigrationDAO dao = mock(IdentityMigrationDAO.class);
        OAuthClientService clientService = mock(OAuthClientService.class);
        ResourceServerService resourceServerService = mock(ResourceServerService.class);
        OAuthSecurityProjectionComponent securityProjection = mock(OAuthSecurityProjectionComponent.class);
        OAuthClientRepository clientRepository = mock(OAuthClientRepository.class);
        OAuthResourceRepository resourceRepository = mock(OAuthResourceRepository.class);
        OAuthGrantRepository grantRepository = mock(OAuthGrantRepository.class);
        TransactionalOutboxResourceServerEventAdapter resourceEvents =
                mock(TransactionalOutboxResourceServerEventAdapter.class);
        OAuthRealmRepository realmRepository = mock(OAuthRealmRepository.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        Environment environment = mock(Environment.class);
        when(environment.getProperty("egon.tianquan-shoubing.oauth.public-origin"))
                .thenReturn("https://identity.example");

        ClientRowDTO sourceClient = ClientRowDTO.builder().clientId("client-service")
                .appId("platform-service").clientName("Platform service").clientType("CONFIDENTIAL")
                .status("ACTIVE").pkceRequired(false).accessTokenTtlSeconds(300).refreshTokenTtlSeconds(0)
                .version(1).createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        ResourceRowDTO sourceResource = ResourceRowDTO.builder().id("legacy-resource-1")
                .resourceServerId("resource-1").resourceUri("https://api.example/resource-1")
                .bizCode("identity").appCode("portal").environment("prod").displayName("Portal API")
                .managementClientId("client-service").rbacApplicationCode("portal")
                .entryPermissionCode("portal:read").status("ACTIVE").version(1)
                .createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        GrantRowDTO sourceGrant = GrantRowDTO.builder().id("grant-service").clientId("client-service")
                .resourceServerId("resource-1").grantType("CLIENT_CREDENTIALS").grantContext("PLATFORM")
                .allowedScopes("[\"service.read\"]").status("ACTIVE").version(1)
                .createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        String plaintextSecret = "local-only-migration-secret-value-0123456789";
        String secretHash = new BCryptPasswordEncoder(12).encode(plaintextSecret);
        OAuthClientBO targetClient = OAuthClientBO.builder().id(501L).version(1L)
                .clientId(sourceClient.getClientId()).appId(sourceClient.getAppId())
                .clientName(sourceClient.getClientName()).clientType(OAuthClient.ClientType.CONFIDENTIAL)
                .purpose(OAuthClientPurposeEnum.SERVICE).status(OAuthClient.Status.ACTIVE)
                .secretHash(secretHash).credentialVersion(1L).redirectUris(List.of()).resourceUris(List.of())
                .allowedScopes(List.of("service.read")).accessTtlSeconds(300).refreshTtlSeconds(0)
                .createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        OAuthResourceBO targetResource = OAuthResourceBO.builder().id(601L).version(1L)
                .resourceServerId(sourceResource.getResourceServerId()).resourceUri(URI.create(sourceResource.getResourceUri()))
                .bizCode(sourceResource.getBizCode()).appCode(sourceResource.getAppCode())
                .environment(sourceResource.getEnvironment()).displayName(sourceResource.getDisplayName())
                .managementClientId(sourceResource.getManagementClientId()).managementClientPk(501L)
                .rbacApplicationCode(sourceResource.getRbacApplicationCode())
                .entryPermissionCode(sourceResource.getEntryPermissionCode()).status(ResourceServerStatus.ACTIVE)
                .admissionTicketTtl(Duration.ofMinutes(5)).createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        OAuthGrantBO targetGrant = OAuthGrantBO.builder().id(701L).version(1L).clientPk(501L)
                .resourceServerId("resource-1").grantType(ResourceGrantType.CLIENT_CREDENTIALS)
                .allowedScopes(List.of("service.read")).status(ClientResourceGrant.Status.ACTIVE)
                .resourceVersion(1L).build();
        when(dao.selectUsersAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.selectCredentialsAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.selectTenantsAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.selectMembershipsAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.selectAuditAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.selectClientsAfter(nullable(String.class), eq(500))).thenAnswer(call ->
                call.getArgument(0) == null ? List.of(sourceClient) : List.of());
        when(dao.selectResourcesAfter(nullable(String.class), eq(500))).thenAnswer(call ->
                call.getArgument(0) == null ? List.of(sourceResource) : List.of());
        when(dao.selectClientById("client-service")).thenReturn(sourceClient);
        when(dao.selectResourceById("resource-1")).thenReturn(sourceResource);
        when(dao.selectGrantsForClient("client-service", 65)).thenReturn(List.of(sourceGrant));
        when(dao.selectRedirectUrisForClient("client-service", 17)).thenReturn(List.of());
        when(dao.selectOutboxEventsAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.countLegacyClientSecrets()).thenReturn(0L);
        when(dao.countLegacySigningKeys()).thenReturn(0L);
        when(dao.restoreClientHistory(any(Long.class), eq("client-service"), eq(1L), eq(CREATED_AT), eq(CREATED_AT)))
                .thenReturn(1);
        when(dao.restoreResourceHistory(any(Long.class), eq("resource-1"), eq(1L), eq(CREATED_AT), eq(CREATED_AT)))
                .thenReturn(1);
        when(dao.restoreGrantHistory(any(Long.class), eq(501L), eq("resource-1"),
                eq("CLIENT_CREDENTIALS"), eq(1L), eq(CREATED_AT), eq(CREATED_AT))).thenReturn(1);
        when(realmRepository.findByRealmKey(any())).thenReturn(Optional.empty());
        AtomicInteger clientLookups = new AtomicInteger();
        when(clientRepository.findByClientId(any(), eq("client-service"))).thenAnswer(call ->
                clientLookups.getAndIncrement() == 0 ? Optional.empty() : Optional.of(targetClient));
        when(clientService.create(any(), any(), any(), any())).thenReturn(CreatedOAuthClientVO.builder()
                .clientSecret(plaintextSecret).build());
        when(resourceServerService.create(any(), any())).thenReturn(null);
        AtomicInteger resourceLookups = new AtomicInteger();
        when(resourceRepository.findByResourceServerId(any(), eq("resource-1"))).thenAnswer(call ->
                resourceLookups.getAndIncrement() == 0 ? Optional.empty() : Optional.of(targetResource));
        AtomicInteger grantLookups = new AtomicInteger();
        when(grantRepository.findGrant(any(), eq(501L), eq("resource-1"),
                eq(ResourceGrantType.CLIENT_CREDENTIALS))).thenAnswer(call ->
                grantLookups.getAndIncrement() == 0 ? Optional.empty() : Optional.of(targetGrant));
        when(grantRepository.save(any(), any(), eq(0L))).thenAnswer(call -> call.getArgument(1));

        Path freezeMarker = temporaryDirectory.resolve("platform-source.freeze");
        Path ownershipMap = temporaryDirectory.resolve("platform-owner-map.json");
        Path secretDirectory = temporaryDirectory.resolve("platform-secrets");
        Files.createDirectory(secretDirectory);
        Files.setPosixFilePermissions(secretDirectory, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        writeOwnerOnly(freezeMarker, "realm=platform\nsnapshot=snapshot-platform\n");
        writeOwnerOnly(ownershipMap, """
                {"schemaVersion":1,"sourceSnapshotId":"snapshot-platform","assignments":[
                  {"clientId":"client-service","realmKey":"platform","purpose":"SERVICE",
                   "approvedGrantScopesByResource":{"resource-1":["service.read"]}}
                ]}
                """);
        OAuthMigrationServiceImpl service = new OAuthMigrationServiceImpl(
                dao, new IdentityDirectoryContext(77351065313480704L, "tenantId"), clientService,
                resourceServerService, securityProjection, clientRepository, resourceRepository, grantRepository,
                resourceEvents, realmRepository, transactionManager, new ObjectMapper(), environment);
        OAuthMigrationCommand dryRun = OAuthMigrationCommand.builder().realmKey("platform")
                .sourceSnapshotId("snapshot-platform").dryRun(true).freezeMarkerFile(freezeMarker)
                .ownershipMapFile(ownershipMap).checkpointFile(temporaryDirectory.resolve("platform-checkpoint.json"))
                .secretDistributionDirectory(secretDirectory).build();
        OAuthMigrationReportResult approved = service.importRealm(dryRun);
        OAuthMigrationCommand apply = OAuthMigrationCommand.builder().realmKey(dryRun.getRealmKey())
                .sourceSnapshotId(dryRun.getSourceSnapshotId()).dryRun(false)
                .freezeMarkerFile(dryRun.getFreezeMarkerFile()).ownershipMapFile(dryRun.getOwnershipMapFile())
                .checkpointFile(dryRun.getCheckpointFile())
                .secretDistributionDirectory(dryRun.getSecretDistributionDirectory())
                .approvedReportSha256(approved.getReportSha256()).build();

        OAuthMigrationReportResult imported = service.importRealm(apply);

        assertThat(imported.getConflictCount()).isZero();
        assertThat(imported.getImportedCount()).isEqualTo(3L);
        ArgumentCaptor<CreateOAuthClientDTO> createCaptor = ArgumentCaptor.forClass(CreateOAuthClientDTO.class);
        verify(clientService).create(any(), any(), createCaptor.capture(), any());
        assertThat(createCaptor.getValue().getResourceUris()).isEmpty();
        assertThat(createCaptor.getValue().getAllowedScopes()).containsExactly("service.read");
        assertThat(createCaptor.getValue().getRefreshTokenTtlSeconds()).isZero();
        Path secretArtifact;
        try (var files = Files.list(secretDirectory)) {
            secretArtifact = files.findFirst().orElseThrow();
        }
        assertThat(Files.getPosixFilePermissions(secretArtifact))
                .containsExactlyInAnyOrder(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
        assertThat(new ObjectMapper().readTree(secretArtifact.toFile()).path("clientSecret").asText())
                .isEqualTo(plaintextSecret);
        verify(resourceEvents).enqueueServiceGrantProjection(any(), any(), any());

        OAuthMigrationReportResult replayed = service.importRealm(apply);
        assertThat(replayed.getImportedCount()).isEqualTo(imported.getImportedCount());
        verify(clientService, org.mockito.Mockito.times(1)).create(any(), any(), any(), any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void importsApprovedUserScopesAndUpdatesOnlyFreshEmptyAssociation(boolean legacyScopesEmpty, boolean freshAssociation) throws Exception {
        IdentityMigrationDAO dao = mock(IdentityMigrationDAO.class);
        OAuthClientService clientService = mock(OAuthClientService.class);
        ResourceServerService resourceServerService = mock(ResourceServerService.class);
        OAuthSecurityProjectionComponent securityProjection = mock(OAuthSecurityProjectionComponent.class);
        OAuthClientRepository clientRepository = mock(OAuthClientRepository.class);
        OAuthResourceRepository resourceRepository = mock(OAuthResourceRepository.class);
        OAuthGrantRepository grantRepository = mock(OAuthGrantRepository.class);
        TransactionalOutboxResourceServerEventAdapter resourceEvents =
                mock(TransactionalOutboxResourceServerEventAdapter.class);
        OAuthRealmRepository realmRepository = mock(OAuthRealmRepository.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        Environment environment = mock(Environment.class);
        when(environment.getProperty("egon.tianquan-shoubing.oauth.public-origin"))
                .thenReturn("https://identity.example");

        ClientRowDTO managementClient = ClientRowDTO.builder().clientId("client-service")
                .appId("platform-service").clientName("Manager").clientType("CONFIDENTIAL")
                .status("ACTIVE").pkceRequired(false).accessTokenTtlSeconds(300).refreshTokenTtlSeconds(0)
                .version(1).createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        ClientRowDTO sourceClient = ClientRowDTO.builder().clientId("client-user").clientName("User portal").clientType("PUBLIC")
                .status("ACTIVE").pkceRequired(true).accessTokenTtlSeconds(300).refreshTokenTtlSeconds(0)
                .version(1).createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        ResourceRowDTO sourceResource = ResourceRowDTO.builder().id("legacy-resource-1")
                .resourceServerId("resource-1").resourceUri("https://api.example/resource-1")
                .bizCode("identity").appCode("portal").environment("prod").displayName("Portal API")
                .managementClientId("client-service").rbacApplicationCode("portal")
                .entryPermissionCode("portal:read").status("ACTIVE").version(1)
                .createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        GrantRowDTO sourceGrant = GrantRowDTO.builder().id("grant-user").clientId("client-user")
                .resourceServerId("resource-1").grantType("USER_DELEGATION")
                .allowedScopes(legacyScopesEmpty ? "[]" : "[\"orders.read\"]").status("ACTIVE").version(1)
                .createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        OAuthClientBO targetClient = OAuthClientBO.builder().id(501L).version(1L)
                .clientId(sourceClient.getClientId()).appId(sourceClient.getAppId())
                .clientName(sourceClient.getClientName()).clientType(OAuthClient.ClientType.PUBLIC)
                .purpose(OAuthClientPurposeEnum.USER).status(OAuthClient.Status.ACTIVE)
                .credentialVersion(1L).redirectUris(List.of("https://portal.example/callback"))
                .resourceUris(List.of("https://api.example/resource-1"))
                .allowedScopes(List.of("orders.read")).accessTtlSeconds(300).refreshTtlSeconds(0)
                .createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        OAuthResourceBO targetResource = OAuthResourceBO.builder().id(601L).version(1L)
                .resourceServerId(sourceResource.getResourceServerId()).resourceUri(URI.create(sourceResource.getResourceUri()))
                .bizCode(sourceResource.getBizCode()).appCode(sourceResource.getAppCode())
                .environment(sourceResource.getEnvironment()).displayName(sourceResource.getDisplayName())
                .managementClientId(sourceResource.getManagementClientId()).managementClientPk(501L)
                .rbacApplicationCode(sourceResource.getRbacApplicationCode())
                .entryPermissionCode(sourceResource.getEntryPermissionCode()).status(ResourceServerStatus.ACTIVE)
                .admissionTicketTtl(Duration.ofMinutes(5)).createdAt(CREATED_AT).updatedAt(CREATED_AT).build();
        OAuthGrantBO targetGrant = OAuthGrantBO.builder().id(701L).version(0L).clientPk(501L)
                .resourceServerId("resource-1").grantType(ResourceGrantType.USER_DELEGATION)
                .allowedScopes(List.of()).status(ClientResourceGrant.Status.ACTIVE)
                .resourceVersion(1L).build();
        when(dao.selectUsersAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.selectCredentialsAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.selectTenantsAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.selectMembershipsAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.selectAuditAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.selectClientsAfter(nullable(String.class), eq(500))).thenAnswer(call ->
                call.getArgument(0) == null ? List.of(managementClient, sourceClient)
                        : "client-service".equals(call.getArgument(0)) ? List.of(sourceClient) : List.of());
        when(dao.selectResourcesAfter(nullable(String.class), eq(500))).thenAnswer(call ->
                call.getArgument(0) == null ? List.of(sourceResource) : List.of());
        when(dao.selectClientById("client-service")).thenReturn(managementClient);
        when(dao.sourceTenantExists("1001")).thenReturn(1);
        when(dao.selectSourceTenantStatus("1001")).thenReturn("ACTIVE");
        when(dao.selectTenantByExternalId(77351065313480704L, "1001")).thenReturn(
                top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.po.IdentityTenantPO.builder()
                        .id(1001L).identityTenantId("1001").status("ACTIVE").build());
        when(dao.selectResourceById("resource-1")).thenReturn(sourceResource);
        when(dao.selectGrantsForClient("client-service", 65)).thenReturn(List.of(
                GrantRowDTO.builder().id("manager-grant").clientId("client-service")
                        .resourceServerId("resource-1").grantType("CLIENT_CREDENTIALS").grantContext("PLATFORM")
                        .allowedScopes("[\"orders.manage\"]").status("ACTIVE").version(1L)
                        .createdAt(CREATED_AT).updatedAt(CREATED_AT).build()));
        when(dao.selectRedirectUrisForClient("client-service", 17)).thenReturn(List.of());
        when(dao.selectGrantsForClient("client-user", 65)).thenReturn(List.of(sourceGrant));
        when(dao.selectRedirectUrisForClient("client-user", 17)).thenReturn(List.of(
                RedirectUriRowDTO.builder().id("redirect-user").clientId("client-user")
                        .redirectUri("https://portal.example/callback").createdAt(CREATED_AT).build()));
        when(dao.selectOutboxEventsAfter(nullable(String.class), eq(500))).thenReturn(List.of());
        when(dao.countLegacyClientSecrets()).thenReturn(0L);
        when(dao.countLegacySigningKeys()).thenReturn(0L);
        when(dao.restoreClientHistory(any(Long.class), eq("client-user"), eq(1L), eq(CREATED_AT), eq(CREATED_AT)))
                .thenReturn(1);
        when(dao.restoreGrantHistory(any(Long.class), eq(501L), eq("resource-1"),
                eq("USER_DELEGATION"), eq(1L), eq(CREATED_AT), eq(CREATED_AT))).thenAnswer(call -> {
                    targetGrant.setVersion(1L);
                    return 1;
                });
        when(realmRepository.findByRealmKey(any())).thenReturn(Optional.empty());
        AtomicInteger clientLookups = new AtomicInteger();
        when(clientRepository.findByClientId(any(), eq("client-user"))).thenAnswer(call ->
                clientLookups.getAndIncrement() == 0 ? Optional.empty() : Optional.of(targetClient));
        when(clientService.create(any(), any(), any(), any())).thenReturn(CreatedOAuthClientVO.builder()
                .build());
        when(resourceRepository.findByResourceServerId(any(), eq("resource-1")))
                .thenReturn(Optional.of(targetResource));
        AtomicReference<OAuthGrantBO> storedGrant = new AtomicReference<>(freshAssociation ? targetGrant : null);
        when(grantRepository.findGrant(any(), eq(501L), eq("resource-1"),
                eq(ResourceGrantType.USER_DELEGATION))).thenAnswer(call -> Optional.ofNullable(storedGrant.get()));
        when(grantRepository.save(any(), any(), eq(0L))).thenAnswer(call -> {
            OAuthGrantBO saved = call.getArgument(1);
            targetGrant.setAllowedScopes(saved.getAllowedScopes()).setVersion(saved.getVersion())
                    .setStatus(saved.getStatus());
            storedGrant.set(targetGrant);
            return targetGrant;
        });

        Path freezeMarker = temporaryDirectory.resolve("user-source.freeze");
        Path ownershipMap = temporaryDirectory.resolve("user-owner-map.json");
        Path secretDirectory = temporaryDirectory.resolve("user-secrets");
        Files.createDirectory(secretDirectory);
        Files.setPosixFilePermissions(secretDirectory, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        writeOwnerOnly(freezeMarker, "realm=tenant:1001\nsnapshot=snapshot-user\n");
        writeOwnerOnly(ownershipMap, """
                {"schemaVersion":1,"sourceSnapshotId":"snapshot-user","assignments":[
                  {"clientId":"client-service","realmKey":"platform","purpose":"SERVICE",
                   "approvedGrantScopesByResource":{"resource-1":["orders.manage"]}},
                  {"clientId":"client-user","realmKey":"tenant:1001","purpose":"USER",
                   "scopeApprovalId":"review-user-business-scopes",
                   "approvedGrantScopesByResource":{"resource-1":["orders.read"]}}
                ]}
                """);
        OAuthMigrationServiceImpl service = new OAuthMigrationServiceImpl(
                dao, new IdentityDirectoryContext(77351065313480704L, "tenantId"), clientService,
                resourceServerService, securityProjection, clientRepository, resourceRepository, grantRepository,
                resourceEvents, realmRepository, transactionManager, new ObjectMapper(), environment);
        OAuthMigrationCommand dryRun = OAuthMigrationCommand.builder().realmKey("tenant:1001")
                .sourceSnapshotId("snapshot-user").dryRun(true).freezeMarkerFile(freezeMarker)
                .ownershipMapFile(ownershipMap).checkpointFile(temporaryDirectory.resolve("user-checkpoint.json"))
                .secretDistributionDirectory(secretDirectory).build();
        OAuthMigrationReportResult approved = service.importRealm(dryRun);
        assertThat(approved.getConflictCodes()).isEmpty();
        OAuthMigrationCommand apply = OAuthMigrationCommand.builder().realmKey(dryRun.getRealmKey())
                .sourceSnapshotId(dryRun.getSourceSnapshotId()).dryRun(false)
                .freezeMarkerFile(dryRun.getFreezeMarkerFile()).ownershipMapFile(dryRun.getOwnershipMapFile())
                .checkpointFile(dryRun.getCheckpointFile())
                .secretDistributionDirectory(dryRun.getSecretDistributionDirectory())
                .approvedReportSha256(approved.getReportSha256()).build();

        OAuthMigrationReportResult imported = service.importRealm(apply);

        assertThat(imported.getConflictCount()).isZero();
        assertThat(imported.getImportedCount()).isEqualTo(1L);
        ArgumentCaptor<CreateOAuthClientDTO> createCaptor = ArgumentCaptor.forClass(CreateOAuthClientDTO.class);
        verify(clientService).create(any(), any(), createCaptor.capture(), any());
        assertThat(createCaptor.getValue().getResourceUris()).containsExactly("https://api.example/resource-1");
        assertThat(createCaptor.getValue().getAllowedScopes()).containsExactly("orders.read");
        assertThat(createCaptor.getValue().getRefreshTokenTtlSeconds()).isZero();
        assertThat(targetGrant.getAllowedScopes()).containsExactly("orders.read");
        assertThat(targetGrant.getVersion()).isEqualTo(1L);
        verify(resourceEvents, never()).enqueueServiceGrantProjection(any(), any(), any());

        OAuthMigrationReportResult replayed = service.importRealm(apply);
        assertThat(replayed.getImportedCount()).isEqualTo(imported.getImportedCount());
        verify(clientService, times(1)).create(any(), any(), any(), any());
        verify(grantRepository, times(1)).save(any(), any(), eq(0L));
        Files.delete(apply.getCheckpointFile());
        targetGrant.setAllowedScopes(List.of()).setVersion(0L);
        assertThatThrownBy(() -> service.importRealm(apply))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OAUTH_MIGRATION_TARGET_GRANT_CONTENT_CONFLICT");
        assertThat(new ObjectMapper().readTree(apply.getCheckpointFile().toFile()).path("phase").asText())
                .isEqualTo("TENANT_CLIENTS");
        targetGrant.setAllowedScopes(List.of("orders.write")).setVersion(1L);
        assertThatThrownBy(() -> service.importRealm(apply))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OAUTH_MIGRATION_TARGET_GRANT_CONTENT_CONFLICT");
        if (legacyScopesEmpty) {
            writeOwnerOnly(ownershipMap, Files.readString(ownershipMap).replace("orders.read", "openid"));
            assertThat(service.importRealm(dryRun).getConflictCodes())
                    .contains("CLIENT:client-user:USER_GRANT_CONTEXT_OR_SCOPE");
        }
    }

    @Test
    void rejectsUnknownAndInconsistentCheckpointStatesBeforeTargetEffects() throws Exception {
        IdentityMigrationDAO dao = mock(IdentityMigrationDAO.class);
        OAuthMigrationServiceImpl service = directoryMigrationService(dao,
                mock(PlatformTransactionManager.class), new ObjectMapper()
                .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        OAuthMigrationCommand command = directoryCommand("invalid-state");
        String digest = service.importRealm(command).getReportSha256();
        command.setDryRun(false).setApprovedReportSha256(digest);
        ObjectMapper mapper = new ObjectMapper();
        for (String invalid : List.of(
                "{\"phase\":\"UNRECOGNIZED\",\"typeIndex\":5}",
                "{\"phase\":\"TENANT_CLIENTS\",\"typeIndex\":5}",
                "{\"phase\":\"COMPLETE\",\"typeIndex\":5,\"complete\":false}",
                "{\"phase\":\"DIRECTORY\",\"typeIndex\":0,\"complete\":true}",
                "{\"phase\":\"DIRECTORY\",\"typeIndex\":0,\"importedCount\":-1}",
                "{\"phase\":\"COMPLETE\",\"typeIndex\":5,\"complete\":true,\"importedCount\":1}",
                "{\"phase\":\"OAUTH_PENDING\",\"typeIndex\":0}",
                "{\"phase\":\"RESOURCES\",\"typeIndex\":5,\"lastSourceId\":\"wrong-directory-cursor\"}",
                "{\"phase\":\"DIRECTORY\",\"typeIndex\":0,\"unknownState\":true}")) {
            ObjectNode checkpoint = checkpoint(mapper, command, digest);
            ((ObjectNode) mapper.readTree(invalid)).fields().forEachRemaining(entry ->
                    checkpoint.set(entry.getKey(), entry.getValue()));
            writeOwnerOnly(command.getCheckpointFile(), mapper.writeValueAsString(checkpoint));
            assertThatThrownBy(() -> service.importRealm(command)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("OAUTH_MIGRATION_CHECKPOINT");
        }
        verify(dao, never()).insertUserMigration(any());
    }

    @Test
    void imports500And501RowsInBoundedTransactionsAndKeepsCompletedReplayStable() throws Exception {
        for (int count : List.of(500, 501)) {
            IdentityMigrationDAO dao = mock(IdentityMigrationDAO.class);
            PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
            Map<String, IdentityUserPO> committed = new LinkedHashMap<>();
            List<Integer> batches = new ArrayList<>();
            AtomicInteger generatedId = new AtomicInteger(1000);
            configureUserTransactions(dao, transactions, count, committed, batches, false);
            OAuthMigrationServiceImpl service = directoryMigrationService(dao, transactions, new ObjectMapper());
            OAuthMigrationCommand command = directoryCommand("batch-" + count);
            command.setApprovedReportSha256(service.importRealm(command).getReportSha256()).setDryRun(false);
            try (var ids = mockStatic(SnowflakeIdGenerator.class)) {
                ids.when(SnowflakeIdGenerator::nextLongId).thenAnswer(call -> (long) generatedId.incrementAndGet());
                OAuthMigrationReportResult applied = service.importRealm(command);
                assertThat(applied.getImportedCount()).isEqualTo(count);
                assertThat(committed).hasSize(count);
                assertThat(batches).containsExactlyElementsOf(count == 500 ? List.of(500) : List.of(500, 1));
                IdentityUserPO last = committed.get(String.format("user-%04d", count));
                assertThat(last.getVersion()).isEqualTo(7);
                assertThat(last.getCreateTime()).isEqualTo(CREATED_AT);
                assertThat(last.getUpdateTime()).isEqualTo(CREATED_AT.plusSeconds(30));
                assertThat(last.getTenantId()).isEqualTo(77351065313480704L);
                assertThat(service.importRealm(command).getImportedCount()).isEqualTo(count);
                verify(dao, times(count)).insertUserMigration(any());
            }
        }
    }

    @Test
    void reconcilesCommittedRowsAfterCheckpointFailureWithoutDuplicateWrites() throws Exception {
        IdentityMigrationDAO dao = mock(IdentityMigrationDAO.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        Map<String, IdentityUserPO> committed = new LinkedHashMap<>();
        List<Integer> batches = new ArrayList<>();
        configureUserTransactions(dao, transactions, 501, committed, batches, false);
        AtomicInteger failures = new AtomicInteger(1);
        ObjectMapper failingCheckpointMapper = new ObjectMapper() {
            @Override
            public byte[] writeValueAsBytes(Object value) throws com.fasterxml.jackson.core.JsonProcessingException {
                if (value.getClass().getSimpleName().equals("MigrationCheckpoint")
                        && failures.getAndDecrement() > 0) {
                    throw new com.fasterxml.jackson.core.JsonGenerationException("injected checkpoint failure", (com.fasterxml.jackson.core.JsonGenerator) null);
                }
                return super.writeValueAsBytes(value);
            }
        };
        OAuthMigrationServiceImpl service = directoryMigrationService(dao, transactions, failingCheckpointMapper);
        OAuthMigrationCommand command = directoryCommand("checkpoint-crash");
        command.setApprovedReportSha256(service.importRealm(command).getReportSha256()).setDryRun(false);
        AtomicInteger generatedId = new AtomicInteger(2000);
        try (var ids = mockStatic(SnowflakeIdGenerator.class)) {
            ids.when(SnowflakeIdGenerator::nextLongId).thenAnswer(call -> (long) generatedId.incrementAndGet());
            assertThatThrownBy(() -> service.importRealm(command))
                    .hasMessage("OAUTH_MIGRATION_CHECKPOINT_WRITE_FAILED");
            assertThat(committed).hasSize(500);
            assertThat(Files.exists(command.getCheckpointFile())).isFalse();
            OAuthMigrationReportResult replay = service.importRealm(command);
            assertThat(replay.getImportedCount()).isEqualTo(1);
            assertThat(replay.getSkippedCount()).isEqualTo(500);
            assertThat(committed).hasSize(501);
            verify(dao, times(501)).insertUserMigration(any());
        }
    }

    @Test
    void reconcilesUnknownCommitAndRejectsStaleApprovalWithoutAdvancingCheckpoint() throws Exception {
        IdentityMigrationDAO dao = mock(IdentityMigrationDAO.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        Map<String, IdentityUserPO> committed = new LinkedHashMap<>();
        List<Integer> batches = new ArrayList<>();
        configureUserTransactions(dao, transactions, 501, committed, batches, true);
        OAuthMigrationServiceImpl service = directoryMigrationService(dao, transactions, new ObjectMapper());
        OAuthMigrationCommand command = directoryCommand("unknown-commit");
        String digest = service.importRealm(command).getReportSha256();
        command.setApprovedReportSha256(digest).setDryRun(false);
        AtomicInteger generatedId = new AtomicInteger(3000);
        try (var ids = mockStatic(SnowflakeIdGenerator.class)) {
            ids.when(SnowflakeIdGenerator::nextLongId).thenAnswer(call -> (long) generatedId.incrementAndGet());
            assertThatThrownBy(() -> service.importRealm(command)).isInstanceOf(TransactionSystemException.class);
            assertThat(committed).hasSize(500);
            assertThat(Files.exists(command.getCheckpointFile())).isFalse();
            ObjectNode stale = checkpoint(new ObjectMapper(), command, "0".repeat(64));
            writeOwnerOnly(command.getCheckpointFile(), stale.toString());
            assertThatThrownBy(() -> service.importRealm(command)).hasMessage("OAUTH_MIGRATION_CHECKPOINT_MISMATCH");
            assertThat(committed).hasSize(500);
            Files.delete(command.getCheckpointFile());
            committed.get("user-0001").setDisplayName("unexpected target change");
            assertThatThrownBy(() -> service.importRealm(command)).hasMessage("OAUTH_MIGRATION_USER_CONTENT_CONFLICT");
            assertThat(Files.exists(command.getCheckpointFile())).isFalse();
            committed.get("user-0001").setDisplayName("Operator 1");
            OAuthMigrationReportResult replay = service.importRealm(command);
            assertThat(replay.getImportedCount()).isEqualTo(1);
            assertThat(replay.getSkippedCount()).isEqualTo(500);
            verify(dao, times(501)).insertUserMigration(any());
        }
    }

    @Test
    void rejectsSourcePageMutationInsideTheImportTransactionBeforeInsertingRows() throws Exception {
        IdentityMigrationDAO dao = mock(IdentityMigrationDAO.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        Map<String, IdentityUserPO> committed = new LinkedHashMap<>();
        configureUserTransactions(dao, transactions, 1, committed, new ArrayList<>(), false);
        OAuthMigrationServiceImpl service = directoryMigrationService(dao, transactions, new ObjectMapper());
        OAuthMigrationCommand command = directoryCommand("changed-page");
        command.setApprovedReportSha256(service.importRealm(command).getReportSha256()).setDryRun(false);
        AtomicInteger firstPageReads = new AtomicInteger();
        when(dao.selectUsersAfter(nullable(String.class), eq(500))).thenAnswer(call -> {
            if (call.getArgument(0) != null) {
                return List.of();
            }
            return List.of(UserRowDTO.builder().id("user-0001").username("operator-1")
                    .normalizedUsername("operator-1").displayName(firstPageReads.incrementAndGet() <= 2
                            ? "Operator 1" : "Changed source")
                    .status("ACTIVE").failedLoginCount(0).version(7).createdAt(CREATED_AT)
                    .updatedAt(CREATED_AT.plusSeconds(30)).build());
        });
        assertThatThrownBy(() -> service.importRealm(command)).hasMessage("OAUTH_MIGRATION_SOURCE_PAGE_CHANGED");
        assertThat(committed).isEmpty();
        assertThat(Files.exists(command.getCheckpointFile())).isFalse();
        verify(dao, never()).insertUserMigration(any());
        verify(transactions).rollback(any());
    }

    private OAuthMigrationCommand directoryCommand(String name) throws Exception {
        Path secretDirectory = temporaryDirectory.resolve(name + "-secrets");
        Files.createDirectory(secretDirectory);
        Files.setPosixFilePermissions(secretDirectory, Set.of(PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        Path marker = temporaryDirectory.resolve(name + ".freeze");
        Path owners = temporaryDirectory.resolve(name + "-owners.json");
        writeOwnerOnly(marker, "realm=platform\nsnapshot=" + name + "\n");
        writeOwnerOnly(owners, "{\"schemaVersion\":1,\"sourceSnapshotId\":\"" + name + "\",\"assignments\":[]}");
        return OAuthMigrationCommand.builder().realmKey("platform").sourceSnapshotId(name).dryRun(true)
                .freezeMarkerFile(marker).ownershipMapFile(owners)
                .checkpointFile(temporaryDirectory.resolve(name + "-checkpoint.json"))
                .secretDistributionDirectory(secretDirectory).build();
    }

    private static ObjectNode checkpoint(ObjectMapper mapper, OAuthMigrationCommand command, String digest) {
        ObjectNode checkpoint = mapper.createObjectNode();
        checkpoint.put("realmKey", command.getRealmKey()).put("sourceSnapshotId", command.getSourceSnapshotId())
                .put("reportSha256", digest).put("typeIndex", 0).putNull("lastSourceId")
                .put("importedCount", 0).put("skippedCount", 0).put("phase", "DIRECTORY")
                .putNull("oauthCursor").put("complete", false);
        return checkpoint;
    }

    private OAuthMigrationServiceImpl directoryMigrationService(IdentityMigrationDAO dao,
            PlatformTransactionManager transactions, ObjectMapper mapper) {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("egon.tianquan-shoubing.oauth.public-origin")).thenReturn("https://idp.example");
        return new OAuthMigrationServiceImpl(dao, new IdentityDirectoryContext(77351065313480704L, "tenantId"),
                mock(OAuthClientService.class), mock(ResourceServerService.class),
                mock(OAuthSecurityProjectionComponent.class), mock(OAuthClientRepository.class),
                mock(OAuthResourceRepository.class), mock(OAuthGrantRepository.class),
                mock(TransactionalOutboxResourceServerEventAdapter.class), mock(OAuthRealmRepository.class),
                transactions, mapper, environment);
    }

    private static void configureUserTransactions(IdentityMigrationDAO dao, PlatformTransactionManager transactions,
            int count, Map<String, IdentityUserPO> committed, List<Integer> batches, boolean unknownFirstCommit) {
        List<UserRowDTO> source = java.util.stream.IntStream.rangeClosed(1, count).mapToObj(index ->
                UserRowDTO.builder().id(String.format("user-%04d", index)).username("operator-" + index)
                        .normalizedUsername("operator-" + index).displayName("Operator " + index)
                        .status("ACTIVE").failedLoginCount(0).version(7).createdAt(CREATED_AT)
                        .updatedAt(CREATED_AT.plusSeconds(30)).build()).toList();
        when(dao.selectUsersAfter(nullable(String.class), eq(500))).thenAnswer(call -> {
            String cursor = call.getArgument(0);
            return source.stream().filter(row -> cursor == null || row.getId().compareTo(cursor) > 0)
                    .limit(500).toList();
        });
        Map<String, IdentityUserPO> staged = new LinkedHashMap<>();
        when(dao.selectUserByIdentitySub(eq(77351065313480704L), any())).thenAnswer(call ->
                committed.get(call.getArgument(1)));
        when(dao.insertUserMigration(any())).thenAnswer(call -> {
            IdentityUserPO row = call.getArgument(0);
            assertThat(committed).doesNotContainKey(row.getIdentitySub());
            assertThat(staged.put(row.getIdentitySub(), row)).isNull();
            return 1;
        });
        when(transactions.getTransaction(any())).thenAnswer(call -> {
            staged.clear();
            return new SimpleTransactionStatus();
        });
        AtomicInteger commitNumber = new AtomicInteger();
        doAnswer(call -> {
            batches.add(staged.size());
            committed.putAll(staged);
            staged.clear();
            if (unknownFirstCommit && commitNumber.getAndIncrement() == 0) {
                throw new TransactionSystemException("injected unknown commit outcome");
            }
            return null;
        }).when(transactions).commit(any());
        doAnswer(call -> { staged.clear(); return null; }).when(transactions).rollback(any());
    }

    private OAuthMigrationServiceImpl migrationService(IdentityMigrationDAO dao, OAuthClientService clientService) {
        return new OAuthMigrationServiceImpl(
                dao,
                new IdentityDirectoryContext(77351065313480704L, "tenantId"),
                clientService,
                mock(ResourceServerService.class),
                mock(OAuthSecurityProjectionComponent.class),
                mock(OAuthClientRepository.class),
                mock(OAuthResourceRepository.class),
                mock(OAuthGrantRepository.class),
                mock(TransactionalOutboxResourceServerEventAdapter.class),
                mock(OAuthRealmRepository.class),
                mock(PlatformTransactionManager.class),
                new ObjectMapper(),
                mock(Environment.class)
        );
    }

    private static void writeOwnerOnly(Path path, String content) throws Exception {
        Files.writeString(path, content);
        Files.setPosixFilePermissions(path, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("Missing planned migration contract: " + name, exception);
        }
    }
}
