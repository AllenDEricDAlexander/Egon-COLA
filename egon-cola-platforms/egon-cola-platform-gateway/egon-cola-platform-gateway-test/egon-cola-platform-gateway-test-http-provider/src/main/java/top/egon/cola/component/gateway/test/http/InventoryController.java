package top.egon.cola.component.gateway.test.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

@RestController
@RequestMapping("/api/internal/inventory")
@GatewayInterfaceGroup(
        businessDomainCode = "supply",
        businessDomainName = "供应链域",
        entityDomainCode = "inventory",
        entityDomainName = "库存实体域",
        code = "inventory-internal",
        name = "库存内部接口组"
)
public class InventoryController {

    private final String providerId;

    public InventoryController(
            @Value("${gateway.test.provider-id:http-provider-default}")
            String providerId) {
        this.providerId = providerId;
    }

    @GetMapping("/{sku}")
    @GatewayOperation(
            name = "查询库存",
            summary = "仅允许内网调用",
            owner = "gateway-test",
            externalAccessible = false,
            tags = {"internal", "query"}
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
