package top.egon.cola.component.yuheng.edge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClientException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.yuheng.edge.controller.domain.EdgeCsrfResult;
import top.egon.cola.component.yuheng.edge.controller.domain.EdgeLogoutFailureResult;
import top.egon.cola.component.yuheng.edge.controller.domain.EdgeSessionResult;
import top.egon.cola.component.yuheng.edge.config.EdgeOAuthProperties;
import top.egon.cola.component.yuheng.edge.service.EdgeLogoutService;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeAuthorizedClientRepository;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry;
import top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO;
import top.egon.cola.platform.tianquan.shoubing.contract.PrincipalType;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

@RestController("edgeSessionController")
@RequestMapping("/bff")
@RequiredArgsConstructor
@Slf4j
public class EdgeSessionController {
    public static final String SESSION_CREATED_AT_ATTRIBUTE =
            EdgeSessionController.class.getName() + ".CREATED_AT";
    public static final String SESSION_REGISTRATION_ATTRIBUTE =
            EdgeSessionController.class.getName() + ".REGISTRATION_ID";
    private static final CacheControl NO_STORE = CacheControl.noStore();

    @Qualifier("edgeAuthorizedClientRepository")
    private final EdgeAuthorizedClientRepository authorizedClients;
    @Qualifier("edgeLogoutService")
    private final EdgeLogoutService logoutService;
    @Qualifier("clientRegistrationRepository")
    private final ReactiveClientRegistrationRepository registrations;
    @Qualifier("edgeOAuthProperties")
    private final EdgeOAuthProperties edgeProperties;
    @Qualifier("edgeIdpStarterProperties")
    private final IdpStarterProperties idpProperties;
    @Qualifier("edgeAccessTokenVerifier")
    private final Function<String, VerifiedJwtBO> accessTokenVerifier;
    @Qualifier("edgeRevocationCheck")
    private final Function<VerifiedJwtBO, Mono<OAuthRevocationReader.Result>> revocationCheck;
    @Qualifier("edgeClock")
    private final Clock clock;

    @GetMapping("/session")
    @Operation(operationId = "oauthBffSession", summary = "Read the current BFF session")
    @SecurityRequirement(name = "bffSessionCookie")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current verified identity"),
            @ApiResponse(responseCode = "401", description = "Session or access token is invalid"),
            @ApiResponse(responseCode = "503", description = "Shared security state is unavailable")
    })
    public Mono<ResponseEntity<?>> session(Authentication authentication, ServerWebExchange exchange) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthAuthentication)
                || !authentication.isAuthenticated()) {
            return Mono.just(unauthorized());
        }
        String registrationId = oauthAuthentication.getAuthorizedClientRegistrationId();
        return exchange.getSession().flatMap(session -> {
            Instant createdAt = session.getAttribute(SESSION_CREATED_AT_ATTRIBUTE);
            if (createdAt == null
                    || !clock.instant().isBefore(createdAt.plus(edgeProperties.getSession().getAbsoluteTimeout()))) {
                return Mono.just(unauthorized());
            }
            return registrations.findByRegistrationId(registrationId)
                    .flatMap(registration -> authorizedClients
                            .loadAuthorizedClient(registrationId, authentication, exchange)
                            .flatMap(client -> verifySession(oauthAuthentication, registration, client)))
                    .defaultIfEmpty(SessionCheck.invalid())
                    .map(this::toResponse);
        });
    }

    @GetMapping("/csrf")
    @Operation(operationId = "oauthBffCsrf", summary = "Read the current CSRF token")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "CSRF header and token")})
    public Mono<ResponseEntity<?>> csrf(ServerWebExchange exchange) {
        Object value = exchange.getAttribute(CsrfToken.class.getName());
        if (value instanceof Mono<?> tokenMono) {
            @SuppressWarnings("unchecked")
            Mono<CsrfToken> csrf = (Mono<CsrfToken>) tokenMono;
            return csrf.map(token -> ResponseEntity.ok()
                    .cacheControl(NO_STORE)
                    .body(new EdgeCsrfResult(token.getHeaderName(), token.getToken())));
        }
        if (value instanceof CsrfToken token) {
            return Mono.just(ResponseEntity.ok()
                    .cacheControl(NO_STORE)
                    .body(new EdgeCsrfResult(token.getHeaderName(), token.getToken())));
        }
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .cacheControl(NO_STORE)
                .body(Map.of("error", "temporarily_unavailable")));
    }

    @PostMapping("/logout")
    @Operation(operationId = "oauthBffLogout", summary = "End the local BFF session and revoke its refresh tokens")
    @SecurityRequirements({
            @SecurityRequirement(name = "bffSessionCookie"),
            @SecurityRequirement(name = "bffCsrfHeader")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Local session ended and all remote revocations confirmed"),
            @ApiResponse(responseCode = "503", description = "Local logout completed; remote revocation is pending or unknown")
    })
    public Mono<ResponseEntity<?>> logout(Authentication authentication, ServerWebExchange exchange) {
        if (!(authentication instanceof OAuth2AuthenticationToken) || !authentication.isAuthenticated()) {
            return Mono.just(unauthorized());
        }
        return logoutService.logout(authentication, exchange)
                .map(remoteState -> switch (remoteState) {
                    case CONFIRMED -> ResponseEntity.noContent().cacheControl(NO_STORE).build();
                    case PENDING -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .cacheControl(NO_STORE)
                            .body(new EdgeLogoutFailureResult(
                                    "REMOTE_REVOCATION_PENDING",
                                    "本地已退出，远端撤销待恢复",
                                    true,
                                    "PENDING"));
                    case UNKNOWN -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .cacheControl(NO_STORE)
                            .body(new EdgeLogoutFailureResult(
                                    "REMOTE_REVOCATION_PENDING",
                                    "本地已退出，远端撤销状态未知",
                                    true,
                                    "UNKNOWN"));
                });
    }

    private Mono<SessionCheck> verifySession(
            OAuth2AuthenticationToken authentication,
            ClientRegistration registration,
            OAuth2AuthorizedClient client) {
        if (client.getAccessToken() == null
                || !registrationIssuer(registration).equals(clientIssuer(client))
                || client.getAccessToken().getExpiresAt() == null
                || !client.getAccessToken().getExpiresAt().isAfter(clock.instant())) {
            return Mono.just(SessionCheck.invalid());
        }
        return Mono.fromCallable(() -> accessTokenVerifier.apply(client.getAccessToken().getTokenValue()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(verified -> checkVerifiedSession(
                        authentication,
                        registration,
                        client.getAccessToken().getTokenValue(),
                        verified))
                .onErrorResume(RestClientException.class, exception -> {
                    log.warn("BFF session token verification dependency is unavailable; failureType={}",
                            exception.getClass().getSimpleName());
                    return Mono.just(SessionCheck.unavailable());
                })
                .onErrorResume(TenantIssuerAuthenticationManagerComponent.InvalidIssuerException.class,
                        exception -> invalidSession(exception))
                .onErrorResume(JwtException.class, exception -> invalidSession(exception))
                .onErrorResume(IllegalArgumentException.class, exception -> invalidSession(exception));
    }

    private Mono<SessionCheck> invalidSession(RuntimeException exception) {
        log.debug("BFF session token is invalid; failureType={}", exception.getClass().getSimpleName());
        return Mono.just(SessionCheck.invalid());
    }

    private Mono<SessionCheck> checkVerifiedSession(
            OAuth2AuthenticationToken authentication,
            ClientRegistration registration,
            String accessTokenValue,
            VerifiedJwtBO verified) {
        TrustedIssuerRegistry.TrustedIssuerVO trusted = verified.getTrustedIssuer();
        String tenantId = tenantId(trusted.issuer().toString());
        boolean exactIdentity = verified.getPrincipalType() == PrincipalType.USER
                && tenantId != null
                && tenantId.equals(verified.getTenantId())
                && registrationIssuer(registration).equals(trusted.issuer().toString())
                && registrationIssuer(registration).equals(canonicalIssuer(
                        verified.getJwt().getIssuer() == null ? null : verified.getJwt().getIssuer().toString()))
                && accessTokenValue.equals(verified.getJwt().getTokenValue())
                && authentication.getName().equals(verified.getSubject())
                && verified.getSubject().equals(verified.getJwt().getSubject())
                && idpProperties.getResourceUri() != null
                && verified.getJwt().getAudience().contains(idpProperties.getResourceUri().toASCIIString());
        if (!exactIdentity) {
            return Mono.just(SessionCheck.invalid());
        }
        return revocationCheck.apply(verified).map(result -> switch (result.status()) {
            case VALID -> SessionCheck.valid(verified, tenantId, displayName(authentication, verified.getSubject()));
            case REVOKED -> SessionCheck.invalid();
            case UNAVAILABLE -> SessionCheck.unavailable();
        });
    }

    private ResponseEntity<?> toResponse(SessionCheck check) {
        if (check.state() == CheckState.UNAVAILABLE) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .cacheControl(NO_STORE)
                    .body(Map.of("error", "temporarily_unavailable"));
        }
        if (check.state() != CheckState.VALID) {
            return unauthorized();
        }
        return ResponseEntity.ok()
                .cacheControl(NO_STORE)
                .body(new EdgeSessionResult(true, check.tenantId(), check.subject(), check.displayName()));
    }

    private static ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .cacheControl(NO_STORE)
                .body(Map.of("error", "invalid_token"));
    }

    private static String displayName(OAuth2AuthenticationToken authentication, String subject) {
        Object name = authentication.getPrincipal().getAttributes().get("name");
        if (!(name instanceof String value) || value.isBlank()) {
            return subject;
        }
        String bounded = value.replace("\r", "").replace("\n", "").trim();
        return bounded.length() <= 256 ? bounded : bounded.substring(0, 256);
    }

    private static String clientIssuer(OAuth2AuthorizedClient client) {
        return registrationIssuer(client.getClientRegistration());
    }

    private static String registrationIssuer(ClientRegistration registration) {
        String issuer = registration.getProviderDetails().getIssuerUri();
        return issuer == null ? "" : canonicalIssuer(issuer);
    }

    private static String canonicalIssuer(String issuer) {
        if (issuer == null || issuer.isBlank()) {
            return "";
        }
        return URI.create(issuer).normalize().toString().replaceAll("/$", "");
    }

    private static String tenantId(String issuer) {
        String path = URI.create(issuer).getPath();
        if (path != null && path.matches("/tenants/[1-9][0-9]*")) {
            return path.substring("/tenants/".length());
        }
        return null;
    }

    private enum CheckState {
        VALID,
        INVALID,
        UNAVAILABLE
    }

    private record SessionCheck(CheckState state, String tenantId, String subject, String displayName) {
        private static SessionCheck valid(VerifiedJwtBO ignored, String tenantId, String displayName) {
            return new SessionCheck(CheckState.VALID, tenantId, ignored.getSubject(), displayName);
        }

        private static SessionCheck invalid() {
            return new SessionCheck(CheckState.INVALID, null, null, null);
        }

        private static SessionCheck unavailable() {
            return new SessionCheck(CheckState.UNAVAILABLE, null, null, null);
        }
    }
}
