package top.egon.cola.platform.idp.admin.oauth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import top.egon.cola.platform.idp.core.oauth.AuthorizationFacade;
import top.egon.cola.platform.idp.core.oauth.AuthorizationRequest;
import top.egon.cola.platform.idp.core.oauth.OAuthException;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthErrorVO;
import top.egon.cola.platform.idp.admin.support.security.IdpSsoPrincipal;

import java.net.URI;
import java.security.Principal;
import java.util.Objects;

@RestController
public class OAuthAuthorizationController {

    private final AuthorizationFacade authorizationFacade;

    public OAuthAuthorizationController(
            AuthorizationFacade authorizationFacade
    ) {
        this.authorizationFacade = Objects.requireNonNull(
                authorizationFacade,
                "authorizationFacade"
        );
    }

    @GetMapping("/oauth2/authorize")
    public ResponseEntity<Void> authorize(
            @RequestParam("response_type") String responseType,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("audience") String audience,
            @RequestParam("tenant_id") String tenantId,
            @RequestParam("state") String state,
            @RequestParam("nonce") String nonce,
            @RequestParam("code_challenge") String codeChallenge,
            @RequestParam("code_challenge_method")
            String codeChallengeMethod,
            Principal principal
    ) {
        if (!(principal instanceof Authentication authentication)
                || !(authentication.getPrincipal()
                instanceof IdpSsoPrincipal ssoPrincipal)) {
            throw new OAuthException(
                    "login_required",
                    "authenticated identity is required"
            );
        }
        AuthorizationFacade.AuthorizationResult result =
                authorizationFacade.authorize(
                        new AuthorizationRequest(
                                responseType,
                                clientId,
                                redirectUri,
                                audience,
                                tenantId,
                                state,
                                nonce,
                                codeChallenge,
                                codeChallengeMethod
                        ),
                        ssoPrincipal.identitySub(),
                        ssoPrincipal.sessionId()
                );
        URI location = UriComponentsBuilder
                .fromUriString(result.redirectUri())
                .queryParam("code", result.code())
                .queryParam("state", result.state())
                .build()
                .encode()
                .toUri();
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(location)
                .build();
    }

    @ExceptionHandler(OAuthException.class)
    public ResponseEntity<OAuthErrorVO> oauthError(
            OAuthException exception
    ) {
        HttpStatus status = "login_required".equals(exception.oauthError())
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new OAuthErrorVO(
                exception.oauthError(),
                exception.getMessage()
        ));
    }
}
