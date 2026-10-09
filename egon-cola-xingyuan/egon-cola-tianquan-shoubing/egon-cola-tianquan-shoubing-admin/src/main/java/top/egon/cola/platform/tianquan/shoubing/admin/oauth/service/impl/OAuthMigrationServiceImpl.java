package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.po.OAuthAuditPO;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.po.IdentityCredentialPO;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.domain.po.IdentityUserPO;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityDirectoryContext;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.IdentityMigrationDAO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.command.OAuthMigrationCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.AuditRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.ClientOwnershipAssignmentDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.ClientOwnershipMapDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.ClientRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.CredentialRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.GrantRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.MembershipRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.OutboxEventMetadataRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.RedirectUriRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.ResourceRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.SourceType;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.TenantRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.IdentityMigrationRowDTO.UserRowDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.RotateClientSecretDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.UpdateOAuthClientDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthClientPO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.CreatedOAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.RotatedClientSecretVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.result.OAuthMigrationReportResult;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthMigrationService;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthSecurityProjectionComponent;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.CreateResourceServerDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.DeleteClientResourceGrantDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.ResourceVersionDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.UpsertClientResourceGrantDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ClientResourceGrantVO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ResourceServerVO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerService;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthResourceRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthGrantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox.TransactionalOutboxResourceServerEventAdapter;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.po.IdentityTenantMembershipPO;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.po.IdentityTenantPO;

/** Reads the reviewed legacy directory snapshot and explicitly imports historical columns. */
@Service("oauthMigrationService")
@RequiredArgsConstructor
@Slf4j
@Validated
public class OAuthMigrationServiceImpl implements OAuthMigrationService {

    private static final int BATCH_SIZE = 500;
    private static final String MIGRATION_ACTOR_PREFIX = "migration:";
    private static final DateTimeFormatter UTC_FORMAT = DateTimeFormatter.ISO_INSTANT;

    @Qualifier("identityMigrationDAO")
    private final IdentityMigrationDAO migrationDAO;
    @Qualifier("identityDirectoryContext")
    private final IdentityDirectoryContext directoryContext;
    @Qualifier("OAuthClientServiceImpl")
    private final OAuthClientService clientService;
    @Qualifier("resourceServerServiceImpl")
    private final ResourceServerService resourceServerService;
    @Qualifier("oauthSecurityProjectionComponent")
    private final OAuthSecurityProjectionComponent securityProjection;
    @Qualifier("oAuthClientRepository")
    private final OAuthClientRepository clientRepository;
    @Qualifier("oAuthResourceRepository")
    private final OAuthResourceRepository resourceRepository;
    @Qualifier("oAuthGrantRepository")
    private final OAuthGrantRepository grantRepository;
    @Qualifier("resourceServerEventAdapter")
    private final TransactionalOutboxResourceServerEventAdapter resourceEvents;
    @Qualifier("oAuthRealmRepository")
    private final OAuthRealmRepository realmRepository;
    @Qualifier("transactionManager")
    private final PlatformTransactionManager transactionManager;
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    @Qualifier("environment")
    private final Environment environment;

    private static final BCryptPasswordEncoder SECRET_MATCHER = new BCryptPasswordEncoder(12);

    @Override
    public OAuthMigrationReportResult importRealm(OAuthMigrationCommand command) {
        validateCommand(command);
        ClientOwnershipMapDTO ownershipMap = readOwnershipMap(command);
        MigrationPlan plan = analyze(command, ownershipMap);
        if (command.isDryRun()) {
            return report(command, plan, true, 0L, 0L, checkpointId(command));
        }
        if (plan.conflictCount() != 0L) {
            throw new IllegalStateException("OAUTH_MIGRATION_SOURCE_CONFLICTS");
        }
        if (!plan.reportSha256().equalsIgnoreCase(command.getApprovedReportSha256())) {
            throw new IllegalStateException("OAUTH_MIGRATION_APPROVAL_DIGEST_MISMATCH");
        }

        MigrationProgress progress = migrateDirectoryRows(command, plan);
        TrustedRealmBO targetRealm = ensureRealm(command);
        TrustedRealmBO platformRealm = ensureRealm(command, "platform");
        MigrationProgress oauthProgress = migrateOAuthRows(command, plan, targetRealm, platformRealm, progress);
        return report(command, plan, false, oauthProgress.importedCount(), oauthProgress.skippedCount(),
                oauthProgress.checkpointId());
    }

    private ClientOwnershipMapDTO readOwnershipMap(OAuthMigrationCommand command) {
        Path path = command.getOwnershipMapFile();
        if (path == null || !path.isAbsolute() || Files.isSymbolicLink(path)
                || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || !ownerOnly(path)) {
            throw new IllegalStateException("OAUTH_MIGRATION_OWNERSHIP_MAP_PERMISSIONS_INVALID");
        }
        try {
            if (Files.size(path) > 10_485_760L) {
                throw new IllegalStateException("OAUTH_MIGRATION_OWNERSHIP_MAP_TOO_LARGE");
            }
            ClientOwnershipMapDTO map = objectMapper.readerFor(ClientOwnershipMapDTO.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(path.toFile());
            if (map.getSchemaVersion() == null || map.getSchemaVersion() != 1
                    || !Objects.equals(map.getSourceSnapshotId(), command.getSourceSnapshotId())
                    || map.getAssignments() == null || map.getAssignments().size() > 10_000) {
                throw new IllegalStateException("OAUTH_MIGRATION_OWNERSHIP_MAP_INVALID");
            }
            Set<String> clientIds = new LinkedHashSet<>();
            for (ClientOwnershipAssignmentDTO assignment : map.getAssignments()) {
                if (assignment == null || blank(assignment.getClientId())
                        || !assignment.getClientId().matches("[A-Za-z0-9._-]{1,128}")
                        || !clientIds.add(assignment.getClientId()) || !validRealmKey(assignment.getRealmKey())
                        || !Set.of("USER", "SERVICE").contains(assignment.getPurpose())
                        || assignment.getApprovedGrantScopesByResource() == null) {
                    throw new IllegalStateException("OAUTH_MIGRATION_OWNERSHIP_MAP_INVALID");
                }
                for (Map.Entry<String, List<String>> entry : assignment.getApprovedGrantScopesByResource().entrySet()) {
                    List<String> scopes = entry.getValue();
                    if (blank(entry.getKey()) || !entry.getKey().matches("[A-Za-z0-9._-]{1,128}")
                            || scopes == null || scopes.size() > 64
                            || scopes.stream().anyMatch(scope -> blank(scope)
                            || !scope.matches("[A-Za-z0-9._:-]{1,128}"))
                            || scopes.stream().distinct().count() != scopes.size()) {
                        throw new IllegalStateException("OAUTH_MIGRATION_OWNERSHIP_MAP_SCOPE_INVALID");
                    }
                }
                if (assignment.getScopeApprovalId() != null
                        && !assignment.getScopeApprovalId().matches("[A-Za-z0-9._:-]{1,128}")) {
                    throw new IllegalStateException("OAUTH_MIGRATION_SCOPE_APPROVAL_ID_INVALID");
                }
            }
            return map;
        } catch (IOException exception) {
            throw new IllegalStateException("OAUTH_MIGRATION_OWNERSHIP_MAP_INVALID");
        }
    }

    private static boolean validRealmKey(String realmKey) {
        return "platform".equals(realmKey)
                || realmKey != null && realmKey.matches("tenant:[1-9][0-9]{0,18}");
    }

    private static String ownershipMapFingerprint(ClientOwnershipMapDTO map) {
        StringBuilder canonical = new StringBuilder(map.getSourceSnapshotId()).append('\n');
        map.getAssignments().stream().sorted(Comparator.comparing(ClientOwnershipAssignmentDTO::getClientId))
                .forEach(assignment -> {
                    canonical.append(assignment.getClientId()).append('|').append(assignment.getRealmKey())
                            .append('|').append(assignment.getPurpose()).append('|')
                            .append(assignment.getScopeApprovalId()).append('\n');
                    assignment.getApprovedGrantScopesByResource().entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .forEach(entry -> canonical.append(entry.getKey()).append('=')
                                    .append(String.join(",", entry.getValue().stream().sorted().toList()))
                                    .append('\n'));
                });
        return sha256(canonical.toString());
    }

    private static ClientOwnershipAssignmentDTO ownershipAssignment(ClientOwnershipMapDTO map, String clientId) {
        if (clientId == null) {
            return null;
        }
        return map.getAssignments().stream()
                .filter(assignment -> clientId.equals(assignment.getClientId()))
                .findFirst().orElse(null);
    }

    private MigrationPlan analyze(OAuthMigrationCommand command, ClientOwnershipMapDTO ownershipMap) {
        MessageDigest reportDigest = digest();
        List<String> conflictCodes = new ArrayList<>();
        long[] counts = {0L, 0L, 0L};
        String ownershipFingerprint = ownershipMapFingerprint(ownershipMap);
        updateDigest(reportDigest, "OWNERSHIP_MAP", ownershipFingerprint);
        scanUsers(command, reportDigest, counts, conflictCodes);
        scanCredentials(command, reportDigest, counts, conflictCodes);
        scanTenants(command, reportDigest, counts, conflictCodes);
        scanMemberships(command, reportDigest, counts, conflictCodes);
        scanAudit(command, reportDigest, counts, conflictCodes);
        scanLegacyResources(ownershipMap, reportDigest, counts, conflictCodes);
        List<String> secretFileReferences = new ArrayList<>();
        scanLegacyClients(command, ownershipMap, reportDigest, counts, conflictCodes, secretFileReferences);
        scanLegacyOutbox(reportDigest, counts, conflictCodes);
        long oldSecretCount = migrationDAO.countLegacyClientSecrets();
        long oldKeyCount = migrationDAO.countLegacySigningKeys();
        counts[0] += oldSecretCount + oldKeyCount;
        updateDigest(reportDigest, "LEGACY_SECRET_COUNT", Long.toString(oldSecretCount));
        updateDigest(reportDigest, "LEGACY_KEY_COUNT", Long.toString(oldKeyCount));
        String fingerprint = HexFormat.of().formatHex(reportDigest.digest());
        List<String> normalizedConflicts = conflictCodes.stream().distinct().sorted().toList();
        String reportSha = sha256(command.getRealmKey() + "\n" + command.getSourceSnapshotId() + "\n"
                + ownershipFingerprint + "\n" + counts[0] + "\n" + fingerprint + "\n"
                + String.join(",", normalizedConflicts));
        return new MigrationPlan(counts[0], counts[1], fingerprint, reportSha, normalizedConflicts,
                ownershipMap, ownershipFingerprint, counts[2], oldKeyCount,
                secretFileReferences.stream().sorted().toList());
    }

    private void scanLegacyResources(ClientOwnershipMapDTO ownershipMap, MessageDigest digest, long[] counts,
            List<String> conflictCodes) {
        String cursor = null;
        while (true) {
            List<ResourceRowDTO> page = migrationDAO.selectResourcesAfter(cursor, BATCH_SIZE);
            if (page.isEmpty()) {
                return;
            }
            for (ResourceRowDTO resource : page) {
                counts[0]++;
                ClientOwnershipAssignmentDTO manager = ownershipAssignment(ownershipMap,
                        resource.getManagementClientId());
                if (resourceConflict(resource, manager) != null) {
                    conflictCodes.add("RESOURCE:" + resource.getResourceServerId() + ":"
                            + resourceConflict(resource, manager));
                    counts[1]++;
                }
                updateDigest(digest, "RESOURCE", safeResourceFingerprint(resource));
            }
            cursor = page.get(page.size() - 1).getId();
        }
    }

    private void scanLegacyClients(OAuthMigrationCommand command, ClientOwnershipMapDTO ownershipMap,
            MessageDigest digest, long[] counts, List<String> conflictCodes, List<String> secretFileReferences) {
        Set<String> observedClients = new LinkedHashSet<>();
        String cursor = null;
        while (true) {
            List<ClientRowDTO> page = migrationDAO.selectClientsAfter(cursor, BATCH_SIZE);
            if (page.isEmpty()) {
                break;
            }
            for (ClientRowDTO client : page) {
                counts[0]++;
                observedClients.add(client.getClientId());
                ClientOwnershipAssignmentDTO assignment = ownershipAssignment(ownershipMap, client.getClientId());
                List<GrantRowDTO> grants = migrationDAO.selectGrantsForClient(client.getClientId(), 65);
                List<RedirectUriRowDTO> redirects = migrationDAO.selectRedirectUrisForClient(client.getClientId(), 17);
                String conflict = clientConflict(ownershipMap, client, assignment, grants, redirects);
                if (conflict != null) {
                    conflictCodes.add("CLIENT:" + safeClientId(client.getClientId()) + ":" + conflict);
                    counts[1]++;
                }
                updateDigest(digest, "CLIENT", safeClientFingerprint(client),
                        assignment == null ? "UNMAPPED" : safeAssignmentFingerprint(assignment));
                if (assignment != null && assignment.getRealmKey().equals(command.getRealmKey())
                        && "CONFIDENTIAL".equals(client.getClientType())) {
                    counts[2]++;
                    secretFileReferences.add(secretFileName(assignment.getRealmKey(), client.getClientId()));
                }
                counts[0] += grants.size() + redirects.size();
                for (GrantRowDTO grant : grants) {
                    updateDigest(digest, "GRANT", safeGrantFingerprint(grant));
                }
                for (RedirectUriRowDTO redirect : redirects) {
                    updateDigest(digest, "REDIRECT", join(redirect.getId(), redirect.getClientId(),
                            redirect.getRedirectUri(), redirect.getCreatedAt()));
                }
            }
            cursor = page.get(page.size() - 1).getClientId();
        }
        for (ClientOwnershipAssignmentDTO assignment : ownershipMap.getAssignments()) {
            if (!observedClients.contains(assignment.getClientId())) {
                conflictCodes.add("OWNER_MAP:UNKNOWN_CLIENT:" + safeClientId(assignment.getClientId()));
                counts[1]++;
            }
        }
    }

    private void scanLegacyOutbox(MessageDigest digest, long[] counts, List<String> conflictCodes) {
        String cursor = null;
        Set<String> acceptedEventTypes = Set.of("IDENTITY_USER_CREATED", "IDENTITY_USER_DISABLED",
                "IDENTITY_USER_ENABLED", "IDENTITY_PASSWORD_RESET", "IDENTITY_TOKEN_REVOKED");
        while (true) {
            List<OutboxEventMetadataRowDTO> page = migrationDAO.selectOutboxEventsAfter(cursor, BATCH_SIZE);
            if (page.isEmpty()) {
                return;
            }
            for (OutboxEventMetadataRowDTO event : page) {
                counts[0]++;
                if (!acceptedEventTypes.contains(event.getEventType())) {
                    conflictCodes.add("OUTBOX:UNRECOGNIZED_EVENT:" + safeClientId(event.getId()));
                    counts[1]++;
                }
                updateDigest(digest, "IDENTITY_OUTBOX_METADATA", join(event.getId(), event.getAggregateType(),
                        event.getAggregateId(), event.getEventType(), event.getStatus(), event.getAttemptCount(),
                        event.getNextAttemptAt(), event.getCreatedAt(), event.getPublishedAt()));
            }
            cursor = page.get(page.size() - 1).getId();
        }
    }

    private String resourceConflict(ResourceRowDTO resource, ClientOwnershipAssignmentDTO manager) {
        if (resource == null || blank(resource.getResourceServerId()) || blank(resource.getResourceUri())
                || blank(resource.getBizCode()) || blank(resource.getAppCode()) || blank(resource.getEnvironment())
                || blank(resource.getDisplayName()) || blank(resource.getManagementClientId())
                || blank(resource.getRbacApplicationCode()) || blank(resource.getEntryPermissionCode())
                || resource.getVersion() < 0L || resource.getCreatedAt() == null || resource.getUpdatedAt() == null) {
            return "RESOURCE_REQUIRED_FIELD";
        }
        if (!Set.of("ACTIVE", "DISABLED").contains(resource.getStatus())) {
            return "RESOURCE_STATUS";
        }
        if (!isExactHttpsUri(resource.getResourceUri())) {
            return "RESOURCE_URI";
        }
        if (manager == null || !"platform".equals(manager.getRealmKey())
                || !"SERVICE".equals(manager.getPurpose())) {
            return "RESOURCE_MANAGER_NOT_PLATFORM_SERVICE";
        }
        ClientRowDTO managerClient = migrationDAO.selectClientById(resource.getManagementClientId());
        if (managerClient == null || !"CONFIDENTIAL".equals(managerClient.getClientType())
                || !"ACTIVE".equals(managerClient.getStatus()) || blank(managerClient.getAppId())) {
            return "RESOURCE_MANAGER_CLIENT_INVALID";
        }
        return null;
    }

    private String clientConflict(ClientOwnershipMapDTO ownershipMap, ClientRowDTO client,
            ClientOwnershipAssignmentDTO assignment,
            List<GrantRowDTO> grants, List<RedirectUriRowDTO> redirects) {
        if (client == null || blank(client.getClientId()) || blank(client.getClientName())
                || client.getVersion() < 0L || client.getCreatedAt() == null || client.getUpdatedAt() == null
                || client.getAccessTokenTtlSeconds() == null || client.getAccessTokenTtlSeconds() < 300
                || client.getAccessTokenTtlSeconds() > 600 || client.getRefreshTokenTtlSeconds() == null
                || client.getRefreshTokenTtlSeconds() < 0 || client.getRefreshTokenTtlSeconds() > 604_800) {
            return "CLIENT_REQUIRED_OR_UNSUPPORTED_TTL";
        }
        if (!Set.of("PUBLIC", "CONFIDENTIAL").contains(client.getClientType())
                || !Set.of("ACTIVE", "DISABLED").contains(client.getStatus())) {
            return "CLIENT_TYPE_OR_STATUS";
        }
        if (assignment == null) {
            return "CLIENT_OWNER_UNMAPPED";
        }
        if ("USER".equals(assignment.getPurpose()) && !Boolean.TRUE.equals(client.getPkceRequired())) {
            return "USER_CLIENT_PKCE_REQUIRED";
        }
        if ("SERVICE".equals(assignment.getPurpose()) && client.getRefreshTokenTtlSeconds() != 0) {
            return "SERVICE_CLIENT_REFRESH_TTL";
        }
        if ("PUBLIC".equals(client.getClientType())) {
            if (!"USER".equals(assignment.getPurpose()) || client.getAppId() != null
                    || client.getRefreshTokenTtlSeconds() != 0) {
                return "PUBLIC_CLIENT_FIELDS";
            }
        } else if (blank(client.getAppId()) || !client.getAppId().matches("[a-z][a-z0-9-]{2,127}")) {
            return "CONFIDENTIAL_CLIENT_APP_ID";
        }
        if ("platform".equals(assignment.getRealmKey())
                && (!"SERVICE".equals(assignment.getPurpose()) || !"CONFIDENTIAL".equals(client.getClientType()))) {
            return "PLATFORM_CLIENT_MUST_BE_CONFIDENTIAL_SERVICE";
        }
        if (assignment.getRealmKey().startsWith("tenant:")) {
            String tenantId = assignment.getRealmKey().substring("tenant:".length());
            if (migrationDAO.sourceTenantExists(tenantId) != 1) {
                return "CLIENT_OWNER_TENANT_NOT_FOUND";
            }
            if (!"ACTIVE".equals(migrationDAO.selectSourceTenantStatus(tenantId))) {
                return "CLIENT_OWNER_TENANT_NOT_ACTIVE";
            }
        }
        if (grants.isEmpty() || grants.size() >= 65 || redirects.size() >= 17) {
            return grants.isEmpty() ? "CLIENT_GRANT_OWNER_MISSING"
                    : grants.size() >= 65 ? "CLIENT_GRANT_LIMIT" : "CLIENT_REDIRECT_LIMIT";
        }
        Set<String> grantResources = new LinkedHashSet<>();
        boolean hasUserGrant = false;
        boolean hasServiceGrant = false;
        for (GrantRowDTO grant : grants) {
            if (grant == null || blank(grant.getResourceServerId()) || !grantResources.add(grant.getResourceServerId())
                    || !Set.of("ACTIVE", "DISABLED").contains(grant.getStatus())
                    || grant.getVersion() < 0L || grant.getCreatedAt() == null || grant.getUpdatedAt() == null) {
                return "CLIENT_GRANT_REQUIRED_OR_DUPLICATE";
            }
            ResourceRowDTO resource = migrationDAO.selectResourceById(grant.getResourceServerId());
            String resourceError = resourceConflict(resource,
                    ownershipAssignment(ownershipMap, resource == null ? null : resource.getManagementClientId()));
            if (resourceError != null) {
                return "CLIENT_RESOURCE_" + resourceError;
            }
            if (!"ACTIVE".equals(resource.getStatus())) {
                return "CLIENT_RESOURCE_DISABLED";
            }
            List<String> sourceScopes = parseLegacyScopes(grant.getAllowedScopes());
            if (sourceScopes == null) {
                return "CLIENT_GRANT_SCOPE_JSON";
            }
            List<String> mappedScopes = assignment.getApprovedGrantScopesByResource()
                    .get(grant.getResourceServerId());
            if (mappedScopes == null) {
                return "CLIENT_GRANT_SCOPE_MAP_MISSING";
            }
            if ("USER_DELEGATION".equals(grant.getGrantType())) {
                hasUserGrant = true;
                if (!"USER".equals(assignment.getPurpose()) || grant.getTenantId() != null
                        || grant.getGrantContext() != null || mappedScopes.isEmpty()) {
                    return "USER_GRANT_CONTEXT_OR_SCOPE";
                }
                try {
                    new ClientResourceGrant(client.getClientId(), grant.getResourceServerId(),
                            ResourceGrantType.USER_DELEGATION, null, Set.copyOf(mappedScopes),
                            ClientResourceGrant.Status.ACTIVE, 0L);
                } catch (IllegalArgumentException exception) {
                    return "USER_GRANT_CONTEXT_OR_SCOPE";
                }
                if (!sourceScopes.isEmpty() && !new LinkedHashSet<>(sourceScopes).equals(new LinkedHashSet<>(mappedScopes))) {
                    return "USER_GRANT_SCOPE_EXPANSION";
                }
                if (sourceScopes.isEmpty() && blank(assignment.getScopeApprovalId())) {
                    return "USER_GRANT_SCOPE_APPROVAL_REQUIRED";
                }
            } else if ("CLIENT_CREDENTIALS".equals(grant.getGrantType())) {
                hasServiceGrant = true;
                if (!"SERVICE".equals(assignment.getPurpose()) || sourceScopes.isEmpty()
                        || !new LinkedHashSet<>(sourceScopes).equals(new LinkedHashSet<>(mappedScopes))) {
                    return "SERVICE_GRANT_PURPOSE_OR_SCOPE";
                }
                if ("platform".equals(assignment.getRealmKey())) {
                    if (!"PLATFORM".equals(grant.getGrantContext()) || grant.getTenantId() != null) {
                        return "PLATFORM_SERVICE_GRANT_CONTEXT";
                    }
                } else if (!"TENANT".equals(grant.getGrantContext())
                        || !assignment.getRealmKey().substring("tenant:".length()).equals(grant.getTenantId())) {
                    return "TENANT_SERVICE_GRANT_CONTEXT";
                }
            } else {
                return "CLIENT_GRANT_TYPE";
            }
        }
        if (hasUserGrant && hasServiceGrant) {
            return "CLIENT_MIXED_PURPOSE_GRANTS";
        }
        if ("USER".equals(assignment.getPurpose()) != hasUserGrant
                || "SERVICE".equals(assignment.getPurpose()) != hasServiceGrant) {
            return "CLIENT_PURPOSE_GRANT_MISMATCH";
        }
        if (!assignment.getApprovedGrantScopesByResource().keySet().equals(grantResources)) {
            return "CLIENT_SCOPE_MAP_RESOURCE_MISMATCH";
        }
        if ("USER".equals(assignment.getPurpose())) {
            if (redirects.isEmpty()) {
                return "USER_CLIENT_REDIRECT_REQUIRED";
            }
            if ("CONFIDENTIAL".equals(client.getClientType())
                    && client.getRefreshTokenTtlSeconds() != 0
                    && client.getRefreshTokenTtlSeconds() < 3_600) {
                return "USER_CLIENT_REFRESH_TTL_UNSUPPORTED";
            }
            try {
                clientAllowedScopes(assignment);
            } catch (IllegalStateException exception) {
                return "USER_CLIENT_SCOPE_INTERSECTION_EMPTY";
            }
            Set<String> distinctRedirects = new LinkedHashSet<>();
            for (RedirectUriRowDTO redirect : redirects) {
                if (redirect == null || !client.getClientId().equals(redirect.getClientId())
                        || !isExactHttpsUri(redirect.getRedirectUri())
                        || !distinctRedirects.add(redirect.getRedirectUri())) {
                    return "CLIENT_REDIRECT_INVALID_OR_DUPLICATE";
                }
            }
        } else if (!redirects.isEmpty()) {
            return "SERVICE_CLIENT_REDIRECT_UNEXPECTED";
        }
        return null;
    }

    private String safeClientFingerprint(ClientRowDTO client) {
        return join(client.getClientId(), client.getAppId(), client.getClientName(), client.getClientType(),
                client.getStatus(), client.getPkceRequired(), client.getAccessTokenTtlSeconds(),
                client.getRefreshTokenTtlSeconds(), client.getVersion(), client.getCreatedAt(), client.getUpdatedAt());
    }

    private String safeResourceFingerprint(ResourceRowDTO resource) {
        return join(resource.getId(), resource.getResourceServerId(), resource.getResourceUri(), resource.getBizCode(),
                resource.getAppCode(), resource.getEnvironment(), resource.getDisplayName(),
                resource.getManagementClientId(), resource.getRbacApplicationCode(),
                resource.getEntryPermissionCode(), resource.getStatus(), resource.getVersion(),
                resource.getCreatedAt(), resource.getUpdatedAt());
    }

    private String safeGrantFingerprint(GrantRowDTO grant) {
        return join(grant.getId(), grant.getClientId(), grant.getResourceServerId(), grant.getGrantType(),
                grant.getGrantContext(), grant.getTenantId(), grant.getAllowedScopes(), grant.getStatus(),
                grant.getVersion(), grant.getCreatedAt(), grant.getUpdatedAt());
    }

    private String safeAssignmentFingerprint(ClientOwnershipAssignmentDTO assignment) {
        StringBuilder value = new StringBuilder(assignment.getRealmKey()).append('|').append(assignment.getPurpose());
        assignment.getApprovedGrantScopesByResource().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> value.append('|').append(entry.getKey()).append('=')
                        .append(String.join(",", entry.getValue().stream().sorted().toList())));
        return value.toString();
    }

    private boolean isExactHttpsUri(String value) {
        if (blank(value)) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null
                    && !value.contains("*") && uri.normalize().toASCIIString().equals(uri.toASCIIString());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private List<String> parseLegacyScopes(String value) {
        if (blank(value)) {
            return null;
        }
        try {
            JsonNode array = objectMapper.readTree(value);
            if (array == null || !array.isArray() || array.size() > 64) {
                return null;
            }
            List<String> scopes = new ArrayList<>();
            for (JsonNode scope : array) {
                if (!scope.isTextual() || blank(scope.textValue())
                        || !scope.textValue().matches("[A-Za-z0-9._:-]{1,128}")) {
                    return null;
                }
                scopes.add(scope.textValue());
            }
            if (scopes.stream().distinct().count() != scopes.size()) {
                return null;
            }
            return scopes.stream().sorted().toList();
        } catch (IOException exception) {
            return null;
        }
    }

    private static String safeClientId(String value) {
        return value != null && value.matches("[A-Za-z0-9._-]{1,128}")
                ? value : sha256(String.valueOf(value)).substring(0, 16);
    }

    private static String secretFileName(String realmKey, String clientId) {
        return "client-" + sha256(String.valueOf(realmKey) + "\n" + clientId).substring(0, 24) + ".secret.json";
    }

    private void scanUsers(OAuthMigrationCommand command, MessageDigest digest, long[] counts,
            List<String> conflictCodes) {
        scanPages(SourceType.USER, null, migrationDAO::selectUsersAfter, UserRowDTO::getId,
                row -> userConflict(row), row -> safeUserFingerprint(row), digest, counts, conflictCodes);
    }

    private void scanCredentials(OAuthMigrationCommand command, MessageDigest digest, long[] counts,
            List<String> conflictCodes) {
        scanPages(SourceType.CREDENTIAL, null, migrationDAO::selectCredentialsAfter, CredentialRowDTO::getId,
                row -> credentialConflict(row), row -> safeCredentialFingerprint(row), digest, counts, conflictCodes);
    }

    private void scanTenants(OAuthMigrationCommand command, MessageDigest digest, long[] counts,
            List<String> conflictCodes) {
        scanPages(SourceType.TENANT, null, migrationDAO::selectTenantsAfter, TenantRowDTO::getId,
                row -> tenantConflict(row), row -> safeTenantFingerprint(row), digest, counts, conflictCodes);
    }

    private void scanMemberships(OAuthMigrationCommand command, MessageDigest digest, long[] counts,
            List<String> conflictCodes) {
        scanPages(SourceType.MEMBERSHIP, null, migrationDAO::selectMembershipsAfter, MembershipRowDTO::getId,
                row -> membershipConflict(row), row -> safeMembershipFingerprint(row), digest, counts, conflictCodes);
    }

    private void scanAudit(OAuthMigrationCommand command, MessageDigest digest, long[] counts,
            List<String> conflictCodes) {
        scanPages(SourceType.AUDIT, null, migrationDAO::selectAuditAfter, AuditRowDTO::getId,
                row -> auditConflict(row), row -> safeAuditFingerprint(row), digest, counts, conflictCodes);
    }

    private <T> void scanPages(SourceType type, String initialCursor, PageReader<T> reader,
            Function<T, String> sourceKey, Function<T, String> conflict, Function<T, String> safeFingerprint,
            MessageDigest reportDigest, long[] counts, List<String> conflictCodes) {
        String cursor = initialCursor;
        while (true) {
            List<T> page = reader.read(cursor, BATCH_SIZE);
            if (page.isEmpty()) {
                return;
            }
            for (T row : page) {
                counts[0]++;
                String key = sourceKey.apply(row);
                String rowConflict = conflict.apply(row);
                if (rowConflict != null) {
                    counts[1]++;
                    conflictCodes.add(rowConflict);
                }
                updateDigest(reportDigest, type.name(), key, safeFingerprint.apply(row), secretFingerprint(type, row));
            }
            cursor = sourceKey.apply(page.get(page.size() - 1));
        }
    }

    private String userConflict(UserRowDTO row) {
        if (row == null || blank(row.getId()) || blank(row.getUsername()) || blank(row.getNormalizedUsername())
                || blank(row.getDisplayName()) || row.getVersion() < 0L || row.getCreatedAt() == null
                || row.getUpdatedAt() == null || row.getFailedLoginCount() == null || row.getFailedLoginCount() < 0) {
            return "IDENTITY_USER_REQUIRED_FIELD";
        }
        if (!Set.of("ACTIVE", "DISABLED", "LOCKED").contains(row.getStatus())) {
            return "IDENTITY_USER_STATUS";
        }
        return null;
    }

    private String credentialConflict(CredentialRowDTO row) {
        if (row == null || blank(row.getId()) || blank(row.getIdentitySub()) || blank(row.getPasswordHash())
                || row.getPasswordChangedAt() == null || row.getVersion() < 0L
                || row.getCreatedAt() == null || row.getUpdatedAt() == null) {
            return "IDENTITY_CREDENTIAL_REQUIRED_FIELD";
        }
        if (!"PASSWORD".equals(row.getCredentialType()) || !Set.of("ACTIVE", "REVOKED").contains(row.getStatus())) {
            return "IDENTITY_CREDENTIAL_TYPE_OR_STATUS";
        }
        if (migrationDAO.sourceUserExists(row.getIdentitySub()) != 1) {
            return "IDENTITY_CREDENTIAL_ORPHAN_SUBJECT";
        }
        return null;
    }

    private String tenantConflict(TenantRowDTO row) {
        if (row == null || canonicalPositiveLong(row.getId()) == null || blank(row.getTenantCode())
                || !row.getTenantCode().matches("[a-z][a-z0-9-]{2,63}") || blank(row.getTenantName())
                || row.getVersion() < 0L || row.getCreatedAt() == null || row.getUpdatedAt() == null
                || blank(row.getSettings())) {
            return "IDENTITY_TENANT_REQUIRED_OR_NONCANONICAL_FIELD";
        }
        if (!Set.of("INITIALIZING", "ACTIVE", "SUSPENDED", "CLOSED").contains(row.getStatus())) {
            return "IDENTITY_TENANT_STATUS";
        }
        try {
            JsonNode settings = objectMapper.readTree(row.getSettings());
            if (!settings.isObject() || row.getSettings().getBytes(StandardCharsets.UTF_8).length > 65_536) {
                return "IDENTITY_TENANT_SETTINGS_SHAPE_OR_SIZE";
            }
        } catch (IOException exception) {
            return "IDENTITY_TENANT_SETTINGS_JSON";
        }
        return null;
    }

    private String membershipConflict(MembershipRowDTO row) {
        if (row == null || blank(row.getId()) || canonicalPositiveLong(row.getTenantId()) == null
                || blank(row.getIdentitySub()) || row.getVersion() < 0L
                || row.getCreatedAt() == null || row.getUpdatedAt() == null) {
            return "IDENTITY_MEMBERSHIP_REQUIRED_OR_NONCANONICAL_FIELD";
        }
        if (!Set.of("ACTIVE", "DISABLED").contains(row.getStatus())) {
            return "IDENTITY_MEMBERSHIP_STATUS";
        }
        if (migrationDAO.sourceTenantExists(row.getTenantId()) != 1
                || migrationDAO.sourceUserExists(row.getIdentitySub()) != 1) {
            return "IDENTITY_MEMBERSHIP_ORPHAN";
        }
        return null;
    }

    private String auditConflict(AuditRowDTO row) {
        if (row == null || blank(row.getId()) || row.getId().length() > 64 || blank(row.getEventType())
                || row.getEventType().length() > 128 || blank(row.getResult()) || blank(row.getReason())
                || row.getReason().length() > 128 || row.getOccurredAt() == null) {
            return "IDENTITY_AUDIT_REQUIRED_OR_OVERSIZE_FIELD";
        }
        if (!Set.of("SUCCESS", "DENIED", "FAILED", "FAILURE").contains(row.getResult())) {
            return "IDENTITY_AUDIT_RESULT";
        }
        if (row.getTenantId() != null && canonicalPositiveLong(row.getTenantId()) == null) {
            return "IDENTITY_AUDIT_TENANT_ID";
        }
        if (blank(row.getPayload())) {
            return "IDENTITY_AUDIT_PAYLOAD_REQUIRED";
        }
        try {
            JsonNode payload = objectMapper.readTree(row.getPayload());
            if (payload == null || !payload.isObject()
                    || row.getPayload().getBytes(StandardCharsets.UTF_8).length > 16_384) {
                return "IDENTITY_AUDIT_PAYLOAD_SHAPE_OR_SIZE";
            }
        } catch (IOException exception) {
            return "IDENTITY_AUDIT_PAYLOAD_JSON";
        }
        return null;
    }

    private String safeUserFingerprint(UserRowDTO row) {
        return join(row.getId(), row.getUsername(), row.getNormalizedUsername(), row.getDisplayName(), row.getStatus(),
                row.getFailedLoginCount(), row.getLockedUntil(), row.getLastLoginAt(), row.getVersion(),
                row.getCreatedAt(), row.getUpdatedAt());
    }

    private String safeCredentialFingerprint(CredentialRowDTO row) {
        return join(row.getId(), row.getIdentitySub(), row.getCredentialType(), row.getPasswordChangedAt(),
                row.getMustChangePassword(), row.getStatus(), row.getVersion(), row.getCreatedAt(), row.getUpdatedAt());
    }

    private String safeTenantFingerprint(TenantRowDTO row) {
        return join(row.getId(), row.getTenantCode(), row.getTenantName(), row.getStatus(), row.getVersion(),
                row.getCreatedAt(), row.getUpdatedAt(), row.getCreatedBy(), row.getUpdatedBy());
    }

    private String safeMembershipFingerprint(MembershipRowDTO row) {
        return join(row.getId(), row.getTenantId(), row.getIdentitySub(), row.getStatus(), row.getVersion(),
                row.getCreatedAt(), row.getUpdatedAt(), row.getCreatedBy(), row.getUpdatedBy());
    }

    private String safeAuditFingerprint(AuditRowDTO row) {
        return join(row.getId(), row.getEventType(), row.getActorSub(), row.getTargetSub(), row.getTenantId(),
                sha256(row.getSourceIp()), sha256(row.getUserAgent()), row.getResult(), row.getReason(),
                row.getTraceId(), row.getOccurredAt(), sha256(row.getPayload()));
    }

    private MigrationProgress migrateDirectoryRows(OAuthMigrationCommand command, MigrationPlan plan) {
        MigrationCheckpoint checkpoint = readCheckpoint(command, plan);
        if (checkpoint != null && checkpoint.complete()) {
            return new MigrationProgress(checkpoint.importedCount(), checkpoint.skippedCount(),
                    checkpointId(checkpoint));
        }
        if (checkpoint != null && !"DIRECTORY".equals(checkpoint.phase())
                && !"OAUTH_PENDING".equals(checkpoint.phase())) {
            return new MigrationProgress(checkpoint.importedCount(), checkpoint.skippedCount(),
                    checkpointId(checkpoint));
        }
        long imported = checkpoint == null ? 0L : checkpoint.importedCount();
        long skipped = checkpoint == null ? 0L : checkpoint.skippedCount();
        int firstType = checkpoint == null ? 0 : checkpoint.typeIndex();
        String cursor = checkpoint == null ? null : checkpoint.lastSourceId();
        for (int typeIndex = firstType; typeIndex < SourceType.values().length; typeIndex++) {
            SourceType type = SourceType.values()[typeIndex];
            String lastId = typeIndex == firstType ? cursor : null;
            while (true) {
                List<?> page = readPage(type, lastId);
                if (page.isEmpty()) {
                    checkpoint = new MigrationCheckpoint(command.getRealmKey(), command.getSourceSnapshotId(),
                            plan.reportSha256(), typeIndex + 1, null, imported, skipped,
                            "DIRECTORY", null, false);
                    writeCheckpoint(command.getCheckpointFile(), checkpoint);
                    break;
                }
                String sourceCursor = lastId;
                String pageHash = pageContentFingerprint(type, page);
                long[] batchCounts = directoryContext.withPlatformDirectoryPartition(migrationActor(command),
                        () -> new TransactionTemplate(transactionManager).execute(status -> {
                            List<?> currentPage = readPage(type, sourceCursor);
                            if (!pageHash.equals(pageContentFingerprint(type, currentPage))) {
                                throw new IllegalStateException("OAUTH_MIGRATION_SOURCE_PAGE_CHANGED");
                            }
                            long[] counts = {0L, 0L};
                            for (Object sourceRow : currentPage) {
                                if (writeIfAbsentOrVerify(type, sourceRow, command)) {
                                    counts[0]++;
                                } else {
                                    counts[1]++;
                                }
                            }
                            return counts;
                        }));
                imported += batchCounts[0];
                skipped += batchCounts[1];
                lastId = sourceKey(type, page.get(page.size() - 1));
                checkpoint = new MigrationCheckpoint(command.getRealmKey(), command.getSourceSnapshotId(),
                        plan.reportSha256(), typeIndex, lastId, imported, skipped,
                        "DIRECTORY", null, false);
                writeCheckpoint(command.getCheckpointFile(), checkpoint);
            }
        }
        checkpoint = new MigrationCheckpoint(command.getRealmKey(), command.getSourceSnapshotId(),
                plan.reportSha256(), SourceType.values().length, null, imported, skipped,
                "OAUTH_PENDING", null, false);
        writeCheckpoint(command.getCheckpointFile(), checkpoint);
        return new MigrationProgress(imported, skipped, checkpointId(checkpoint));
    }

    private MigrationProgress migrateOAuthRows(OAuthMigrationCommand command, MigrationPlan plan,
            TrustedRealmBO targetRealm, TrustedRealmBO platformRealm, MigrationProgress directoryProgress) {
        MigrationCheckpoint checkpoint = readCheckpoint(command, plan);
        long imported = directoryProgress.importedCount();
        long skipped = directoryProgress.skippedCount();
        securityProjection.recoverRegisteredRealms();

        if (checkpoint != null && checkpoint.complete()) {
            verifyCompletedSecretArtifacts(command, plan, targetRealm);
            return new MigrationProgress(checkpoint.importedCount(), checkpoint.skippedCount(),
                    checkpointId(checkpoint));
        } else if (checkpoint == null || "OAUTH_PENDING".equals(checkpoint.phase())) {
            checkpoint = oauthCheckpoint(command, plan, imported, skipped,
                    targetRealm.getRealmType() == RealmTypeEnum.PLATFORM ? "PLATFORM_CLIENTS" : "TENANT_CLIENTS",
                    null, false);
        } else {
            imported = checkpoint.importedCount();
            skipped = checkpoint.skippedCount();
        }
        String phase = checkpoint.phase();
        String cursor = checkpoint.oauthCursor();

        if (targetRealm.getRealmType() == RealmTypeEnum.PLATFORM) {
            if ("PLATFORM_CLIENTS".equals(phase)) {
                StageProgress stage = importClients(command, plan, platformRealm, platformRealm, cursor, false,
                        imported, skipped);
                imported += stage.imported();
                skipped += stage.skipped();
                checkpoint = oauthCheckpoint(command, plan, imported, skipped, "RESOURCES", null, false);
                writeCheckpoint(command.getCheckpointFile(), checkpoint);
                phase = checkpoint.phase();
                cursor = null;
            }
            if ("RESOURCES".equals(phase)) {
                StageProgress stage = importResources(command, plan, platformRealm, cursor, imported, skipped);
                imported += stage.imported();
                skipped += stage.skipped();
                checkpoint = oauthCheckpoint(command, plan, imported, skipped, "PLATFORM_GRANTS", null, false);
                writeCheckpoint(command.getCheckpointFile(), checkpoint);
                phase = checkpoint.phase();
                cursor = null;
            }
            if ("PLATFORM_GRANTS".equals(phase)) {
                StageProgress stage = importGrantsForRealm(command, plan, platformRealm, platformRealm, cursor,
                        "platform", imported, skipped);
                imported += stage.imported();
                skipped += stage.skipped();
            }
        } else {
            if ("TENANT_CLIENTS".equals(phase)) {
                StageProgress stage = importClients(command, plan, targetRealm, platformRealm, cursor, true,
                        imported, skipped);
                imported += stage.imported();
                skipped += stage.skipped();
            }
        }

        checkpoint = oauthCheckpoint(command, plan, imported, skipped, "COMPLETE", null, true);
        writeCheckpoint(command.getCheckpointFile(), checkpoint);
        return new MigrationProgress(imported, skipped, checkpointId(checkpoint));
    }

    private void verifyCompletedSecretArtifacts(OAuthMigrationCommand command, MigrationPlan plan,
            TrustedRealmBO targetRealm) {
        String cursor = null;
        while (true) {
            List<ClientRowDTO> page = migrationDAO.selectClientsAfter(cursor, BATCH_SIZE);
            if (page.isEmpty()) {
                return;
            }
            for (ClientRowDTO source : page) {
                ClientOwnershipAssignmentDTO assignment = ownershipAssignment(plan.ownershipMap(), source.getClientId());
                if (assignment != null && targetRealm.getRealmKey().equals(assignment.getRealmKey())
                        && "CONFIDENTIAL".equals(source.getClientType())) {
                    OAuthClientBO client = clientRepository.findByClientId(targetRealm, source.getClientId())
                            .orElseThrow(() -> new IllegalStateException("OAUTH_MIGRATION_COMPLETED_CLIENT_MISSING"));
                    verifySecretArtifact(command, targetRealm, client);
                }
                cursor = source.getClientId();
            }
        }
    }

    private StageProgress importClients(OAuthMigrationCommand command, MigrationPlan plan,
            TrustedRealmBO realm, TrustedRealmBO platformRealm, String initialCursor, boolean importGrants,
            long priorImported, long priorSkipped) {
        long imported = 0L;
        long skipped = 0L;
        String cursor = initialCursor;
        while (true) {
            List<ClientRowDTO> page = migrationDAO.selectClientsAfter(cursor, BATCH_SIZE);
            if (page.isEmpty()) {
                return new StageProgress(imported, skipped);
            }
            for (ClientRowDTO source : page) {
                ClientOwnershipAssignmentDTO assignment = ownershipAssignment(plan.ownershipMap(), source.getClientId());
                boolean targetOwner = assignment != null && realm.getRealmKey().equals(assignment.getRealmKey());
                if (targetOwner) {
                    Boolean created = new TransactionTemplate(transactionManager).execute(status -> {
                        boolean inserted = importClient(command, plan, realm, platformRealm, source, assignment);
                        if (importGrants) {
                            importGrantsForClient(command, plan, realm, platformRealm, source, assignment, inserted);
                        }
                        return inserted;
                    });
                    if (Boolean.TRUE.equals(created)) {
                        imported++;
                    } else {
                        skipped++;
                    }
                }
                cursor = source.getClientId();
                MigrationCheckpoint progress = oauthCheckpoint(command, plan,
                        priorImported + imported, priorSkipped + skipped,
                        importGrants ? "TENANT_CLIENTS" : "PLATFORM_CLIENTS", cursor, false);
                writeCheckpoint(command.getCheckpointFile(), progress);
            }
        }
    }

    private StageProgress importResources(OAuthMigrationCommand command, MigrationPlan plan,
            TrustedRealmBO platformRealm, String initialCursor, long priorImported, long priorSkipped) {
        long imported = 0L;
        long skipped = 0L;
        String cursor = initialCursor;
        while (true) {
            List<ResourceRowDTO> page = migrationDAO.selectResourcesAfter(cursor, BATCH_SIZE);
            if (page.isEmpty()) {
                return new StageProgress(imported, skipped);
            }
            for (ResourceRowDTO source : page) {
                Boolean created = new TransactionTemplate(transactionManager).execute(status ->
                        importResource(command, plan, platformRealm, source));
                if (Boolean.TRUE.equals(created)) {
                    imported++;
                } else {
                    skipped++;
                }
                cursor = source.getId();
                MigrationCheckpoint progress = oauthCheckpoint(command, plan,
                        priorImported + imported, priorSkipped + skipped,
                        "RESOURCES", cursor, false);
                writeCheckpoint(command.getCheckpointFile(), progress);
            }
        }
    }

    private StageProgress importGrantsForRealm(OAuthMigrationCommand command, MigrationPlan plan,
            TrustedRealmBO realm, TrustedRealmBO platformRealm, String initialCursor, String targetRealmKey,
            long priorImported, long priorSkipped) {
        long imported = 0L;
        long skipped = 0L;
        String cursor = initialCursor;
        while (true) {
            List<ClientRowDTO> page = migrationDAO.selectClientsAfter(cursor, BATCH_SIZE);
            if (page.isEmpty()) {
                return new StageProgress(imported, skipped);
            }
            for (ClientRowDTO source : page) {
                ClientOwnershipAssignmentDTO assignment = ownershipAssignment(plan.ownershipMap(), source.getClientId());
                if (assignment != null && targetRealmKey.equals(assignment.getRealmKey())) {
                    Boolean changed = new TransactionTemplate(transactionManager).execute(status ->
                            importGrantsForClient(command, plan, realm, platformRealm, source, assignment, false));
                    if (Boolean.TRUE.equals(changed)) {
                        imported++;
                    } else {
                        skipped++;
                    }
                }
                cursor = source.getClientId();
                MigrationCheckpoint progress = oauthCheckpoint(command, plan,
                        priorImported + imported, priorSkipped + skipped,
                        "PLATFORM_GRANTS", cursor, false);
                writeCheckpoint(command.getCheckpointFile(), progress);
            }
        }
    }

    private boolean importClient(OAuthMigrationCommand command, MigrationPlan plan, TrustedRealmBO realm,
            TrustedRealmBO platformRealm, ClientRowDTO source, ClientOwnershipAssignmentDTO assignment) {
        if (assignment == null || !realm.getRealmKey().equals(assignment.getRealmKey())) {
            throw new IllegalStateException("OAUTH_MIGRATION_CLIENT_OWNER_MISMATCH");
        }
        List<GrantRowDTO> sourceGrants = migrationDAO.selectGrantsForClient(source.getClientId(), 65);
        List<RedirectUriRowDTO> sourceRedirects = migrationDAO.selectRedirectUrisForClient(source.getClientId(), 17);
        String conflict = clientConflict(plan.ownershipMap(), source, assignment, sourceGrants, sourceRedirects);
        if (conflict != null) {
            throw new IllegalStateException("OAUTH_MIGRATION_CLIENT_CONFLICT:" + conflict);
        }
        List<String> redirectUris = sourceRedirects.stream().map(RedirectUriRowDTO::getRedirectUri).sorted().toList();
        List<String> resourceUris = "USER".equals(assignment.getPurpose())
                ? sourceGrants.stream()
                .map(grant -> migrationDAO.selectResourceById(grant.getResourceServerId()))
                .filter(Objects::nonNull)
                .map(ResourceRowDTO::getResourceUri)
                .distinct().sorted().toList()
                : List.of();
        List<String> allowedScopes = clientAllowedScopes(assignment);

        if ("USER".equals(assignment.getPurpose())) {
            for (GrantRowDTO grant : sourceGrants) {
                OAuthResourceBO targetResource = resourceRepository.findByResourceServerId(
                                platformRealm, grant.getResourceServerId())
                        .orElseThrow(() -> new IllegalStateException("OAUTH_MIGRATION_TARGET_RESOURCE_MISSING"));
                if (targetResource.getStatus() != ResourceServerStatus.ACTIVE) {
                    throw new IllegalStateException("OAUTH_MIGRATION_TARGET_RESOURCE_NOT_ACTIVE");
                }
            }
        }

        OAuthClient.ClientType clientType = OAuthClient.ClientType.valueOf(source.getClientType());
        OAuthClientPurposeEnum purpose = OAuthClientPurposeEnum.valueOf(assignment.getPurpose());
        OAuthClientBO existing = clientRepository.findByClientId(realm, source.getClientId()).orElse(null);
        if (existing != null) {
            if (!sameMigratedClient(existing, source, assignment, redirectUris, resourceUris, allowedScopes)) {
                throw new IllegalStateException("OAUTH_MIGRATION_TARGET_CLIENT_CONTENT_CONFLICT");
            }
            if (clientType == OAuthClient.ClientType.CONFIDENTIAL) {
                verifySecretArtifact(command, realm, existing);
            }
            return false;
        }

        if (clientType == OAuthClient.ClientType.CONFIDENTIAL
                && Files.exists(secretArtifactPath(command, realm.getRealmKey(), source.getClientId()),
                LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("OAUTH_MIGRATION_ORPHAN_SECRET_ARTIFACT");
        }
        CreateOAuthClientDTO create = CreateOAuthClientDTO.builder()
                .appId(source.getAppId())
                .clientId(source.getClientId())
                .clientName(source.getClientName())
                .clientType(clientType)
                .purpose(purpose)
                .accessTokenTtlSeconds(source.getAccessTokenTtlSeconds())
                .refreshTokenTtlSeconds(purpose == OAuthClientPurposeEnum.SERVICE
                        ? 0 : source.getRefreshTokenTtlSeconds())
                .redirectUris(redirectUris)
                .resourceUris(resourceUris)
                .allowedScopes(allowedScopes)
                .build();
        CreatedOAuthClientVO created = clientService.create(realm, platformRealm, create, migrationOperator(command));
        OAuthClientBO target = clientRepository.findByClientId(realm, source.getClientId())
                .orElseThrow(() -> new IllegalStateException("OAUTH_MIGRATION_CLIENT_NOT_PERSISTED"));
        if ("DISABLED".equals(source.getStatus())) {
            setClientStatus(realm, target, OAuthClient.Status.DISABLED);
            target = clientRepository.findByClientId(realm, source.getClientId())
                    .orElseThrow(() -> new IllegalStateException("OAUTH_MIGRATION_CLIENT_NOT_PERSISTED"));
        }
        requireInserted(migrationDAO.restoreClientHistory(realm.getStoragePartitionId(), source.getClientId(),
                source.getVersion(), source.getCreatedAt(), source.getUpdatedAt()),
                "OAUTH_MIGRATION_CLIENT_HISTORY_NOT_RESTORED");
        target = clientRepository.findByClientId(realm, source.getClientId())
                .orElseThrow(() -> new IllegalStateException("OAUTH_MIGRATION_CLIENT_NOT_PERSISTED"));
        if (clientType == OAuthClient.ClientType.CONFIDENTIAL) {
            writeSecretArtifact(command, realm, target, created.getClientSecret());
        }
        return true;
    }

    private boolean importResource(OAuthMigrationCommand command, MigrationPlan plan, TrustedRealmBO platformRealm,
            ResourceRowDTO source) {
        ClientOwnershipAssignmentDTO manager = ownershipAssignment(plan.ownershipMap(), source.getManagementClientId());
        String conflict = resourceConflict(source, manager);
        if (conflict != null) {
            throw new IllegalStateException("OAUTH_MIGRATION_RESOURCE_CONFLICT:" + conflict);
        }
        OAuthResourceBO existing = resourceRepository.findByResourceServerId(platformRealm,
                source.getResourceServerId()).orElse(null);
        if (existing != null) {
            if (!sameMigratedResource(existing, source)) {
                throw new IllegalStateException("OAUTH_MIGRATION_TARGET_RESOURCE_CONTENT_CONFLICT");
            }
            return false;
        }
        OAuthClientBO managementClient = clientRepository.findByClientId(platformRealm, source.getManagementClientId())
                .orElseThrow(() -> new IllegalStateException("OAUTH_MIGRATION_RESOURCE_MANAGER_NOT_IMPORTED"));
        if (managementClient.getPurpose() != OAuthClientPurposeEnum.SERVICE
                || managementClient.getClientType() != OAuthClient.ClientType.CONFIDENTIAL
                || managementClient.getStatus() != OAuthClient.Status.ACTIVE
                || blank(managementClient.getSecretHash())) {
            throw new IllegalStateException("OAUTH_MIGRATION_RESOURCE_MANAGER_INVALID");
        }
        CreateResourceServerDTO commandDTO = new CreateResourceServerDTO(
                source.getResourceServerId(), source.getResourceUri(), source.getBizCode(), source.getAppCode(),
                source.getEnvironment(), source.getDisplayName(), source.getManagementClientId(),
                source.getRbacApplicationCode(), source.getEntryPermissionCode());
        ResourceServerVO created = resourceServerService.create(platformRealm, commandDTO);
        if ("DISABLED".equals(source.getStatus())) {
            resourceServerService.disable(platformRealm, source.getResourceServerId(),
                    new ResourceVersionDTO(created.getVersion()));
        }
        requireInserted(migrationDAO.restoreResourceHistory(platformRealm.getStoragePartitionId(),
                source.getResourceServerId(), source.getVersion(), source.getCreatedAt(), source.getUpdatedAt()),
                "OAUTH_MIGRATION_RESOURCE_HISTORY_NOT_RESTORED");
        return true;
    }

    private boolean importGrantsForClient(OAuthMigrationCommand command, MigrationPlan plan,
            TrustedRealmBO clientRealm, TrustedRealmBO platformRealm, ClientRowDTO source,
            ClientOwnershipAssignmentDTO assignment, boolean importedClient) {
        List<GrantRowDTO> sourceGrants = migrationDAO.selectGrantsForClient(source.getClientId(), 65);
        if (sourceGrants.size() >= 65) {
            throw new IllegalStateException("OAUTH_MIGRATION_GRANT_PAGE_LIMIT");
        }
        OAuthClientBO client = clientRepository.findByClientId(clientRealm, source.getClientId())
                .orElseThrow(() -> new IllegalStateException("OAUTH_MIGRATION_TARGET_CLIENT_MISSING"));
        boolean changed = false;
        for (GrantRowDTO sourceGrant : sourceGrants) {
            ResourceRowDTO sourceResource = migrationDAO.selectResourceById(sourceGrant.getResourceServerId());
            if (sourceResource == null || !"ACTIVE".equals(sourceResource.getStatus())) {
                throw new IllegalStateException("OAUTH_MIGRATION_GRANT_RESOURCE_NOT_ACTIVE");
            }
            OAuthResourceBO resource = resourceRepository.findByResourceServerId(platformRealm,
                            sourceGrant.getResourceServerId())
                    .orElseThrow(() -> new IllegalStateException("OAUTH_MIGRATION_TARGET_RESOURCE_MISSING"));
            if (resource.getStatus() != ResourceServerStatus.ACTIVE) {
                throw new IllegalStateException("OAUTH_MIGRATION_TARGET_RESOURCE_NOT_ACTIVE");
            }
            ResourceGrantType grantType = ResourceGrantType.valueOf(sourceGrant.getGrantType());
            List<String> approvedScopes = assignment.getApprovedGrantScopesByResource()
                    .get(sourceGrant.getResourceServerId()).stream().sorted().toList();
            List<String> storedScopes = approvedScopes;
            ClientResourceGrant.Status desiredStatus = ClientResourceGrant.Status.valueOf(sourceGrant.getStatus());
            OAuthGrantBO current = grantRepository.findGrant(clientRealm, client.getId(),
                    sourceGrant.getResourceServerId(), grantType).orElse(null);
            boolean createdDuringMigration = current == null || importedClient;
            boolean grantChanged = false;
            boolean initializeUserScopes = importedClient && current != null
                    && grantType == ResourceGrantType.USER_DELEGATION
                    && current.getVersion() == 0L && current.getStatus() == ClientResourceGrant.Status.ACTIVE
                    && current.getAllowedScopes().isEmpty();
            if (current != null && (!Objects.equals(current.getResourceVersion(), resource.getVersion())
                    || (!initializeUserScopes && !Objects.equals(current.getAllowedScopes(), storedScopes)))) {
                throw new IllegalStateException("OAUTH_MIGRATION_TARGET_GRANT_CONTENT_CONFLICT");
            }
            if (current == null) {
                OAuthGrantBO importedGrant = OAuthGrantBO.builder()
                        .version(0L)
                        .clientPk(client.getId())
                        .resourceServerId(sourceGrant.getResourceServerId())
                        .grantType(grantType)
                        .allowedScopes(storedScopes)
                        .status(desiredStatus)
                        .resourceVersion(resource.getVersion())
                        .build();
                current = grantRepository.save(clientRealm, importedGrant, 0L);
                changed = true;
                grantChanged = true;
            } else if (!importedClient && (current.getStatus() != desiredStatus
                    || current.getVersion() != sourceGrant.getVersion())) {
                throw new IllegalStateException("OAUTH_MIGRATION_TARGET_GRANT_CONTENT_CONFLICT");
            } else if (initializeUserScopes || current.getStatus() != desiredStatus) {
                OAuthGrantBO updated = OAuthGrantBO.builder()
                        .id(current.getId())
                        .version(Math.addExact(current.getVersion(), 1L))
                        .clientPk(current.getClientPk())
                        .resourceServerId(current.getResourceServerId())
                        .grantType(current.getGrantType())
                        .allowedScopes(storedScopes)
                        .status(desiredStatus)
                        .resourceVersion(current.getResourceVersion())
                        .build();
                current = grantRepository.save(clientRealm, updated, current.getVersion());
                changed = true;
                grantChanged = true;
            } else if (current.getVersion() != sourceGrant.getVersion() && !importedClient) {
                throw new IllegalStateException("OAUTH_MIGRATION_TARGET_GRANT_VERSION_CONFLICT");
            }
            if (createdDuringMigration || current.getVersion() != sourceGrant.getVersion()) {
                requireInserted(migrationDAO.restoreGrantHistory(clientRealm.getStoragePartitionId(), client.getId(),
                        sourceGrant.getResourceServerId(), grantType.name(), sourceGrant.getVersion(),
                        sourceGrant.getCreatedAt(), sourceGrant.getUpdatedAt()),
                        "OAUTH_MIGRATION_GRANT_HISTORY_NOT_RESTORED");
                current = grantRepository.findGrant(clientRealm, client.getId(), sourceGrant.getResourceServerId(),
                                grantType)
                        .orElseThrow(() -> new IllegalStateException("OAUTH_MIGRATION_TARGET_GRANT_MISSING"));
            }
            if (grantType == ResourceGrantType.CLIENT_CREDENTIALS && grantChanged) {
                resourceEvents.enqueueServiceGrantProjection(clientRealm, client, current);
            }
        }
        return changed;
    }

    private boolean sameMigratedResource(OAuthResourceBO existing, ResourceRowDTO source) {
        return Objects.equals(existing.getResourceServerId(), source.getResourceServerId())
                && Objects.equals(existing.getResourceUri().toASCIIString(), source.getResourceUri())
                && Objects.equals(existing.getBizCode(), source.getBizCode())
                && Objects.equals(existing.getAppCode(), source.getAppCode())
                && Objects.equals(existing.getEnvironment(), source.getEnvironment())
                && Objects.equals(existing.getDisplayName(), source.getDisplayName())
                && Objects.equals(existing.getManagementClientId(), source.getManagementClientId())
                && Objects.equals(existing.getRbacApplicationCode(), source.getRbacApplicationCode())
                && Objects.equals(existing.getEntryPermissionCode(), source.getEntryPermissionCode())
                && existing.getStatus().name().equals(source.getStatus())
                && existing.getVersion() == source.getVersion()
                && Objects.equals(existing.getCreatedAt(), source.getCreatedAt())
                && Objects.equals(existing.getUpdatedAt(), source.getUpdatedAt());
    }

    private List<String> clientAllowedScopes(ClientOwnershipAssignmentDTO assignment) {
        Set<String> values = new LinkedHashSet<>();
        List<Set<String>> perResource = assignment.getApprovedGrantScopesByResource().values().stream()
                .map(Set::copyOf).toList();
        if (perResource.isEmpty()) {
            throw new IllegalStateException("OAUTH_MIGRATION_CLIENT_SCOPE_MAP_EMPTY");
        }
        if ("USER".equals(assignment.getPurpose())) {
            values.addAll(perResource.get(0));
            for (Set<String> scopes : perResource.subList(1, perResource.size())) {
                values.retainAll(scopes);
            }
        } else {
            perResource.forEach(values::addAll);
        }
        if (values.isEmpty()) {
            throw new IllegalStateException("OAUTH_MIGRATION_CLIENT_SCOPE_SET_EMPTY");
        }
        return values.stream().sorted().toList();
    }

    private boolean sameMigratedClient(OAuthClientBO existing, ClientRowDTO source,
            ClientOwnershipAssignmentDTO assignment, List<String> redirectUris,
            List<String> resourceUris, List<String> allowedScopes) {
        return Objects.equals(existing.getClientId(), source.getClientId())
                && Objects.equals(existing.getAppId(), source.getAppId())
                && Objects.equals(existing.getClientName(), source.getClientName())
                && existing.getClientType() == OAuthClient.ClientType.valueOf(source.getClientType())
                && existing.getPurpose() == OAuthClientPurposeEnum.valueOf(assignment.getPurpose())
                && existing.getStatus() == OAuthClient.Status.valueOf(source.getStatus())
                && Objects.equals(existing.getAccessTtlSeconds(), source.getAccessTokenTtlSeconds())
                && Objects.equals(existing.getRefreshTtlSeconds(), "SERVICE".equals(assignment.getPurpose())
                ? 0 : source.getRefreshTokenTtlSeconds())
                && Objects.equals(existing.getRedirectUris().stream().sorted().toList(), redirectUris)
                && Objects.equals(existing.getResourceUris().stream().sorted().toList(), resourceUris)
                && Objects.equals(existing.getAllowedScopes().stream().sorted().toList(), allowedScopes)
                && Objects.equals(existing.getVersion(), source.getVersion())
                && Objects.equals(existing.getCreatedAt(), source.getCreatedAt())
                && Objects.equals(existing.getUpdatedAt(), source.getUpdatedAt());
    }

    private void setClientStatus(TrustedRealmBO realm, OAuthClientBO client, OAuthClient.Status status) {
        if (client.getStatus() == status) {
            return;
        }
        clientService.update(realm, client.getClientId(), UpdateOAuthClientDTO.builder()
                .clientName(client.getClientName())
                .status(status)
                .accessTokenTtlSeconds(client.getAccessTtlSeconds())
                .refreshTokenTtlSeconds(client.getRefreshTtlSeconds())
                .expectedVersion(client.getVersion())
                .allowedScopes(client.getAllowedScopes())
                .build());
    }

    private void verifySecretArtifact(OAuthMigrationCommand command, TrustedRealmBO realm, OAuthClientBO client) {
        Path path = secretArtifactPath(command, realm.getRealmKey(), client.getClientId());
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)
                || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || !ownerOnly(path)) {
            throw new IllegalStateException("OAUTH_MIGRATION_CLIENT_SECRET_ARTIFACT_MISSING");
        }
        try {
            JsonNode artifact = objectMapper.readTree(path.toFile());
            String secret = artifact.path("clientSecret").asText(null);
            if (artifact.path("schemaVersion").asInt() != 1
                    || !realm.getRealmKey().equals(artifact.path("realmKey").asText())
                    || !realm.getIssuer().toASCIIString().equals(artifact.path("issuer").asText())
                    || !client.getClientId().equals(artifact.path("clientId").asText())
                    || artifact.path("credentialVersion").asLong(-1L) != client.getCredentialVersion()
                    || !command.getSourceSnapshotId().equals(artifact.path("sourceSnapshotId").asText())
                    || secret == null || client.getSecretHash() == null
                    || !SECRET_MATCHER.matches(secret, client.getSecretHash())) {
                throw new IllegalStateException("OAUTH_MIGRATION_CLIENT_SECRET_ARTIFACT_MISMATCH");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("OAUTH_MIGRATION_CLIENT_SECRET_ARTIFACT_INVALID");
        }
    }

    private void writeSecretArtifact(OAuthMigrationCommand command, TrustedRealmBO realm,
            OAuthClientBO client, String plaintextSecret) {
        if (blank(plaintextSecret) || blank(client.getSecretHash())
                || !SECRET_MATCHER.matches(plaintextSecret, client.getSecretHash())) {
            throw new IllegalStateException("OAUTH_MIGRATION_CLIENT_SECRET_MATERIAL_INVALID");
        }
        Path directory = command.getSecretDistributionDirectory();
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(directory)
                || !ownerDirectory(directory)) {
            throw new IllegalStateException("OAUTH_MIGRATION_SECRET_DIRECTORY_INVALID");
        }
        ObjectNode artifact = objectMapper.createObjectNode();
        artifact.put("schemaVersion", 1);
        artifact.put("realmKey", realm.getRealmKey());
        artifact.put("sourceSnapshotId", command.getSourceSnapshotId());
        artifact.put("issuer", realm.getIssuer().toASCIIString());
        artifact.put("clientId", client.getClientId());
        artifact.put("credentialVersion", client.getCredentialVersion());
        artifact.put("clientSecret", plaintextSecret);
        writeOwnerOnlyArtifact(secretArtifactPath(command, realm.getRealmKey(), client.getClientId()),
                artifact.toString());
    }

    private Path secretArtifactPath(OAuthMigrationCommand command, String realmKey, String clientId) {
        return command.getSecretDistributionDirectory().resolve(secretFileName(realmKey, clientId));
    }

    private static void writeOwnerOnlyArtifact(Path path, String content) {
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
            throw new IllegalStateException("OAUTH_MIGRATION_SECRET_ARTIFACT_EXISTS");
        }
        Path temporary = null;
        try {
            temporary = Files.createTempFile(path.getParent(), ".oauth-secret-", ".tmp");
            Files.setPosixFilePermissions(temporary, OWNER_FILE_PERMISSIONS);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                channel.write(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));
                channel.force(true);
            }
            Files.createLink(path, temporary);
            Files.delete(temporary);
            temporary = null;
            try (FileChannel directory = FileChannel.open(path.getParent(), StandardOpenOption.READ)) {
                directory.force(true);
            }
        } catch (IOException | UnsupportedOperationException exception) {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    log.warn("Could not remove temporary OAuth secret artifact");
                }
            }
            throw new IllegalStateException("OAUTH_MIGRATION_SECRET_ARTIFACT_WRITE_FAILED");
        }
    }

    private List<?> readPage(SourceType type, String lastId) {
        return switch (type) {
            case USER -> migrationDAO.selectUsersAfter(lastId, BATCH_SIZE);
            case CREDENTIAL -> migrationDAO.selectCredentialsAfter(lastId, BATCH_SIZE);
            case TENANT -> migrationDAO.selectTenantsAfter(lastId, BATCH_SIZE);
            case MEMBERSHIP -> migrationDAO.selectMembershipsAfter(lastId, BATCH_SIZE);
            case AUDIT -> migrationDAO.selectAuditAfter(lastId, BATCH_SIZE);
        };
    }

    private boolean writeIfAbsentOrVerify(SourceType type, Object row, OAuthMigrationCommand command) {
        long partitionId = directoryContext.directoryPartitionId();
        return switch (type) {
            case USER -> writeUser((UserRowDTO) row, partitionId, migrationActor(command));
            case CREDENTIAL -> writeCredential((CredentialRowDTO) row, partitionId, migrationActor(command));
            case TENANT -> writeTenant((TenantRowDTO) row, partitionId, command);
            case MEMBERSHIP -> writeMembership((MembershipRowDTO) row, partitionId, migrationActor(command));
            case AUDIT -> writeAudit((AuditRowDTO) row, partitionId, command);
        };
    }

    private boolean writeUser(UserRowDTO source, long partitionId, String actor) {
        IdentityUserPO existing = migrationDAO.selectUserByIdentitySub(partitionId, source.getId());
        IdentityUserPO expected = mapUser(source, partitionId, SnowflakeIdGenerator.nextLongId(), actor);
        if (existing != null) {
            if (!sameUser(existing, expected)) {
                throw new IllegalStateException("OAUTH_MIGRATION_USER_CONTENT_CONFLICT");
            }
            return false;
        }
        requireInserted(migrationDAO.insertUserMigration(expected), "OAUTH_MIGRATION_USER_INSERT_NOT_APPLIED");
        return true;
    }

    private boolean writeCredential(CredentialRowDTO source, long partitionId, String actor) {
        IdentityCredentialPO existing = migrationDAO.selectCredentialByLegacyId(partitionId, source.getId());
        IdentityCredentialPO expected = mapCredential(source, partitionId, SnowflakeIdGenerator.nextLongId(),
                actor);
        if (existing != null) {
            if (!sameCredential(existing, expected)) {
                throw new IllegalStateException("OAUTH_MIGRATION_CREDENTIAL_CONTENT_CONFLICT");
            }
            return false;
        }
        requireInserted(migrationDAO.insertCredentialMigration(expected), "OAUTH_MIGRATION_CREDENTIAL_INSERT_NOT_APPLIED");
        return true;
    }

    private boolean writeTenant(TenantRowDTO source, long partitionId, OAuthMigrationCommand command) {
        IdentityTenantPO existing = migrationDAO.selectTenantByExternalId(partitionId, source.getId());
        IdentityTenantPO expected = mapTenant(source, partitionId, SnowflakeIdGenerator.nextLongId(),
                migrationActor(command));
        if (existing != null) {
            if (!sameTenant(existing, expected)) {
                throw new IllegalStateException("OAUTH_MIGRATION_TENANT_CONTENT_CONFLICT");
            }
            return false;
        }
        requireInserted(migrationDAO.insertTenantMigration(expected), "OAUTH_MIGRATION_TENANT_INSERT_NOT_APPLIED");
        return true;
    }

    private boolean writeMembership(MembershipRowDTO source, long partitionId, String actor) {
        IdentityTenantMembershipPO existing = migrationDAO.selectMembershipById(partitionId, source.getId());
        IdentityTenantMembershipPO expected = mapMembership(source, partitionId, SnowflakeIdGenerator.nextLongId(),
                actor);
        if (existing != null) {
            if (!sameMembership(existing, expected)) {
                throw new IllegalStateException("OAUTH_MIGRATION_MEMBERSHIP_CONTENT_CONFLICT");
            }
            return false;
        }
        requireInserted(migrationDAO.insertMembershipMigration(expected), "OAUTH_MIGRATION_MEMBERSHIP_INSERT_NOT_APPLIED");
        return true;
    }

    private boolean writeAudit(AuditRowDTO source, long partitionId, OAuthMigrationCommand command) {
        OAuthAuditPO existing = migrationDAO.selectAuditByEventId(partitionId, source.getId());
        OAuthAuditPO expected = mapAudit(source, partitionId, SnowflakeIdGenerator.nextLongId(), migrationActor(command));
        if (existing != null) {
            if (!sameAudit(existing, expected)) {
                throw new IllegalStateException("OAUTH_MIGRATION_AUDIT_CONTENT_CONFLICT");
            }
            return false;
        }
        requireInserted(migrationDAO.insertAuditMigration(expected), "OAUTH_MIGRATION_AUDIT_INSERT_NOT_APPLIED");
        return true;
    }

    private IdentityUserPO mapUser(UserRowDTO source, long partitionId, long targetId, String actor) {
        IdentityUserPO row = new IdentityUserPO();
        row.setId(targetId);
        row.setTenantId(partitionId);
        row.setVersion(source.getVersion());
        row.setCreateTime(source.getCreatedAt());
        row.setCreateUserId(actor);
        row.setUpdateTime(source.getUpdatedAt());
        row.setUpdateUserId(actor);
        row.setDeletedAt(null);
        row.setIdentitySub(source.getId());
        row.setUsername(source.getUsername());
        row.setUsernameNormalized(source.getNormalizedUsername());
        row.setDisplayName(source.getDisplayName());
        row.setStatus(source.getStatus());
        row.setFailedLoginCount(source.getFailedLoginCount());
        row.setLockedUntil(source.getLockedUntil());
        row.setLastLoginAt(source.getLastLoginAt());
        return row;
    }

    private IdentityCredentialPO mapCredential(CredentialRowDTO source, long partitionId, long targetId, String actor) {
        IdentityCredentialPO row = new IdentityCredentialPO();
        row.setId(targetId);
        row.setTenantId(partitionId);
        row.setVersion(source.getVersion());
        row.setCreateTime(source.getCreatedAt());
        row.setCreateUserId(actor);
        row.setUpdateTime(source.getUpdatedAt());
        row.setUpdateUserId(actor);
        row.setDeletedAt(null);
        row.setLegacyId(source.getId());
        row.setIdentitySub(source.getIdentitySub());
        row.setCredentialType(source.getCredentialType());
        row.setPasswordHash(source.getPasswordHash());
        row.setPasswordChangedAt(source.getPasswordChangedAt());
        row.setMustChangePassword(source.getMustChangePassword());
        row.setStatus(source.getStatus());
        return row;
    }

    private IdentityTenantPO mapTenant(TenantRowDTO source, long partitionId, long targetId, String actor) {
        try {
            JsonNode settings = objectMapper.readTree(source.getSettings());
            IdentityTenantPO row = new IdentityTenantPO();
            row.setId(targetId);
            row.setTenantId(partitionId);
            row.setVersion(source.getVersion());
            row.setCreateTime(source.getCreatedAt());
            row.setCreateUserId(sourceActor(source.getCreatedBy(), actor));
            row.setUpdateTime(source.getUpdatedAt());
            row.setUpdateUserId(sourceActor(source.getUpdatedBy(), actor));
            row.setDeletedAt(null);
            row.setIdentityTenantId(source.getId());
            row.setTenantCode(source.getTenantCode());
            row.setTenantName(source.getTenantName());
            row.setStatus(source.getStatus());
            row.setSettings(settings);
            return row;
        } catch (IOException exception) {
            throw new IllegalStateException("OAUTH_MIGRATION_TENANT_SETTINGS_INVALID");
        }
    }

    private IdentityTenantMembershipPO mapMembership(MembershipRowDTO source, long partitionId, long targetId,
            String actor) {
        IdentityTenantMembershipPO row = new IdentityTenantMembershipPO();
        row.setId(targetId);
        row.setTenantId(partitionId);
        row.setVersion(source.getVersion());
        row.setCreateTime(source.getCreatedAt());
        row.setCreateUserId(sourceActor(source.getCreatedBy(), actor));
        row.setUpdateTime(source.getUpdatedAt());
        row.setUpdateUserId(sourceActor(source.getUpdatedBy(), actor));
        row.setDeletedAt(null);
        row.setMembershipId(source.getId());
        row.setIdentityTenantId(source.getTenantId());
        row.setIdentitySub(source.getIdentitySub());
        row.setStatus(source.getStatus());
        return row;
    }

    private OAuthAuditPO mapAudit(AuditRowDTO source, long partitionId, long targetId, String actor) {
        ObjectNode payload = sanitizeAuditPayload(source);
        OAuthAuditPO row = new OAuthAuditPO();
        row.setId(targetId);
        row.setTenantId(partitionId);
        row.setVersion(0L);
        row.setCreateTime(source.getOccurredAt());
        row.setCreateUserId(actor);
        row.setUpdateTime(source.getOccurredAt());
        row.setUpdateUserId(actor);
        row.setDeletedAt(null);
        row.setEventId(source.getId());
        row.setEventType(source.getEventType());
        row.setSubjectRef(source.getTargetSub());
        row.setResult(source.getResult());
        row.setReasonCode(source.getReason());
        row.setTraceId(source.getTraceId());
        row.setOccurredAt(source.getOccurredAt());
        row.setPayload(payload);
        row.setAuditKind("IDENTITY");
        row.setPublicId(source.getId());
        row.setActorSub(source.getActorSub());
        row.setTargetSub(source.getTargetSub());
        row.setIdentityTenantId(source.getTenantId());
        return row;
    }

    private boolean sameUser(IdentityUserPO existing, IdentityUserPO expected) {
        return Objects.equals(existing.getIdentitySub(), expected.getIdentitySub())
                && Objects.equals(existing.getUsername(), expected.getUsername())
                && Objects.equals(existing.getUsernameNormalized(), expected.getUsernameNormalized())
                && Objects.equals(existing.getDisplayName(), expected.getDisplayName())
                && Objects.equals(existing.getStatus(), expected.getStatus())
                && Objects.equals(existing.getFailedLoginCount(), expected.getFailedLoginCount())
                && Objects.equals(existing.getLockedUntil(), expected.getLockedUntil())
                && Objects.equals(existing.getLastLoginAt(), expected.getLastLoginAt())
                && Objects.equals(existing.getVersion(), expected.getVersion())
                && Objects.equals(existing.getCreateTime(), expected.getCreateTime())
                && Objects.equals(existing.getUpdateTime(), expected.getUpdateTime());
    }

    private boolean sameCredential(IdentityCredentialPO existing, IdentityCredentialPO expected) {
        return Objects.equals(existing.getLegacyId(), expected.getLegacyId())
                && Objects.equals(existing.getIdentitySub(), expected.getIdentitySub())
                && Objects.equals(existing.getCredentialType(), expected.getCredentialType())
                && constantTimeEquals(existing.getPasswordHash(), expected.getPasswordHash())
                && Objects.equals(existing.getPasswordChangedAt(), expected.getPasswordChangedAt())
                && Objects.equals(existing.getMustChangePassword(), expected.getMustChangePassword())
                && Objects.equals(existing.getStatus(), expected.getStatus())
                && Objects.equals(existing.getVersion(), expected.getVersion())
                && Objects.equals(existing.getCreateTime(), expected.getCreateTime())
                && Objects.equals(existing.getUpdateTime(), expected.getUpdateTime());
    }

    private boolean sameTenant(IdentityTenantPO existing, IdentityTenantPO expected) {
        return Objects.equals(existing.getIdentityTenantId(), expected.getIdentityTenantId())
                && Objects.equals(existing.getTenantCode(), expected.getTenantCode())
                && Objects.equals(existing.getTenantName(), expected.getTenantName())
                && Objects.equals(existing.getStatus(), expected.getStatus())
                && Objects.equals(existing.getSettings(), expected.getSettings())
                && Objects.equals(existing.getVersion(), expected.getVersion())
                && Objects.equals(existing.getCreateTime(), expected.getCreateTime())
                && Objects.equals(existing.getUpdateTime(), expected.getUpdateTime());
    }

    private boolean sameMembership(IdentityTenantMembershipPO existing, IdentityTenantMembershipPO expected) {
        return Objects.equals(existing.getMembershipId(), expected.getMembershipId())
                && Objects.equals(existing.getIdentityTenantId(), expected.getIdentityTenantId())
                && Objects.equals(existing.getIdentitySub(), expected.getIdentitySub())
                && Objects.equals(existing.getStatus(), expected.getStatus())
                && Objects.equals(existing.getVersion(), expected.getVersion())
                && Objects.equals(existing.getCreateTime(), expected.getCreateTime())
                && Objects.equals(existing.getUpdateTime(), expected.getUpdateTime());
    }

    private boolean sameAudit(OAuthAuditPO existing, OAuthAuditPO expected) {
        return Objects.equals(existing.getEventId(), expected.getEventId())
                && Objects.equals(existing.getEventType(), expected.getEventType())
                && Objects.equals(existing.getResult(), expected.getResult())
                && Objects.equals(existing.getReasonCode(), expected.getReasonCode())
                && Objects.equals(existing.getTraceId(), expected.getTraceId())
                && Objects.equals(existing.getOccurredAt(), expected.getOccurredAt())
                && Objects.equals(existing.getAuditKind(), expected.getAuditKind())
                && Objects.equals(existing.getActorSub(), expected.getActorSub())
                && Objects.equals(existing.getTargetSub(), expected.getTargetSub())
                && Objects.equals(existing.getIdentityTenantId(), expected.getIdentityTenantId())
                && Objects.equals(existing.getPayload(), expected.getPayload());
    }

    private ObjectNode sanitizeAuditPayload(AuditRowDTO source) {
        try {
            JsonNode original = objectMapper.readTree(source.getPayload());
            JsonNode sanitized = redactSensitiveAuditFields(original);
            ObjectNode result = objectMapper.createObjectNode();
            if (sanitized.isObject()) {
                result.setAll((ObjectNode) sanitized);
            }
            result.put("legacyPayloadSha256", sha256(source.getPayload()));
            if (source.getSourceIp() != null) {
                result.put("sourceIp", source.getSourceIp());
            }
            if (source.getUserAgent() != null) {
                result.put("userAgent", redactTokenLike(source.getUserAgent()));
            }
            if (result.toString().getBytes(StandardCharsets.UTF_8).length > 16_384) {
                throw new IllegalStateException("OAUTH_MIGRATION_AUDIT_PAYLOAD_TOO_LARGE");
            }
            return result;
        } catch (IOException exception) {
            throw new IllegalStateException("OAUTH_MIGRATION_AUDIT_PAYLOAD_INVALID");
        }
    }

    private JsonNode redactSensitiveAuditFields(JsonNode value) {
        if (value.isObject()) {
            ObjectNode result = objectMapper.createObjectNode();
            value.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                String normalized = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
                if (normalized.contains("token") || normalized.contains("secret") || normalized.contains("password")
                        || normalized.contains("credential") || normalized.contains("authorization")
                        || normalized.contains("cookie") || normalized.contains("privatekey")
                        || normalized.equals("jwk") || normalized.equals("jws")) {
                    return;
                }
                result.set(key, redactSensitiveAuditFields(entry.getValue()));
            });
            return result;
        }
        if (value.isArray()) {
            var result = objectMapper.createArrayNode();
            value.forEach(item -> result.add(redactSensitiveAuditFields(item)));
            return result;
        }
        if (value.isTextual()) {
            return objectMapper.getNodeFactory().textNode(redactTokenLike(value.textValue()));
        }
        return value.deepCopy();
    }

    private static String redactTokenLike(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("Bearer ") || trimmed.matches(
                "[A-Za-z0-9_-]{16,}\\.[A-Za-z0-9_-]{16,}\\.[A-Za-z0-9_-]{8,}")) {
            return "[REDACTED]";
        }
        return value;
    }

    private TrustedRealmBO ensureRealm(OAuthMigrationCommand command) {
        return ensureRealm(command, command.getRealmKey());
    }

    private TrustedRealmBO ensureRealm(OAuthMigrationCommand command, String realmKey) {
        RealmTypeEnum realmType;
        Long businessTenantId;
        long storagePartitionId;
        String issuerPath;
        String status = "ACTIVE";
        if ("platform".equals(realmKey)) {
            realmType = RealmTypeEnum.PLATFORM;
            businessTenantId = null;
            storagePartitionId = directoryContext.directoryPartitionId();
            issuerPath = "/platform";
        } else {
            String tenantValue = realmKey.substring("tenant:".length());
            Long tenantId = canonicalPositiveLong(tenantValue);
            if (tenantId == null || migrationDAO.selectTenantByExternalId(
                    directoryContext.directoryPartitionId(), tenantValue) == null) {
                throw new IllegalStateException("OAUTH_MIGRATION_REALM_TENANT_NOT_IMPORTED");
            }
            IdentityTenantPO tenant = migrationDAO.selectTenantByExternalId(
                    directoryContext.directoryPartitionId(), tenantValue);
            realmType = RealmTypeEnum.TENANT;
            businessTenantId = tenantId;
            storagePartitionId = tenantId;
            issuerPath = "/tenants/" + tenantId;
            status = switch (tenant.getStatus()) {
                case "ACTIVE" -> "ACTIVE";
                case "INITIALIZING" -> "INITIALIZING";
                case "SUSPENDED" -> "SUSPENDED";
                case "CLOSED" -> "CLOSED";
                default -> throw new IllegalStateException("OAUTH_MIGRATION_REALM_TENANT_STATUS_INVALID");
            };
        }
        URI issuer = issuer(issuerPath);
        TrustedRealmBO trustedRealm = TrustedRealmBO.builder()
                .issuer(issuer)
                .realmKey(realmKey)
                .realmType(realmType)
                .storagePartitionId(storagePartitionId)
                .businessTenantId(businessTenantId)
                .trustVersion(0L)
                .build();
        trustedRealm.validateTrustedPartition();
        OAuthRealmBO existing = realmRepository.findByRealmKey(trustedRealm).orElse(null);
        if (existing != null) {
            if (!Objects.equals(existing.getIssuer(), issuer)
                    || !Objects.equals(existing.getRealmType(), realmType)
                    || !Objects.equals(existing.getBusinessTenantId(), businessTenantId)
                    || !Objects.equals(existing.getStoragePartitionId(), storagePartitionId)) {
                throw new IllegalStateException("OAUTH_MIGRATION_REALM_CONTENT_CONFLICT");
            }
            return TrustedRealmBO.builder()
                    .issuer(issuer)
                    .realmKey(realmKey)
                    .realmType(realmType)
                    .storagePartitionId(storagePartitionId)
                    .businessTenantId(businessTenantId)
                    .trustVersion(existing.getTrustVersion())
                    .build();
        }
        OAuthRealmBO realm = OAuthRealmBO.builder()
                .issuer(issuer)
                .realmKey(realmKey)
                .realmType(realmType)
                .businessTenantId(businessTenantId)
                .storagePartitionId(storagePartitionId)
                .status(status)
                .trustVersion(0L)
                .build();
        realmRepository.save(trustedRealm, realm, 0L);
        return trustedRealm;
    }

    private URI issuer(String path) {
        String configuredOrigin = environment.getProperty("egon.tianquan-shoubing.oauth.public-origin");
        if (blank(configuredOrigin)) {
            throw new IllegalStateException("OAUTH_MIGRATION_PUBLIC_ORIGIN_REQUIRED");
        }
        URI origin = URI.create(configuredOrigin);
        if (!"https".equalsIgnoreCase(origin.getScheme()) || origin.getHost() == null
                || origin.getUserInfo() != null || origin.getQuery() != null || origin.getFragment() != null
                || !("".equals(origin.getPath()) || "/".equals(origin.getPath()))) {
            throw new IllegalStateException("OAUTH_MIGRATION_PUBLIC_ORIGIN_INVALID");
        }
        return URI.create(origin.toASCIIString().replaceAll("/+$", "") + path);
    }

    private void validateCommand(OAuthMigrationCommand command) {
        Objects.requireNonNull(command, "command");
        if (blank(command.getRealmKey()) || blank(command.getSourceSnapshotId())
                || command.getFreezeMarkerFile() == null || command.getOwnershipMapFile() == null
                || command.getCheckpointFile() == null
                || command.getSecretDistributionDirectory() == null) {
            throw new IllegalArgumentException("OAUTH_MIGRATION_COMMAND_INCOMPLETE");
        }
        if (!"platform".equals(command.getRealmKey())
                && !command.getRealmKey().matches("tenant:[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException("OAUTH_MIGRATION_REALM_KEY_INVALID");
        }
        if (!command.isDryRun() && (command.getApprovedReportSha256() == null
                || !command.getApprovedReportSha256().matches("[0-9a-fA-F]{64}"))) {
            throw new IllegalArgumentException("OAUTH_MIGRATION_APPROVAL_REQUIRED");
        }
        if (!validFreezeMarker(command)) {
            throw new IllegalStateException("OAUTH_MIGRATION_FREEZE_MARKER_INVALID");
        }
        if (!command.getCheckpointFile().isAbsolute() || !command.getSecretDistributionDirectory().isAbsolute()) {
            throw new IllegalArgumentException("OAUTH_MIGRATION_PATH_MUST_BE_ABSOLUTE");
        }
    }

    private boolean validFreezeMarker(OAuthMigrationCommand command) {
        Path path = command.getFreezeMarkerFile();
        if (!path.isAbsolute() || Files.isSymbolicLink(path) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || !ownerOnly(path)) {
            return false;
        }
        try {
            String expected = "realm=" + command.getRealmKey() + "\nsnapshot=" + command.getSourceSnapshotId() + "\n";
            return expected.equals(Files.readString(path));
        } catch (IOException exception) {
            return false;
        }
    }

    private MigrationCheckpoint readCheckpoint(OAuthMigrationCommand command, MigrationPlan plan) {
        Path path = command.getCheckpointFile();
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        if (Files.isSymbolicLink(path) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || !ownerOnly(path)) {
            throw new IllegalStateException("OAUTH_MIGRATION_CHECKPOINT_PERMISSIONS_INVALID");
        }
        try {
            if (Files.size(path) > 65_536L) {
                throw new IllegalStateException("OAUTH_MIGRATION_CHECKPOINT_INVALID");
            }
            MigrationCheckpoint checkpoint = objectMapper.readerFor(MigrationCheckpoint.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .with(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                    .with(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                    .readValue(path.toFile());
            if (!Objects.equals(checkpoint.realmKey(), command.getRealmKey())
                    || !Objects.equals(checkpoint.sourceSnapshotId(), command.getSourceSnapshotId())
                    || !Objects.equals(checkpoint.reportSha256(), plan.reportSha256())
                    || checkpoint.typeIndex() < 0 || checkpoint.typeIndex() > SourceType.values().length
                    || !validCheckpointState(command, checkpoint)
                    || checkpoint.importedCount() > plan.sourceCount()
                    || checkpoint.skippedCount() > plan.sourceCount() - checkpoint.importedCount()) {
                throw new IllegalStateException("OAUTH_MIGRATION_CHECKPOINT_MISMATCH");
            }
            return checkpoint;
        } catch (IOException exception) {
            throw new IllegalStateException("OAUTH_MIGRATION_CHECKPOINT_INVALID");
        }
    }

    private static boolean validCheckpointState(OAuthMigrationCommand command, MigrationCheckpoint checkpoint) {
        if (blank(checkpoint.phase()) || checkpoint.importedCount() < 0L || checkpoint.skippedCount() < 0L
                || (checkpoint.lastSourceId() != null
                && (blank(checkpoint.lastSourceId()) || checkpoint.lastSourceId().length() > 256))
                || (checkpoint.oauthCursor() != null
                && (blank(checkpoint.oauthCursor()) || checkpoint.oauthCursor().length() > 256))) {
            return false;
        }
        if ("DIRECTORY".equals(checkpoint.phase())) {
            return !checkpoint.complete() && checkpoint.oauthCursor() == null
                    && (checkpoint.typeIndex() < SourceType.values().length || checkpoint.lastSourceId() == null);
        }
        if (checkpoint.typeIndex() != SourceType.values().length || checkpoint.lastSourceId() != null) {
            return false;
        }
        return switch (checkpoint.phase()) {
            case "OAUTH_PENDING" -> !checkpoint.complete() && checkpoint.oauthCursor() == null;
            case "PLATFORM_CLIENTS", "RESOURCES", "PLATFORM_GRANTS" -> !checkpoint.complete()
                    && "platform".equals(command.getRealmKey());
            case "TENANT_CLIENTS" -> !checkpoint.complete() && command.getRealmKey().startsWith("tenant:");
            case "COMPLETE" -> checkpoint.complete() && checkpoint.oauthCursor() == null;
            default -> false;
        };
    }

    private void writeCheckpoint(Path path, MigrationCheckpoint checkpoint) {
        Path parent = path.getParent();
        if (parent == null || !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(parent)) {
            throw new IllegalStateException("OAUTH_MIGRATION_CHECKPOINT_PARENT_INVALID");
        }
        Path temporary = null;
        try {
            temporary = Files.createTempFile(parent, ".oauth-migration-", ".tmp");
            Files.setPosixFilePermissions(temporary, OWNER_FILE_PERMISSIONS);
            byte[] json = objectMapper.writeValueAsBytes(checkpoint);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(json);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            try (FileChannel directory = FileChannel.open(parent, StandardOpenOption.READ)) {
                directory.force(true);
            }
        } catch (IOException | UnsupportedOperationException exception) {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    log.warn("Could not remove temporary migration checkpoint file");
                }
            }
            throw new IllegalStateException("OAUTH_MIGRATION_CHECKPOINT_WRITE_FAILED");
        }
    }

    private boolean ownerOnly(Path path) {
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS);
            return permissions.equals(OWNER_FILE_PERMISSIONS);
        } catch (IOException | UnsupportedOperationException exception) {
            return false;
        }
    }

    private boolean ownerDirectory(Path path) {
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS);
            return permissions.equals(Set.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        } catch (IOException | UnsupportedOperationException exception) {
            return false;
        }
    }

    private OAuthMigrationReportResult report(OAuthMigrationCommand command, MigrationPlan plan, boolean dryRun,
            long importedCount, long skippedCount, String checkpointId) {
        return OAuthMigrationReportResult.builder()
                .realmKey(command.getRealmKey())
                .sourceSnapshotId(command.getSourceSnapshotId())
                .dryRun(dryRun)
                .sourceFingerprint(plan.sourceFingerprint())
                .ownershipMapFingerprint(plan.ownershipMapFingerprint())
                .reportSha256(plan.reportSha256())
                .checkpointId(checkpointId)
                .sourceCount(plan.sourceCount())
                .importedCount(importedCount)
                .skippedCount(skippedCount)
                .conflictCount(plan.conflictCount())
                .conflictCodes(plan.conflictCodes())
                .confidentialClientSecretFilesPending(dryRun ? plan.confidentialClientCount() : 0L)
                .secretFileReferences(plan.secretFileReferences())
                .legacySigningKeysRequireReplacement(plan.legacySigningKeyCount())
                .build();
    }

    private String pageContentFingerprint(SourceType type, List<?> rows) {
        MessageDigest digest = digest();
        for (Object row : rows) {
            updateDigest(digest, type.name(), sourceKey(type, row), safeFingerprint(type, row), secretFingerprint(type, row));
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private String sourceKey(SourceType type, Object row) {
        return switch (type) {
            case USER -> ((UserRowDTO) row).getId();
            case CREDENTIAL -> ((CredentialRowDTO) row).getId();
            case TENANT -> ((TenantRowDTO) row).getId();
            case MEMBERSHIP -> ((MembershipRowDTO) row).getId();
            case AUDIT -> ((AuditRowDTO) row).getId();
        };
    }

    private String safeFingerprint(SourceType type, Object row) {
        return switch (type) {
            case USER -> safeUserFingerprint((UserRowDTO) row);
            case CREDENTIAL -> safeCredentialFingerprint((CredentialRowDTO) row);
            case TENANT -> safeTenantFingerprint((TenantRowDTO) row);
            case MEMBERSHIP -> safeMembershipFingerprint((MembershipRowDTO) row);
            case AUDIT -> safeAuditFingerprint((AuditRowDTO) row);
        };
    }

    private String secretFingerprint(SourceType type, Object row) {
        if (type == SourceType.CREDENTIAL) {
            return sha256(((CredentialRowDTO) row).getPasswordHash());
        }
        if (type == SourceType.TENANT) {
            return sha256(((TenantRowDTO) row).getSettings());
        }
        if (type == SourceType.AUDIT) {
            return sha256(((AuditRowDTO) row).getPayload());
        }
        return "";
    }

    private static void requireInserted(int rows, String error) {
        if (rows != 1) {
            throw new IllegalStateException(error);
        }
    }

    private static String sourceActor(String source, String fallback) {
        return blank(source) ? fallback : source;
    }

    private String migrationActor(OAuthMigrationCommand command) {
        return MIGRATION_ACTOR_PREFIX + command.getSourceSnapshotId();
    }

    private static String migrationOperator(OAuthMigrationCommand command) {
        return "migration-" + sha256(command.getRealmKey() + "\n" + command.getSourceSnapshotId()).substring(0, 32);
    }

    private static Long canonicalPositiveLong(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0L && Long.toString(parsed).equals(value) ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return left == right;
        }
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private static String join(Object... fields) {
        StringBuilder value = new StringBuilder();
        for (Object field : fields) {
            if (value.length() > 0) {
                value.append('\u001f');
            }
            if (field instanceof Instant instant) {
                value.append(UTC_FORMAT.format(instant));
            } else if (field != null) {
                value.append(field);
            }
        }
        return value.toString();
    }

    private static void updateDigest(MessageDigest digest, String... values) {
        for (String value : values) {
            digest.update((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
        }
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static String sha256(String value) {
        return HexFormat.of().formatHex(digest().digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8)));
    }

    private static String checkpointId(OAuthMigrationCommand command) {
        return sha256(command.getRealmKey() + "\n" + command.getSourceSnapshotId()).substring(0, 32);
    }

    private static String checkpointId(MigrationCheckpoint checkpoint) {
        return sha256(checkpoint.realmKey() + "\n" + checkpoint.sourceSnapshotId() + "\n"
                + checkpoint.typeIndex() + "\n" + checkpoint.phase() + "\n" + checkpoint.lastSourceId()
                + "\n" + checkpoint.oauthCursor()).substring(0, 32);
    }

    private MigrationCheckpoint oauthCheckpoint(OAuthMigrationCommand command, MigrationPlan plan,
            long imported, long skipped, String phase, String cursor, boolean complete) {
        return new MigrationCheckpoint(command.getRealmKey(), command.getSourceSnapshotId(), plan.reportSha256(),
                SourceType.values().length, null, imported, skipped, phase, cursor, complete);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static final Set<PosixFilePermission> OWNER_FILE_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE
    );

    @FunctionalInterface
    private interface PageReader<T> {
        List<T> read(String afterId, int limit);
    }

    private record MigrationPlan(long sourceCount, long conflictCount, String sourceFingerprint,
            String reportSha256, List<String> conflictCodes, ClientOwnershipMapDTO ownershipMap,
            String ownershipMapFingerprint, long confidentialClientCount, long legacySigningKeyCount,
            List<String> secretFileReferences) {
    }

    private record MigrationProgress(long importedCount, long skippedCount, String checkpointId) {
    }

    private record StageProgress(long imported, long skipped) {
    }

    private record MigrationCheckpoint(String realmKey, String sourceSnapshotId, String reportSha256,
            int typeIndex, String lastSourceId, long importedCount, long skippedCount,
            String phase, String oauthCursor, boolean complete) {
    }
}
