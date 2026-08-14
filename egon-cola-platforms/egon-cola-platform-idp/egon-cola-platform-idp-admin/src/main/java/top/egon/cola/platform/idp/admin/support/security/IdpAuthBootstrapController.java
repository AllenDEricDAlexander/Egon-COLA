package top.egon.cola.platform.idp.admin.support.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.contract.auth.BootstrapView;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationBootstrapService;

import java.util.Objects;

/** Unified SSO bootstrap endpoint for the IdP administration web application. */
@RestController
@RequestMapping("/api/v1/auth")
public class IdpAuthBootstrapController {

    private final AuthorizationBootstrapService bootstrap;
    private final IdpAdminAuthorizationPort authorization;

    public IdpAuthBootstrapController(
            AuthorizationBootstrapService bootstrap,
            IdpAdminAuthorizationPort authorization) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
        this.authorization = Objects.requireNonNull(authorization, "authorization");
    }

    @GetMapping("/bootstrap")
    public BootstrapView bootstrap(
            @AuthenticationPrincipal IdentityPrincipal principal) {
        authorization.require(principal, "idp:bootstrap:read");
        return bootstrap.current();
    }
}
