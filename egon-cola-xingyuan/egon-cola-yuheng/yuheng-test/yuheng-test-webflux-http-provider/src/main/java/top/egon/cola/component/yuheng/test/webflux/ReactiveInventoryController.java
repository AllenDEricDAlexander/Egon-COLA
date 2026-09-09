package top.egon.cola.component.yuheng.test.webflux;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

@RestController
@RequestMapping("/test/items")
@Tag(name = "inventory-reactive")
@EgonApiCatalog(
        businessDomainCode = "supply",
        businessDomainName = "供应链域",
        entityDomainCode = "inventory",
        entityDomainName = "库存实体域",
        interfaceGroupCode = "inventory-reactive"
)
public class ReactiveInventoryController {

    private final String providerId;

    public ReactiveInventoryController(
            @Value("${gateway.test.provider-id:webflux-http-provider-default}")
            String providerId) {
        this.providerId = providerId;
    }

    @GetMapping("/{id}")
    @Operation(
            operationId = "inventory-reactive.item",
            summary = "通过 Mono 返回单个库存对象",
            tags = {"query", "webflux"}
    )
    @EgonGatewayPolicy(
            owner = "gateway-test",
            exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.TRUE
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
