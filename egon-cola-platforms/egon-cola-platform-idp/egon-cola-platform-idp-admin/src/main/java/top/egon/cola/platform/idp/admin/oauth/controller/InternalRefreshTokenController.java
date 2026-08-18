package top.egon.cola.platform.idp.admin.oauth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.core.token.RefreshTokenStatus;
import top.egon.cola.platform.idp.core.token.TokenException;
import top.egon.cola.platform.idp.core.token.TokenFacade;
import top.egon.cola.platform.idp.starter.security.RequiresServiceScope;

import java.util.List;
import java.util.Objects;

/**
 * Internal SERVICE-only endpoint for checking USER refresh-token online state.
 */
@RestController
@RequestMapping("/internal/v1/oauth2/refresh-token")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "oauth-protocol",
        entityDomainName = "OAuth 协议域",
        code = "internal-refresh-token",
        name = "内部 Refresh Token 状态接口组")
@EgonHttpService(
        serviceName = "idp-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/internal/v1")
public class InternalRefreshTokenController {

    private static final int MAX_REFRESH_TOKEN_LENGTH = 4096;

    private final TokenFacade tokens;

    public InternalRefreshTokenController(TokenFacade tokens) {
        this.tokens = Objects.requireNonNull(tokens, "tokens");
    }

    @PostMapping(value = "/validate",
            consumes = "application/x-www-form-urlencoded")
    @RequiresServiceScope("idp:refresh-token:validate")
    @GatewayOperation(
            name = "idp-internal-refresh-token-validate-v1",
            summary = "验证 USER Refresh Token 在线状态",
            externalAccessible = false,
            tags = {"idp", "internal", "oauth"})
    public ResponseEntity<ResultRecord<RefreshTokenStatus>> validate(
            @RequestParam MultiValueMap<String, String> form) {
        String rawToken = token(form);
        RefreshTokenStatus status = tokens.validateRefresh(rawToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(ResultRecord.success(status));
    }

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<ResultRecord<Void>> invalidRefreshToken(
            TokenException ignored) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(ResultRecord.failure(ResultCode.UNAUTHORIZED));
    }

    private String token(MultiValueMap<String, String> form) {
        if (form == null || form.size() != 1 || !form.containsKey("token")) {
            throw invalidGrant();
        }
        List<String> values = form.get("token");
        if (values == null || values.size() != 1) {
            throw invalidGrant();
        }
        String value = values.getFirst();
        if (value == null || value.isBlank() || !value.equals(value.trim())
                || value.length() > MAX_REFRESH_TOKEN_LENGTH) {
            throw invalidGrant();
        }
        return value;
    }

    private TokenException invalidGrant() {
        return new TokenException("invalid_grant");
    }
}
