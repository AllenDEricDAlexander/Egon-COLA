package top.egon.cola.component.gateway.admin.interfaces.management;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationBootstrapService;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;

import java.util.Objects;

/** Unified SSO bootstrap endpoint for the Gateway administration web application. */
@RestController
@RequestMapping("/api/v1/auth")
public class GatewayAuthBootstrapController {

    private final AuthorizationBootstrapService bootstrap;

    public GatewayAuthBootstrapController(AuthorizationBootstrapService bootstrap) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
    }

    @GetMapping("/bootstrap")
    @RequiresPermission("gateway:read")
    public AuthorizationBootstrapService.BootstrapView bootstrap() {
        return bootstrap.current();
    }
}
