package top.egon.cola.platform.rbac3.admin.interfaces.http;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationBootstrapService;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;

import java.util.Objects;

/** Unified SSO bootstrap endpoint for the RBAC3 administration web application. */
@RestController
@RequestMapping("/api/v1/auth")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity",
        entityDomainName = "统一身份域",
        code = "rbac3-auth-bootstrap",
        name = "RBAC3统一身份启动接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/v1")
public class Rbac3AuthBootstrapController {

    private final AuthorizationBootstrapService bootstrap;

    public Rbac3AuthBootstrapController(AuthorizationBootstrapService bootstrap) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
    }

    @GetMapping("/bootstrap")
    @RequiresPermission("system:bootstrap:read")
    @GatewayOperation(name = "rbac3-unified-auth-bootstrap-v1",
            summary = "查询RBAC3管理端统一身份启动上下文",
            externalAccessible = false, tags = {"rbac3", "identity"})
    public AuthorizationBootstrapService.BootstrapView bootstrap() {
        return bootstrap.current();
    }
}
