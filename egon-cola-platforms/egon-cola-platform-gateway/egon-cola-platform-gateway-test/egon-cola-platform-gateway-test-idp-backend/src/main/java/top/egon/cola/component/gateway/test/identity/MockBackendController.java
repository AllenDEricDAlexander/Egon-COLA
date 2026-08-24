package top.egon.cola.component.gateway.test.identity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
    public IdentityView read(
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal identity) {
        return view("read", identity);
    }

    @GetMapping("/admin")
    @RequiresPermission("mock:admin")
    @GatewayOperation(
            name = "统一身份管理验证",
            externalAccessible = true,
            tags = {"identity", "authorization", "admin"})
    public IdentityView admin(
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal identity) {
        return view("admin", identity);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Void> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    private IdentityView view(String operation, IdentityPrincipal identity) {
        if (identity == null) {
            throw new IllegalStateException("validated identity is required");
        }
        return new IdentityView(
                identity.subject(),
                identity.tenantId(),
                operation);
    }

    public record IdentityView(
            String subject,
            String tenantId,
            String operation) {
    }
}
