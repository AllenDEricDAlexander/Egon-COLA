package top.egon.cola.component.ddc.admin.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3AboutView;
import top.egon.cola.platform.rbac3.starter.authorization.Rbac3AboutService;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Unified SSO bootstrap endpoint for the DDC administration web application. */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(
        prefix = "egon.cola.platform.rbac3",
        name = "enabled",
        havingValue = "true"
)
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "ddc-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        code = "ddc-admin-ddc-auth-bootstrap-controller",
        name = "DdcAuthBootstrapController 管理接口组")
public class DdcAuthBootstrapController {

    private final Rbac3AboutService bootstrap;

    public DdcAuthBootstrapController(Rbac3AboutService bootstrap) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
    }

    @GatewayOperation(externalAccessible = true)
    @GetMapping("/bootstrap")
    @RequiresPermission("DDC_READ")
    public Map<String, Object> bootstrap() {
        Rbac3AboutView about = bootstrap.current();
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", about.user().subject());
        user.put("tenantId", about.user().tenantId());
        user.put("identitySub", about.user().subject());
        user.put("status", about.user().status());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", user);
        result.put("activeRoleContexts", about.activeRoles());
        result.put("permissions", about.permissions());
        result.put("apps", List.of());
        result.put("menus", List.of());
        result.put("routes", List.of());
        result.put("actions", List.of());
        result.put("fieldPolicies", about.fieldPolicies());
        result.put("defaultApplicationCode", about.currentApplicationCode());
        result.put("defaultRoute", about.landingRouteCode());
        result.put("authVersion", about.authVersion());
        result.put("policyVersion", about.policyVersion());
        return result;
    }
}
