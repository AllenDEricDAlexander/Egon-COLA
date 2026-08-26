package top.egon.cola.component.gateway.test.webflux;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;

@RestController
@RequestMapping("/api/providers")
@Tag(name = "inventory-reactive")
@EgonApiCatalog(
        businessDomainCode = "gateway-test",
        businessDomainName = "Gateway 测试域",
        entityDomainCode = "provider",
        entityDomainName = "Provider 实例",
        interfaceGroupCode = "inventory-reactive"
)
public class ProviderIdentityController {

    private final String providerId;

    public ProviderIdentityController(
            @Value("${gateway.test.provider-id:"
                    + "webflux-http-provider-default}")
            String providerId) {
        this.providerId = providerId;
    }

    @GetMapping("/{requestId}")
    @Operation(
            operationId = "inventory-reactive.providerIdentity",
            summary = "返回实际处理请求的 Provider 实例与运行时类型",
            tags = {"query", "provider-identity"}
    )
    @EgonGatewayPolicy(
            owner = "gateway-test",
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.TRUE
    )
    public Mono<ProviderIdentity> identity(
            @PathVariable("requestId") String requestId) {
        return Mono.just(new ProviderIdentity(
                requestId,
                providerId,
                "webflux"
        ));
    }

    public record ProviderIdentity(
            String requestId,
            String providerId,
            String framework
    ) {
    }
}
