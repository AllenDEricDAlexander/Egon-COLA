package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthSecuritySnapshotBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.command.ConsumeOAuthTokenCommand;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientCredentialsAccessPolicy;

@Validated
public interface OAuthTokenIssueService {

    Authentication consumeAndIssue(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid ConsumeOAuthTokenCommand command,
            @NotNull Supplier<Authentication> delegate);

    ClientCredentialsIssueContext authorizeClientCredentials(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long clientPk,
            @NotNull URI resource,
            @NotNull Set<String> scopes);

    UserAuthorizationIssueContext authorizeUserToken(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthAuthorizationBO authorization);

    void recordClientCredentialsIssued(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long clientPk,
            @NotNull URI resource);

    void recordAuthorizationCodeCreated(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthAuthorizationBO authorization);

    record ClientCredentialsIssueContext(
            long authorizationId,
            OAuthSecuritySnapshotBO securitySnapshot,
            ClientCredentialsAccessPolicy.ServiceResourceAccess resourceAccess,
            String appId,
            String credentialId) {

        public ClientCredentialsIssueContext {
            if (appId == null || appId.isBlank() || !appId.equals(appId.trim())
                    || credentialId == null || credentialId.isBlank()
                    || !credentialId.equals(credentialId.trim())) {
                throw new IllegalArgumentException("SERVICE client identity claims are required");
            }
        }

        public ClientCredentialsIssueContext(
                long authorizationId,
                OAuthSecuritySnapshotBO securitySnapshot,
                ClientCredentialsAccessPolicy.ServiceResourceAccess resourceAccess) {
            this(authorizationId, securitySnapshot, resourceAccess,
                    resourceAccess.sourceAppCode(), resourceAccess.sourceClientId());
        }
    }

    record UserAuthorizationIssueContext(
            long authorizationId,
            OAuthSecuritySnapshotBO securitySnapshot,
            String subject,
            Set<URI> audiences) {
    }
}
