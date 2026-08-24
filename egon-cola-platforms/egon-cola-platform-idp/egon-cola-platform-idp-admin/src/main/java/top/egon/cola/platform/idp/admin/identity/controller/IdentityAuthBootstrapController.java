package top.egon.cola.platform.idp.admin.identity.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.auth.AuthorizationBootstrapView;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3AboutView;
import top.egon.cola.platform.rbac3.starter.authorization.Rbac3AboutService;

import java.util.Objects;

/** Provides the IdP administration web with its own authorization bootstrap. */
@RestController
@RequestMapping("/api/v1/identity/auth")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity-auth",
        entityDomainName = "统一身份认证上下文域",
        code = "identity-auth-bootstrap",
        name = "统一身份管理台授权上下文接口组")
@EgonHttpService(
        serviceName = "idp-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/v1/identity")
public final class IdentityAuthBootstrapController {

    private final Rbac3AboutService bootstrap;
    private final IdpAdminAuthorizationPort authorization;

    public IdentityAuthBootstrapController(
            Rbac3AboutService bootstrap,
            IdpAdminAuthorizationPort authorization) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    @GatewayOperation(
            name = "idp-identity-auth-bootstrap-v1",
            summary = "查询统一身份管理台当前授权上下文",
            externalAccessible = true,
            tags = {"idp", "identity"})
    @GetMapping("/bootstrap")
    public AuthorizationBootstrapView bootstrap(
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal) {
        authorization.require(principal, "idp:bootstrap:read");
        Rbac3AboutView about = bootstrap.current();
        return AuthorizationBootstrapView.from(about);
    }
}
