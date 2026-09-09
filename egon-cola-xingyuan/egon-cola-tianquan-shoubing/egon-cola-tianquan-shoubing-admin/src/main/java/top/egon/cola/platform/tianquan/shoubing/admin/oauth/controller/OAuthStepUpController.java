package top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.OAuthStepUpDTO;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.core.identity.AuthenticatedIdentity;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityFacade;
import top.egon.cola.platform.tianquan.shoubing.core.token.TokenFacade;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

/**
 * Re-authenticates the current subject and replaces only its short-lived USER AT.
 */
@RestController
@Tag(name = "tianquan-shoubing-oauth-step-up", description = "Tianquan-Shoubing OAuth 二次认证接口组")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "oauth-protocol",
        entityDomainName = "OAuth 协议域",
        interfaceGroupCode = "tianquan-shoubing-oauth"
)

public class OAuthStepUpController {

    private final IdentityFacade identities;
    private final TokenFacade tokens;
    private final Clock clock;
    private final boolean secureCookie;

    public OAuthStepUpController(
            IdentityFacade identities,
            TokenFacade tokens,
            @Qualifier("idpClock") Clock clock,
            @Value("${egon.tianquan-shoubing.oauth.refresh-cookie-secure:true}")
            boolean secureCookie) {
        this.identities = Objects.requireNonNull(identities, "identities");
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.secureCookie = secureCookie;
    }

    @PostMapping("/oauth2/step-up")
    @Operation(
            operationId = "tianquan-shoubing-oauth-step-up-v1",
            summary = "重新校验密码并签发强化认证 USER Access Token",
            tags = {"tianquan-shoubing", "oauth"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResponseEntity<Void> stepUp(
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal,
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
                top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext.of(
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
