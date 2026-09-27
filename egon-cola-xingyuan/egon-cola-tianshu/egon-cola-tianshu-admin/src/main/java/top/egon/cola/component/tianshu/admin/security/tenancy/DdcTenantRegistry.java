package top.egon.cola.component.tianshu.admin.security.tenancy;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.tianshu.admin.config.DdcAdminProperties;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component("ddcTenantRegistry")
@RequiredArgsConstructor
public class DdcTenantRegistry {

    @Qualifier("egon.cola.component.tianshu.admin.tenancy-top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantAdminProperties")
    private final DdcTenantAdminProperties tenantProperties;

    @Qualifier("egon.cola.component.tianshu.admin-top.egon.cola.component.tianshu.admin.config.DdcAdminProperties")
    private final DdcAdminProperties adminProperties;

    private Map<String, TenantBinding> bindingsByIdentity = Map.of();
    private Map<Long, TenantBinding> bindingsById = Map.of();
    private Map<String, CredentialBinding> credentialBindings = Map.of();

    @PostConstruct
    public void initialize() {
        List<DdcTenantBindingDTO> configuredBindings = requiredList(
                tenantProperties.getBindings(), "bindings");
        List<DdcCredentialTenantDTO> configuredCredentials = requiredList(
                tenantProperties.getCredentialBindings(), "credentialBindings");
        Set<String> credentialIds = configuredCredentialIds();
        LinkedHashMap<String, TenantBinding> identityMap = new LinkedHashMap<>();
        LinkedHashMap<Long, TenantBinding> idMap = new LinkedHashMap<>();
        for (DdcTenantBindingDTO value : configuredBindings) {
            if (value == null || value.getTenantId() == null || value.getTenantId() <= 0
                    || value.getIdentityTenantId() == null
                    || value.getIdentityTenantId().isBlank()
                    || "*".equals(value.getIdentityTenantId().trim())) {
                throw new IllegalStateException("Invalid Tianshu tenant binding");
            }
            TenantBinding binding = new TenantBinding(
                    value.getTenantId(), value.getIdentityTenantId().trim(), value.isEnabled());
            if (identityMap.putIfAbsent(binding.identityTenantId(), binding) != null
                    || idMap.putIfAbsent(binding.tenantId(), binding) != null) {
                throw new IllegalStateException("Duplicate Tianshu tenant binding");
            }
        }
        LinkedHashMap<String, CredentialBinding> credentialMap = new LinkedHashMap<>();
        for (DdcCredentialTenantDTO value : configuredCredentials) {
            if (value == null || value.getCredentialId() == null
                    || value.getCredentialId().isBlank() || value.getTenantId() == null
                    || value.getTenantId() <= 0) {
                throw new IllegalStateException("Invalid Tianshu credential tenant binding");
            }
            String credentialId = value.getCredentialId().trim();
            TenantBinding tenant = idMap.get(value.getTenantId());
            if (!credentialIds.contains(credentialId) || tenant == null || !tenant.enabled()) {
                throw new IllegalStateException("Credential tenant binding references an unknown or disabled identity");
            }
            LinkedHashSet<String> namespacePatterns = new LinkedHashSet<>();
            List<String> configuredPatterns = value.getNamespacePatterns();
            if (configuredPatterns != null) {
                for (String pattern : configuredPatterns) {
                    if (pattern == null || pattern.isBlank()) {
                        throw new IllegalStateException("Namespace patterns must not contain blank values");
                    }
                    namespacePatterns.add(pattern.trim());
                }
            }
            if (credentialMap.putIfAbsent(
                    credentialId,
                    new CredentialBinding(tenant.tenantId(), Set.copyOf(namespacePatterns))) != null) {
                throw new IllegalStateException("Duplicate Tianshu credential tenant binding");
            }
        }
        bindingsByIdentity = Map.copyOf(identityMap);
        bindingsById = Map.copyOf(idMap);
        credentialBindings = Map.copyOf(credentialMap);
        log.info("Initialized Tianshu tenant bindings: tenants={}, credentials={}",
                bindingsById.size(), credentialBindings.size());
    }

    public DdcTenantBindingDTO requireByIdentityTenantId(String identityTenantId) {
        TenantBinding binding = identityTenantId == null
                ? null
                : bindingsByIdentity.get(identityTenantId);
        if (binding == null || !binding.enabled()) {
            throw new IllegalArgumentException("TENANT_IDENTITY_UNREGISTERED");
        }
        return bindingDTO(binding);
    }

    public DdcTenantBindingDTO requireEnabledTenant(Long tenantId) {
        TenantBinding binding = tenantId == null ? null : bindingsById.get(tenantId);
        if (binding == null || !binding.enabled()) {
            throw new IllegalArgumentException("TENANT_ID_UNREGISTERED");
        }
        return bindingDTO(binding);
    }

    public DdcCredentialTenantDTO requireCredentialBinding(String credentialId) {
        CredentialBinding credential = credentialId == null
                ? null
                : credentialBindings.get(credentialId);
        if (credential == null) {
            throw new IllegalArgumentException("TENANT_CREDENTIAL_UNREGISTERED");
        }
        TenantBinding tenant = bindingsById.get(credential.tenantId());
        if (tenant == null || !tenant.enabled()) {
            throw new IllegalArgumentException("TENANT_ID_UNREGISTERED");
        }
        return DdcCredentialTenantDTO.builder()
                .credentialId(credentialId)
                .tenantId(credential.tenantId())
                .namespacePatterns(List.copyOf(credential.namespacePatterns()))
                .build();
    }

    public DdcTenantContextDTO resolveUserContext(String identityTenantId, String subject) {
        DdcTenantBindingDTO binding = requireByIdentityTenantId(identityTenantId);
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("TENANT_ACTOR_REQUIRED");
        }
        return new DdcTenantContextDTO(
                binding.getTenantId(), binding.getIdentityTenantId(), "user:" + subject.trim());
    }

    public DdcTenantContextDTO resolveCredentialContext(
            String credentialId,
            String signedTenantId) {
        DdcCredentialTenantDTO credential = requireCredentialBinding(credentialId);
        if (signedTenantId == null || signedTenantId.isBlank()) {
            throw new IllegalArgumentException("TENANT_METADATA_REQUIRED");
        }
        long parsedTenantId;
        try {
            parsedTenantId = Long.parseLong(signedTenantId);
        } catch (NumberFormatException invalidTenantId) {
            throw new IllegalArgumentException("TENANT_METADATA_INVALID", invalidTenantId);
        }
        if (parsedTenantId <= 0 || !Long.toString(parsedTenantId).equals(signedTenantId)
                || !credential.getTenantId().equals(parsedTenantId)) {
            throw new IllegalArgumentException("TENANT_CREDENTIAL_MISMATCH");
        }
        DdcTenantBindingDTO tenant = requireEnabledTenant(parsedTenantId);
        return new DdcTenantContextDTO(
                tenant.getTenantId(), tenant.getIdentityTenantId(), "service:" + credentialId);
    }

    public Set<String> namespacePatternsForCredential(String credentialId) {
        DdcCredentialTenantDTO binding = requireCredentialBinding(credentialId);
        return Set.copyOf(binding.getNamespacePatterns());
    }

    private Set<String> configuredCredentialIds() {
        List<DdcAdminProperties.Credential> credentials = adminProperties.getRpc().getCredentials();
        if (credentials == null) {
            return Set.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (DdcAdminProperties.Credential credential : credentials) {
            if (credential != null && credential.getCredentialId() != null
                    && !credential.getCredentialId().isBlank()) {
                String credentialId = credential.getCredentialId().trim();
                if (!result.add(credentialId)) {
                    throw new IllegalStateException("Duplicate Tianshu RPC credentialId");
                }
            }
        }
        return Set.copyOf(result);
    }

    private static <T> List<T> requiredList(
            List<T> values,
            String name) {
        if (values == null) {
            throw new IllegalStateException("Tianshu tenant " + name + " must not be null");
        }
        return List.copyOf(values);
    }

    private static DdcTenantBindingDTO bindingDTO(TenantBinding binding) {
        return DdcTenantBindingDTO.builder()
                .tenantId(binding.tenantId())
                .identityTenantId(binding.identityTenantId())
                .enabled(binding.enabled())
                .build();
    }

    private record TenantBinding(long tenantId, String identityTenantId, boolean enabled) {
    }

    private record CredentialBinding(long tenantId, Set<String> namespacePatterns) {
        private CredentialBinding {
            namespacePatterns = Set.copyOf(namespacePatterns);
        }
    }
}
