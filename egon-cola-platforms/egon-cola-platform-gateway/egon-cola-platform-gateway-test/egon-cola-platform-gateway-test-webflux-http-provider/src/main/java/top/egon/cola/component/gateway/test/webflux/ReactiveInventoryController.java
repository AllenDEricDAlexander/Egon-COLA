package top.egon.cola.component.gateway.test.webflux;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

@RestController
@RequestMapping("/test/items")
@GatewayInterfaceGroup(
        businessDomainCode = "supply",
        businessDomainName = "供应链域",
        entityDomainCode = "inventory",
        entityDomainName = "库存实体域",
        code = "inventory-reactive",
        name = "响应式库存接口组",
        description = "真实 WebFlux HTTP Provider 的非流式接口"
)
public class ReactiveInventoryController {

    private final String providerId;

    public ReactiveInventoryController(
            @Value("${gateway.test.provider-id:webflux-http-provider-default}")
            String providerId) {
        this.providerId = providerId;
    }

    @GetMapping("/{id}")
    @GatewayOperation(
            name = "查询响应式库存",
            summary = "通过 Mono 返回单个库存对象",
            owner = "gateway-test",
            externalAccessible = true,
            tags = {"query", "idempotent", "webflux"}
    )
    public Mono<InventoryResponse> item(@PathVariable("id") String id) {
        return Mono.just(new InventoryResponse(id, providerId, "webflux"));
    }

    public record InventoryResponse(
            String id,
            String providerId,
            String framework) {
    }
}
