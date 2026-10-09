package top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.client.RedisException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;

@RestController("oauthConsentController")
@RequiredArgsConstructor
@Slf4j
@Validated
public class OAuthConsentController {

    @NonNull
    @Qualifier("tenantIssuerService")
    private final TenantIssuerService issuers;
    @NonNull
    @Qualifier("tenantAuthorizationService")
    private final OAuth2AuthorizationService authorizations;
    @NonNull
    @Qualifier("tenantRegisteredClientRepository")
    private final RegisteredClientRepository clients;

    @GetMapping(value = "/tenants/{tenantId}/consent", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(operationId = "oauthSasConsentPage", summary = "Render the current native tenant consent transaction")
    @SecurityRequirement(name = "oauthAuthorizationSession")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Escaped consent form with session CSRF",
                    content = @Content(mediaType = "text/html", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Invalid bounded query"),
            @ApiResponse(responseCode = "401", description = "SAS session required"),
            @ApiResponse(responseCode = "404", description = "No matching current consent transaction"),
            @ApiResponse(responseCode = "503", description = "Authorization store unavailable")
    })
    public ResponseEntity<String> consentPage(
            @Parameter(description = "Canonical positive business tenant identifier")
            @Pattern(regexp = "[1-9][0-9]{0,18}") @PathVariable String tenantId,
            @NotBlank @Size(max = 128) @RequestParam("client_id") String clientId,
            @NotBlank @Size(max = 256) @Pattern(regexp = "[A-Za-z0-9_-]{32,256}={0,2}") @RequestParam String state,
            @Size(max = 8320) @RequestParam(required = false) String scope,
            HttpServletRequest request) {
        if (!boundedSingleQuery(request)) {
            return failure(400, "Request rejected");
        }
        try {
            TrustedRealmBO realm = issuers.resolveCurrentIssuer();
            if (realm.getRealmType() != RealmTypeEnum.TENANT
                    || !tenantId.equals(Long.toString(realm.getBusinessTenantId()))) {
                return failure(404, "No current authorization transaction");
            }
            OAuth2Authorization authorization = authorizations.findByToken(state, new OAuth2TokenType("state"));
            RegisteredClient client = clients.findByClientId(clientId);
            OAuth2AuthorizationRequest authorizationRequest = authorization == null ? null
                    : authorization.getAttribute(OAuth2AuthorizationRequest.class.getName());
            if (authorizationRequest == null || client == null
                    || !client.getId().equals(authorization.getRegisteredClientId())
                    || !clientId.equals(authorizationRequest.getClientId())
                    || !matchingRequestedScopes(scope, authorizationRequest.getScopes())) {
                return failure(404, "No current authorization transaction");
            }
            Object csrfAttribute = request.getAttribute(CsrfToken.class.getName());
            if (!(csrfAttribute instanceof CsrfToken csrf)) {
                return failure(400, "Request rejected");
            }
            String commonFields = hidden("client_id", clientId) + hidden("state", state)
                    + hidden(csrf.getParameterName(), csrf.getToken());
            String action = escape(realm.getIssuer().getRawPath() + "/oauth2/authorize");
            StringBuilder html = new StringBuilder("<!doctype html><html><head><meta charset=\"utf-8\">"
                    + "<meta name=\"referrer\" content=\"no-referrer\"><title>Authorize application</title></head><body>"
                    + "<main><h1>Authorize application</h1><p>");
            html.append(escape(client.getClientName())).append("</p><p>Tenant ").append(escape(tenantId))
                    .append("</p><p>Select only the permissions you allow.</p><form method=\"post\" action=\"")
                    .append(action).append("\">").append(commonFields).append("<fieldset><legend>Requested permissions</legend>");
            List<String> scopes = authorizationRequest.getScopes().stream().sorted().toList();
            for (int index = 0; index < scopes.size(); index++) {
                String requested = scopes.get(index);
                if ("openid".equals(requested)) {
                    html.append("<p>OpenID identity is included only when permissions are approved.</p>");
                    continue;
                }
                html.append("<label for=\"scope-").append(index).append("\"><input id=\"scope-")
                        .append(index).append("\" type=\"checkbox\" name=\"scope\" value=\"")
                        .append(escape(requested)).append("\">").append(escape(requested)).append("</label>");
            }
            html.append("</fieldset><button type=\"submit\">Allow selected permissions</button></form>")
                    .append("<form method=\"post\" action=\"").append(action).append("\">").append(commonFields)
                    .append(hidden("consent_action", "deny"))
                    .append("<button type=\"submit\">Cancel authorization</button></form></main></body></html>");
            return response(200, html.toString());
        } catch (OAuth2AuthenticationException rejected) {
            return failure("temporarily_unavailable".equals(rejected.getError().getErrorCode()) ? 503 : 404,
                    "Request could not be completed");
        } catch (DataAccessException | RedisException unavailable) {
            return failure(503, "Request temporarily unavailable");
        }
    }

    @ExceptionHandler({ConstraintViolationException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<String> invalidQuery() {
        return failure(400, "Request rejected");
    }

    private static boolean boundedSingleQuery(HttpServletRequest request) {
        if (request.getQueryString() != null && request.getQueryString().length() > 16 * 1024
                || !Set.of("client_id", "state", "scope").containsAll(request.getParameterMap().keySet())) {
            return false;
        }
        return request.getParameterMap().values().stream().allMatch(values -> values != null && values.length == 1);
    }

    private static boolean matchingRequestedScopes(String supplied, Set<String> requested) {
        if (supplied == null) {
            return true;
        }
        List<String> values = Arrays.asList(supplied.split(" ", -1));
        return values.size() <= 64 && values.stream().noneMatch(String::isBlank)
                && Set.copyOf(values).size() == values.size() && Set.copyOf(values).equals(requested);
    }

    private static String hidden(String name, String value) {
        return "<input type=\"hidden\" name=\"" + escape(name) + "\" value=\"" + escape(value) + "\">";
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static ResponseEntity<String> failure(int status, String message) {
        return response(status, "<!doctype html><html><body><main><p>" + message + "</p></main></body></html>");
    }

    private static ResponseEntity<String> response(int status, String html) {
        return ResponseEntity.status(status).contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noStore()).header("Referrer-Policy", "no-referrer")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'")
                .body(html);
    }
}
