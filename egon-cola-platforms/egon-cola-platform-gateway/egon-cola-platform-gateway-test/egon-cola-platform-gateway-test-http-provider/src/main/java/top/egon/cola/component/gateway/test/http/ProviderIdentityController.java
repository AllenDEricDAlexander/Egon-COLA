package top.egon.cola.component.gateway.test.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

@RestController
@RequestMapping("/api/providers")
@GatewayInterfaceGroup(
        businessDomainCode = "gateway-test",
        businessDomainName = "Gateway 测试域",
        entityDomainCode = "provider",
        entityDomainName = "Provider 实例",
        code = "provider-identity",
        name = "Provider 身份接口组",
        description = "MVC 与 WebFlux Provider 共享的负载验证接口"
)
public class ProviderIdentityController {

    private final String providerId;

    public ProviderIdentityController(
            @Value("${gateway.test.provider-id:http-provider-default}")
            String providerId) {
        this.providerId = providerId;
    }

    @GetMapping("/{requestId}")
    @GatewayOperation(
            name = "查询 Provider 身份",
            summary = "返回实际处理请求的 Provider 实例与运行时类型",
            owner = "gateway-test",
            externalAccessible = true,
            idempotent = true,
            tags = {"query", "provider-identity"}
    )
    public ProviderIdentity identity(
            @PathVariable("requestId") String requestId) {
        return new ProviderIdentity(requestId, providerId, "mvc");
    }

    public record ProviderIdentity(
            String requestId,
            String providerId,
            String framework
    ) {
    }
}
