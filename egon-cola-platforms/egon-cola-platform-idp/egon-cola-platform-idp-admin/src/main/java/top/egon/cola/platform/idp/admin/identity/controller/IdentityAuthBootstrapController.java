package top.egon.cola.platform.idp.admin.identity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.idp.admin.support.security.IdpAdminAuthorizationPort;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.auth.AuthorizationBootstrapView;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3AboutView;
import top.egon.cola.platform.rbac3.starter.authorization.Rbac3AboutService;

import java.util.Objects;

/** Provides the IdP administration web with its own authorization bootstrap. */
@RestController
@RequestMapping("/api/v1/identity/auth")
@Tag(name = "identity-auth-bootstrap", description = "统一身份管理台授权上下文接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity-auth",
        entityDomainName = "统一身份认证上下文域",
        interfaceGroupCode = "identity-auth-bootstrap"
)

public final class IdentityAuthBootstrapController {

    private final Rbac3AboutService bootstrap;
    private final IdpAdminAuthorizationPort authorization;

    public IdentityAuthBootstrapController(
            Rbac3AboutService bootstrap,
            IdpAdminAuthorizationPort authorization) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    @Operation(
            operationId = "idp-identity-auth-bootstrap-v1",
            summary = "查询统一身份管理台当前授权上下文",
            tags = {"idp", "identity"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    @GetMapping("/bootstrap")
    public AuthorizationBootstrapView bootstrap(
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal principal) {
        authorization.require(principal, "idp:bootstrap:read");
        Rbac3AboutView about = bootstrap.current();
        return AuthorizationBootstrapView.from(about);
    }
}
