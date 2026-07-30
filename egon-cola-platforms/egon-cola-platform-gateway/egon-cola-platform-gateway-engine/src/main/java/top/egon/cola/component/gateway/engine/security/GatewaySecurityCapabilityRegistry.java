package top.egon.cola.component.gateway.engine.security;

import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.gateway.core.security.GatewayAuthorizationProvider;
import top.egon.cola.component.gateway.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.gateway.core.security.GatewayIdentityMapper;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class GatewaySecurityCapabilityRegistry {

    private final Map<String, GatewayCredentialExtractor> extractors;

    private final Map<String, GatewayAuthenticationProvider> authentications;

    private final Map<String, GatewayAuthorizationProvider> authorizations;

    private final Map<String, GatewayIdentityMapper> identityMappers;

    public GatewaySecurityCapabilityRegistry(
            Collection<GatewayCredentialExtractor> extractors,
            Collection<GatewayAuthenticationProvider> authentications,
            Collection<GatewayAuthorizationProvider> authorizations,
            Collection<GatewayIdentityMapper> identityMappers) {
        this.extractors = index(
                extractors,
                GatewayCredentialExtractor::extractorId,
                "credential extractor"
        );
        this.authentications = index(
                authentications,
                GatewayAuthenticationProvider::providerId,
                "authentication provider"
        );
        this.authorizations = index(
                authorizations,
                GatewayAuthorizationProvider::providerId,
                "authorization provider"
        );
        this.identityMappers = index(
                identityMappers,
                GatewayIdentityMapper::mapperId,
                "identity mapper"
        );
    }

    public static GatewaySecurityCapabilityRegistry empty() {
        return new GatewaySecurityCapabilityRegistry(
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of()
        );
    }

    public void validate(
            GatewaySecurityPolicy policy,
            Set<GatewayProtocol> protocols) {
        Set<String> credentialTypes = policy.credentialExtractorIds().stream()
                .map(id -> extractor(id).credentialType())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        for (String id : policy.authenticationProviderIds()) {
            GatewayAuthenticationProvider provider = authentication(id);
            if (provider.supportedCredentialTypes().stream()
                    .noneMatch(credentialTypes::contains)) {
                throw new IllegalArgumentException(
                        "authentication provider "
                                + id
                                + " does not support extracted credentials"
                );
            }
        }
        policy.authorizationProviderIds().forEach(this::authorization);
        if (policy.identityMapperId() != null) {
            GatewayIdentityMapper mapper = identityMapper(
                    policy.identityMapperId()
            );
            if (!mapper.supportedProtocols().containsAll(protocols)) {
                throw new IllegalArgumentException(
                        "identity mapper "
                                + mapper.mapperId()
                                + " does not support "
                                + protocols
                );
            }
        }
    }

    public GatewayCredentialExtractor extractor(String id) {
        return required(extractors, id, "credential extractor");
    }

    public GatewayAuthenticationProvider authentication(String id) {
        return required(authentications, id, "authentication provider");
    }

    public GatewayAuthorizationProvider authorization(String id) {
        return required(authorizations, id, "authorization provider");
    }

    public GatewayIdentityMapper identityMapper(String id) {
        return required(identityMappers, id, "identity mapper");
    }

    private <T> Map<String, T> index(
            Collection<T> values,
            Function<T, String> identifier,
            String type) {
        Map<String, T> result = new LinkedHashMap<>();
        for (T value : values) {
            String id = identifier.apply(value);
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException(type + " id is required");
            }
            if (result.putIfAbsent(id.trim(), value) != null) {
                throw new IllegalArgumentException(
                        "duplicate " + type + " id " + id
                );
            }
        }
        return Map.copyOf(result);
    }

    private <T> T required(Map<String, T> values, String id, String type) {
        T value = values.get(id);
        if (value == null) {
            throw new IllegalArgumentException(
                    "missing " + type + " " + id
            );
        }
        return value;
    }
}
