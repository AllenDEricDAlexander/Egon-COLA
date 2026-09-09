package top.egon.cola.component.tianshu.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.jianshen.contract.auth.AuthorizationBootstrapView;
import top.egon.cola.platform.tianquan.jianshen.contract.auth.Rbac3AboutView;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.Rbac3AboutService;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RequiresPermission;

import java.util.Objects;

/** Unified SSO bootstrap endpoint for the DDC administration web application. */
@RestController
@RequestMapping({"/api/v1/auth", "/api/v1/ddc/auth"})
@ConditionalOnProperty(
        prefix = "egon.cola.platform.rbac3",
        name = "enabled",
        havingValue = "true"
)
@Tag(name = "ddc-admin-ddc-auth-bootstrap-controller", description = "DdcAuthBootstrapController 管理接口组")
@EgonApiCatalog(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "ddc-admin",
        entityDomainName = "Dynamic Config Center 管理实体域",
        interfaceGroupCode = "ddc"
)
public class DdcAuthBootstrapController {

    private final Rbac3AboutService bootstrap;

    public DdcAuthBootstrapController(Rbac3AboutService bootstrap) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
    }

    @Operation(operationId = "ddc.ddcAuthBootstrapController.bootstrap")
    @EgonGatewayPolicy(exposure = EgonGatewayPolicy.Exposure.EXTERNAL)
    @GetMapping("/bootstrap")
    @RequiresPermission("DDC_READ")
    public AuthorizationBootstrapView bootstrap() {
        Rbac3AboutView about = bootstrap.current();
        return AuthorizationBootstrapView.from(about);
    }
}
