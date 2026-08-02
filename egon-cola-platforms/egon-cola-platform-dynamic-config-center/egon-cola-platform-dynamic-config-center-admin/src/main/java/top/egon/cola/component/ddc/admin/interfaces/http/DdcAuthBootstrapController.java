package top.egon.cola.component.ddc.admin.interfaces.http;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationBootstrapService;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;

import java.util.Objects;

/** Unified SSO bootstrap endpoint for the DDC administration web application. */
@RestController
@RequestMapping("/api/v1/auth")
public class DdcAuthBootstrapController {

    private final AuthorizationBootstrapService bootstrap;

    public DdcAuthBootstrapController(AuthorizationBootstrapService bootstrap) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
    }

    @GetMapping("/bootstrap")
    @RequiresPermission("DDC_READ")
    public AuthorizationBootstrapService.BootstrapView bootstrap() {
        return bootstrap.current();
    }
}
