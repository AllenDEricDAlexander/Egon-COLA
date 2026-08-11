package top.egon.cola.platform.idp.admin.oauth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthErrorVO;
import top.egon.cola.platform.idp.admin.support.security.IdpSsoPrincipal;
import top.egon.cola.platform.idp.core.oauth.AuthorizationFacade;
import top.egon.cola.platform.idp.core.oauth.AuthorizationRequest;
import top.egon.cola.platform.idp.core.oauth.OAuthException;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Objects;

/**
 * OAuth Authorization Endpoint 的浏览器协议控制器。
 *
 * <p>Browser protocol controller for the OAuth Authorization Endpoint.</p>
 */
@RestController
public class OAuthAuthorizationController {

    /** OAuth 授权用例门面；OAuth authorization use-case facade. */
    private final AuthorizationFacade authorizationFacade;

    /**
     * 创建 OAuth Authorization Endpoint 控制器。
     *
     * <p>Creates the OAuth Authorization Endpoint controller.</p>
     *
     * @param authorizationFacade OAuth 授权用例门面；OAuth authorization use-case facade
     */
    public OAuthAuthorizationController(
            AuthorizationFacade authorizationFacade
    ) {
        this.authorizationFacade = Objects.requireNonNull(
                authorizationFacade,
                "authorizationFacade"
        );
    }

    /**
     * 校验浏览器请求并重定向回带一次性授权码。
     *
     * <p>Validates a browser request and redirects back with a one-time authorization code.</p>
     *
     * <p>{@code resource} 必须且只能出现一次；旧 {@code audience} 参数无条件拒绝。</p>
     *
     * <p>{@code resource} must occur exactly once; the legacy {@code audience} parameter is
     * rejected unconditionally.</p>
     *
     * @param parameters 原始查询参数；raw query parameters
     * @param principal 当前 SSO 身份；current SSO identity
     * @return 302 回调响应；302 callback response
     */
    @GetMapping("/oauth2/authorize")
    public ResponseEntity<Void> authorize(
            @RequestParam MultiValueMap<String, String> parameters,
            Principal principal
    ) {
        if (parameters.containsKey("audience")) {
            throw oauth("invalid_request");
        }
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
                                single(parameters, "response_type"),
                                single(parameters, "client_id"),
                                single(parameters, "redirect_uri"),
                                single(parameters, "resource"),
                                single(parameters, "tenant_id"),
                                single(parameters, "state"),
                                single(parameters, "nonce"),
                                single(parameters, "code_challenge"),
                                single(parameters, "code_challenge_method")
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

    /**
     * 将 OAuth 协议异常映射为安全错误响应。
     *
     * <p>Maps an OAuth protocol exception to a safe error response.</p>
     *
     * @param exception OAuth 协议异常；OAuth protocol exception
     * @return OAuth 错误响应；OAuth error response
     */
    @ExceptionHandler(OAuthException.class)
    public ResponseEntity<OAuthErrorVO> oauthError(OAuthException exception) {
        HttpStatus status = "login_required".equals(exception.oauthError())
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new OAuthErrorVO(
                exception.oauthError(),
                exception.getMessage()
        ));
    }

    /**
     * 读取且只接受一个非空、无首尾空白的参数值。
     *
     * <p>Reads and accepts exactly one non-blank value without surrounding whitespace.</p>
     *
     * @param parameters 原始参数；raw parameters
     * @param name 参数名；parameter name
     * @return 唯一参数值；single parameter value
     */
    private static String single(
            MultiValueMap<String, String> parameters,
            String name
    ) {
        List<String> values = parameters.get(name);
        if (values == null || values.size() != 1) {
            throw oauth("invalid_request");
        }
        String value = values.getFirst();
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw oauth("invalid_request");
        }
        return value;
    }

    /**
     * 创建不暴露请求细节的 OAuth 异常。
     *
     * <p>Creates an OAuth exception without exposing request details.</p>
     *
     * @param error OAuth 错误码；OAuth error code
     * @return OAuth 异常；OAuth exception
     */
    private static OAuthException oauth(String error) {
        return new OAuthException(error, "OAuth request is invalid");
    }
}
