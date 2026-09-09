package top.egon.cola.component.yuheng.test.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.jianshen.starter.security.RequiresPermission;

@RestController
@RequestMapping("/api/mock")
@Tag(name = "identity", description = "统一身份端到端验证")
@EgonApiCatalog(
        businessDomainCode = "identity",
        businessDomainName = "统一身份",
        entityDomainCode = "authorization-fixture",
        entityDomainName = "授权验证后端",
        interfaceGroupCode = "identity")
public class MockBackendController {

    @GetMapping("/read")
    @RequiresPermission("mock:read")
    @Operation(
            operationId = "identity.read",
            summary = "统一身份读取验证",
            tags = {"identity", "authorization", "read"})
    @EgonGatewayPolicy(
            owner = "yuheng-test",
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.AUTO)
    public IdentityView read(
            @AuthenticationPrincipal(expression = "identity()") IdentityPrincipal identity) {
        return view("read", identity);
    }

    @GetMapping("/admin")
    @RequiresPermission("mock:admin")
    @Operation(
            operationId = "identity.admin",
            summary = "统一身份管理验证",
            tags = {"identity", "authorization", "admin"})
    @EgonGatewayPolicy(
            owner = "yuheng-test",
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.AUTO)
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
            @Schema(description = "主体标识")
            String subject,
            @Schema(description = "租户标识")
            String tenantId,
            @Schema(description = "验证操作")
            String operation) {
    }
}
