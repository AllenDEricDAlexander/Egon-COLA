package top.egon.cola.platform.tianquan.shoubing.admin.support.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
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
