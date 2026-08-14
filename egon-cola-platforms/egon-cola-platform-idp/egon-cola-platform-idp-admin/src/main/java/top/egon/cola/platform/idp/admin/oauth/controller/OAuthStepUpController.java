package top.egon.cola.platform.idp.admin.oauth.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.OAuthStepUpDTO;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.core.identity.AuthenticatedIdentity;
import top.egon.cola.platform.idp.core.identity.IdentityFacade;
import top.egon.cola.platform.idp.core.token.TokenFacade;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

/**
 * Re-authenticates the current subject and replaces only its short-lived USER AT.
 */
@RestController
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "oauth-protocol",
        entityDomainName = "OAuth 协议域",
        code = "idp-oauth-step-up",
        name = "IdP OAuth 二次认证接口组")
public class OAuthStepUpController {

    private final IdentityFacade identities;
    private final TokenFacade tokens;
    private final Clock clock;
    private final boolean secureCookie;

    public OAuthStepUpController(
            IdentityFacade identities,
            TokenFacade tokens,
            @Qualifier("idpClock") Clock clock,
            @Value("${egon.idp.oauth.refresh-cookie-secure:true}")
            boolean secureCookie) {
        this.identities = Objects.requireNonNull(identities, "identities");
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.secureCookie = secureCookie;
    }

    @PostMapping("/oauth2/step-up")
    @GatewayOperation(name = "idp-oauth-step-up-v1",
            summary = "重新校验密码并签发强化认证 USER Access Token",
            externalAccessible = true,
            tags = {"idp", "oauth"})
    public ResponseEntity<Void> stepUp(
            @AuthenticationPrincipal IdentityPrincipal principal,
            @RequestBody OAuthStepUpDTO request) {
        IdentityPrincipal current = Objects.requireNonNull(principal, "principal");
        char[] password = required(request == null ? null : request.password())
                .toCharArray();
        AuthenticatedIdentity authenticated;
        try {
            authenticated = identities.authenticateCurrent(
                    current.subject(), password, clock.instant());
        } finally {
            Arrays.fill(password, '\0');
        }
        if (!current.subject().equals(authenticated.identitySub())) {
            throw new IllegalStateException("step-up subject mismatch");
        }
        TokenFacade.AccessTokenIssue issue = tokens.issueStepUp(
                current.subject(),
                current.tenantId(),
                top.egon.cola.platform.idp.contract.AuthenticationContext.of(
                        "STRONG", clock.instant()));
        Duration maxAge = Duration.between(clock.instant(), issue.expiresAt());
        ResponseCookie cookie = ResponseCookie.from(
                        OAuthLoginController.accessCookieName(secureCookie),
                        issue.accessToken())
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("password is required");
        }
        return value;
    }
}
