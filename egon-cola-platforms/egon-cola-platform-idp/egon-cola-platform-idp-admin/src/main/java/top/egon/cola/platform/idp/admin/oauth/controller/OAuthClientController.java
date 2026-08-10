package top.egon.cola.platform.idp.admin.oauth.controller;

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
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.OAuthValueDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.dto.UpdateOAuthClientDTO;
import top.egon.cola.platform.idp.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.idp.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/identity/clients")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity",
        entityDomainName = "统一身份实体域",
        code = "oauth-clients",
        name = "OAuth客户端接口组")
@EgonHttpService(
        serviceName = "idp-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/v1/identity")
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
    @GatewayOperation(name = "idp-oauth-client-list-v1",
            summary = "查询OAuth客户端", externalAccessible = true,
            tags = {"idp", "oauth-client"})
    public List<OAuthClientVO> list(
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:read");
        return clients.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @GatewayOperation(name = "idp-oauth-client-create-v1",
            summary = "创建OAuth客户端", externalAccessible = true,
            tags = {"idp", "oauth-client"})
    public OAuthClientVO create(
            @Valid @RequestBody CreateOAuthClientDTO request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:create");
        return clients.create(request);
    }

    @PatchMapping("/{clientId}")
    @GatewayOperation(name = "idp-oauth-client-update-v1",
            summary = "更新OAuth客户端", externalAccessible = true,
            tags = {"idp", "oauth-client"})
    public OAuthClientVO update(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody UpdateOAuthClientDTO request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:update");
        return clients.update(clientId, request);
    }

    @PutMapping("/{clientId}/redirect-uris")
    @GatewayOperation(name = "idp-oauth-client-redirect-put-v1",
            summary = "登记OAuth回调地址", externalAccessible = true,
            tags = {"idp", "oauth-client"})
    public OAuthClientVO putRedirect(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody OAuthValueDTO request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:update");
        return clients.putRedirectUri(clientId, request.value());
    }

    @DeleteMapping("/{clientId}/redirect-uris")
    @GatewayOperation(name = "idp-oauth-client-redirect-delete-v1",
            summary = "删除OAuth回调地址", externalAccessible = true,
            tags = {"idp", "oauth-client"})
    public OAuthClientVO deleteRedirect(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody OAuthValueDTO request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:update");
        return clients.deleteRedirectUri(clientId, request.value());
    }

    @PutMapping("/{clientId}/audiences")
    @GatewayOperation(name = "idp-oauth-client-audience-put-v1",
            summary = "登记OAuth受众", externalAccessible = true,
            tags = {"idp", "oauth-client"})
    public OAuthClientVO putAudience(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody OAuthValueDTO request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:update");
        return clients.putAudience(clientId, request.value());
    }

    @DeleteMapping("/{clientId}/audiences")
    @GatewayOperation(name = "idp-oauth-client-audience-delete-v1",
            summary = "删除OAuth受众", externalAccessible = true,
            tags = {"idp", "oauth-client"})
    public OAuthClientVO deleteAudience(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody OAuthValueDTO request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:update");
        return clients.deleteAudience(clientId, request.value());
    }
}
