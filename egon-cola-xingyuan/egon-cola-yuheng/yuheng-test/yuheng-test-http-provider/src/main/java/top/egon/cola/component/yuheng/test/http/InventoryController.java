package top.egon.cola.component.yuheng.test.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

@RestController
@RequestMapping("/api/internal/inventory")
@Tag(name = "inventory")
@EgonApiCatalog(
        businessDomainCode = "supply",
        businessDomainName = "供应链域",
        entityDomainCode = "inventory",
        entityDomainName = "库存实体域",
        interfaceGroupCode = "inventory"
)
public class InventoryController {

    private final String providerId;

    public InventoryController(
            @Value("${yuheng.test.provider-id:http-provider-default}")
            String providerId) {
        this.providerId = providerId;
    }

    @GetMapping("/{sku}")
    @Operation(
            operationId = "inventory.get",
            summary = "仅允许内网调用",
            tags = {"internal", "query"}
    )
    @EgonGatewayPolicy(
            owner = "yuheng-test",
            exposure = EgonGatewayPolicy.Exposure.INTERNAL,
            idempotency = EgonGatewayPolicy.Idempotency.AUTO
    )
    public InventoryView inventory(@PathVariable("sku") String sku) {
        return new InventoryView(sku, 100, providerId);
    }

    public record InventoryView(
            String sku,
            int available,
            String providerId) {
    }
}
