package top.egon.cola.component.gateway.test.identity;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;

@RestController
@RequestMapping("/api/mock")
@GatewayInterfaceGroup(
        businessDomainCode = "identity",
        businessDomainName = "统一身份",
        entityDomainCode = "authorization-fixture",
        entityDomainName = "授权验证后端",
        code = "unified-identity-fixture",
        name = "统一身份端到端验证接口")
public class MockBackendController {

    @GetMapping("/read")
    @RequiresPermission("mock:read")
    @GatewayOperation(
            name = "统一身份读取验证",
            externalAccessible = true,
            tags = {"identity", "authorization", "read"})
    public IdentityView read() {
        return view("read");
    }

    @GetMapping("/admin")
    @RequiresPermission("mock:admin")
    @GatewayOperation(
            name = "统一身份管理验证",
            externalAccessible = true,
            tags = {"identity", "authorization", "admin"})
    public IdentityView admin() {
        return view("admin");
    }

    private IdentityView view(String operation) {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof IdentityPrincipal identity)) {
            throw new IllegalStateException("validated identity is required");
        }
        return new IdentityView(
                identity.subject(),
                identity.tenantId(),
                identity.sessionId(),
                operation);
    }

    public record IdentityView(
            String subject,
            String tenantId,
            String sessionId,
            String operation) {
    }
}
