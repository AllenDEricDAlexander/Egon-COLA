package top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.decision.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.jianshen.contract.auth.Rbac3AboutView;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.Rbac3AboutService;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RequiresPermission;

/** Returns only the current user's RBAC authorization facts for local frontend filtering. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "tianquan-jianshen-auth-about", description = "RBAC3当前授权上下文接口组")
@EgonApiCatalog(
        businessDomainCode = "xingyuan",
        businessDomainName = "平台治理域",
        entityDomainCode = "tianquan-jianshen",
        entityDomainName = "RBAC3权限实体域",
        interfaceGroupCode = "iam"
)
public class Rbac3AboutController {

    private final Rbac3AboutService aboutService;

    public Rbac3AboutController(Rbac3AboutService aboutService) {
        this.aboutService = aboutService;
    }

    @GetMapping("/about")
    @RequiresPermission(value = "system:about:read")
    @Operation(
            operationId = "tianquan-jianshen-auth-about-v1",
            summary = "查询当前授权上下文",
            tags = {"tianquan-jianshen", "identity"}
    )
    @EgonGatewayPolicy(
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL
    )
    public ResultRecord<Rbac3AboutView> about() {
        return ResultRecord.success(aboutService.current());
    }
}
