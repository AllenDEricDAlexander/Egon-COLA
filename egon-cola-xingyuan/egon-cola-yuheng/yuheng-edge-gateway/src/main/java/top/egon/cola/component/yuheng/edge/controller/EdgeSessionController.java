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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.component.yuheng.edge.service.domain.enums.EdgeSessionBindingStateEnum;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.edge.controller.domain.EdgeCsrfResult;
import top.egon.cola.component.yuheng.edge.controller.domain.EdgeLogoutFailureResult;
import top.egon.cola.component.yuheng.edge.controller.domain.EdgeSessionResult;
import top.egon.cola.component.yuheng.edge.config.EdgeOAuthProperties;
import top.egon.cola.component.yuheng.edge.service.EdgeLogoutService;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeAuthorizedClientRepository;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO;

import java.time.Clock;
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
    @Qualifier("reactiveClientRegistrationRepository")
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
            @ApiResponse(responseCode = "200", description = "Current verified identity", content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = EdgeSessionResult.class))),
            @ApiResponse(responseCode = "400", description = "Unexpected query or request body"),
            @ApiResponse(responseCode = "401", description = "Session or access token is invalid"),
            @ApiResponse(responseCode = "503", description = "Shared security state is unavailable")
    })
    public Mono<ResponseEntity<?>> session(Authentication authentication, ServerWebExchange exchange) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthAuthentication)
                || !authentication.isAuthenticated()) {
            return Mono.just(unauthorized());
        }
        if (!exchange.getRequest().getQueryParams().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().cacheControl(NO_STORE).body(Map.of("error", "invalid_request")));
        }
        return exchange.getRequest().getBody().doOnNext(DataBufferUtils::release).hasElements().flatMap(body -> {
            if (body) { return Mono.just(ResponseEntity.badRequest().cacheControl(NO_STORE).body(Map.of("error", "invalid_request"))); }
            var bindings = authorizedClients.bindingComponent();
            return bindings.load(exchange).flatMap(binding -> {
                if (binding.getState() != EdgeSessionBindingStateEnum.ACTIVE
                        || !binding.getSubject().equals(authentication.getName())) { return Mono.just(unauthorized()); }
                AccessTokenVerification<IdentityPrincipal> noValidToken = new AccessTokenVerification.Invalid<>("JWT_INVALID");
                return authorizedClients.loadAll(authentication, exchange).flatMap(clients -> reactor.core.publisher.Flux.fromIterable(clients)
                        .concatMap(client -> bindings.verifyClient(client, authentication, java.util.UUID.randomUUID().toString().replace("-", "")))
                        .takeUntil(result -> result instanceof AccessTokenVerification.Unavailable<?>)
                        .reduce(noValidToken,
                                (best, current) -> current instanceof AccessTokenVerification.Unavailable<?>
                                        || !(best instanceof AccessTokenVerification.Valid<?>) ? current : best)
                        .flatMap(result -> {
                            if (result instanceof AccessTokenVerification.Unavailable<?>) {
                                return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).cacheControl(NO_STORE).body(Map.of("error", "temporarily_unavailable")));
                            }
                            if (result instanceof AccessTokenVerification.Valid<?> valid
                                    && valid.proof() != null && binding.getIssuer().equals(valid.proof().issuer())
                                    && binding.getTenantId().equals(valid.principal().tenantId()) && binding.getSubject().equals(valid.principal().subject())) {
                                return bindings.load(exchange).map(current -> current.getGeneration() == binding.getGeneration()
                                        && current.getBindingId().equals(binding.getBindingId()) && current.getState() == EdgeSessionBindingStateEnum.ACTIVE
                                        ? ResponseEntity.ok().cacheControl(NO_STORE).body(new EdgeSessionResult(true, binding.getTenantId(), binding.getSubject(), displayName(oauthAuthentication, binding.getSubject())))
                                        : unauthorized());
                            }
                            return Mono.just(unauthorized());
                        }));
            });
        }).onErrorResume(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class, failure -> {
            if ("temporarily_unavailable".equals(failure.getError().getErrorCode())) {
                return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).cacheControl(NO_STORE).body(Map.of("error", "temporarily_unavailable")));
            }
            if ("invalid_token".equals(failure.getError().getErrorCode())) {
                exchange.getResponse().addCookie(org.springframework.http.ResponseCookie.from("__Host-egon_bff", "").path("/")
                        .secure(true).httpOnly(true).sameSite("Lax").maxAge(0).build());
                return exchange.getSession().flatMap(session -> session.invalidate()).thenReturn(unauthorized());
            }
            return Mono.just(unauthorized());
        });
    }

    @GetMapping("/csrf")
    @Operation(operationId = "oauthBffCsrf", summary = "Read the current CSRF token")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "CSRF header and token", content = @io.swagger.v3.oas.annotations.media.Content(
            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = EdgeCsrfResult.class))),
            @ApiResponse(responseCode = "400", description = "Unexpected query or request body"),
            @ApiResponse(responseCode = "503", description = "CSRF state is unavailable")})
    public Mono<ResponseEntity<?>> csrf(ServerWebExchange exchange) {
        if (!exchange.getRequest().getQueryParams().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().cacheControl(NO_STORE).body(Map.of("error", "invalid_request")));
        }
        return exchange.getRequest().getBody().doOnNext(DataBufferUtils::release)
                .hasElements().flatMap(hasBody -> {
                    if (hasBody) {
                        return Mono.just(ResponseEntity.badRequest().cacheControl(NO_STORE).body(Map.of("error", "invalid_request")));
                    }
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
                });
    }

    @PostMapping("/logout")
    @Operation(operationId = "oauthBffLogout", summary = "End the local BFF session and revoke its refresh tokens")
    @SecurityRequirements({
            @SecurityRequirement(name = "bffSessionCookie"),
            @SecurityRequirement(name = "bffCsrfHeader")
    })
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Local session ended and all remote revocations confirmed"),
            @ApiResponse(responseCode = "400", description = "Unexpected query or request body"),
            @ApiResponse(responseCode = "401", description = "Authenticated BFF session is required"),
            @ApiResponse(responseCode = "403", description = "CSRF verification failed"),
            @ApiResponse(responseCode = "503", description = "Local logout completed; remote revocation is pending or unknown", content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = EdgeLogoutFailureResult.class)))
    })
    public Mono<ResponseEntity<?>> logout(Authentication authentication, ServerWebExchange exchange) {
        if (!(authentication instanceof OAuth2AuthenticationToken) || !authentication.isAuthenticated()) {
            return Mono.just(unauthorized());
        }
        if (!exchange.getRequest().getQueryParams().isEmpty()) { return Mono.just(ResponseEntity.badRequest().cacheControl(NO_STORE).body(Map.of("error", "invalid_request"))); }
        return exchange.getRequest().getBody().doOnNext(DataBufferUtils::release).hasElements().flatMap(body -> {
            if (body) { return Mono.just(ResponseEntity.badRequest().cacheControl(NO_STORE).body(Map.of("error", "invalid_request"))); }
            return logoutService.logout(authentication, exchange).map(remoteState -> switch (remoteState) {
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
        });
    }

    private static ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(org.springframework.http.HttpHeaders.WWW_AUTHENTICATE, "Bearer")
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

}
