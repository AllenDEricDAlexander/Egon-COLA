package top.egon.cola.platform.idp.admin.interfaces.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.util.Objects;
import java.util.Set;

@RestController
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity-profile",
        entityDomainName = "统一身份本人信息域",
        code = "identity-profile",
        name = "统一身份本人信息接口组")
@EgonHttpService(
        serviceName = "idp-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/")
public class IdentityProfileController {

    private final IdpAdminAuthorizationPort authorization;

    public IdentityProfileController(
            IdpAdminAuthorizationPort authorization
    ) {
        this.authorization = Objects.requireNonNull(
                authorization,
                "authorization"
        );
    }

    @GetMapping("/api/v1/identity/me")
    @GatewayOperation(
            name = "idp-identity-me-v1",
            summary = "查询当前统一身份",
            externalAccessible = true,
            tags = {"idp", "identity"})
    public IdentityPrincipal me(
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        authorization.require(principal, "idp:identity:self:read");
        return Objects.requireNonNull(principal, "principal");
    }

    @GetMapping("/oauth2/userinfo")
    @GatewayOperation(
            name = "idp-oauth-userinfo-v1",
            summary = "查询 OAuth 当前身份声明",
            externalAccessible = true,
            tags = {"idp", "oauth"})
    public UserInfo userInfo(
            @AuthenticationPrincipal IdentityPrincipal principal
    ) {
        IdentityPrincipal identity = Objects.requireNonNull(
                principal,
                "principal"
        );
        return new UserInfo(
                identity.subject(),
                identity.tenantId(),
                identity.sessionId(),
                identity.clientId(),
                identity.tokenVersion(),
                identity.audience()
        );
    }

    public record UserInfo(
            String sub,
            String tid,
            String sid,
            @JsonProperty("client_id") String clientId,
            @JsonProperty("token_version") long tokenVersion,
            Set<String> aud
    ) {
    }
}
