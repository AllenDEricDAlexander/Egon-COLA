package top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.OAuthValueDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.RotateClientSecretDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.UpdateOAuthClientDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.CreatedOAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.RotatedClientSecretVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.tianquan.shoubing.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;

import java.util.List;

@RestController("oauthClientController")
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/tianquan-shoubing/clients")
@Tag(name = "oauth-clients", description = "OAuth客户端接口组")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity",
        entityDomainName = "统一身份实体域",
        interfaceGroupCode = "tianquan-shoubing-identity"
)

public class OAuthClientController {

    @NonNull
    @Qualifier("OAuthClientServiceImpl")
    private final OAuthClientService clients;
    @NonNull
    @Qualifier("idpAdminAuthorizationPort")
    private final IdpAdminAuthorizationPort authorization;
    @NonNull
    @Qualifier("oauthManagementRealmResolver")
    private final OAuthManagementRealmResolver realms;

    @GetMapping
    @Operation(
            operationId = "tianquan-shoubing-oauth-client-list-v1",
            summary = "查询OAuth客户端",
            tags = {"tianquan-shoubing", "oauth-client"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public List<OAuthClientVO> list(
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:oauth-client:read");
        return clients.list(realms.resolve(principal, realm));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            operationId = "tianquan-shoubing-oauth-client-create-v1",
            summary = "创建OAuth客户端",
            tags = {"tianquan-shoubing", "oauth-client"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public CreatedOAuthClientVO create(
            @Valid @RequestBody CreateOAuthClientDTO request,
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal,
            HttpServletResponse response
    ) {
        authorization.require(principal, "tianquan-shoubing:oauth-client:create");
        TrustedRealmBO trustedRealm = realms.resolve(principal, realm);
        response.setHeader("Cache-Control", "no-store");
        return clients.create(trustedRealm, realms.platformRealm(), request, operator(principal));
    }

    @PostMapping("/{clientId}/secret-rotations")
    @Operation(
            operationId = "tianquan-shoubing-oauth-client-secret-rotate-v1",
            summary = "轮换OAuth客户端Secret",
            tags = {"tianquan-shoubing", "oauth-client", "secret"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public RotatedClientSecretVO rotateSecret(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody RotateClientSecretDTO request,
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal,
            HttpServletResponse response
    ) {
        authorization.require(principal, "tianquan-shoubing:oauth-client:update");
        TrustedRealmBO trustedRealm = realms.resolve(principal, realm);
        response.setHeader("Cache-Control", "no-store");
        return clients.rotateSecret(trustedRealm, clientId, request, operator(principal));
    }

    @PatchMapping("/{clientId}")
    @Operation(
            operationId = "tianquan-shoubing-oauth-client-update-v1",
            summary = "更新OAuth客户端",
            tags = {"tianquan-shoubing", "oauth-client"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public OAuthClientVO update(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody UpdateOAuthClientDTO request,
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:oauth-client:update");
        return clients.update(realms.resolve(principal, realm), clientId, request);
    }

    @PutMapping("/{clientId}/redirect-uris")
    @Operation(
            operationId = "tianquan-shoubing-oauth-client-redirect-put-v1",
            summary = "登记OAuth回调地址",
            tags = {"tianquan-shoubing", "oauth-client"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public OAuthClientVO putRedirect(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody OAuthValueDTO request,
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:oauth-client:update");
        return clients.putRedirectUri(realms.resolve(principal, realm), clientId, request.getValue());
    }

    @DeleteMapping("/{clientId}/redirect-uris")
    @Operation(
            operationId = "tianquan-shoubing-oauth-client-redirect-delete-v1",
            summary = "删除OAuth回调地址",
            tags = {"tianquan-shoubing", "oauth-client"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public OAuthClientVO deleteRedirect(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody OAuthValueDTO request,
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:oauth-client:update");
        return clients.deleteRedirectUri(realms.resolve(principal, realm), clientId, request.getValue());
    }

    @PutMapping("/{clientId}/resource-uris")
    @Operation(
            operationId = "tianquan-shoubing-oauth-client-resource-put-v1",
            summary = "登记OAuth Resource URI",
            tags = {"tianquan-shoubing", "oauth-client"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public OAuthClientVO putResourceUri(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody OAuthValueDTO request,
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:oauth-client:update");
        TrustedRealmBO trustedRealm = realms.resolve(principal, realm);
        return clients.putResourceUri(trustedRealm, realms.platformRealm(), clientId, request.getValue());
    }

    @DeleteMapping("/{clientId}/resource-uris")
    @Operation(
            operationId = "tianquan-shoubing-oauth-client-resource-delete-v1",
            summary = "删除OAuth Resource URI",
            tags = {"tianquan-shoubing", "oauth-client"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public OAuthClientVO deleteResourceUri(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody OAuthValueDTO request,
            @RequestParam(value = "realm", required = false) String realm,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "tianquan-shoubing:oauth-client:update");
        TrustedRealmBO trustedRealm = realms.resolve(principal, realm);
        return clients.deleteResourceUri(trustedRealm, realms.platformRealm(), clientId, request.getValue());
    }

    private static String operator(IdentityPrincipal principal) {
        return principal == null ? "SYSTEM" : principal.subject();
    }
}
