package top.egon.cola.platform.rbac3.admin.authorization.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3AboutView;
import top.egon.cola.platform.rbac3.starter.authorization.Rbac3AboutService;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;

/** Returns only the current user's RBAC authorization facts for local frontend filtering. */
@RestController
@RequestMapping("/api/v1/auth")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/v1")
public final class Rbac3AboutController {

    private final Rbac3AboutService aboutService;

    public Rbac3AboutController(Rbac3AboutService aboutService) {
        this.aboutService = aboutService;
    }

    @GetMapping("/about")
    @RequiresPermission("system:about:read")
    @GatewayOperation(
            name = "rbac3-auth-about-v1",
            summary = "查询当前授权上下文",
            externalAccessible = true,
            tags = {"rbac3", "identity"})
    public ResultRecord<Rbac3AboutView> about() {
        return ResultRecord.success(aboutService.current());
    }
}
