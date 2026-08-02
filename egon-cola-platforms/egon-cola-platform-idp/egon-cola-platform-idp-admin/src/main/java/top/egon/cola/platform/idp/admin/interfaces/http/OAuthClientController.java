package top.egon.cola.platform.idp.admin.interfaces.http;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
import top.egon.cola.platform.idp.admin.oauth.application.OAuthClientAdminService;
import top.egon.cola.platform.idp.admin.oauth.domain.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.security.IdpAdminAuthorizationPort;
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

    private final OAuthClientAdminService clients;
    private final IdpAdminAuthorizationPort authorization;

    public OAuthClientController(
            OAuthClientAdminService clients,
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
    public List<OAuthClientAdminService.ClientView> list(
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
    public OAuthClientAdminService.ClientView create(
            @Valid @RequestBody CreateClientRequest request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:create");
        return clients.create(new OAuthClientAdminService.CreateClientCommand(
                request.clientId(), request.clientName(),
                request.accessTokenTtlSeconds(),
                request.refreshTokenTtlSeconds(),
                request.redirectUris(), request.audiences()
        ));
    }

    @PatchMapping("/{clientId}")
    @GatewayOperation(name = "idp-oauth-client-update-v1",
            summary = "更新OAuth客户端", externalAccessible = true,
            tags = {"idp", "oauth-client"})
    public OAuthClientAdminService.ClientView update(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody UpdateClientRequest request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:update");
        return clients.update(clientId,
                new OAuthClientAdminService.UpdateClientCommand(
                        request.clientName(), request.status(),
                        request.accessTokenTtlSeconds(),
                        request.refreshTokenTtlSeconds(),
                        request.expectedVersion()
                ));
    }

    @PutMapping("/{clientId}/redirect-uris")
    @GatewayOperation(name = "idp-oauth-client-redirect-put-v1",
            summary = "登记OAuth回调地址", externalAccessible = true,
            tags = {"idp", "oauth-client"})
    public OAuthClientAdminService.ClientView putRedirect(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody ValueRequest request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:update");
        return clients.putRedirectUri(clientId, request.value());
    }

    @DeleteMapping("/{clientId}/redirect-uris")
    @GatewayOperation(name = "idp-oauth-client-redirect-delete-v1",
            summary = "删除OAuth回调地址", externalAccessible = true,
            tags = {"idp", "oauth-client"})
    public OAuthClientAdminService.ClientView deleteRedirect(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody ValueRequest request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:update");
        return clients.deleteRedirectUri(clientId, request.value());
    }

    @PutMapping("/{clientId}/audiences")
    @GatewayOperation(name = "idp-oauth-client-audience-put-v1",
            summary = "登记OAuth受众", externalAccessible = true,
            tags = {"idp", "oauth-client"})
    public OAuthClientAdminService.ClientView putAudience(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody ValueRequest request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:update");
        return clients.putAudience(clientId, request.value());
    }

    @DeleteMapping("/{clientId}/audiences")
    @GatewayOperation(name = "idp-oauth-client-audience-delete-v1",
            summary = "删除OAuth受众", externalAccessible = true,
            tags = {"idp", "oauth-client"})
    public OAuthClientAdminService.ClientView deleteAudience(
            @PathVariable("clientId") String clientId,
            @Valid @RequestBody ValueRequest request,
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:oauth-client:update");
        return clients.deleteAudience(clientId, request.value());
    }

    public record CreateClientRequest(
            @NotBlank String clientId,
            @NotBlank String clientName,
            @Positive int accessTokenTtlSeconds,
            @Positive int refreshTokenTtlSeconds,
            @NotEmpty List<@NotBlank String> redirectUris,
            @NotEmpty List<@NotBlank String> audiences
    ) {
    }

    public record UpdateClientRequest(
            @NotBlank String clientName,
            @NotNull IdentityClientEntity.Status status,
            @Positive int accessTokenTtlSeconds,
            @Positive int refreshTokenTtlSeconds,
            @PositiveOrZero long expectedVersion
    ) {
    }

    public record ValueRequest(@NotBlank String value) {
    }
}
