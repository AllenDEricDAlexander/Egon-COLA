package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter;

import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthConsentBO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SasConsentConverter extends BaseConverter<OAuthConsentBO, OAuth2AuthorizationConsent> {

    @Override
    default OAuth2AuthorizationConsent toTarget(OAuthConsentBO source) {
        OAuthConsentBO consent = Objects.requireNonNull(source, "consent");
        if (consent.getClientPk() == null || consent.getClientPk() <= 0L
                || consent.getSubject() == null || consent.getSubject().isBlank()
                || consent.getAuthorities() == null) {
            throw new IllegalArgumentException("OAUTH_CONSENT_INVALID");
        }
        OAuth2AuthorizationConsent.Builder builder = OAuth2AuthorizationConsent.withId(
                Long.toString(consent.getClientPk()), consent.getSubject());
        consent.getAuthorities().stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(builder::authority);
        return builder.build();
    }

    @Override
    default OAuthConsentBO toSource(OAuth2AuthorizationConsent source) {
        OAuth2AuthorizationConsent consent = Objects.requireNonNull(source, "consent");
        return OAuthConsentBO.builder()
                .clientPk(parseClientPk(consent.getRegisteredClientId()))
                .subject(consent.getPrincipalName())
                .authorities(consent.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .sorted()
                        .toList())
                .build();
    }

    private static long parseClientPk(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0L) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("OAUTH_CONSENT_CLIENT_ID_INVALID", failure);
        }
    }
}
