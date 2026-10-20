package top.egon.cola.platform.tianquan.shoubing.admin.integration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityDirectoryContext;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.CreateResourceServerDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.ResourceVersionDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.UpsertClientResourceGrantDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ClientResourceGrantVO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ResourceServerVO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerService;
import top.egon.cola.platform.tianquan.shoubing.admin.support.bootstrap.IdpBootstrapService;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantMembershipBO;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.dto.CreateTenantDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.dto.UpsertTenantMembershipDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.TenantMembershipService;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.service.TenantService;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSigningKeyBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthKeyStatusEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthSigningKeyRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthKeyCipherComponent;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthSecurityProjectionComponent;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUserStatus;
import top.egon.cola.platform.tianquan.shoubing.core.identity.UsernameNormalizer;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.port.IdentityUserStore;
import top.egon.cola.platform.tianquan.shoubing.core.port.PasswordCredentialStore;
import top.egon.cola.platform.tianquan.shoubing.core.port.PasswordHashPort;
import top.egon.cola.platform.tianquan.shoubing.core.port.TenantMembershipPort;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Real owner orchestration used only by the guarded, explicitly opted-in integration fixture. */
@Component("isolatedOAuthProvisioningComponent")
@Validated
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class IsolatedOAuthProvisioningComponent {

    @Qualifier("isolatedOwnershipGuard")
    private final Runnable ownershipGuard;
    @Qualifier("idpBootstrapService")
    private final IdpBootstrapService bootstrap;
    @Qualifier("identityPersistenceAdapter")
    private final IdentityUserStore identities;
    @Qualifier("identityDirectoryContext")
    private final IdentityDirectoryContext directory;
    @Qualifier("tenantServiceImpl")
    private final TenantService tenants;
    @Qualifier("tenantMembershipServiceImpl")
    private final TenantMembershipService memberships;
    @Qualifier("defaultValidator")
    private final Validator validator;
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper mapper;
    @Qualifier("oAuthRealmRepository")
    private final OAuthRealmRepository realms;
    @Qualifier("oAuthSigningKeyRepository")
    private final OAuthSigningKeyRepository signingKeys;
    @Qualifier("oauthKeyCipherComponent")
    private final OAuthKeyCipherComponent keyCipher;
    @Qualifier("transactionManager")
    private final PlatformTransactionManager transactionManager;
    @Qualifier("idpClock")
    private final Clock clock;
    @Qualifier("OAuthClientServiceImpl")
    private final ObjectProvider<OAuthClientService> clientOwners;
    @Qualifier("resourceServerServiceImpl")
    private final ObjectProvider<ResourceServerService> resourceOwners;
    @Qualifier("oauthSecurityProjectionComponent")
    private final ObjectProvider<OAuthSecurityProjectionComponent> securityProjections;
    @Qualifier("oAuthClientRepository")
    private final ObjectProvider<OAuthClientRepository> clientRepositories;
    @Qualifier("identityPersistenceAdapter")
    private final PasswordCredentialStore credentials;
    @Qualifier("springPasswordHashAdapter")
    private final PasswordHashPort passwordHashes;

    public void executePhase(@NotNull @Valid IsolatedOAuthProvisioningIT.IsolatedPhaseCommandDTO command) {
        requireValid(command);
        ownershipGuard.run();
        Map<IsolatedOAuthProvisioningIT.ProvisionStageEnum, Consumer<IsolatedOAuthProvisioningIT.IsolatedPhaseCommandDTO>> stages = Map.of(
                IsolatedOAuthProvisioningIT.ProvisionStageEnum.IDENTITY, this::prepareIdentity,
                IsolatedOAuthProvisioningIT.ProvisionStageEnum.REALMS, this::prepareRealms,
                IsolatedOAuthProvisioningIT.ProvisionStageEnum.SECURITY, this::prepareSecurity,
                IsolatedOAuthProvisioningIT.ProvisionStageEnum.MANAGEMENT_CLIENTS, this::prepareManagementClients,
                IsolatedOAuthProvisioningIT.ProvisionStageEnum.RESOURCES, this::prepareResources,
                IsolatedOAuthProvisioningIT.ProvisionStageEnum.CLIENTS, this::prepareClients,
                IsolatedOAuthProvisioningIT.ProvisionStageEnum.GRANTS, this::prepareGrants);
        stages.get(command.getStage()).accept(command);
        ownershipGuard.run();
        log.info("Isolated provisioning stage reconciled runId={} stage={}", command.getManifest().getRunId(), command.getStage());
    }

    private void prepareIdentity(IsolatedOAuthProvisioningIT.IsolatedPhaseCommandDTO command) {
        if (command.getPasswordFiles() == null) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
        }
        try {
            command.setIdentityFacts(bootstrapIdentityAndTenants(command.getManifest(), command.getPasswordFiles(), command.isResume()));
        } catch (IOException unavailableInput) {
            throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE");
        }
    }

    private void prepareRealms(IsolatedOAuthProvisioningIT.IsolatedPhaseCommandDTO command) {
        if (command.getIdentityFacts() == null) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
        }
        List<TrustedRealmBO> contexts = bootstrapRealms(command.getManifest(), command.getIdentityFacts(), command.isResume());
        Map<String, TrustedRealmBO> selected = new LinkedHashMap<>();
        selected.put("platform", contexts.getFirst());
        for (String code : command.getManifest().getTenantCodes()) {
            String id = command.getIdentityFacts().getTenantIds().get(code);
            selected.put(code, contexts.stream().filter(realm -> realm.getRealmKey().equals("tenant:" + id))
                    .findFirst().orElseThrow(() -> new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN")));
        }
        command.setRealms(Map.copyOf(selected));
    }

    private void prepareSecurity(IsolatedOAuthProvisioningIT.IsolatedPhaseCommandDTO command) {
        for (String code : List.of("platform", "default", "tenant-b")) {
            recoverSecurity(command.getManifest(), preparedRealm(command, code));
        }
    }

    private void prepareManagementClients(IsolatedOAuthProvisioningIT.IsolatedPhaseCommandDTO command) {
        requireRecipe(command);
        for (var client : command.getRecipe().getManagementClients()) {
            if (!"platform".equals(client.getRealmCode())
                    || client.getInput().getPurpose() != top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum.SERVICE) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
            prepareClient(command, client);
        }
    }

    private void prepareResources(IsolatedOAuthProvisioningIT.IsolatedPhaseCommandDTO command) {
        requireRecipe(command);
        for (CreateResourceServerDTO resource : command.getRecipe().getResources()) {
            ResourceServerVO committed = provisionResource(command.getManifest(), preparedRealm(command, "platform"), resource, command.isResume());
            command.getCommittedResources().put(committed.getResourceServerId(), committed);
        }
    }

    private void prepareClients(IsolatedOAuthProvisioningIT.IsolatedPhaseCommandDTO command) {
        requireRecipe(command);
        for (var client : command.getRecipe().getClients()) {
            if (!command.getManifest().getTenantCodes().contains(client.getRealmCode())) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
            prepareClient(command, client);
        }
    }

    private void prepareClient(IsolatedOAuthProvisioningIT.IsolatedPhaseCommandDTO command,
                               IsolatedOAuthProvisioningIT.OAuthClientPreparationDTO client) {
        String operator = command.getIdentityFacts().getSubjects().get("admin");
        if (operator == null) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
        }
        try {
            OAuthClientBO committed = provisionClient(command.getManifest(), preparedRealm(command, client.getRealmCode()),
                    preparedRealm(command, "platform"), client.getInput(), operator,
                    client.getSecretFile() == null ? null : Path.of(client.getSecretFile()), command.isResume());
            command.getCommittedClients().put(client.getRealmCode() + ":" + committed.getClientId(), committed);
        } catch (IOException unavailableInput) {
            throw new IllegalStateException("XINGYUAN_INPUT_UNAVAILABLE");
        }
    }

    private void prepareGrants(IsolatedOAuthProvisioningIT.IsolatedPhaseCommandDTO command) {
        requireRecipe(command);
        for (var grant : command.getRecipe().getGrants()) {
            TrustedRealmBO realm = preparedRealm(command, grant.getRealmCode());
            ClientResourceGrantVO committed = provisionGrant(command.getManifest(), realm,
                    preparedRealm(command, "platform"), grant.getClientId(), grant.getResourceServerId(),
                    grantForPreparedRealm(realm, grant.getInput()));
            command.getCommittedGrants().add(committed);
        }
    }

    static UpsertClientResourceGrantDTO grantForPreparedRealm(TrustedRealmBO realm, UpsertClientResourceGrantDTO input) {
        realm.validateTrustedPartition();
        if (input.getGrantType() != top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType.CLIENT_CREDENTIALS
                || realm.getRealmType() != RealmTypeEnum.TENANT) {
            return input;
        }
        String tenantId = realm.getBusinessTenantId().toString();
        if (input.getTenantId() != null && !tenantId.equals(input.getTenantId())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        return UpsertClientResourceGrantDTO.builder().grantType(input.getGrantType()).tenantId(tenantId)
                .allowedScopes(input.getAllowedScopes()).expectedResourceVersion(input.getExpectedResourceVersion())
                .expectedGrantVersion(input.getExpectedGrantVersion()).build();
    }

    private TrustedRealmBO preparedRealm(IsolatedOAuthProvisioningIT.IsolatedPhaseCommandDTO command, String code) {
        if (command.getRealms() == null || !command.getRealms().containsKey(code)) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
        }
        return command.getRealms().get(code);
    }

    private void requireRecipe(IsolatedOAuthProvisioningIT.IsolatedPhaseCommandDTO command) {
        if (command.getRecipe() == null || command.getIdentityFacts() == null) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
        }
        requireValid(command.getRecipe());
        if (!command.getManifest().getRunId().equals(command.getRecipe().getRunId())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
    }

    public long recoverSecurity(
            @NotNull @Valid IsolatedOAuthProvisioningIT.IsolatedIntegrationManifestDTO manifest,
            @NotNull @Valid TrustedRealmBO trusted) {
        requirePreparedRealm(manifest, trusted);
        OAuthSecurityProjectionComponent projection = securityProjections.getObject();
        ownershipGuard.run();
        long revision = projection.recover(trusted);
        ownershipGuard.run();
        if (revision < 0L || !projection.isIssuerReady(trusted)) {
            throw new IllegalStateException("XINGYUAN_DEPENDENCY_UNAVAILABLE");
        }
        return revision;
    }

    public ResourceServerVO provisionResource(
            @NotNull @Valid IsolatedOAuthProvisioningIT.IsolatedIntegrationManifestDTO manifest,
            @NotNull @Valid TrustedRealmBO platform,
            @NotNull @Valid CreateResourceServerDTO input) {
        return provisionResource(manifest, platform, input, false);
    }

    public ResourceServerVO provisionResource(
            @NotNull @Valid IsolatedOAuthProvisioningIT.IsolatedIntegrationManifestDTO manifest,
            @NotNull @Valid TrustedRealmBO platform,
            @NotNull @Valid CreateResourceServerDTO input, boolean resume) {
        requirePreparedRealm(manifest, platform);
        requireValid(input);
        if (platform.getRealmType() != RealmTypeEnum.PLATFORM || !"local".equals(input.environment())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        ResourceServerService owner = resourceOwners.getObject();
        ownershipGuard.run();
        var matching = owner.list(platform).stream().filter(resource -> input.resourceServerId().equals(resource.getResourceServerId())).toList();
        if (matching.size() > 1 || !matching.isEmpty() && !resume) {
            throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
        }
        ResourceServerVO created;
        if (matching.isEmpty()) {
            ownershipGuard.run();
            created = owner.create(platform, input);
        } else {
            created = matching.getFirst();
            if (!resourceDefinitionMatches(input, created)
                    || !("ACTIVE".equals(created.getStatus()) && created.getVersion() == 1L
                    || "DISABLED".equals(created.getStatus()) && created.getVersion() == 0L)) {
                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
            }
        }
        ownershipGuard.run();
        ResourceServerVO active = "ACTIVE".equals(created.getStatus()) ? created
                : owner.enable(platform, created.getResourceServerId(), new ResourceVersionDTO(created.getVersion()));
        ownershipGuard.run();
        ResourceServerVO committed = owner.detail(platform, input.resourceServerId());
        if (!"ACTIVE".equals(committed.getStatus()) || committed.getVersion() != active.getVersion()
                || !resourceDefinitionMatches(input, committed)) {
            throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
        }
        return committed;
    }

    private boolean resourceDefinitionMatches(CreateResourceServerDTO input, ResourceServerVO resource) {
        return input.resourceServerId().equals(resource.getResourceServerId()) && input.resourceUri().equals(resource.getResourceUri())
                && input.managementClientId().equals(resource.getManagementClientId()) && input.bizCode().equals(resource.getBizCode())
                && input.appCode().equals(resource.getAppCode()) && input.environment().equals(resource.getEnvironment())
                && input.displayName().trim().equals(resource.getDisplayName())
                && input.rbacApplicationCode().equals(resource.getRbacApplicationCode())
                && input.entryPermissionCode().equals(resource.getEntryPermissionCode());
    }

    public OAuthClientBO provisionClient(
            @NotNull @Valid IsolatedOAuthProvisioningIT.IsolatedIntegrationManifestDTO manifest,
            @NotNull @Valid TrustedRealmBO trusted,
            @NotNull @Valid TrustedRealmBO platform,
            @NotNull @Valid CreateOAuthClientDTO input,
            @NotNull String operator,
            Path secretFile) throws IOException {
        return provisionClient(manifest, trusted, platform, input, operator, secretFile, false);
    }

    public OAuthClientBO provisionClient(
            @NotNull @Valid IsolatedOAuthProvisioningIT.IsolatedIntegrationManifestDTO manifest,
            @NotNull @Valid TrustedRealmBO trusted,
            @NotNull @Valid TrustedRealmBO platform,
            @NotNull @Valid CreateOAuthClientDTO input,
            @NotNull String operator, Path secretFile, boolean resume) throws IOException {
        requirePreparedRealm(manifest, trusted);
        requirePreparedRealm(manifest, platform);
        requireValid(input);
        if (platform.getRealmType() != RealmTypeEnum.PLATFORM) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        ownershipGuard.run();
        if (identities.findById(operator).filter(user -> user.status() == IdentityUserStatus.ACTIVE).isEmpty()) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        boolean confidential = input.getClientType() == OAuthClient.ClientType.CONFIDENTIAL;
        Path secretRoot = Path.of(manifest.getRuntimeRoot()).resolve("secrets");
        if (confidential) {
            if (secretFile == null) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_UNAVAILABLE");
            }
        } else if (secretFile != null) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        if (resume) {
            ownershipGuard.run();
            var existing = clientRepositories.getObject().findByClientId(trusted, input.getClientId());
            if (existing.isPresent()) {
                OAuthClientBO committed = existing.get();
                requireClientDefinition(input, committed);
                if (committed.getVersion() != 0L || confidential && committed.getCredentialVersion() != 1L) {
                    throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                }
                if (confidential) {
                    requireClientSecret(secretFile, secretRoot, committed);
                }
                return committed;
            }
            if (confidential && java.nio.file.Files.exists(secretFile, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
            }
        }
        if (confidential) {
            IsolatedOAuthProvisioningIT.validatePrivateSecretDestination(secretFile, secretRoot);
        }
        OAuthClientService owner = clientOwners.getObject();
        var created = new TransactionTemplate(transactionManager).execute(status -> {
            ownershipGuard.run();
            var result = owner.create(trusted, platform, input, operator);
            if (confidential) {
                if (result.getClientSecret() == null || result.getClientSecret().isBlank()) {
                    throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
                }
                byte[] bytes = result.getClientSecret().getBytes(StandardCharsets.UTF_8);
                try {
                    ownershipGuard.run();
                    IsolatedOAuthProvisioningIT.writePrivateSecret(secretFile, secretRoot, bytes);
                } catch (IOException failedPublication) {
                    throw new IllegalStateException("XINGYUAN_DEPENDENCY_UNAVAILABLE");
                } finally {
                    Arrays.fill(bytes, (byte) 0);
                    result.setClientSecret(null);
                }
            } else if (result.getClientSecret() != null) {
                throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
            }
            return result;
        });
        ownershipGuard.run();
        OAuthClientBO committed = clientRepositories.getObject().findByClientId(trusted, input.getClientId())
                .orElseThrow(() -> new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"));
        requireClientDefinition(input, committed);
        if (created == null || committed.getVersion() != created.getVersion()
                || !input.getClientId().equals(created.getClientId())
                || !trusted.getIssuer().toASCIIString().equals(created.getIssuer())) {
            throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
        }
        if (confidential) {
            requireClientSecret(secretFile, secretRoot, committed);
        }
        return committed;
    }

    private void requireClientDefinition(CreateOAuthClientDTO input, OAuthClientBO committed) {
        if (committed.getId() == null || committed.getId() <= 0L || committed.getStatus() != OAuthClient.Status.ACTIVE
                || !input.getClientId().equals(committed.getClientId())
                || !Objects.equals(input.getAppId(), committed.getAppId())
                || !input.getClientName().trim().equals(committed.getClientName())
                || committed.getClientType() != input.getClientType() || committed.getPurpose() != input.getPurpose()
                || !Objects.equals(input.getAccessTokenTtlSeconds(), committed.getAccessTtlSeconds())
                || !Objects.equals(input.getRefreshTokenTtlSeconds(), committed.getRefreshTtlSeconds())
                || !input.getRedirectUris().equals(committed.getRedirectUris())
                || !input.getResourceUris().equals(committed.getResourceUris())
                || committed.getAllowedScopes() == null
                || !Set.copyOf(input.getAllowedScopes()).equals(Set.copyOf(committed.getAllowedScopes()))) {
            throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
        }
    }

    private void requireClientSecret(Path secretFile, Path secretRoot, OAuthClientBO committed) throws IOException {
        if (committed.getSecretHash() == null || !java.nio.file.Files.exists(secretFile, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
        }
        byte[] secret = IsolatedOAuthProvisioningIT.readProtectedBytes(secretFile, secretRoot, 4096);
        try {
            if (!new BCryptPasswordEncoder().matches(new String(secret, StandardCharsets.UTF_8), committed.getSecretHash())) {
                throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
            }
        } finally {
            Arrays.fill(secret, (byte) 0);
        }
    }

    public ClientResourceGrantVO provisionGrant(
            @NotNull @Valid IsolatedOAuthProvisioningIT.IsolatedIntegrationManifestDTO manifest,
            @NotNull @Valid TrustedRealmBO trusted,
            @NotNull @Valid TrustedRealmBO platform,
            @NotNull String clientId, @NotNull String resourceId,
            @NotNull @Valid UpsertClientResourceGrantDTO input) {
        requirePreparedRealm(manifest, trusted);
        requirePreparedRealm(manifest, platform);
        requireValid(input);
        ResourceServerService owner = resourceOwners.getObject();
        ownershipGuard.run();
        var existing = owner.listGrants(trusted, platform, clientId).stream()
                .filter(grant -> resourceId.equals(grant.getResourceServerId())
                        && input.getGrantType().name().equals(grant.getGrantType())
                        && Objects.equals(input.getTenantId(), grant.getTenantId())).toList();
        if (existing.size() > 1) {
            throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
        }
        if (!existing.isEmpty() && "ACTIVE".equals(existing.getFirst().getStatus())
                && input.getAllowedScopes().equals(existing.getFirst().getAllowedScopes())) {
            ownershipGuard.run();
            ResourceServerVO resource = owner.detail(platform, resourceId);
            if (resource.getVersion() != input.getExpectedResourceVersion()
                    || input.getExpectedGrantVersion() != null && input.getExpectedGrantVersion() != existing.getFirst().getVersion()) {
                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
            }
            return existing.getFirst();
        }
        ownershipGuard.run();
        ClientResourceGrantVO saved = owner.putGrant(trusted, platform, clientId, resourceId, input);
        ownershipGuard.run();
        var matching = owner.listGrants(trusted, platform, clientId).stream()
                .filter(grant -> resourceId.equals(grant.getResourceServerId())
                        && input.getGrantType().name().equals(grant.getGrantType())
                        && Objects.equals(input.getTenantId(), grant.getTenantId())).toList();
        if (matching.size() != 1 || !"ACTIVE".equals(matching.getFirst().getStatus())
                || matching.getFirst().getVersion() != saved.getVersion()
                || !input.getAllowedScopes().equals(matching.getFirst().getAllowedScopes())) {
            throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
        }
        return matching.getFirst();
    }

    private void requirePreparedRealm(IsolatedOAuthProvisioningIT.IsolatedIntegrationManifestDTO manifest,
                                      TrustedRealmBO trusted) {
        IsolatedOAuthProvisioningIT.validateTargets(manifest, Path.of(manifest.getRepoRoot()));
        ownershipGuard.run();
        requireValid(trusted);
        trusted.validateTrustedPartition();
        long directoryPartition = Long.parseLong(manifest.getPlatformStoragePartitionId());
        String suffix = trusted.getRealmType() == RealmTypeEnum.PLATFORM ? "/platform" : "/tenants/" + trusted.getBusinessTenantId();
        if (!URI.create(manifest.getPublicOrigin() + suffix).equals(trusted.getIssuer())
                || (trusted.getRealmType() == RealmTypeEnum.PLATFORM
                ? trusted.getStoragePartitionId() != directoryPartition : trusted.getStoragePartitionId() == directoryPartition)) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        ownershipGuard.run();
        OAuthRealmBO committed = realms.findByIssuer(trusted)
                .orElseThrow(() -> new IllegalStateException("XINGYUAN_STATE_CONFLICT"));
        if (!"ACTIVE".equals(committed.getStatus()) || committed.getActiveKid() == null
                || !trusted.getRealmKey().equals(committed.getRealmKey())
                || !trusted.getStoragePartitionId().equals(committed.getStoragePartitionId())
                || !trusted.getTrustVersion().equals(committed.getTrustVersion())) {
            throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
        }
    }

    public List<TrustedRealmBO> bootstrapRealms(
            @NotNull @Valid IsolatedOAuthProvisioningIT.IsolatedIntegrationManifestDTO manifest,
            @NotNull @Valid IdentityFactsBO identityFacts) {
        return bootstrapRealms(manifest, identityFacts, false);
    }

    public List<TrustedRealmBO> bootstrapRealms(
            @NotNull @Valid IsolatedOAuthProvisioningIT.IsolatedIntegrationManifestDTO manifest,
            @NotNull @Valid IdentityFactsBO identityFacts, boolean resume) {
        IsolatedOAuthProvisioningIT.validateTargets(manifest, Path.of(manifest.getRepoRoot()));
        ownershipGuard.run();
        Map<String, String> tenantIds = identityFacts.getTenantIds();
        if (tenantIds == null || !tenantIds.keySet().equals(Set.copyOf(manifest.getTenantCodes()))
                || tenantIds.values().stream().anyMatch(id -> id == null || !id.matches("[1-9][0-9]{0,18}"))) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        long platformPartition = Long.parseLong(manifest.getPlatformStoragePartitionId());
        List<TrustedRealmBO> contexts = new ArrayList<>();
        contexts.add(TrustedRealmBO.builder().issuer(URI.create(manifest.getPublicOrigin() + "/platform"))
                .realmKey("platform").realmType(RealmTypeEnum.PLATFORM).storagePartitionId(platformPartition)
                .trustVersion(0L).build());
        Set<Long> partitions = new HashSet<>();
        partitions.add(platformPartition);
        for (String code : manifest.getTenantCodes()) {
            long tenantId;
            try {
                tenantId = Long.parseLong(tenantIds.get(code));
            } catch (NumberFormatException invalidId) {
                throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
            }
            if (!partitions.add(tenantId)) {
                throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
            }
            contexts.add(TrustedRealmBO.builder().issuer(URI.create(manifest.getPublicOrigin() + "/tenants/" + tenantId))
                    .realmKey("tenant:" + tenantId).realmType(RealmTypeEnum.TENANT)
                    .storagePartitionId(tenantId).businessTenantId(tenantId).trustVersion(0L).build());
        }
        for (TrustedRealmBO trusted : contexts) {
            trusted.validateTrustedPartition();
            requireValid(trusted);
            ownershipGuard.run();
            if (resume) {
                var existing = realms.findByRealmKey(trusted);
                if (existing.isPresent()) {
                    if (!"ACTIVE".equals(existing.get().getStatus()) || existing.get().getActiveKid() == null) {
                        throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                    }
                    verifyCommittedRealm(trusted, existing.get().getActiveKid());
                    continue;
                }
            }
            String kid = new TransactionTemplate(transactionManager).execute(status -> {
                ownershipGuard.run();
                if (realms.findByRealmKeyForUpdate(trusted).isPresent()) {
                    throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                }
                OAuthRealmBO realm = OAuthRealmBO.builder().issuer(trusted.getIssuer()).realmKey(trusted.getRealmKey())
                        .realmType(trusted.getRealmType()).storagePartitionId(trusted.getStoragePartitionId())
                        .businessTenantId(trusted.getBusinessTenantId()).status("INITIALIZING").trustVersion(0L).build();
                requireValid(realm);
                ownershipGuard.run();
                OAuthRealmBO inserted = realms.save(trusted, realm, 0L);
                OAuthSigningKeyBO key = initialSigningKey(trusted);
                requireValid(key);
                ownershipGuard.run();
                signingKeys.save(trusted, key, 0L);
                inserted.setVersion(1L).setStatus("ACTIVE").setActiveKid(key.getKid());
                ownershipGuard.run();
                realms.save(trusted, inserted, 0L);
                return key.getKid();
            });
            verifyCommittedRealm(trusted, kid);
        }
        log.info("Isolated initial realms committed runId={} realmCount={}", manifest.getRunId(), contexts.size());
        return List.copyOf(contexts);
    }

    private void verifyCommittedRealm(TrustedRealmBO trusted, String kid) {
        // These repository reads begin after the owning transaction has completed.
        ownershipGuard.run();
        OAuthRealmBO committed = realms.findByRealmKey(trusted)
                .orElseThrow(() -> new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"));
        ownershipGuard.run();
        OAuthSigningKeyBO committedKey = signingKeys.findByKid(trusted, kid)
                .orElseThrow(() -> new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"));
        if (!"ACTIVE".equals(committed.getStatus()) || committed.getVersion() != 1L
                || !Objects.equals(kid, committed.getActiveKid())
                || !trusted.getIssuer().equals(committed.getIssuer())
                || !trusted.getRealmKey().equals(committed.getRealmKey())
                || !trusted.getStoragePartitionId().equals(committed.getStoragePartitionId())
                || !Objects.equals(trusted.getBusinessTenantId(), committed.getBusinessTenantId())
                || !Objects.equals(trusted.getTrustVersion(), committed.getTrustVersion())
                || committedKey.getStatus() != OAuthKeyStatusEnum.ACTIVE
                || !Objects.equals(kid, committedKey.getKid())) {
            throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
        }
    }

    private OAuthSigningKeyBO initialSigningKey(TrustedRealmBO trusted) {
        byte[] encodedPrivateKey = null;
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(3072);
            var pair = generator.generateKeyPair();
            encodedPrivateKey = pair.getPrivate().getEncoded();
            String kid = UUID.randomUUID().toString();
            RSAKey publicKey = new RSAKey.Builder((RSAPublicKey) pair.getPublic()).keyID(kid)
                    .algorithm(JWSAlgorithm.RS256).keyUse(KeyUse.SIGNATURE).build();
            String privatePem = "-----BEGIN PRIVATE KEY-----\n"
                    + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(encodedPrivateKey)
                    + "\n-----END PRIVATE KEY-----";
            ownershipGuard.run();
            String ciphertext = keyCipher.encryptPrivateKey(trusted, kid, privatePem);
            Instant now = clock.instant();
            return OAuthSigningKeyBO.builder().kid(kid).algorithm("RS256")
                    .publicJwk(mapper.valueToTree(publicKey.toPublicJWK().toJSONObject()))
                    .privateKeyCiphertext(ciphertext).status(OAuthKeyStatusEnum.ACTIVE)
                    .publishedAt(now).activatedAt(now).build();
        } catch (GeneralSecurityException unavailableAlgorithm) {
            throw new IllegalStateException("XINGYUAN_DEPENDENCY_UNAVAILABLE");
        } finally {
            if (encodedPrivateKey != null) {
                Arrays.fill(encodedPrivateKey, (byte) 0);
            }
        }
    }

    public IdentityFactsBO bootstrapIdentityAndTenants(
            @NotNull @Valid IsolatedOAuthProvisioningIT.IsolatedIntegrationManifestDTO manifest,
            @NotNull Map<String, Path> passwordFiles) throws IOException {
        return bootstrapIdentityAndTenants(manifest, passwordFiles, false);
    }

    public IdentityFactsBO bootstrapIdentityAndTenants(
            @NotNull @Valid IsolatedOAuthProvisioningIT.IsolatedIntegrationManifestDTO manifest,
            @NotNull Map<String, Path> passwordFiles, boolean resume) throws IOException {
        IsolatedOAuthProvisioningIT.validateTargets(manifest, Path.of(manifest.getRepoRoot()));
        if (directory.directoryPartitionId() != Long.parseLong(manifest.getPlatformStoragePartitionId())) {
            throw new IllegalArgumentException("XINGYUAN_TARGET_UNSAFE");
        }
        if (!passwordFiles.keySet().equals(Set.of("admin", "viewer", "provisioner"))) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
        Map<String, String> subjects = new LinkedHashMap<>();
        Path secretRoot = Path.of(manifest.getRuntimeRoot()).resolve("secrets");
        for (String userKey : List.of("admin", "viewer", "provisioner")) {
            String username = switch (userKey) { case "admin" -> "alice"; case "viewer" -> "viewer"; default -> "xingyuan-it-provisioner"; };
            ownershipGuard.run();
            var existing = identities.findByNormalizedUsername(new UsernameNormalizer().normalize(username));
            if (existing.isPresent() && !resume) {
                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
            }
            char[] password = IsolatedOAuthProvisioningIT.readProtectedCharacters(passwordFiles.get(userKey), secretRoot, 4096);
            try {
                if (password.length < 12) {
                    throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
                }
                ownershipGuard.run();
                if (existing.isEmpty()) {
                    bootstrap.bootstrap(username, password);
                } else {
                    var user = existing.get();
                    ownershipGuard.run();
                    var credential = credentials.findActive(user.id())
                            .orElseThrow(() -> new IllegalStateException("XINGYUAN_STATE_CONFLICT"));
                    if (user.status() != IdentityUserStatus.ACTIVE || !username.equals(user.username())
                            || credential.mustChangePassword() || !passwordHashes.matches(password, credential.passwordHash())) {
                        throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                    }
                }
            } finally {
                Arrays.fill(password, '\0');
            }
            ownershipGuard.run();
            var created = identities.findByNormalizedUsername(new UsernameNormalizer().normalize(username))
                    .orElseThrow(() -> new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN"));
            if (created.status() != IdentityUserStatus.ACTIVE) {
                throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
            }
            if (created.id() == null || !created.id().matches("[A-Za-z0-9._:-]{1,128}") || subjects.containsValue(created.id())) {
                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
            }
            subjects.put(userKey, created.id());
        }
        Map<String, String> tenantIds = new LinkedHashMap<>();
        String operator = subjects.get("admin");
        for (String code : manifest.getTenantCodes()) {
            CreateTenantDTO input = new CreateTenantDTO(code, "Isolated " + code, mapper.createObjectNode());
            requireValid(input);
            ownershipGuard.run();
            var matching = tenants.list().stream().filter(tenant -> code.equals(tenant.tenantCode())).toList();
            if (matching.size() > 1 || !matching.isEmpty() && !resume) {
                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
            }
            TenantService.TenantView created;
            if (matching.isEmpty()) {
                ownershipGuard.run();
                created = tenants.create(new TenantService.CreateTenantCommand(
                        input.tenantCode(), input.tenantName(), input.settings().toString(), operator));
            } else {
                created = matching.getFirst();
                if (!input.tenantName().equals(created.tenantName())
                        || !mapper.readTree(created.settings()).equals(input.settings())
                        || created.status() != IdentityTenantBO.Status.INITIALIZING && created.status() != IdentityTenantBO.Status.ACTIVE) {
                    throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                }
            }
            if (created.tenantId().equals(manifest.getPlatformStoragePartitionId())) {
                throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
            }
            ownershipGuard.run();
            TenantService.TenantView active = created.status() == IdentityTenantBO.Status.ACTIVE ? created
                    : tenants.update(created.tenantId(), new TenantService.UpdateTenantCommand(
                    created.version(), null, null, IdentityTenantBO.Status.ACTIVE, operator));
            for (String subject : subjects.values()) {
                UpsertTenantMembershipDTO membership = new UpsertTenantMembershipDTO(
                        IdentityTenantMembershipBO.Status.ACTIVE, null);
                requireValid(membership);
                ownershipGuard.run();
                var current = memberships.listByTenant(active.tenantId()).stream()
                        .filter(value -> subject.equals(value.identitySub())).toList();
                if (current.size() > 1 || !current.isEmpty() && (!resume || current.getFirst().status() != IdentityTenantMembershipBO.Status.ACTIVE)) {
                    throw new IllegalStateException("XINGYUAN_STATE_CONFLICT");
                }
                if (current.isEmpty()) {
                    ownershipGuard.run();
                    memberships.upsert(new TenantMembershipService.UpsertMembershipCommand(
                            active.tenantId(), subject, membership.status(), membership.expectedVersion(), operator));
                }
                ownershipGuard.run();
                if (memberships.resolve(subject, active.tenantId()).effectiveStatus()
                        != TenantMembershipPort.MembershipStatus.ACTIVE) {
                    throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
                }
            }
            ownershipGuard.run();
            if (tenants.get(active.tenantId()).status() != IdentityTenantBO.Status.ACTIVE) {
                throw new IllegalStateException("XINGYUAN_COMMIT_UNKNOWN");
            }
            tenantIds.put(code, active.tenantId());
        }
        log.info("Isolated identity phase committed runId={} tenantCount={} userCount={}",
                manifest.getRunId(), tenantIds.size(), subjects.size());
        var facts = IdentityFactsBO.builder().tenantIds(Map.copyOf(tenantIds))
                .subjects(Map.of("admin", subjects.get("admin"), "viewer", subjects.get("viewer")))
                .provisioningSubject(subjects.get("provisioner")).build();
        requireValid(facts);
        IsolatedOAuthProvisioningIT.validateProvisioningIdentity(facts.getSubjects(), facts.getProvisioningSubject());
        return facts;
    }

    private void requireValid(Object input) {
        if (!validator.validate(input).isEmpty()) {
            throw new IllegalArgumentException("XINGYUAN_INPUT_INVALID");
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    @Builder
    public static class IdentityFactsBO {
        private Map<String, String> tenantIds;
        @JsonIgnore
        @ToString.Exclude
        private Map<String, String> subjects;
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._:-]{1,128}")
        @JsonIgnore @ToString.Exclude
        private String provisioningSubject;
    }
}
