package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repo.IdentityDirectoryContext;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;

@Service("tenantIssuerService")
@Slf4j
@Validated
public class TenantIssuerServiceImpl implements TenantIssuerService {

    private static final Pattern TENANT_PATH = Pattern.compile("/tenants/([1-9][0-9]{0,18})");

    private final URI oauthPublicOrigin;
    private final String publicOriginRoot;
    private final OAuthRealmRepository realmRepository;
    private final IdentityDirectoryContext identityDirectoryContext;

    public TenantIssuerServiceImpl(
            @Qualifier("oauthPublicOrigin") URI oauthPublicOrigin,
            @Qualifier("oAuthRealmRepository") OAuthRealmRepository realmRepository,
            @Qualifier("identityDirectoryContext") IdentityDirectoryContext identityDirectoryContext) {
        this.oauthPublicOrigin = validatePublicOrigin(oauthPublicOrigin);
        String origin = this.oauthPublicOrigin.toASCIIString();
        this.publicOriginRoot = origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
        this.realmRepository = Objects.requireNonNull(realmRepository, "realmRepository");
        this.identityDirectoryContext = Objects.requireNonNull(identityDirectoryContext, "identityDirectoryContext");
    }

    @Override
    public TrustedRealmBO resolveExactIssuer(URI issuer) {
        URI requested = Objects.requireNonNull(issuer, "issuer");
        String path = exactIssuerPath(requested);
        IssuerCandidate candidate = candidate(path);
        Optional<OAuthRealmBO> realm = realmRepository.findByIssuer(candidate.trustedRealm());
        OAuthRealmBO registered = realm.orElseThrow(TenantIssuerServiceImpl::unknownIssuer);
        if (!"ACTIVE".equals(registered.getStatus())
                || registered.getIssuer() == null
                || !candidate.issuer().toASCIIString().equals(registered.getIssuer().toASCIIString())
                || !candidate.trustedRealm().getRealmKey().equals(registered.getRealmKey())
                || candidate.trustedRealm().getRealmType() != registered.getRealmType()
                || !Objects.equals(candidate.trustedRealm().getBusinessTenantId(), registered.getBusinessTenantId())
                || !Objects.equals(candidate.trustedRealm().getStoragePartitionId(), registered.getStoragePartitionId())
                || registered.getTrustVersion() == null || registered.getTrustVersion() < 0L) {
            throw unknownIssuer();
        }

        TrustedRealmBO trustedRealm = TrustedRealmBO.builder()
                .issuer(registered.getIssuer())
                .realmKey(registered.getRealmKey())
                .realmType(registered.getRealmType())
                .storagePartitionId(registered.getStoragePartitionId())
                .businessTenantId(registered.getBusinessTenantId())
                .trustVersion(registered.getTrustVersion())
                .build();
        trustedRealm.validateTrustedPartition();
        return trustedRealm;
    }

    private String exactIssuerPath(URI issuer) {
        if (!issuer.isAbsolute() || issuer.getHost() == null || issuer.getUserInfo() != null
                || issuer.getQuery() != null || issuer.getFragment() != null
                || !"https".equalsIgnoreCase(issuer.getScheme())
                || !issuer.getHost().equalsIgnoreCase(oauthPublicOrigin.getHost())
                || effectivePort(issuer) != effectivePort(oauthPublicOrigin)) {
            throw unknownIssuer();
        }
        String path = issuer.getRawPath();
        if ("/platform".equals(path)) {
            return path;
        }
        Matcher matcher = path == null ? null : TENANT_PATH.matcher(path);
        if (matcher == null || !matcher.matches()) {
            throw unknownIssuer();
        }
        return path;
    }

    private IssuerCandidate candidate(String path) {
        URI issuer = URI.create(publicOriginRoot + path);
        if ("/platform".equals(path)) {
            TrustedRealmBO trustedRealm = TrustedRealmBO.builder()
                    .issuer(issuer)
                    .realmKey("platform")
                    .realmType(RealmTypeEnum.PLATFORM)
                    .storagePartitionId(identityDirectoryContext.directoryPartitionId())
                    .businessTenantId(null)
                    .trustVersion(0L)
                    .build();
            trustedRealm.validateTrustedPartition();
            return new IssuerCandidate(issuer, trustedRealm);
        }
        Matcher matcher = TENANT_PATH.matcher(path);
        if (!matcher.matches()) {
            throw unknownIssuer();
        }
        long tenantId;
        try {
            tenantId = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException failure) {
            throw unknownIssuer();
        }
        TrustedRealmBO trustedRealm = TrustedRealmBO.builder()
                .issuer(issuer)
                .realmKey("tenant:" + tenantId)
                .realmType(RealmTypeEnum.TENANT)
                .storagePartitionId(tenantId)
                .businessTenantId(tenantId)
                .trustVersion(0L)
                .build();
        trustedRealm.validateTrustedPartition();
        return new IssuerCandidate(issuer, trustedRealm);
    }

    private static URI validatePublicOrigin(URI origin) {
        Objects.requireNonNull(origin, "oauthPublicOrigin");
        String path = origin.getRawPath();
        if (!origin.isAbsolute() || !"https".equalsIgnoreCase(origin.getScheme())
                || origin.getHost() == null || origin.getUserInfo() != null
                || origin.getQuery() != null || origin.getFragment() != null
                || (path != null && !path.isEmpty() && !"/".equals(path))) {
            throw new IllegalArgumentException("OAUTH_PUBLIC_ORIGIN_INVALID");
        }
        return origin;
    }

    private static int effectivePort(URI uri) {
        return uri.getPort() < 0 ? 443 : uri.getPort();
    }

    private static OAuth2AuthenticationException unknownIssuer() {
        return new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "Request rejected", null));
    }

    private record IssuerCandidate(URI issuer, TrustedRealmBO trustedRealm) {
    }
}
