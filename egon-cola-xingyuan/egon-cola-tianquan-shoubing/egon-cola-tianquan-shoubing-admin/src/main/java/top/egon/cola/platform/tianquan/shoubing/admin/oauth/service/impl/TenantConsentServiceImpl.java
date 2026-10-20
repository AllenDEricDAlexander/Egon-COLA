package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthConsentBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.SasConsentConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthConsentRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;

@Service("tenantConsentService")
@Slf4j
@RequiredArgsConstructor
@Validated
public class TenantConsentServiceImpl implements OAuth2AuthorizationConsentService {

    @Qualifier("tenantIssuerService")
    private final TenantIssuerService tenantIssuerService;
    @Qualifier("oAuthConsentRepository")
    private final OAuthConsentRepository consentRepository;
    @Qualifier("oAuthClientRepository")
    private final OAuthClientRepository clientRepository;
    @Qualifier("sasConsentConverterImpl")
    private final SasConsentConverter converter;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;

    @Override
    @Transactional
    public void save(OAuth2AuthorizationConsent source) {
        OAuth2AuthorizationConsent consent = Objects.requireNonNull(source, "consent");
        TrustedRealmBO trustedRealm = tenantIssuerService.resolveCurrentIssuer();
        requirePrincipal(consent.getPrincipalName());
        long clientPk = parsePositiveId(consent.getRegisteredClientId(), "OAUTH_CONSENT_CLIENT_ID_INVALID");
        OAuthClientBO client = loadActiveClient(trustedRealm, clientPk);
        List<String> authorities = consent.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();
        requireAllowedScopeAuthorities(authorities, client);

        OAuthConsentBO mapped = converter.toSource(consent);
        mapped.setAuthorities(authorities);
        OAuthConsentBO current = consentRepository.findConsentForUpdate(
                trustedRealm, clientPk, consent.getPrincipalName()).orElse(null);
        mapped.setGrantedAt(clock.instant().truncatedTo(ChronoUnit.MICROS));
        long expectedVersion = 0L;
        if (current == null) {
            mapped.setId(null);
            mapped.setVersion(0L);
        } else {
            expectedVersion = current.getVersion();
            mapped.setId(current.getId());
            mapped.setVersion(Math.addExact(current.getVersion(), 1L));
        }
        mapped.setStatus("ACTIVE");
        consentRepository.save(trustedRealm, mapped, expectedVersion);
    }

    @Override
    @Transactional
    public void remove(OAuth2AuthorizationConsent source) {
        OAuth2AuthorizationConsent consent = Objects.requireNonNull(source, "consent");
        TrustedRealmBO trustedRealm = tenantIssuerService.resolveCurrentIssuer();
        requirePrincipal(consent.getPrincipalName());
        long clientPk = parsePositiveId(consent.getRegisteredClientId(), "OAUTH_CONSENT_CLIENT_ID_INVALID");
        OAuthConsentBO current = consentRepository.findConsentForUpdate(
                trustedRealm, clientPk, consent.getPrincipalName()).orElse(null);
        if (current == null || "REVOKED".equals(current.getStatus())) {
            return;
        }
        current.setStatus("REVOKED");
        current.setVersion(Math.addExact(current.getVersion(), 1L));
        consentRepository.save(trustedRealm, current, current.getVersion() - 1L);
    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2AuthorizationConsent findById(
            String registeredClientId,
            String principalName) {
        requirePrincipal(principalName);
        long clientPk = parsePositiveId(registeredClientId, "OAUTH_CONSENT_CLIENT_ID_INVALID");
        TrustedRealmBO trustedRealm = tenantIssuerService.resolveCurrentIssuer();
        OAuthConsentBO consent = consentRepository.findConsent(trustedRealm, clientPk, principalName)
                .filter(value -> "ACTIVE".equals(value.getStatus()))
                .orElse(null);
        if (consent == null) {
            return null;
        }
        OAuthClientBO client = loadActiveClient(trustedRealm, clientPk);
        requireAllowedScopeAuthorities(consent.getAuthorities(), client);
        return converter.toTarget(consent);
    }

    private OAuthClientBO loadActiveClient(TrustedRealmBO trustedRealm, long clientPk) {
        return clientRepository.findById(trustedRealm, clientPk)
                .filter(value -> value.getStatus() == OAuthClient.Status.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("OAUTH_CONSENT_CLIENT_NOT_FOUND"));
    }

    private static void requireAllowedScopeAuthorities(List<String> authorities, OAuthClientBO client) {
        if (authorities == null || client.getAllowedScopes() == null) {
            throw new IllegalArgumentException("OAUTH_CONSENT_AUTHORITIES_INVALID");
        }
        for (String authority : authorities) {
            if (authority == null || !authority.startsWith("SCOPE_")) {
                throw new IllegalArgumentException("OAUTH_CONSENT_NON_SCOPE_AUTHORITY");
            }
            String scope = authority.substring("SCOPE_".length());
            if (scope.isBlank() || !client.getAllowedScopes().contains(scope)) {
                throw new IllegalArgumentException("OAUTH_CONSENT_SCOPE_NOT_ALLOWED");
            }
        }
    }

    private static void requirePrincipal(String principalName) {
        if (principalName == null || principalName.isBlank() || principalName.length() > 128) {
            throw new IllegalArgumentException("OAUTH_CONSENT_PRINCIPAL_INVALID");
        }
    }

    private static long parsePositiveId(String value, String errorCode) {
        if (value == null || !value.matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException(errorCode);
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(errorCode, failure);
        }
    }
}
