package top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.tianquan.shoubing.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/identity/clients")
@Tag(name = "oauth-clients", description = "OAuth客户端接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity",
        entityDomainName = "统一身份实体域",
        interfaceGroupCode = "idp-identity"
)

public class OAuthClientController {

    private final OAuthClientService clients;
    private final IdpAdminAuthorizationPort authorization;

    public OAuthClientController(
            OAuthClientService clients,
            IdpAdminAuthorizationPort authorization
    ) {
        this.clients = Objects.requireNonNull(clients, "clients");
        this.authorization = Objects.requireNonNull(
                authorization,
                "authorization"
        );
    }

    @GetMapping
    @Operation(
            operationId = "idp-oauth-client-list-v1",
            summary = "查询OAuth客户端",
            tags = {"idp", "oauth-client"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public List<OAuthClientVO> list(
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:read");
        return clients.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            operationId = "idp-oauth-client-create-v1",
            summary = "创建OAuth客户端",
            tags = {"idp", "oauth-client"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public CreatedOAuthClientVO create(
            @Valid @RequestBody CreateOAuthClientDTO request,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal,
            HttpServletResponse response
    ) {
        authorization.require(principal, "idp:oauth-client:create");
        response.setHeader("Cache-Control", "no-store");
        return clients.create(request, operator(principal));
    }

    @PostMapping("/{clientId}/secret-rotations")
    @Operation(
            operationId = "idp-oauth-client-secret-rotate-v1",
            summary = "轮换OAuth客户端Secret",
            tags = {"idp", "oauth-client", "secret"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public RotatedClientSecretVO rotateSecret(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody RotateClientSecretDTO request,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal,
            HttpServletResponse response
    ) {
        authorization.require(principal, "idp:oauth-client:update");
        response.setHeader("Cache-Control", "no-store");
        return clients.rotateSecret(clientId, request, operator(principal));
    }

    @PatchMapping("/{clientId}")
    @Operation(
            operationId = "idp-oauth-client-update-v1",
            summary = "更新OAuth客户端",
            tags = {"idp", "oauth-client"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public OAuthClientVO update(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody UpdateOAuthClientDTO request,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:update");
        return clients.update(clientId, request);
    }

    @PutMapping("/{clientId}/redirect-uris")
    @Operation(
            operationId = "idp-oauth-client-redirect-put-v1",
            summary = "登记OAuth回调地址",
            tags = {"idp", "oauth-client"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public OAuthClientVO putRedirect(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody OAuthValueDTO request,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:update");
        return clients.putRedirectUri(clientId, request.value());
    }

    @DeleteMapping("/{clientId}/redirect-uris")
    @Operation(
            operationId = "idp-oauth-client-redirect-delete-v1",
            summary = "删除OAuth回调地址",
            tags = {"idp", "oauth-client"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public OAuthClientVO deleteRedirect(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody OAuthValueDTO request,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:update");
        return clients.deleteRedirectUri(clientId, request.value());
    }

    @PutMapping("/{clientId}/resource-uris")
    @Operation(
            operationId = "idp-oauth-client-resource-put-v1",
            summary = "登记OAuth Resource URI",
            tags = {"idp", "oauth-client"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public OAuthClientVO putResourceUri(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody OAuthValueDTO request,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:update");
        return clients.putResourceUri(clientId, request.value());
    }

    @DeleteMapping("/{clientId}/resource-uris")
    @Operation(
            operationId = "idp-oauth-client-resource-delete-v1",
            summary = "删除OAuth Resource URI",
            tags = {"idp", "oauth-client"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public OAuthClientVO deleteResourceUri(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody OAuthValueDTO request,
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:update");
        return clients.deleteResourceUri(clientId, request.value());
    }

    private static String operator(IdentityPrincipal principal) {
        return principal == null ? "SYSTEM" : principal.subject();
    }
}
