package top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;

@RestController("tenantLoginController")
@RequiredArgsConstructor
@Slf4j
public class TenantLoginController {

    @NonNull
    @Qualifier("tenantIssuerService")
    private final TenantIssuerService tenantIssuerService;
    @NonNull
    @Qualifier("oauthAuthorizationRequestCache")
    private final RequestCache requestCache;

    @GetMapping(value = "/tenants/{tenantId:[1-9][0-9]{0,18}}/login", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(operationId = "oauthSasLoginPage", summary = "Render the exact tenant issuer login form")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login form",
                    content = @Content(mediaType = "text/html", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "404", description = "No current authorization transaction"),
            @ApiResponse(responseCode = "503", description = "Issuer registry unavailable")
    })
    public ResponseEntity<String> loginPage(
            @Parameter(description = "Positive business tenant identifier") @PathVariable String tenantId,
            HttpServletRequest request) {
        TrustedRealmBO trustedRealm = tenantIssuerService.resolveCurrentIssuer();
        if (trustedRealm.getRealmType() != RealmTypeEnum.TENANT
                || !tenantId.equals(Long.toString(trustedRealm.getBusinessTenantId()))) {
            return ResponseEntity.notFound().cacheControl(CacheControl.noStore()).build();
        }
        SavedRequest saved = requestCache.getRequest(request, null);
        if (!isAuthorizeRequest(saved, trustedRealm)) {
            return ResponseEntity.notFound().cacheControl(CacheControl.noStore()).build();
        }
        Object csrfValue = request.getAttribute(CsrfToken.class.getName());
        if (!(csrfValue instanceof CsrfToken csrfToken)) {
            return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).build();
        }
        String issuerPath = trustedRealm.getIssuer().getRawPath();
        String html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>Sign in</title>"
                + "<meta name=\"referrer\" content=\"no-referrer\"></head><body>"
                + "<main><h1>Sign in</h1><form method=\"post\" action=\""
                + issuerPath + "/login\">"
                + "<label>Username<input name=\"username\" autocomplete=\"username\" required maxlength=\"128\"></label>"
                + "<label>Password<input type=\"password\" name=\"password\" autocomplete=\"current-password\" required maxlength=\"1024\"></label>"
                + "<input type=\"hidden\" name=\"_csrf\" value=\"" + escapeHtml(csrfToken.getToken()) + "\">"
                + "<button type=\"submit\">Sign in</button></form></main></body></html>";
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer")
                .header("X-Content-Type-Options", "nosniff")
                .body(html);
    }

    private static boolean isAuthorizeRequest(SavedRequest saved, TrustedRealmBO trustedRealm) {
        if (!(saved instanceof DefaultSavedRequest defaultSaved)
                || !"GET".equals(defaultSaved.getMethod())) {
            return false;
        }
        try {
            URI uri = URI.create(defaultSaved.getRedirectUrl());
            return uri.isAbsolute() && uri.getUserInfo() == null && uri.getFragment() == null
                    && Objects.equals(uri.getRawPath(), trustedRealm.getIssuer().getRawPath() + "/oauth2/authorize");
        } catch (IllegalArgumentException failure) {
            return false;
        }
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;")
                .replace("<", "&lt;").replace(">", "&gt;");
    }
}
